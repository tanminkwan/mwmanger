# MwManger Agent 리팩토링 - AI 자율 수행용 요구사항 명세서

> **목적**: 이 문서는 AI(Claude)가 사람과의 상호작용 없이 완전 자율적으로 프로젝트를 수행할 수 있도록 작성된 명세서입니다.

---

## 1. 프로젝트 개요

### 1.1 대상 시스템
- **프로젝트명**: MwManger Agent
- **언어**: Java 1.8
- **유형**: 데몬 프로세스 (원격 서버 관리 에이전트)
- **현재 버전**: 0000.0009.0001
- **목표 버전**: 0000.0009.0010

### 1.2 프로젝트 목표
레거시 Java 에이전트를 보안 강화 및 아키텍처 개선하여 다음을 달성:
1. mTLS + OAuth2 기반 인증 체계 구축
2. 보안 취약점 제거 (Command Injection, Path Traversal)
3. 테스트 가능한 아키텍처로 전환 (DI, 인터페이스 추상화)
4. 215개 이상의 테스트로 품질 보증

---

## 2. 현재 상태 (AS-IS)

### 2.1 디렉토리 구조
```
mwmanger/
├── src/main/java/mwmanger/
│   ├── MwAgent.java           # 메인 진입점
│   ├── PreWork.java           # 등록 로직 (150줄, 복잡)
│   ├── FirstWork.java         # Kafka 초기화
│   ├── MainWork.java          # 명령 폴링
│   ├── common/
│   │   ├── Config.java        # 싱글톤, 테스트 불가
│   │   └── Common.java        # HTTP 통신
│   ├── order/
│   │   ├── Order.java         # 추상 클래스
│   │   ├── ExeShell.java      # 쉘 실행 (취약)
│   │   ├── ExeScript.java     # 스크립트 실행 (취약)
│   │   ├── DownloadFile.java  # 파일 다운로드 (취약)
│   │   └── ReadFullPathFile.java
│   ├── agentfunction/
│   │   ├── AgentFunc.java
│   │   ├── AgentFuncFactory.java
│   │   └── [기타 Func 클래스들]
│   ├── kafka/
│   │   ├── MwConsumerThread.java  # 버그 있음
│   │   ├── MwProducer.java
│   │   └── MwHealthCheckThread.java
│   └── vo/
│       ├── CommandVO.java
│       └── ResultVO.java
└── pom.xml
```

### 2.2 현재 보안 취약점

| ID | 파일 | 라인 | 취약점 | 심각도 |
|----|------|------|--------|--------|
| V1 | ExeShell.java | 50 | Command Injection - `Runtime.exec(command)` 직접 사용 | CRITICAL |
| V2 | ExeScript.java | - | Command Injection - 파라미터 검증 없음 | CRITICAL |
| V3 | DownloadFile.java | - | Path Traversal - `../` 패턴 미검증 | CRITICAL |
| V4 | ReadFullPathFile.java | - | Path Traversal - 경로 검증 없음 | HIGH |
| V5 | Common.java | 268, 317 | 토큰 로깅 - `logger.fine("token: " + token)` | HIGH |
| V6 | MwConsumerThread.java | 83 | 동시성 버그 - `\|\| stopRequested==true` | HIGH |

### 2.3 현재 인증 방식
```
Agent ──[Refresh Token in Header]──> Server
         (탈취 시 무한 사용 가능)
```
- 엔드포인트: `POST /api/v1/security/refresh`
- Content-Type: `application/json`
- 인증: Bearer Token (refresh_token)

---

## 3. 목표 상태 (TO-BE)

