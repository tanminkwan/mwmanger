# CA Server

MwManger Agent를 위한 Certificate Authority 서버입니다.

## 기능

- 인증서 발급 요청 (Bootstrap Token 필요)
- 인증서 갱신 (mTLS 인증)
- 관리자 승인/거부
- Bootstrap Token 발급
- CA 인증서 배포

## 설치

```bash
cd ca-server
pip install -r requirements.txt
```

## 실행

```bash
python app.py
```

서버가 시작되면:
- API: http://localhost:5000
- Swagger UI: http://localhost:5000/swagger

## API 엔드포인트

### Certificate API (`/api/v1/cert`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/issue` | 인증서 발급 요청 (Bootstrap Token 필요) |
| GET | `/status/{request_id}` | 발급 상태 확인 |
| POST | `/renew` | 인증서 갱신 (mTLS) |

### Admin API (`/api/v1/admin`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/cert/pending` | 대기 중인 요청 목록 |
| POST | `/cert/approve/{request_id}` | 요청 승인 |
| POST | `/cert/reject/{request_id}` | 요청 거부 |
| POST | `/bootstrap-token` | Bootstrap Token 생성 |
| GET | `/bootstrap-token` | Token 목록 |
| GET | `/bootstrap-token/{token}/download` | Token 파일 다운로드 |
| GET | `/cert/issued` | 발급된 인증서 목록 |

### CA API (`/ca`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/certificate` | CA 인증서 다운로드 |

## 사용 예시

### 1. Bootstrap Token 생성 (관리자)

```bash
curl -X POST http://localhost:5000/api/v1/admin/bootstrap-token \
  -H "Content-Type: application/json" \
  -d '{
    "expected_cn": "testserver01_appuser_J",
    "validity_hours": 24
  }'
```

### 2. Token 파일 다운로드

```bash
curl -o bootstrap.token \
  "http://localhost:5000/api/v1/admin/bootstrap-token/bt-xxx/download"
```

### 3. 인증서 발급 요청 (Agent)

```bash
curl -X POST http://localhost:5000/api/v1/cert/issue \
  -H "Content-Type: application/json" \
  -d '{
    "csr": "-----BEGIN CERTIFICATE REQUEST-----\n...\n-----END CERTIFICATE REQUEST-----",
    "bootstrap_token": "bt-xxx"
  }'
```

### 4. 요청 승인 (관리자)

```bash
curl -X POST http://localhost:5000/api/v1/admin/cert/approve/req-xxx \
  -H "Content-Type: application/json" \
  -d '{"validity_days": 90}'
```

### 5. 발급 상태 확인 (Agent)

```bash
curl http://localhost:5000/api/v1/cert/status/req-xxx
```

## 데이터 저장

현재 버전은 메모리에 데이터를 저장합니다. 프로덕션 환경에서는 데이터베이스를 사용해야 합니다.

CA 키와 인증서는 `ca-data/` 디렉토리에 저장됩니다:
- `ca-data/ca.key` - CA Private Key
- `ca-data/ca.crt` - CA Certificate

## 보안 주의사항

- CA Private Key는 안전하게 보관해야 합니다
- 프로덕션 환경에서는 HTTPS를 사용해야 합니다
- Bootstrap Token은 1회용이며 사용 후 무효화됩니다
