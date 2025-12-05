# mTLS 인증서 및 JWT 토큰 생성 흐름

이 문서는 mTLS 인증서에 포함된 정보와 인증서버에서 JWT 토큰 생성 시 이를 어떻게 참조하는지 설명합니다.

> **대상 독자**: 인증 서버(Auth Server) 및 CA 서버 개발팀
>
> **목적**: MwManger Agent가 사용하는 인증 체계의 정확한 spec 제공

---

## 시스템 구성 요약

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    CA Server    │     │   Auth Server   │     │   Biz Service   │
│  (인증서 발급)   │     │  (토큰 발급)     │     │  (업무 서비스)   │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         │ 인증서 발급            │ 토큰 발급/검증         │ API 제공
         ▼                       ▼                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                        MwManger Agent                           │
│  - mTLS 클라이언트 인증서로 Auth Server에 인증                    │
│  - JWT Access Token으로 Biz Service API 호출                     │
└─────────────────────────────────────────────────────────────────┘
```

### 각 서버의 역할

| 서버 | 역할 | 구현 필요 사항 |
|------|------|---------------|
| **CA Server** | Agent/Server 인증서 발급 및 관리 | PKI 인프라, 인증서 발급 API |
| **Auth Server** | mTLS 인증 후 JWT 토큰 발급 | OAuth2 엔드포인트, JWT 서명 |
| **Biz Service** | Agent에게 업무 API 제공 | JWT 토큰 검증, 비즈니스 로직 |

## 1. 인증서에 포함된 정보

### Agent 클라이언트 인증서 Subject DN 형식

```
CN={hostname}_{username}_J, OU=agent, O=Leebalso, C=KR
```

| 필드 | 값 | 용도 |
|------|-----|------|
| **CN** (Common Name) | `testserver01_appuser_J` | Agent ID (hostname + username 조합) |
| **OU** (Organizational Unit) | `agent` | 사용자 유형 식별 |
| **O** (Organization) | `Leebalso` | 조직명 |
| **C** (Country) | `KR` | 국가 코드 |

### 예시 인증서

| 인증서 파일 | CN 값 | Hostname | Username |
|------------|-------|----------|----------|
| `agent-test001.p12` | testserver01_appuser_J | testserver01 | appuser |
| `agent-test002.p12` | testserver02_svcuser_J | testserver02 | svcuser |
| `agent-test003.p12` | testserver03_testuser_J | testserver03 | testuser |

### 인증서 생성 스크립트

인증서는 `test-server/generate-certs.sh` 스크립트로 생성됩니다:

```bash
# Agent 클라이언트 인증서 생성 예시
openssl req -new -key agent.key \
    -subj "/C=KR/O=Leebalso/OU=agent/CN=testserver01_appuser_J" \
    -out agent.csr
```

---

## 2. 인증서버에서 DN 파싱

인증서버는 mTLS 연결 시 클라이언트 인증서의 Subject DN을 추출하여 파싱합니다.

**파일**: `test-server/mock_server.py:139-191`

```python
def parse_certificate_dn(cert_dn):
    """
    DN 형식: CN=hostname_username_J, OU=agent, O=Leebalso, C=KR
    """
    # DN에서 각 필드 추출
    cn_match = re.search(r'CN=([^,]+)', cert_dn)   # testserver01_appuser_J
    ou_match = re.search(r'OU=([^,]+)', cert_dn)   # agent
    o_match = re.search(r'O=([^,]+)', cert_dn)     # Leebalso
    c_match = re.search(r'C=([^,]+)', cert_dn)     # KR

    # CN을 파싱하여 hostname과 username 분리
    # 형식: {hostname}_{username}_J
    cn_match_detailed = re.match(r'^(.+)_(.+)_J$', cn)
    if cn_match_detailed:
        hostname = cn_match_detailed.group(1)  # testserver01
        username = cn_match_detailed.group(2)  # appuser

    return {
        "cn": cn,                    # 전체 CN 값
        "ou": ou,                    # usertype
        "o": organization,           # 조직
        "c": country,                # 국가
        "hostname": hostname,        # CN에서 파싱
        "username": username,        # CN에서 파싱
        "agent_id": cn,              # Agent 식별자
        "usertype": ou               # 사용자 유형
    }
