# 토큰 검증 아키텍처: mTLS 기반 시스템 간 신뢰 및 Redis 토큰 공유

이 문서는 Biz Service가 Agent의 Access Token을 검증하는 두 가지 방식을 설명합니다.

## 개요

Agent가 Biz Service에 Access Token을 가지고 접근할 때, Biz Service는 토큰의 유효성을 검증해야 합니다.

```
┌─────────┐   JWT Token   ┌─────────────┐      검증?      ┌─────────────┐
│  Agent  │──────────────>│ Biz Service │ ─ ─ ─ ─ ─ ─ ─ >│ Auth Server │
└─────────┘               └─────────────┘                └─────────────┘
```

### 토큰 검증 방식 비교

| 방식 | 설명 | 장점 | 단점 |
|------|------|------|------|
| 공유 비밀키 (HS256) | 동일 SECRET_KEY 공유 | 구현 간단 | 키 유출 위험 |
| 비대칭키 (RS256) | Public Key만 배포 | 보안성 높음 | 키 관리 필요 |
| JWKS 엔드포인트 | Auth Server가 Public Key 제공 | 키 자동 갱신 | 네트워크 의존 |
| **mTLS + Introspection** | mTLS로 Auth Server에 검증 요청 | 시스템 간 신뢰 | 매 요청마다 호출 |
| **Redis 토큰 공유** | Redis에서 토큰 상태 공유 | 고성능, 실시간 폐기 | Redis 인프라 필요 |

이 문서에서는 **mTLS + Introspection** 방식과 **Redis 토큰 공유** 방식을 다룹니다.

---

## 방식 1: mTLS 기반 시스템 간 신뢰 (Token Introspection)

### 아키텍처

```
┌─────────┐   JWT Token   ┌─────────────┐   mTLS    ┌─────────────┐
│  Agent  │──────────────>│ Biz Service │<────────>│ Auth Server │
└─────────┘               └─────────────┘          └─────────────┘
                                │                         │
                                └─── 동일 CA 인증서 체계 ───┘
```

### 인증서 구조

Auth Server와 Biz Service가 **동일 CA**에서 발급한 인증서를 사용하여 상호 신뢰합니다.

```
                    ┌──────────────┐
                    │   Root CA    │
                    └──────┬───────┘
                           │
        ┌──────────────────┼──────────────────┐
        ▼                  ▼                  ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│ Agent 인증서   │  │ Biz Service   │  │ Auth Server   │
│ OU=agent      │  │ OU=service    │  │ OU=auth       │
│ CN=host_user_J│  │ CN=biz-svc-01 │  │ CN=auth-svc   │
└───────────────┘  └───────────────┘  └───────────────┘
```

### 토큰 검증 흐름

```
Agent                      Biz Service                    Auth Server
  │                             │                              │
  │ 1. API 호출 + JWT           │                              │
  │────────────────────────────>│                              │
  │                             │                              │
  │                             │ 2. mTLS 연결                  │
  │                             │  (서버 인증서로 상호 검증)     │
  │                             │<────────────────────────────>│
  │                             │                              │
  │                             │ 3. POST /oauth2/introspect   │
  │                             │    { "token": "eyJ..." }     │
  │                             │─────────────────────────────>│
  │                             │                              │
  │                             │ 4. 검증 결과 응답             │
  │                             │    { "active": true,         │
  │                             │      "sub": "agent_id",      │
  │                             │      "scope": "..." }        │
  │                             │<─────────────────────────────│
  │                             │                              │
  │ 5. API 응답                 │                              │
  │<────────────────────────────│                              │
```

### Auth Server: Token Introspection 엔드포인트 (RFC 7662)

```python
@app.route('/oauth2/introspect', methods=['POST'])
def introspect_token():
    """
    Token Introspection 엔드포인트
    - mTLS로 Biz Service 인증서 검증 (시스템 간 신뢰)
    - 토큰 유효성 검사 후 결과 반환
    """
    # 1. mTLS로 Biz Service 인증서 검증
    client_cert_dn = request.environ.get('SSL_CLIENT_S_DN')
    if not is_trusted_service(client_cert_dn):  # OU=service 등으로 구분
        return jsonify({"error": "unauthorized_client"}), 401

    # 2. 토큰 추출 및 검증
    token = request.form.get('token')
    payload = verify_access_token(token)

    # 3. 검증 결과 반환
    if payload:
        return jsonify({
            "active": True,
            "sub": payload.get("sub"),
            "scope": payload.get("scope"),
            "hostname": payload.get("hostname"),
            "username": payload.get("username"),
            "client_ip": payload.get("client_ip"),
            "exp": payload.get("exp"),
            "iat": payload.get("iat")
        })
    else:
        return jsonify({"active": False})


def is_trusted_service(cert_dn):
    """
    Biz Service 인증서인지 확인
    OU=service인 인증서만 introspection 허용
    """
    if not cert_dn:
        return False
    ou_match = re.search(r'OU=([^,]+)', cert_dn)
    return ou_match and ou_match.group(1) == "service"
```