### 3.1 목표 디렉토리 구조
```
mwmanger/
├── src/main/java/mwmanger/
│   ├── MwAgent.java                    # 진입점만
│   ├── AgentRegistrationPhase.java     # 등록 위임
│   ├── InitializationPhase.java        # 초기화 위임
│   ├── CommandProcessingLoop.java      # 명령 처리 위임
│   │
│   ├── lifecycle/                      # [신규] Phase 1
│   │   ├── AgentLifecycle.java         # 인터페이스
│   │   ├── LifecycleState.java         # enum (CREATED/STARTING/RUNNING/STOPPING/STOPPED/FAILED)
│   │   ├── AgentLifecycleManager.java  # 생명주기 관리
│   │   └── GracefulShutdownHandler.java # 30초 타임아웃 종료
│   │
│   ├── service/                        # [신규] Phase 1
│   │   ├── KafkaService.java           # Consumer/Producer/HealthCheck 통합
│   │   ├── CommandExecutorService.java # ThreadPool 관리
│   │   └── registration/
│   │       ├── BootstrapService.java
│   │       ├── RegistrationService.java
│   │       └── AgentStatusService.java
│   │
│   ├── infrastructure/                 # [신규] Phase 3
│   │   ├── config/
│   │   │   └── ConfigurationProvider.java  # 인터페이스
│   │   └── http/
│   │       ├── HttpClient.java             # 인터페이스
│   │       ├── HttpClientException.java
│   │       └── ApacheHttpClientAdapter.java
│   │
│   ├── application/
│   │   └── ApplicationContext.java     # [신규] DI 컨테이너
│   │
│   ├── common/
│   │   ├── Config.java                 # ConfigurationProvider 구현
│   │   ├── Common.java                 # mTLS 지원 추가
│   │   ├── Version.java                # [신규] 버전 단일 소스
│   │   └── SecurityValidator.java      # [신규] Phase 2
│   │
│   ├── order/                          # [수정] 보안 강화
│   │   └── [기존 + 보안 검증 추가]
│   │
│   └── vo/
│       ├── AgentStatus.java            # [신규] enum
│       ├── AgentErrorCode.java         # [신규] enum
│       ├── RegistrationRequest.java    # [신규]
│       ├── RegistrationResponse.java   # [신규]
│       └── [기존 VO들]
│
├── src/test/java/mwmanger/            # [신규] 215개 테스트
│
├── test-server/                        # [신규] Python mTLS 서버
├── biz-service/                        # [신규] Python JWT 검증 예제
├── ca-server/                          # [신규] Python CA 서버
└── docs/                               # [신규] 기술 문서
```

### 3.2 목표 인증 체계

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    CA Server    │     │   Auth Server   │     │   Biz Service   │
│  (인증서 발급)   │     │  (토큰 발급)     │     │  (업무 API)     │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         │ 인증서                 │ mTLS+OAuth2           │ JWT
         ▼                       ▼                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                        MwManger Agent                           │
└─────────────────────────────────────────────────────────────────┘
```

#### 인증서 Subject DN 형식
```
CN={hostname}_{username}_J, OU=agent, O=Leebalso, C=KR
```

#### OAuth2 엔드포인트
- URL: `POST /oauth2/token`
- Content-Type: `application/x-www-form-urlencoded`
- Grant Types:
  - `client_credentials` (mTLS 인증)
  - `refresh_token` (토큰 갱신)

#### 계단식 토큰 갱신 로직
```java
public static int renewAccessTokenWithFallback() {
    // 1차: refresh_token으로 시도
    int result = updateTokenWithRefreshToken();

    // 2차: 실패 시 mTLS로 fallback (use_mtls=true인 경우)
    if (result == -401 && config.isUseMtls()) {
        result = updateTokenWithMtls();
    }

    return result;
}
```

---

## 4. 기술 스펙

### 4.1 빌드 환경
| 항목 | 값 |
|------|-----|
| JDK | 1.8 (필수) |
| 빌드 도구 | Maven 3.9.6 (tools/ 디렉토리에 포함) |
| 프록시 | `http://70.10.15.10:8080` |
| Gradle | 사용 금지 (프록시/SSL 문제) |

### 4.2 빌드 명령어
```bash
# 테스트 실행
HTTP_PROXY=http://70.10.15.10:8080 HTTPS_PROXY=http://70.10.15.10:8080 \
./tools/apache-maven-3.9.6/bin/mvn test

# 오프라인 빌드 (Windows)
/c/Windows/System32/cmd.exe //c "cd /d C:\GitHub\mwmanger && build-offline.bat"
```

### 4.3 의존성 (pom.xml)
```xml
<!-- 테스트 프레임워크 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.8.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>3.12.4</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.21.0</version>
    <scope>test</scope>
</dependency>
```

### 4.4 하위 호환성 요구사항
**두 가지 인증 모드 모두 동작해야 함**:

| 모드 | 설정 | 엔드포인트 | Content-Type |
|------|------|-----------|--------------|
| mTLS | `use_mtls=true` | `/oauth2/token` | `application/x-www-form-urlencoded` |
| Legacy | `use_mtls=false` | `/api/v1/security/refresh` | `application/json` |

