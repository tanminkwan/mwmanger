# 테스트 커버리지 가이드

## 현재 커버리지 상황

### 통계
- **소스 파일**: 40개
- **테스트 파일**: 9개 (새로 추가: 4개)
- **추정 커버리지**: ~22%

### 새로 추가된 테스트
1. ✅ `AgentStatusTest.java` - AgentStatus enum 테스트
2. ✅ `RegistrationRequestTest.java` - 등록 요청 VO 테스트
3. ✅ `RegistrationResponseTest.java` - 등록 응답 VO 테스트
4. ✅ `RegistrationServiceTest.java` - 등록 서비스 테스트 (기본)

---

## JaCoCo 커버리지 도구 사용법

### Maven으로 커버리지 리포트 생성

```bash
# 1. 테스트 실행 및 커버리지 리포트 생성
mvn clean test

# 2. 커버리지 리포트 확인
# HTML 리포트: target/site/jacoco/index.html
start target/site/jacoco/index.html  # Windows
open target/site/jacoco/index.html   # Mac
xdg-open target/site/jacoco/index.html  # Linux

# 3. 커버리지 검증 (최소 기준 체크)
mvn jacoco:check

# 4. 전체 빌드 + 테스트 + 커버리지
mvn clean verify
```

### Gradle로 커버리지 리포트 생성

```bash
# 1. 테스트 실행 및 커버리지 리포트 생성
gradle clean test jacocoTestReport

# 2. 커버리지 리포트 확인
# HTML 리포트: build/reports/jacoco/test/html/index.html
start build/reports/jacoco/test/html/index.html  # Windows
open build/reports/jacoco/test/html/index.html   # Mac
xdg-open build/reports/jacoco/test/html/index.html  # Linux

# 3. 커버리지 검증
gradle jacocoTestCoverageVerification

# 4. 전체 빌드 + 테스트 + 커버리지
gradle clean build
```

---

## 커버리지 리포트 해석

### JaCoCo 리포트 항목

1. **Line Coverage (라인 커버리지)**
   - 실행된 코드 라인 비율
   - 목표: 70% 이상
   - 현재 기준: 20% 이상

2. **Branch Coverage (분기 커버리지)**
   - if/else, switch 등 분기문의 실행 비율
   - 목표: 60% 이상
   - 현재 기준: 15% 이상

3. **Method Coverage (메서드 커버리지)**
   - 호출된 메서드 비율

4. **Class Coverage (클래스 커버리지)**
   - 테스트된 클래스 비율

### 리포트 색상 의미
- 🟢 **녹색**: 완전히 커버됨
- 🟡 **노란색**: 부분적으로 커버됨
- 🔴 **빨간색**: 커버되지 않음

---

## 커버리지를 높이는 전략

### Phase 1: 새로 만든 모듈 테스트 (완료)
- ✅ AgentStatus enum
- ✅ RegistrationRequest/Response VO
- 🔲 RegistrationService (Mock 필요)
- 🔲 AgentStatusService (Mock 필요)
- 🔲 BootstrapService (Mock 필요)
- 🔲 PreWork (통합 테스트)

### Phase 2: VO 클래스 테스트 (쉬움, 빠른 효과)
- 🔲 RawCommandsVO
- 🔲 MwResponseVO
- ✅ CommandVO (기존)
- ✅ ResultVO (기존)

### Phase 3: Order 구현체 테스트 (중요 - 보안 취약점)
우선순위 HIGH - 보안 취약점이 있는 클래스
- 🔲 ExeShell (Command Injection 위험)
- 🔲 ExeScript
- 🔲 ExeText
- 🔲 DownloadFile (Path Traversal 위험)
- 🔲 ReadFile (Path Traversal 위험)

우선순위 MEDIUM
- 🔲 ExeAgentFunc
- 🔲 GetRefreshToken
- 🔲 ReadFullPathFile
- 🔲 ReadPlainFile

### Phase 4: Core 클래스 테스트 (복잡)
- 🔲 FirstWork
- 🔲 MainWork
- 🔲 OrderCaller