### Biz Service: mTLS 클라이언트로 토큰 검증

```python
import requests

class TokenValidator:
    def __init__(self, auth_server_url, client_cert, client_key, ca_cert):
        self.auth_server_url = auth_server_url
        self.client_cert = (client_cert, client_key)
        self.ca_cert = ca_cert

    def validate(self, token):
        """mTLS로 Auth Server에 introspection 요청"""
        response = requests.post(
            f"{self.auth_server_url}/oauth2/introspect",
            data={"token": token},
            cert=self.client_cert,  # Biz Service 인증서
            verify=self.ca_cert     # CA 인증서로 Auth Server 검증
        )

        if response.status_code == 200:
            result = response.json()
            if result.get("active"):
                return result

        return None


# 사용 예시
validator = TokenValidator(
    auth_server_url="https://auth-server:8443",
    client_cert="./certs/biz-service.crt",
    client_key="./certs/biz-service.key",
    ca_cert="./certs/ca.crt"
)

@app.route('/api/resource')
def protected_resource():
    token = extract_bearer_token(request.headers.get('Authorization'))

    token_info = validator.validate(token)
    if not token_info:
        return jsonify({"error": "invalid_token"}), 401

    # 토큰 정보 활용
    agent_id = token_info.get("sub")
    scope = token_info.get("scope")

    return jsonify({"data": "protected resource"})
```

### 캐싱을 통한 성능 최적화

```python
from cachetools import TTLCache

class CachedTokenValidator:
    def __init__(self, auth_server_url, client_cert, client_key, ca_cert):
        self.auth_server_url = auth_server_url
        self.client_cert = (client_cert, client_key)
        self.ca_cert = ca_cert
        self.cache = TTLCache(maxsize=1000, ttl=60)  # 60초 캐시

    def validate(self, token):
        # 1. 캐시 확인
        if token in self.cache:
            return self.cache[token]

        # 2. mTLS로 Auth Server에 introspection 요청
        response = requests.post(
            f"{self.auth_server_url}/oauth2/introspect",
            data={"token": token},
            cert=self.client_cert,
            verify=self.ca_cert
        )

        if response.status_code == 200:
            result = response.json()
            if result.get("active"):
                # 3. 결과 캐싱
                self.cache[token] = result
                return result

        return None
```

### mTLS Introspection 방식의 장단점

| 장점 | 단점 |
|------|------|
| 키 배포 불필요 (Secret/Public Key 없음) | 매 요청마다 Auth Server 호출 |
| 실시간 검증 (토큰 폐기 즉시 반영) | mTLS 핸드셰이크로 인한 latency |
| 시스템 간 상호 인증 (Biz Service도 검증됨) | Auth Server 단일 장애점 |
| 중앙 집중 토큰 관리 | 네트워크 의존성 |

---

## 방식 2: Redis를 활용한 토큰 공유

### 아키텍처

```
┌─────────┐   JWT Token   ┌─────────────┐         ┌─────────────┐
│  Agent  │──────────────>│ Biz Service │────────>│    Redis    │
└─────────┘               └─────────────┘         └──────┬──────┘
                                                         │
                          ┌─────────────┐                │
                          │ Auth Server │────────────────┘
                          └─────────────┘
                           (토큰 발급 시 Redis에 저장)
```

### Redis 키 구조

```
┌─────────────────────────────────────────────────────────┐
│                        Redis                            │
├─────────────────────────────────────────────────────────┤
│ Key: token:{token_prefix}                               │
│ TTL: 1800 (토큰 만료시간과 동일)                          │
│ Value: {                                                │
│   "active": true,                                       │
│   "sub": "testserver01_appuser_J",                     │
│   "scope": "agent:commands agent:results",             │
│   "hostname": "testserver01",                          │
│   "username": "appuser",                               │
│   "client_ip": "127.0.0.1",                            │
│   "exp": 1735123456,                                   │
│   "iat": 1735121656                                    │
│ }                                                       │
└─────────────────────────────────────────────────────────┘
```