---

## 5. Phase별 상세 작업

### Phase 1: Lifecycle Management

#### 목표
- 체계적인 생명주기 관리 프레임워크 구축
- Graceful shutdown 구현

#### 생성할 파일

**1. `lifecycle/LifecycleState.java`**
```java
public enum LifecycleState {
    CREATED, STARTING, RUNNING, STOPPING, STOPPED, FAILED;

    public boolean canTransitionTo(LifecycleState target) {
        // CREATED -> STARTING -> RUNNING -> STOPPING -> STOPPED
        // 어느 상태에서든 -> FAILED 가능
    }
}
```

**2. `lifecycle/AgentLifecycle.java`**
```java
public interface AgentLifecycle {
    void start();
    void stop();
    LifecycleState getState();
    String getName();
}
```

**3. `lifecycle/GracefulShutdownHandler.java`**
- 서비스를 LIFO 순서로 종료
- 각 서비스에 30초 타임아웃 적용
- 종료 전 로그 flush

**4. `lifecycle/AgentLifecycleManager.java`**
- 4단계 실행: Bootstrap → Init → Runtime → Shutdown
- 상태 전이 관리

**5. `service/KafkaService.java`**
- MwConsumerThread, MwProducer, MwHealthCheckThread 통합
- AgentLifecycle 구현

**6. `service/CommandExecutorService.java`**
- ThreadPool 관리
- 실행 중인 작업 완료 대기

**7. `service/registration/` 패키지**
- BootstrapService: 전체 등록 프로세스 조율
- RegistrationService: Agent 등록만 담당
- AgentStatusService: Agent 상태 확인만 담당

**8. `vo/AgentStatus.java`**
```java
public enum AgentStatus {
    NOT_REGISTERED(-1),
    PENDING_APPROVAL(-2),
    APPROVED(1),
    REJECTED(-3),
    ERROR(-99);
}
```

**9. `vo/RegistrationRequest.java`, `vo/RegistrationResponse.java`**
- 등록 요청/응답 VO

#### 테스트 요구사항
- `LifecycleStateTest.java`: 상태 전이 테스트
- `GracefulShutdownHandlerTest.java`: 종료 순서, 타임아웃 테스트
- `CommandExecutorServiceTest.java`: ThreadPool 테스트
- `BootstrapServiceTest.java`, `RegistrationServiceTest.java`

---

### Phase 2: Security Hardening

#### 목표
- Command Injection 방어
- Path Traversal 방어
- 토큰 로깅 마스킹

#### 생성할 파일

**1. `common/SecurityValidator.java`**
```java
public class SecurityValidator {
    // Command Injection 방어
    private static final Pattern DANGEROUS_CHARS =
        Pattern.compile("[;|`$()&<>\\n\\r]");

    // Path Traversal 방어
    private static final Pattern PATH_TRAVERSAL =
        Pattern.compile("\\.\\.[\\\\/]");

    public static boolean containsPathTraversal(String path);
    public static boolean containsDangerousCharacters(String input);
    public static String maskToken(String token);  // 끝 10자리만 표시
    public static void validateFilename(String filename);  // 경로 구분자 포함 거부
}
```

#### 수정할 파일

**1. `order/ExeShell.java`**
- `SecurityValidator.containsDangerousCharacters()` 호출 추가
- 설정으로 on/off 가능: `security.command_injection_check`

**2. `order/ExeScript.java`**
- 동일하게 보안 검증 추가

**3. `order/DownloadFile.java`**
- `SecurityValidator.containsPathTraversal()` 호출 추가
- `SecurityValidator.validateFilename()` 호출 추가

**4. `order/ReadFullPathFile.java`**
- Path Traversal 검증 추가

**5. `common/Common.java`**
- 토큰 로깅을 `SecurityValidator.maskToken()` 사용으로 변경

**6. `kafka/MwConsumerThread.java:83`**
```java
// Before (버그)
} while (!StringUtils.equals(message, FIN_MESSAGE) || stopRequested==true);

