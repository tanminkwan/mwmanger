# Work History - MwManger Agent

## 2025-11-20 (오늘 작업)

### 작업 브랜치
- `refactoring_major_202511` 브랜치에서 작업

### 완료된 작업

#### 1. 빌드 및 실행 문제 해결
- ✅ build-offline.sh 실행 후 JAR 실행 방법 파악
- ✅ RedHat Linux에서 classpath 이슈 해결
  - 문제: `java -cp ".:lib/*" mwmanger.MwAgent` 실패
  - 원인: `.`은 클래스 파일만 참조, JAR 파일은 명시 필요
  - 해결: `java -cp "build/jar/mwmanger-0000.0009.0001.jar:lib/*" mwmanger.MwAgent`

#### 2. 등록(Registration) 모듈화 완료 🎉
**목표**: 최초 실행 시 가입(register) 단계를 모듈화

**Before**: PreWork.java (150줄)
- 모든 로직이 한 클래스에 집중
- 테스트 불가능, 재사용 불가능
- Config 싱글톤에 강하게 결합

**After**: 모듈화된 구조 (6개 클래스)
- ✅ `AgentStatus.java` - 상태 enum (type-safe)
  - NOT_REGISTERED, PENDING_APPROVAL, APPROVED, etc.
  - 매직 넘버(-1, -2) 제거
- ✅ `RegistrationRequest.java` - 등록 요청 VO
- ✅ `RegistrationResponse.java` - 등록 응답 VO
- ✅ `RegistrationService.java` - Agent 등록 로직
- ✅ `AgentStatusService.java` - Agent 상태 확인
- ✅ `BootstrapService.java` - 전체 등록 프로세스 관리
- ✅ `PreWork.java` 리팩토링 (150줄 → 40줄, 73% 감소)

**구조**:
```
src/main/java/mwmanger/
├── service/registration/
│   ├── BootstrapService.java          # 전체 등록 프로세스 조율
│   ├── RegistrationService.java       # Agent 등록만 담당
│   └── AgentStatusService.java        # Agent 상태 확인만 담당
├── vo/
│   ├── AgentStatus.java               # 상태 enum
│   ├── RegistrationRequest.java       # 등록 요청
│   └── RegistrationResponse.java      # 등록 응답
└── PreWork.java (40줄)                 # 단순한 wrapper
```

**개선 효과**:
1. 단일 책임 원칙 (SRP) 준수
2. 테스트 용이성 (DI constructor 제공)
3. 재사용성 향상
4. 타입 안정성 (enum 사용)
5. 코드 가독성 대폭 향상