### Auth Server: 토큰 발급 시 Redis에 저장

```python
import redis
import json
import hashlib
from datetime import datetime, timedelta

r = redis.Redis(host='redis-server', port=6379, db=0, decode_responses=True)

def generate_token_key(token):
    """토큰에서 Redis 키 생성 (토큰 해시 사용)"""
    token_hash = hashlib.sha256(token.encode()).hexdigest()[:32]
    return f"token:{token_hash}"


def generate_access_token(agent_id, client_ip, scope, method):
    """Access Token 생성 및 Redis에 저장"""
    agent = AGENTS_DB.get(agent_id)
    exp_time = datetime.utcnow() + timedelta(minutes=30)

    payload = {
        "sub": agent_id,
        "iss": ISSUER,
        "aud": AUDIENCE,
        "exp": exp_time.timestamp(),
        "iat": datetime.utcnow().timestamp(),
        "scope": scope,
        "usertype": agent.get("usertype", "agent"),
        "hostname": agent.get("hostname", ""),
        "username": agent.get("username", ""),
        "client_ip": client_ip,
        "client_auth_method": method,
        "token_type": "access_token"
    }

    # JWT 토큰 생성
    token = jwt.encode(payload, SECRET_KEY, algorithm="HS256")

    # Redis에 토큰 정보 저장
    token_key = generate_token_key(token)
    token_data = {
        "active": True,
        "sub": agent_id,
        "scope": scope,
        "hostname": agent.get("hostname", ""),
        "username": agent.get("username", ""),
        "usertype": agent.get("usertype", "agent"),
        "client_ip": client_ip,
        "exp": int(exp_time.timestamp()),
        "iat": int(datetime.utcnow().timestamp())
    }

    # TTL = 토큰 만료시간 (30분)
    r.setex(token_key, timedelta(minutes=30), json.dumps(token_data))

    return token
```

### Auth Server: 토큰 폐기 (Revoke)

```python
@app.route('/oauth2/revoke', methods=['POST'])
def revoke_token():
    """토큰 폐기 - Redis에서 즉시 삭제 또는 비활성화"""
    token = request.form.get('token')
    token_key = generate_token_key(token)

    # 방법 1: 즉시 삭제
    r.delete(token_key)

    # 방법 2: active=False로 변경 (감사 로그 유지)
    # token_data = r.get(token_key)
    # if token_data:
    #     data = json.loads(token_data)
    #     data["active"] = False
    #     data["revoked_at"] = datetime.utcnow().isoformat()
    #     r.setex(token_key, timedelta(minutes=5), json.dumps(data))

    return jsonify({"status": "revoked"})


@app.route('/oauth2/revoke-all', methods=['POST'])
def revoke_all_tokens():
    """특정 Agent의 모든 토큰 폐기"""
    agent_id = request.form.get('agent_id')

    # Agent의 모든 토큰 키 패턴으로 검색 및 삭제
    # 주의: 이 방식은 토큰과 agent_id 매핑이 필요
    # 대안: agent별 토큰 목록을 별도 관리

    return jsonify({"status": "all tokens revoked"})
```

### Biz Service: Redis에서 토큰 검증

```python
import redis
import json
import hashlib

r = redis.Redis(host='redis-server', port=6379, db=0, decode_responses=True)

def generate_token_key(token):
    """토큰에서 Redis 키 생성"""
    token_hash = hashlib.sha256(token.encode()).hexdigest()[:32]
    return f"token:{token_hash}"


def verify_token(token):
    """Redis에서 토큰 정보 조회 및 검증"""
    token_key = generate_token_key(token)

    # Redis에서 토큰 정보 조회
    token_data = r.get(token_key)

    if not token_data:
        return None  # 토큰 없음 또는 만료

    data = json.loads(token_data)

    if not data.get("active"):
        return None  # 토큰 폐기됨

    return data


# 사용 예시
@app.route('/api/resource')
def protected_resource():
    token = extract_bearer_token(request.headers.get('Authorization'))

    token_info = verify_token(token)
    if not token_info:
        return jsonify({"error": "invalid_token"}), 401

    # scope 검증
    if "agent:commands" not in token_info.get("scope", ""):
        return jsonify({"error": "insufficient_scope"}), 403

    return jsonify({"data": "protected resource"})
```

### 하이브리드: Redis + JWT 서명 검증