### Phase 5: AgentFunction 구현체 테스트
- 🔲 HelloFunc
- 🔲 DownloadNUnzipFunc
- 🔲 JmxStatFunc
- 🔲 SSLCertiFunc
- 🔲 SSLCertiFileFunc
- 🔲 SuckSyperFunc

### Phase 6: Kafka 테스트 (통합 테스트 필요)
- 🔲 MwProducer
- 🔲 MwConsumerThread
- 🔲 MwHealthCheckThread

---

## 테스트 작성 가이드라인

### 1. Unit Test 작성 원칙
```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {

    @Mock
    private Dependency dependency;

    private MyService service;

    @BeforeEach
    void setUp() {
        service = new MyService(dependency);
    }

    @Test
    void methodName_WhenCondition_ShouldExpectedResult() {
        // Given (준비)
        when(dependency.method()).thenReturn(value);

        // When (실행)
        Result result = service.method();

        // Then (검증)
        assertThat(result).isNotNull();
        verify(dependency).method();
    }
}
```

### 2. 테스트하기 어려운 코드 개선

#### Before: 테스트 불가능 (static method 직접 호출)
```java
public class MyService {
    public void doSomething() {
        Config config = Config.getInstance();  // Singleton
        String value = config.getValue();
        Common.httpPOST(url, token, body);     // Static method
    }
}
```

#### After: 테스트 가능 (의존성 주입)
```java
public class MyService {
    private final ConfigProvider config;
    private final HttpClient httpClient;

    public MyService(ConfigProvider config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    public void doSomething() {
        String value = config.getValue();
        httpClient.post(url, token, body);
    }
}
```

### 3. 커버리지 목표 설정

#### 단계별 목표
```
현재:   20% (line), 15% (branch)
1개월:  40% (line), 30% (branch)
2개월:  60% (line), 50% (branch)
3개월:  70% (line), 60% (branch) ⬅ 최종 목표
```

#### 클래스별 우선순위
1. **신규 리팩토링 코드**: 90% 이상
2. **보안 취약점 클래스**: 80% 이상
3. **핵심 비즈니스 로직**: 70% 이상
4. **유틸리티/VO**: 50% 이상
5. **Thread/Main**: 제외 가능

---

## 커버리지 리포트 예시

### 좋은 커버리지 예시
```
Package: mwmanger.vo
├── AgentStatus.java          95% (19/20 lines)
├── RegistrationRequest.java  100% (12/12 lines)
└── RegistrationResponse.java 100% (15/15 lines)
```

### 개선 필요한 예시
```
Package: mwmanger.order
├── ExeShell.java            20% (15/75 lines) ⚠️
├── ExeScript.java           18% (12/68 lines) ⚠️
└── DownloadFile.java        10% (8/80 lines)  🔴
```

---

## CI/CD 통합

### GitHub Actions 예시
```yaml
name: Test Coverage

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 1.8
        uses: actions/setup-java@v2
        with:
          java-version: '8'
          distribution: 'adopt'

      - name: Run tests with coverage
        run: mvn clean test

      - name: Generate coverage report
        run: mvn jacoco:report

      - name: Check coverage threshold
        run: mvn jacoco:check

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v2
        with:
          file: ./target/site/jacoco/jacoco.xml
```

---

## 다음 단계

### 즉시 실행 가능
1. `mvn test` 또는 `gradle test` 실행
2. HTML 리포트 열어서 현재 커버리지 확인
3. 커버되지 않은 클래스 확인
4. 우선순위에 따라 테스트 작성

### 이번 주 목표
- [ ] Phase 1 완료: Registration 모듈 테스트 (90% 이상)
- [ ] Phase 2 시작: VO 클래스 테스트 추가
- [ ] 전체 커버리지 30% 달성

### 이번 달 목표
- [ ] Phase 3 완료: Order 구현체 테스트 (보안 취약점 클래스)
- [ ] 전체 커버리지 50% 달성

---

**참고 자료**
- [JaCoCo 공식 문서](https://www.jacoco.org/jacoco/trunk/doc/)
- [TESTING.md](./TESTING.md) - 기본 테스트 가이드
- [REFACTORING_PLAN.md](./REFACTORING_PLAN.md) - Phase 5 테스트 계획