// After (수정)
} while (!StringUtils.equals(message, FIN_MESSAGE) && !stopRequested);
```

#### 설정 추가 (agent.properties)
```properties
security.path_traversal_check=true      # 기본값: true
security.command_injection_check=false  # 기본값: false (특수문자 차단)
```

#### 테스트 요구사항
- `SecurityValidatorTest.java`: 모든 검증 메서드 테스트 (최소 30개 테스트 케이스)

---

### Phase 3: Dependency Injection

#### 목표
- 테스트 가능한 아키텍처
- 인터페이스 추상화

#### 생성할 파일

**1. `infrastructure/config/ConfigurationProvider.java`**
```java
public interface ConfigurationProvider {
    String getString(String key);
    String getString(String key, String defaultValue);
    int getInt(String key);
    boolean getBoolean(String key);

    // Agent 정보
    String getAgentId();
    String getServerUrl();

    // mTLS 설정
    boolean isUseMtls();
    String getKeystorePath();
    String getKeystorePassword();
    String getTruststorePath();
    String getTruststorePassword();
}
```

**2. `infrastructure/http/HttpClient.java`**
```java
public interface HttpClient {
    HttpResponse get(String path, Map<String, String> headers);
    HttpResponse post(String path, Map<String, String> headers, String body);
    HttpResponse postForm(String path, Map<String, String> headers, Map<String, String> formData);
    void close();
}
```

**3. `infrastructure/http/ApacheHttpClientAdapter.java`**
- HttpClient 구현
- HTTP, HTTPS, mTLS 모두 지원

**4. `infrastructure/http/HttpClientException.java`**
- HTTP 에러용 커스텀 예외

**5. `application/ApplicationContext.java`**
```java
public class ApplicationContext {
    private static ApplicationContext instance;
    private final Map<Class<?>, Object> beans = new ConcurrentHashMap<>();

    public static ApplicationContext getInstance();
    public <T> void register(Class<T> type, T instance);
    public <T> T getBean(Class<T> type);
}
```

#### 수정할 파일

**1. `common/Config.java`**
- `ConfigurationProvider` 인터페이스 구현 추가
- 기존 기능 유지

#### 테스트용 파일

**1. `test/.../MockConfigurationProvider.java`**
- 테스트용 Mock 구현

#### 테스트 요구사항
- `ConfigurationProviderTest.java`
- `HttpClientTest.java`
- `ApplicationContextTest.java`

---

### Phase 4: mTLS 인증 환경

#### 목표
- mTLS 클라이언트 인증 구현
- OAuth2 토큰 엔드포인트 마이그레이션
- 테스트 환경 구축

#### 수정할 파일

**1. `common/Common.java`**
```java
// 추가할 메서드
public static CloseableHttpClient createMtlsClient();
public static int updateTokenWithMtls();  // client_credentials grant
public static int updateTokenWithRefreshToken();  // refresh_token grant
public static int renewAccessTokenWithFallback();  // 계단식 갱신

// httpPOSTFormUrlEncoded 추가
public static MwResponseVO httpPOSTFormUrlEncoded(String path, Map<String, String> params);
```

**2. `common/Config.java`**
```java
// 추가할 설정 필드 및 getter
private boolean use_mtls;
private String clientKeystorePath;
private String clientKeystorePassword;
private String truststorePath;
private String truststorePassword;
```

#### 생성할 파일 (test-server/)

**1. `test-server/generate-certs.sh` (Linux/Mac)**
**2. `test-server/generate-certs.bat` (Windows)**
```bash
# 생성할 인증서:
# - ca.crt, ca.key (CA)
# - server.crt, server.key (서버)
# - agent-test001.p12, agent-test002.p12, agent-test003.p12 (클라이언트)
# - truststore.jks (Java truststore)
```

**3. `test-server/mock_server.py`**
```python
# Flask 기반 mTLS OAuth2 서버
# 엔드포인트:
# - POST /oauth2/token (client_credentials, refresh_token)
# - GET /api/v1/agent/getRefreshToken/{agent_id}
# - POST /test/expire-refresh-token/{agent_id}  # 테스트용