더 안전한 방식으로, **JWT 서명도 검증**하면서 Redis로 활성 상태를 확인합니다.

```python
import jwt
import redis
import json
import hashlib

r = redis.Redis(host='redis-server', port=6379, db=0, decode_responses=True)

def verify_token_hybrid(token):
    """
    하이브리드 검증:
    1. JWT 서명 검증 (위변조 방지)
    2. Redis에서 활성 상태 확인 (폐기 여부)
    """
    # 1. JWT 서명 검증
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=["HS256"], audience=AUDIENCE)
    except jwt.ExpiredSignatureError:
        return None, "token_expired"
    except jwt.InvalidTokenError:
        return None, "invalid_token"

    # 2. Redis에서 활성 상태 확인
    token_key = generate_token_key(token)
    token_data = r.get(token_key)

    if not token_data:
        return None, "token_not_found"

    data = json.loads(token_data)

    if not data.get("active"):
        return None, "token_revoked"

    return payload, None
```

### Redis 고가용성 구성

#### Sentinel 구성

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Auth Server │     │ Biz Service │     │ Biz Service │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           ▼
                 ┌─────────────────┐
                 │  Redis Sentinel │
                 │    (HA 구성)     │
                 ├────────┬────────┤
                 ▼        ▼        ▼
              Master   Replica   Replica
```

#### Sentinel 연결 코드

```python
from redis.sentinel import Sentinel

sentinel = Sentinel([
    ('sentinel-1', 26379),
    ('sentinel-2', 26379),
    ('sentinel-3', 26379)
], socket_timeout=0.1)

# Master에 쓰기
master = sentinel.master_for('mymaster', socket_timeout=0.1)
master.setex(token_key, ttl, token_data)

# Replica에서 읽기 (읽기 부하 분산)
slave = sentinel.slave_for('mymaster', socket_timeout=0.1)
token_data = slave.get(token_key)
```

#### Cluster 구성

```python
from redis.cluster import RedisCluster

rc = RedisCluster(
    host='redis-cluster',
    port=6379,
    decode_responses=True
)

# 자동으로 적절한 노드에 라우팅
rc.setex(token_key, ttl, token_data)
token_data = rc.get(token_key)
```

### Redis 방식의 장단점

| 장점 | 단점 |
|------|------|
| 고성능 (~1ms 응답) | Redis 인프라 필요 |
| 토큰 폐기 즉시 반영 | Redis 장애 시 영향 |
| Auth Server 부하 분산 | 데이터 동기화 관리 |
| 수평 확장 용이 (Cluster) | 추가 운영 비용 |

---

## 방식 비교 및 선택 가이드

### 상세 비교

| 항목 | mTLS Introspection | Redis |
|------|-------------------|-------|
| **네트워크 호출** | 매 요청마다 Auth Server 호출 | Redis만 조회 |
| **지연 시간** | 높음 (mTLS 핸드셰이크 포함) | 낮음 (~1ms) |
| **토큰 폐기** | 즉시 반영 | 즉시 반영 |
| **단일 장애점** | Auth Server | Redis (HA 구성으로 해결) |
| **확장성** | Auth Server 부하 집중 | Redis Cluster로 수평 확장 |
| **인프라 요구사항** | mTLS 인증서 체계 | Redis 서버/클러스터 |
| **구현 복잡도** | 중간 (인증서 관리) | 낮음 |
| **보안** | 시스템 간 상호 인증 | 네트워크 보안 필요 |

### 선택 가이드

#### mTLS Introspection이 적합한 경우
- 이미 mTLS 인프라가 구축되어 있는 경우
- 시스템 간 상호 인증이 필수인 경우
- 요청량이 많지 않은 경우
- 중앙 집중식 토큰 관리가 필요한 경우

#### Redis가 적합한 경우
- 고성능/저지연이 필요한 경우
- 요청량이 많은 경우
- 이미 Redis 인프라가 있는 경우
- 수평 확장이 필요한 경우

#### 하이브리드 권장
- **JWT 서명 검증 (RS256)** + **Redis 상태 확인**
- JWT로 위변조 방지, Redis로 실시간 폐기 반영
- 두 방식의 장점을 모두 활용

---

## 관련 문서

- [mTLS 인증서 및 JWT 토큰 생성 흐름](./mTLS-JWT-Authentication-Flow.md)
- [RFC 7662 - OAuth 2.0 Token Introspection](https://tools.ietf.org/html/rfc7662)