```

---

## 3. Agent 검증 단계

인증서 정보를 파싱한 후, 4단계 검증을 수행합니다.

**파일**: `test-server/mock_server.py:194-235`

```python
def validate_agent_identity(cert_info, client_ip):
    """
    4단계 검증 프로세스
    """
    # 1단계: OU 검증 - usertype이 "agent"인지 확인
    if usertype != "agent":
        return False, "Invalid certificate usertype"

    # 2단계: 등록 확인 - Agent가 DB에 등록되어 있고 활성 상태인지 확인
    agent = AGENTS_DB.get(agent_id)
    if not agent or agent.get("status") != "active":
        return False, "Agent not registered or inactive"

    # 3단계: 정보 일치 확인 - 인증서의 hostname/username이 등록 정보와 일치하는지
    if cert_hostname != agent.get("hostname"):
        return False, "Certificate hostname mismatch"
    if cert_username != agent.get("username"):
        return False, "Certificate username mismatch"

    # 4단계: IP 검증 - 클라이언트 IP가 허용 목록에 있는지 (인증서 복사 방지)
    allowed_ips = agent.get("allowed_ips", [])
    if client_ip not in allowed_ips:
        return False, "Client IP not authorized"

    return True, None, agent
```

### 검증 실패 시나리오

| 검증 단계 | 실패 조건 | 에러 메시지 |
|----------|----------|------------|
| OU 검증 | OU가 "agent"가 아님 | Invalid certificate usertype |
| 등록 확인 | Agent가 미등록 또는 비활성 | Agent not registered or inactive |
| 정보 일치 | hostname/username 불일치 | Certificate hostname/username mismatch |
| IP 검증 | IP가 허용 목록에 없음 | Client IP not authorized |

---

## 4. JWT 토큰 생성

검증 성공 후, 인증서에서 추출한 정보를 기반으로 JWT 토큰을 생성합니다.

**파일**: `test-server/mock_server.py:80-106`

```python
def generate_access_token(agent_id, client_ip, scope, method):
    agent = AGENTS_DB.get(agent_id)

    payload = {
        # OAuth2 표준 클레임 (RFC 7519)
        "sub": agent_id,                    # Subject: 인증서 CN
        "iss": "leebalso-auth-server",      # Issuer: 발급자
        "aud": "https://api.mwagent.example.com",  # Audience: 대상
        "exp": datetime.utcnow() + timedelta(minutes=30),  # 만료 시간
        "iat": datetime.utcnow(),           # 발급 시간
        "scope": scope,                     # 권한 범위

        # 인증서에서 추출한 커스텀 클레임
        "usertype": agent.get("usertype", "agent"),  # OU에서 추출
        "hostname": agent.get("hostname", ""),       # CN 파싱
        "username": agent.get("username", ""),       # CN 파싱
        "client_ip": client_ip,             # 요청 시점 클라이언트 IP

        # 토큰 메타데이터
        "client_auth_method": method,       # 인증 방식
        "token_type": "access_token"        # 토큰 유형
    }

    return jwt.encode(payload, SECRET_KEY, algorithm="HS256")
```

### JWT 클레임 상세

| 클레임 | 타입 | 설명 | 예시 값 |
|--------|------|------|---------|
| `sub` | 표준 | Subject (Agent ID) | testserver01_appuser_J |
| `iss` | 표준 | Issuer (발급자) | leebalso-auth-server |
| `aud` | 표준 | Audience (대상) | https://api.mwagent.example.com |
| `exp` | 표준 | Expiration (만료 시간) | 1735123456 |
| `iat` | 표준 | Issued At (발급 시간) | 1735121656 |
| `scope` | 표준 | 권한 범위 | agent:commands agent:results |
| `usertype` | 커스텀 | 사용자 유형 (OU) | agent |
| `hostname` | 커스텀 | 호스트명 (CN 파싱) | testserver01 |
| `username` | 커스텀 | 사용자명 (CN 파싱) | appuser |
| `client_ip` | 커스텀 | 클라이언트 IP | 127.0.0.1 |
| `client_auth_method` | 커스텀 | 인증 방식 | client_credentials_mtls |
| `token_type` | 커스텀 | 토큰 유형 | access_token |

### 토큰 유효 기간

| 토큰 유형 | 유효 기간 |
|----------|----------|
| Access Token | 30분 |
| Refresh Token | 30일 |

---

## 5. 인증서 → JWT 매핑 요약

| 인증서 필드 | 추출 방법 | JWT 클레임 | 예시 값 |
|------------|----------|-----------|---------|
| CN 전체 | 직접 추출 | `sub` | testserver01_appuser_J |
| CN 첫 부분 | 정규식 파싱 | `hostname` | testserver01 |
| CN 두번째 부분 | 정규식 파싱 | `username` | appuser |
| OU | 직접 추출 | `usertype` | agent |
| 요청 IP | SSL 컨텍스트 | `client_ip` | 127.0.0.1 |

---

## 6. 전체 인증 흐름

```
┌─────────────────┐                    ┌─────────────────┐
│   Agent Client  │                    │   Auth Server   │
└────────┬────────┘                    └────────┬────────┘
         │                                      │
         │ 1. mTLS 연결 (인증서 전송)            │
         │  [CN=testserver01_appuser_J,        │
         │   OU=agent, O=Leebalso, C=KR]       │
         │─────────────────────────────────────>│
         │                                      │
         │ 2. POST /oauth2/token               │
         │    grant_type=client_credentials    │
         │─────────────────────────────────────>│
         │                                      │
         │                                      │ 3. DN 파싱
         │                                      │    - agent_id: testserver01_appuser_J
         │                                      │    - hostname: testserver01
         │                                      │    - username: appuser
         │                                      │    - usertype: agent
         │                                      │
         │                                      │ 4. 4단계 검증
         │                                      │    - OU 검증 ✓
         │                                      │    - 등록 확인 ✓
         │                                      │    - 정보 일치 ✓
         │                                      │    - IP 검증 ✓
         │                                      │
         │                                      │ 5. JWT 토큰 생성
         │                                      │    {
         │                                      │      "sub": "testserver01_appuser_J",
         │                                      │      "hostname": "testserver01",
         │                                      │      "username": "appuser",
         │                                      │      "usertype": "agent",
         │                                      │      "client_ip": "127.0.0.1",
         │                                      │      ...
         │                                      │    }
         │                                      │
         │ 6. Access Token 응답                 │
         │<─────────────────────────────────────│
         │  {                                   │
         │    "access_token": "eyJ...",        │
         │    "token_type": "Bearer",          │
         │    "expires_in": 1800,              │
         │    "scope": "agent:commands ..."    │
         │  }                                   │
         │                                      │
         │ 7. API 호출 시 토큰 사용             │
         │    Authorization: Bearer eyJ...     │
         │─────────────────────────────────────>│
