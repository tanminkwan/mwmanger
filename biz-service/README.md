# Biz Service

Agent의 JWT 토큰을 검증하는 샘플 비즈니스 서비스입니다.

## 구조

```
biz-service/
├── app.py              # Flask 앱 및 API 엔드포인트
├── config.py           # 설정
├── token_validator.py  # JWT 토큰 검증 모듈
├── requirements.txt    # 의존성
└── README.md
```

## 설치

```bash
cd biz-service
pip install -r requirements.txt
```

## 실행

```bash
python app.py
```

기본 포트: 5000

## 설정

환경 변수로 설정 가능:

| 환경변수 | 기본값 | 설명 |
|---------|--------|------|
| JWT_SECRET_KEY | your-256-bit-secret-key-here | JWT 서명 검증용 비밀키 |
| REDIS_HOST | localhost | Redis 호스트 |
| REDIS_PORT | 6379 | Redis 포트 |
| USE_REDIS | false | Redis 사용 여부 |
| BIZ_SERVICE_PORT | 5000 | 서비스 포트 |

## API 엔드포인트

### 인증 불필요

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | /health | 헬스 체크 |

### 인증 필요 (Bearer Token)

| Method | Endpoint | 필요 Scope | 설명 |
|--------|----------|-----------|------|
| GET | /api/commands | agent:commands | 대기 중인 명령 조회 |
| POST | /api/results | agent:results | 실행 결과 전송 |
| GET | /api/config | agent:commands | 에이전트 설정 조회 |
| GET | /api/token-info | (any) | 토큰 정보 확인 (디버그) |
| GET | /api/whoami | (any) | 에이전트 신원 확인 (디버그) |

## 사용 예시

### 1. Auth Server에서 토큰 발급

```bash
# mTLS로 토큰 발급
curl -k --cert agent.p12:password \
  -X POST https://localhost:8443/oauth2/token \
  -d "grant_type=client_credentials"
```

### 2. Biz Service API 호출

```bash
# 토큰으로 명령 조회
curl -H "Authorization: Bearer eyJ..." \
  http://localhost:5000/api/commands

# 결과 전송
curl -X POST \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{"command_id": "cmd-001", "status": "completed", "result": "success"}' \
  http://localhost:5000/api/results
```

## 토큰 검증 방식

### 1. 로컬 JWT 검증 (기본)

- JWT 서명 검증 (HS256)
- 발급자(iss), 대상(aud), 만료시간(exp) 검증
- Secret Key를 Auth Server와 공유

### 2. Redis 기반 검증 (선택)

`USE_REDIS=true` 설정 시:
- JWT 서명 검증 후 Redis에서 토큰 활성 상태 확인
- 실시간 토큰 폐기(revoke) 지원

## 에러 응답

```json
{
  "error": "invalid_token",
  "error_description": "token_expired"
}
```

| error | HTTP Status | 설명 |
|-------|-------------|------|
| missing_token | 401 | Authorization 헤더 없음 |
| invalid_token | 401 | 토큰 검증 실패 |
| insufficient_scope | 403 | 필요한 scope 없음 |