#### 3. 빌드 및 검증
- ✅ `build-offline.bat` 실행 성공
- ✅ 새로운 모듈 JAR에 포함 확인
  - mwmanger/service/registration/*.class
  - mwmanger/vo/AgentStatus.class
  - mwmanger/vo/RegistrationRequest.class
  - mwmanger/vo/RegistrationResponse.class

### 현재 상태

#### Git 상태
- 브랜치: `refactoring_major_202511`
- 변경된 파일:
  - Modified: `src/main/java/mwmanger/PreWork.java`
  - New: `src/main/java/mwmanger/service/registration/*.java` (3 files)
  - New: `src/main/java/mwmanger/vo/AgentStatus.java`
  - New: `src/main/java/mwmanger/vo/RegistrationRequest.java`
  - New: `src/main/java/mwmanger/vo/RegistrationResponse.java`
- 커밋 필요: Yes

### 다음 작업 (Phase 2: Critical 보안 취약점 수정)

REFACTORING_PLAN.md의 Phase 2를 진행해야 합니다:

#### 우선순위 CRITICAL
1. [ ] **Command Injection 수정** (ExeShell.java:50)
   - ProcessBuilder 사용으로 전환
   - Command Whitelist 구현
   - ExeScript.java, ExeText.java도 동일 적용

2. [ ] **Path Traversal 수정** (DownloadFile.java, ReadFile.java)
   - PathValidator 구현
   - Canonical path 검증

3. [ ] **토큰 로깅 제거** (Common.java:268, 317)
   - refresh_token, access_token 로깅 삭제
   - 민감 정보 노출 방지

4. [ ] **동시성 버그 수정**
   - MwConsumerThread.java:83 - 논리 연산자 수정
   - SuckSyperFunc.java:63 - null 체크 수정

#### 참고 문서
- `REFACTORING_PLAN.md` - 전체 리팩토링 계획
- Phase 2 상세 내용: REFACTORING_PLAN.md:132-257

---

## 2025-01-23 (이전 작업)

### 작업 브랜치
- `refectoring_202511` 브랜치에서 작업

### 완료된 작업

#### 1. JDK 1.8 호환성 설정
- ✅ `pom.xml` 생성 - Maven 빌드 설정 (JDK 1.8 타겟)
- ✅ `build.gradle` 생성 - Gradle 빌드 설정
- ✅ 모든 필수 라이브러리 의존성 정의
  - Apache HttpClient 4.5.14
  - Apache Kafka Client 2.8.2
  - BouncyCastle 1.70 (AIX용)
  - JSON Simple 1.1.1
  - Commons Codec 1.15
  - SLF4J 1.7.36

#### 2. 테스트 환경 구축
- ✅ `src/test/java/` 디렉토리 구조 생성
- ✅ JUnit 5, Mockito, AssertJ 의존성 추가
- ✅ 테스트 코드 작성:
  - `CommandVOTest.java` - CommandVO 테스트
  - `ResultVOTest.java` - ResultVO 테스트
  - `CommonTest.java` - Common 유틸리티 테스트
  - `OrderTest.java` - Order 추상 클래스 테스트
  - `AgentFuncFactoryTest.java` - Factory 패턴 테스트
- ✅ `test-agent.properties` - 테스트용 설정 파일

#### 3. 문서 작성
- ✅ `DEPENDENCIES.md` - 상세 의존성 문서
  - 각 라이브러리 용도 및 버전
  - JDK 1.8 호환성 확인
  - 빌드 및 실행 방법
- ✅ `TESTING.md` - 테스트 가이드
  - 테스트 실행 방법
  - 테스트 작성 가이드라인
  - CI/CD 통합 예시
- ✅ `README.md` 업데이트
  - 시스템 요구사항 추가
  - 필수 라이브러리 섹션 추가
  - 빌드 방법 추가
  - 테스트 섹션 추가
- ✅ `README_TESTS.md` - 테스트 상세 문서

#### 4. 빌드 및 테스트 스크립트
- ✅ `run-tests.bat` - Windows용 테스트 실행 스크립트
- ✅ `run-tests.sh` - Linux/Mac용 테스트 실행 스크립트

#### 5. 데모 및 예제
- ✅ `TestDataDemo.java` - 테스트 데이터 개념 설명 프로그램
- ✅ `DirectTest.java` - 한글 버전 데모
- ✅ `QuickTest.java` - 간단한 테스트 실행기

#### 6. Git 설정
- ✅ `.gitignore` 업데이트
  - Maven/Gradle 빌드 결과물 제외
  - IDE 설정 파일 제외
  - `.claude/` 디렉토리 제외

### 현재 상태

#### 커밋 상태
- 브랜치: `refectoring_202511`
- 상태: 변경사항 staged 안 됨
- Push 상태: 브랜치는 origin에 있음

#### 테스트 상태
- ✅ 테스트 코드 작성 완료
- ✅ 테스트 데이터 개념 이해
- ✅ 간단한 Java 테스트 실행 성공
- ⚠️ Maven/Gradle로 정식 테스트는 미실행 (도구 미설치)

### 다음 작업 TODO

#### 우선순위 높음
- [ ] 현재 변경사항 커밋
- [ ] 불필요한 데모 파일 정리 (TestDataDemo.java 등)
- [ ] Maven 설치 후 `mvn test` 실행해서 모든 테스트 검증

#### 우선순위 중간
- [ ] 추가 테스트 작성
  - PreWork 클래스 테스트
  - MainWork 클래스 테스트
  - 개별 Order 구현체 테스트
  - 개별 AgentFunc 구현체 테스트
- [ ] 통합 테스트 작성 (Kafka, HTTP)
- [ ] 테스트 커버리지 리포트 생성

#### 우선순위 낮음
- [ ] CI/CD 파이프라인 설정 (GitHub Actions)
- [ ] Jacoco 코드 커버리지 설정
- [ ] SonarQube 정적 분석 설정

### 주요 파일 목록

```
mwmanger/
├── pom.xml                          # Maven 빌드 설정
├── build.gradle                     # Gradle 빌드 설정
├── .gitignore                       # Git 제외 설정
├── README.md                        # 프로젝트 메인 문서
├── DEPENDENCIES.md                  # 의존성 상세 문서
├── TESTING.md                       # 테스트 가이드
├── WORK_HISTORY.md                  # 이 파일
├── run-tests.bat                    # Windows 테스트 스크립트
├── run-tests.sh                     # Linux/Mac 테스트 스크립트
└── src/
    └── test/
        ├── java/mwmanger/
        │   ├── vo/
        │   │   ├── CommandVOTest.java
        │   │   └── ResultVOTest.java
        │   ├── common/
        │   │   └── CommonTest.java
        │   ├── order/
        │   │   └── OrderTest.java
        │   ├── agentfunction/
        │   │   └── AgentFuncFactoryTest.java
        │   └── README_TESTS.md
        └── resources/
            └── test-agent.properties
```

### 중요 결정사항

1. **JDK 1.8 호환**: 모든 라이브러리 JDK 1.8 호환 버전 선택
2. **테스트 프레임워크**: JUnit 5 + Mockito + AssertJ 조합
3. **빌드 도구**: Maven과 Gradle 둘 다 지원
4. **테스트 데이터**: 간단하고 예측 가능한 값 사용 (예: "CMD-123", "server01")

### BouncyCastle 사용처

질문이 있었던 부분:
- `Common.java:70` - AIX에서만 TLS 1.2 Security Provider로 사용
- `SSLCertiFunc.java:132` - SSL 인증서 확인 시 AIX에서만 사용
- **결론**: AIX 환경에서만 필수, 다른 OS에서는 사용 안 함

### 학습한 내용

**테스트 데이터란?**
- 테스트할 때 사용하는 가짜 입력값
- 실제 프로덕션 데이터 대신 사용
- 간단하고 예측 가능하며 안전함
- 예시: "CMD-123" (간단) vs "CMD-2025-01-23-0001" (실제)

---

**Last Updated**: 2025-01-23
**Branch**: refectoring_202511
**Status**: Work in progress