```

---

## 7. 관련 파일

| 파일 | 설명 |
|------|------|
| `test-server/generate-certs.sh` | 인증서 생성 스크립트 |
| `test-server/mock_server.py` | 인증서버 (DN 파싱, JWT 생성) |
| `src/main/java/mwmanger/common/Common.java` | Java Agent mTLS 클라이언트 |
| `src/main/java/mwmanger/common/Config.java` | mTLS 설정 관리 |

---

## 8. 보안 고려사항

### 인증서 복사 방지 (IP 검증)

인증서가 복사되어 다른 서버에서 사용되는 것을 방지하기 위해, 각 Agent에 허용된 IP 목록을 관리합니다.

```python
AGENTS_DB = {
    "testserver01_appuser_J": {
        "allowed_ips": ["127.0.0.1", "10.0.1.100", "192.168.1.100"],
        ...
    }
}
```

### Cascading Token Renewal

Refresh Token이 만료된 경우 mTLS를 통한 자동 갱신을 지원합니다:

1. Refresh Token으로 갱신 시도
2. 401 응답 시 mTLS client_credentials grant로 폴백
3. 새로운 Access Token 발급

```java
public static int renewAccessTokenWithFallback() {
    int result = updateToken();  // refresh_token 시도

    if (result == -401 && config.isUseMtls()) {
        result = renewAccessTokenWithMtls();  // mTLS 폴백
    }

    return result;
}
```

---

## 9. Auth Server API Specification

### 필수 구현 엔드포인트

#### 9.1 Token Endpoint (OAuth2)

**URL**: `POST /oauth2/token`

**인증 방식**: mTLS (클라이언트 인증서 필수)

**Content-Type**: `application/x-www-form-urlencoded`

**Request Parameters**:

| 파라미터 | 필수 | 값 | 설명 |
|---------|------|-----|------|
| `grant_type` | Y | `client_credentials` | OAuth2 grant type |
| `scope` | N | `agent:commands agent:results` | 요청 권한 범위 |

**Request 예시**:
```http
POST /oauth2/token HTTP/1.1
Host: auth-server:8443
Content-Type: application/x-www-form-urlencoded
(mTLS 클라이언트 인증서: CN=testserver01_appuser_J)