# 4단계 검증:
# 1. OU 검증 (usertype == "agent")
# 2. 등록 확인 (Agent DB에 존재)
# 3. 정보 일치 (hostname, username)
# 4. IP 검증 (allowed_ips)
```

**4. `test-server/test-agent.properties`**
```properties
use_mtls=true
client.keystore.path=./test-server/certs/agent-test001.p12
client.keystore.password=agent-password
truststore.path=./test-server/certs/truststore.jks
truststore.password=truststore-password
```

#### 테스트 요구사항
- `CommonMtlsTest.java`: mTLS 클라이언트 생성 테스트
- `ConfigMtlsTest.java`: mTLS 설정 로딩 테스트
- `CascadingTokenRenewalTest.java`: 계단식 갱신 테스트
- `MtlsTokenRenewalIntegrationTest.java`: 통합 테스트 (환경변수로 조건부 실행)

---

### Phase 5: Integration Testing

#### 목표
- E2E 테스트 환경 구축
- Biz Service 예제 구현

#### 생성할 파일 (biz-service/)

**1. `biz-service/app.py`**
```python
# Flask 기반 JWT 검증 예제
# 엔드포인트:
# - GET /api/whoami (Bearer Token 필요)
# - GET /api/commands (scope: agent:commands)
# - POST /api/results (scope: agent:results)
```

**2. `biz-service/token_validator.py`**
```python
# JWT 검증 데코레이터
@require_token
@require_scope("agent:commands")
def get_commands():
    pass
```

#### 생성할 파일 (ca-server/)

**1. `ca-server/app.py`**
```python
# Flask 기반 CA 서버
# 엔드포인트:
# - POST /api/v1/cert/issue (Bootstrap Token)
# - GET /api/v1/cert/status/{request_id}
# - POST /api/v1/cert/renew (mTLS)
# - POST /api/v1/admin/cert/approve/{request_id}
# - POST /api/v1/admin/bootstrap-token
```

#### 테스트 요구사항
- `BizServiceIntegrationTest.java`: E2E 토큰 흐름 테스트
- `SSLCertiFuncTest.java`: SSL 인증서 조회 기능 테스트
- `SSLCertiFileFuncTest.java`: 인증서 파일 파싱 테스트

#### 환경변수 기반 조건부 실행
```java
@Test
void testIntegration() {
    Assumptions.assumeTrue(
        "true".equals(System.getenv("BIZ_SERVICE_INTEGRATION_TEST"))
    );
    // 테스트 코드
}
```

---

### Phase 6: Code Quality

#### 목표
- 버전 관리 단일화
- Naming Convention 개선
- Dead Code 제거

#### 생성할 파일

**1. `common/Version.java`**
```java
public class Version {
    public static final String VERSION = "0000.0009.0010";

    public static void main(String[] args) {
        System.out.println("MwManger Agent Version: " + VERSION);
    }
}
```

**2. `vo/AgentErrorCode.java`**
```java
public enum AgentErrorCode {
    // Authentication (1000-1999)
    AUTH_FAILED(1000),
    AUTH_TOKEN_EXPIRED(1003),

    // Command (2000-2999)
    CMD_EXECUTION_FAILED(2000),

    // Network (3000-3999)
    NET_CONNECTION_FAILED(3000),

    // File (4000-4999)
    FILE_PATH_TRAVERSAL(4001);
}
```

#### 수정할 파일

**1. `common/Config.java`**
```java
// Before
private String agent_version = "0000.0009.0001";

// After
public String getAgent_version() {
    return Version.VERSION;
}
```

**2. Naming Convention 변경**
| Before | After |
|--------|-------|
| `PreWork.java` | 유지 (하위 호환성), 내부에서 `AgentRegistrationPhase` 호출 |
| `FirstWork.java` | 유지, 내부에서 `InitializationPhase` 호출 |
| `MainWork.java` | 유지, 내부에서 `CommandProcessingLoop` 호출 |

**3. `build-offline.bat`, `build-offline.sh`**
- JAR 파일명: `mwmanger.jar` (버전 없이)
- 출력 경로: `build/mwmanger.jar`

---

## 6. 문서 작성 요구사항

### 필수 문서

| 파일 | 내용 |
|------|------|
| `CLAUDE.md` | AI를 위한 프로젝트 컨텍스트 (빌드 명령, 핵심 규칙) |
| `REFACTORING_PLAN.md` | 전체 리팩토링 계획 |
| `WORK_HISTORY.md` | 작업 이력 |
| `TESTING.md` | 테스트 가이드 |
| `docs/mTLS-JWT-Authentication-Flow.md` | 인증 흐름 (Mermaid 다이어그램 포함) |
| `docs/Token-Validation-Architecture.md` | 토큰 검증 아키텍처 |
| `docs/PROJECT_REPORT.md` | 프로젝트 보고서 (Mermaid 다이어그램 포함) |

### CLAUDE.md 필수 내용
```markdown
# Critical Rules
1. 두 가지 인증 모드 모두 동작해야 함 (mTLS, Legacy)
2. 버전은 Version.java에서만 관리
3. Gradle 사용 금지, Maven 사용
4. 로그는 파일로만 (System.err 금지)

