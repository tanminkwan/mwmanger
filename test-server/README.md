# Mock Server for mTLS Token Renewal Testing

Python Flask 기반 목업 서버로, mTLS 인증과 기존 refresh token 방식을 모두 지원합니다.

## Features

- **두 가지 인증 방식 지원**:
  1. **Refresh Token 방식** (기존): JWT refresh token으로 access token 갱신
  2. **mTLS 방식** (신규): Client certificate의 CN으로 access token 갱신

- **Mock Agent Database**: 테스트용 agent 정보 포함
- **Certificate 자동 생성**: 스크립트로 CA, 서버, 클라이언트 인증서 생성

## Prerequisites

### Required Tools
- Python 3.8+
- OpenSSL
- Java keytool (JDK에 포함)

### Installation

```bash
# Python dependencies 설치
pip install -r requirements.txt
```

## Quick Start

### 1. Certificate 생성

**Linux/Mac:**
```bash
chmod +x generate-certs.sh
./generate-certs.sh
```

**Windows:**
```cmd
generate-certs.bat
```

생성되는 파일:
```
certs/
├── ca.crt                  # CA certificate
├── ca.key                  # CA private key
├── server.crt              # Server certificate
├── server.key              # Server private key
├── agent-test001.p12       # Agent 1 client certificate (password: agent-password)
├── agent-test002.p12       # Agent 2 client certificate (password: agent-password)
└── truststore.jks          # Java truststore (password: truststore-password)
```

### 2. Mock 서버 실행

**mTLS 지원 (HTTPS):**
```bash
python mock_server.py --ssl
# Server runs on https://localhost:8443
```

**mTLS 없이 (HTTP - 테스트용):**
```bash
python mock_server.py
# Server runs on http://localhost:8080
```

## API Endpoints

### Authentication Endpoints

#### 1. Refresh Token 방식 (기존)

**Get Refresh Token:**
```
GET /api/v1/agent/getRefreshToken/<agent_id>
Authorization: Bearer <access_token>

Response:
{
  "refresh_token": "refresh-token-test001",
  "agent_id": "agent-test001"
}
```

**Refresh Access Token:**
```
POST /api/v1/security/refresh
Authorization: Bearer <refresh_token>

Response:
{
  "access_token": "eyJ0eXAiOiJKV1QiLCJh...",
  "expires_in": 1800,
  "token_type": "Bearer"
}
```

#### 2. mTLS 방식 (신규)

**Renew Access Token with mTLS:**
```
POST /api/v1/security/token/renew
[Client certificate required]

Response:
{
  "access_token": "eyJ0eXAiOiJKV1QiLCJh...",
  "expires_in": 1800,
  "token_type": "Bearer",
  "method": "mTLS"
}
```

서버는 client certificate의 CN에서 agent_id를 추출하여 검증합니다.

### Test Endpoints

**Test Access Token:**
```
GET /api/v1/agent/test
Authorization: Bearer <access_token>

Response:
{
  "message": "Access token is valid",
  "agent_id": "agent-test001"
}
```

**Get Commands (Mock):**
```
GET /api/v1/commands/<agent_id>
Authorization: Bearer <access_token>

Response:
{
  "return_code": 1,
  "data": [],
  "agent_id": "agent-test001"
}
```

**Health Check:**
```
GET /health

Response:
{
  "status": "ok"
}
```

## Mock Agents

서버에는 다음 테스트 agent들이 등록되어 있습니다:

| Agent ID | Refresh Token | Certificate | Status |
|----------|---------------|-------------|--------|
| agent-test001 | refresh-token-test001 | agent-test001.p12 | active |
| agent-test002 | refresh-token-test002 | agent-test002.p12 | active |

## Testing

### cURL로 테스트

**1. Refresh Token 방식:**
```bash
# Get refresh token
curl -X GET http://localhost:8080/api/v1/agent/getRefreshToken/agent-test001 \
  -H "Authorization: Bearer <initial_access_token>"

# Refresh access token
curl -X POST http://localhost:8080/api/v1/security/refresh \
  -H "Authorization: Bearer refresh-token-test001"
```

**2. mTLS 방식:**
```bash
# Renew token with client certificate
curl -X POST https://localhost:8443/api/v1/security/token/renew \
  --cert ./certs/agent-test001.crt \
  --key ./certs/agent-test001.key \
  --cacert ./certs/ca.crt
```

### Java Agent로 테스트

Java agent 설정 파일 (agent.properties):
```properties
# Server URL
server_url=https://localhost:8443

# Agent ID
agent_id=agent-test001

# mTLS Configuration
use_mtls=true
client.keystore.path=./test-server/certs/agent-test001.p12
client.keystore.password=agent-password
truststore.path=./test-server/certs/truststore.jks
truststore.password=truststore-password
```

## Certificate Details

### CA Certificate
- **Subject**: CN=Test CA, OU=Testing, O=MwAgent, C=KR
- **Validity**: 365 days
- **Key Size**: 4096 bits

### Server Certificate
- **Subject**: CN=localhost, OU=Server, O=MwAgent, C=KR
- **SAN**: DNS:localhost, IP:127.0.0.1
- **Validity**: 365 days

### Agent Client Certificates
- **Subject**: CN=agent-test001, OU=Agents, O=MwAgent, C=KR
- **Format**: PKCS12 (.p12)
- **Password**: agent-password
- **Validity**: 365 days

## Architecture

```
┌─────────────┐                    ┌──────────────┐
│             │   1. mTLS Request  │              │
│    Agent    ├───────────────────>│ Mock Server  │
│             │   (Client Cert)    │              │
└─────────────┘                    └──────────────┘
       │                                   │
       │ 2. Extract CN from cert           │
       │    CN=agent-test001               │
       │                                   │
       │ 3. Verify agent in DB             │
       │                                   │
       │ 4. Generate access token          │
       │<──────────────────────────────────┤
       │    {access_token: "..."}          │
       │                                   │
       │ 5. Use access token               │
       ├──────────────────────────────────>│
       │    Authorization: Bearer ...      │
```

## Troubleshooting

### Certificate 오류
```
[ERROR] SSL certificates not found
```
→ `generate-certs.sh` 또는 `generate-certs.bat` 실행

### Connection refused
```
curl: (7) Failed to connect to localhost port 8443
```
→ 서버가 실행 중인지 확인: `python mock_server.py --ssl`

### Certificate verification failed
```
SSL certificate problem: unable to get local issuer certificate
```
→ cURL에 `--cacert ./certs/ca.crt` 옵션 추가

## Development

서버 코드 수정 후 자동 재시작 (Flask debug mode):
```bash
python mock_server.py --ssl
# or
python mock_server.py
```

Mock agent 추가:
```python
# mock_server.py
AGENTS_DB = {
    "agent-test003": {
        "agent_id": "agent-test003",
        "refresh_token": "refresh-token-test003",
        "status": "active"
    }
}
```

## Security Notes

⚠️ **이 서버는 테스트 전용입니다. 프로덕션 환경에서 사용하지 마세요.**

- 모든 인증서는 자체 서명(self-signed)입니다
- Secret key가 코드에 하드코딩되어 있습니다
- Certificate 검증이 간소화되어 있습니다
- HTTPS 호스트 검증이 비활성화되어 있습니다

## License

Test purpose only - MIT License