grant_type=client_credentials&scope=agent:commands%20agent:results
```

**Success Response** (HTTP 200):
```json
{
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "expires_in": 1800,
    "scope": "agent:commands agent:results",
    "refresh_token": "dGVzdHNlcnZlcjAxX2FwcHVzZXJfSl9yZWZyZXNo..."
}
```

**Error Response** (HTTP 401/403):
```json
{
    "error": "invalid_client",
    "error_description": "Client certificate validation failed"
}
```

#### 9.2 Refresh Token Endpoint

**URL**: `POST /oauth2/token`

**인증 방식**: Bearer Token (refresh_token)

**Content-Type**: `application/x-www-form-urlencoded`

**Request Parameters**:

| 파라미터 | 필수 | 값 | 설명 |
|---------|------|-----|------|
| `grant_type` | Y | `refresh_token` | OAuth2 grant type |
| `refresh_token` | Y | (refresh token 값) | 이전에 발급받은 refresh token |

**Request 예시**:
```http
POST /oauth2/token HTTP/1.1
Host: auth-server:8443
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token&refresh_token=dGVzdHNlcnZlcjAxX2FwcHVzZXJfSl9yZWZyZXNo...
```

#### 9.3 Legacy Token Refresh (Non-mTLS)

**URL**: `POST /api/v1/security/refresh`

**인증 방식**: Bearer Token (refresh_token in header)

**Content-Type**: `application/json`

**Request Headers**:
```http
Authorization: Bearer {refresh_token}
```

**Request Body**:
```json
{
    "agent_id": "testserver01_appuser_J"
}
```

**Success Response** (HTTP 200):
```json
{
    "result_code": "OK",
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "dGVzdHNlcnZlcjAxX2FwcHVzZXJfSl9yZWZyZXNo..."
}
```

### JWT 토큰 요구사항

#### Access Token 필수 클레임

```json
{
    "sub": "testserver01_appuser_J",    // 필수: Agent ID (인증서 CN)
    "iss": "leebalso-auth-server",      // 필수: 발급자 식별자
    "aud": "https://api.mwagent.example.com",  // 필수: 대상 시스템
    "exp": 1735123456,                  // 필수: 만료 시간 (Unix timestamp)
    "iat": 1735121656,                  // 필수: 발급 시간
    "scope": "agent:commands agent:results",   // 필수: 권한 범위

    "usertype": "agent",                // 권장: 사용자 유형 (OU에서 추출)
    "hostname": "testserver01",         // 권장: 호스트명 (CN 파싱)
    "username": "appuser",              // 권장: 사용자명 (CN 파싱)
    "client_ip": "127.0.0.1"            // 권장: 인증 시점 클라이언트 IP
}
```

#### 토큰 서명 알고리즘

| 알고리즘 | 설명 | 권장 |
|---------|------|------|
| HS256 | HMAC + SHA256 (대칭키) | 개발/테스트용 |
| RS256 | RSA + SHA256 (비대칭키) | **운영 권장** |
| ES256 | ECDSA + SHA256 | 고보안 환경 |

### Error Codes

Agent가 처리하는 에러 코드:

| HTTP Status | error | 설명 | Agent 동작 |
|-------------|-------|------|-----------|
| 401 | `invalid_token` | 토큰 만료/무효 | mTLS로 재인증 시도 |
| 401 | `invalid_client` | 인증서 검증 실패 | 에러 로깅 후 종료 |
| 403 | `insufficient_scope` | 권한 부족 | 에러 로깅 |
| 403 | `ip_mismatch` | IP 검증 실패 | 에러 로깅 후 종료 |

---

## 10. CA Server Specification

### 인증서 발급 요구사항

#### Agent 클라이언트 인증서

| 항목 | 요구사항 |
|------|----------|
| **Subject DN 형식** | `CN={hostname}_{username}_J, OU=agent, O={조직명}, C={국가코드}` |
| **Key Usage** | Digital Signature, Key Encipherment |
| **Extended Key Usage** | Client Authentication (1.3.6.1.5.5.7.3.2) |
| **유효기간** | 1년 권장 (운영 정책에 따름) |
| **키 알고리즘** | RSA 2048bit 이상 또는 ECDSA P-256 |
| **파일 형식** | PKCS#12 (.p12) - 키+인증서 포함 |

#### 서버 인증서 (Auth Server, Biz Service)

| 항목 | 요구사항 |
|------|----------|
| **Subject DN 형식** | `CN={hostname}, OU={service|auth}, O={조직명}, C={국가코드}` |
| **SAN (Subject Alternative Name)** | DNS:{hostname}, IP:{ip_address} |
| **Key Usage** | Digital Signature, Key Encipherment |
| **Extended Key Usage** | Server Authentication (1.3.6.1.5.5.7.3.1) |

#### CA 인증서

| 항목 | 요구사항 |
|------|----------|
| **Subject DN 형식** | `CN={CA명}, OU=CA, O={조직명}, C={국가코드}` |
| **Key Usage** | Certificate Sign, CRL Sign |
| **Basic Constraints** | CA:TRUE |
| **유효기간** | 10년 이상 권장 |

### 인증서 배포

Agent에게 배포해야 하는 파일:

| 파일 | 용도 | 형식 |
|------|------|------|
| `agent-{id}.p12` | Agent 클라이언트 인증서+키 | PKCS#12 |
| `truststore.jks` | CA 인증서 (서버 검증용) | Java KeyStore |
| `ca.crt` | CA 인증서 (PEM) | PEM |

### Agent 설정 예시 (agent.properties)

```properties
# mTLS 활성화
use_mtls=true

# 클라이언트 인증서 (PKCS#12)
client.keystore.path=/opt/agent/certs/agent-testserver01.p12
client.keystore.password=changeit

# 서버 검증용 CA 인증서
truststore.path=/opt/agent/certs/truststore.jks
truststore.password=changeit
```