# Build Commands
# Test Commands
# Key Files
```

---

## 7. 성공 기준

### 7.1 테스트
- [ ] 총 테스트 수: 215개 이상
- [ ] 성공률: 100%
- [ ] 통합 테스트 환경변수로 조건부 실행 가능

### 7.2 보안
- [ ] Command Injection 테스트 통과
- [ ] Path Traversal 테스트 통과
- [ ] 토큰 로깅 마스킹 확인

### 7.3 기능
- [ ] mTLS 모드 동작 확인
- [ ] Legacy 모드 동작 확인
- [ ] 계단식 토큰 갱신 동작 확인
- [ ] Graceful shutdown 동작 확인

### 7.4 빌드
- [ ] `mvn test` 성공
- [ ] `build-offline.bat` 성공
- [ ] `build/mwmanger.jar` 생성

---

## 8. 제약 조건

### 8.1 절대 금지 사항
1. **Gradle 사용 금지** - 프록시/SSL 문제로 동작하지 않음
2. **pom.xml 삭제 금지**
3. **tools/apache-maven-3.9.6/ 삭제 금지**
4. **System.err 출력 금지** - 데몬 프로세스이므로 파일 로그만 사용
5. **기존 API 시그니처 변경 금지** - 하위 호환성 유지

### 8.2 반드시 준수 사항
1. **JDK 1.8 호환** - 모든 코드
2. **두 인증 모드 모두 테스트** - mTLS, Legacy
3. **테스트 없이 코드 수정 금지** - TDD 준수
4. **Phase 완료 시 커밋** - 각 Phase별로 커밋

### 8.3 Git 커밋 규칙
```
[Phase N] 작업 내용 요약

- 상세 내용 1
- 상세 내용 2

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
```

---

## 9. 작업 순서

```
1. Phase 1: Lifecycle Management
   └─ 커밋: "Phase 1: Implement lifecycle management framework"

2. Phase 1.5: mTLS Basic Support
   └─ 커밋: "Phase 1.5: Add mTLS client authentication support"

3. Phase 2: Security Hardening
   └─ 커밋: "Phase 2: Implement security validation"

4. Phase 3: Dependency Injection
   └─ 커밋: "Phase 3: Implement DI architecture"

5. Phase 4: mTLS Test Environment
   └─ 커밋: "Phase 4: Add mTLS test environment"

6. Phase 5: Integration Testing
   └─ 커밋: "Phase 5: Add integration tests and sample services"

7. Phase 6: Code Quality
   └─ 커밋: "Phase 6: Code quality improvements"

8. Documentation
   └─ 커밋: "Add project report with Mermaid diagrams"

9. Merge to main
   └─ 커밋: "Merge refactoring branch to main"
```

---

## 10. 검증 체크리스트

### Phase 완료 시 확인
```bash
# 1. 테스트 실행
HTTP_PROXY=http://70.10.15.10:8080 HTTPS_PROXY=http://70.10.15.10:8080 \
./tools/apache-maven-3.9.6/bin/mvn test

# 2. 빌드 확인
/c/Windows/System32/cmd.exe //c "cd /d C:\GitHub\mwmanger && build-offline.bat"

# 3. JAR 파일 확인
ls -la build/mwmanger.jar
```

### 최종 완료 시 확인
```bash
# 모든 통합 테스트 포함
MTLS_INTEGRATION_TEST=true \
BIZ_SERVICE_INTEGRATION_TEST=true \
SSL_CERT_INTEGRATION_TEST=true \
HTTP_PROXY=http://70.10.15.10:8080 \
HTTPS_PROXY=http://70.10.15.10:8080 \
./tools/apache-maven-3.9.6/bin/mvn test
```

---

**문서 작성일**: 2025-12-18
**대상 AI**: Claude (Anthropic)
**예상 작업 시간**: AI 자율 수행 시 약 2-3시간
