# OAuth2 Standard Migration Guide

이 문서는 기존 커스텀 토큰 엔드포인트에서 OAuth2 표준으로 마이그레이션하는 방법을 설명합니다.

## 변경 사항 요약

### 서버측 (Authorization Server)

#### Before (기존 방식):
```
POST /api/v1/security/refresh
Authorization: Bearer {refresh_token}

POST /api/v1/security/token/renew
[Client Certificate]
```

#### After (OAuth2 표준):
```
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Body: grant_type=refresh_token&refresh_token=xxx

POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Body: grant_type=client_credentials
[Client Certificate]
```

### Agent측 (Client)

#### Before (Common.java):
```java
// Refresh Token 방식
POST /api/v1/security/refresh
Authorization: Bearer {refresh_token}

// mTLS 방식
POST /api/v1/security/token/renew
Content-Type: application/json
```

#### After (OAuth2 표준):
```java
// Refresh Token Grant
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Body: grant_type=refresh_token&refresh_token=xxx

// Client Credentials Grant (mTLS)
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Body: grant_type=client_credentials
[Client Certificate in TLS handshake]
```

## OAuth2 Grant Types

### 1. Refresh Token Grant (RFC 6749 Section 6)

**Request:**
```http
POST /oauth2/token HTTP/1.1
Host: auth.example.com
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token&refresh_token=tGzv3JOkF0XG5Qx2TlKWIA
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 1800,
  "scope": "agent:commands agent:results"
}
```

**Java Implementation:**
```java
public static int updateToken() {
    String path = "/oauth2/token";
    String body = "grant_type=refresh_token&refresh_token=" + config.getRefresh_token();

    MwResponseVO mrvo = Common.httpPOST(path, "", body);

    if (mrvo.getResponse() != null) {
        String access_token = (String)mrvo.getResponse().get("access_token");
        String token_type = (String)mrvo.getResponse().get("token_type");
        Long expires_in = (Long)mrvo.getResponse().get("expires_in");
        String scope = (String)mrvo.getResponse().get("scope");

        config.setAccess_token(access_token);
        return 1;
    }
    return -1;
}
```

### 2. Client Credentials Grant with mTLS (RFC 8705)

**Request:**
```http
POST /oauth2/token HTTP/1.1
Host: auth.example.com
Content-Type: application/x-www-form-urlencoded
[TLS Client Certificate: CN=agent-test001]

grant_type=client_credentials
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 1800,
  "scope": "agent:commands agent:results"
}
```

**Java Implementation:**
```java
public static int renewAccessTokenWithMtls() {
    String path = "/oauth2/token";
    String url = config.getServer_url() + path;

    HttpPost request = new HttpPost(url);
    request.addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

    String body = "grant_type=client_credentials";
    request.setEntity(new StringEntity(body));

    HttpResponse response = mtlsHttpClient.execute(request);
    // Process response...
}
```

## JWT Token Structure (OAuth2 Compliant)

### Access Token Claims:

```json
{
  "sub": "agent-test001",                          // Subject (OAuth2 standard)
  "iss": "https://auth.mwagent.example.com",      // Issuer (OAuth2 standard)
  "aud": "https://api.mwagent.example.com",       // Audience (OAuth2 standard)
  "exp": 1735123456,                              // Expiration (OAuth2 standard)
  "iat": 1735121656,                              // Issued At (OAuth2 standard)
  "scope": "agent:commands agent:results",        // Scope (OAuth2 standard)
  "client_auth_method": "client_credentials_mtls",// Custom claim
  "token_type": "access_token"                    // Custom claim
}
```

## 하위 호환성 (Backward Compatibility)

기존 엔드포인트는 **DEPRECATED**로 유지됩니다:

```python
# DEPRECATED: Use /oauth2/token instead
@app.route('/api/v1/security/refresh', methods=['POST'])
def legacy_refresh_token():
    # Still works but deprecated
    pass

@app.route('/api/v1/security/token/renew', methods=['POST'])
def legacy_renew_token_mtls():
    # Still works but deprecated
    pass
```

## 마이그레이션 단계

### Phase 1: 서버 업데이트
1. OAuth2 표준 엔드포인트 추가 (`/oauth2/token`)
2. 기존 엔드포인트 유지 (deprecated)
3. 배포 및 테스트

### Phase 2: Agent 업데이트
1. Agent 코드에서 OAuth2 엔드포인트 사용
2. `grant_type` 파라미터 추가
3. OAuth2 응답 형식 파싱
4. 배포 및 테스트

### Phase 3: 레거시 제거 (선택사항)
1. 모든 Agent가 OAuth2 사용 확인
2. 기존 엔드포인트 제거
3. 문서 업데이트

## 테스트

### cURL로 테스트:

**1. Refresh Token Grant:**
```bash
curl -X POST https://localhost:8443/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token&refresh_token=refresh-token-test001" \
  --cacert ./test-server/certs/ca.crt
```

**2. Client Credentials Grant (mTLS):**
```bash
curl -X POST https://localhost:8443/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  --cert ./test-server/certs/agent-test001.crt \
  --key ./test-server/certs/agent-test001.key \
  --cacert ./test-server/certs/ca.crt
```

### Java Agent 테스트:

```bash
# Mock server 시작
cd test-server
python mock_server.py --ssl

# Agent 실행
cd ..
./tools/apache-maven-3.9.6/bin/mvn test
```

## OAuth2 표준 준수 사항

✅ RFC 6749 (OAuth 2.0 Authorization Framework)
- Token endpoint: `/oauth2/token`
- Grant types: `refresh_token`, `client_credentials`
- Standard error responses
- Standard token response format

✅ RFC 8705 (OAuth 2.0 Mutual-TLS Client Authentication)
- mTLS client authentication
- Certificate-bound access tokens
- Client identification via certificate CN

✅ RFC 7519 (JSON Web Token)
- Standard JWT claims: `sub`, `iss`, `aud`, `exp`, `iat`
- JWT token format

## 참고 자료

- [RFC 6749: OAuth 2.0 Authorization Framework](https://tools.ietf.org/html/rfc6749)
- [RFC 8705: OAuth 2.0 Mutual-TLS Client Authentication](https://tools.ietf.org/html/rfc8705)
- [RFC 7519: JSON Web Token (JWT)](https://tools.ietf.org/html/rfc7519)

## 문의

OAuth2 마이그레이션 관련 문의사항은 README.md를 참조하세요.
