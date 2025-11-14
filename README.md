# MwManger - Leebalso Agent

MwManger는 Leebalso(리발소) 프로젝트의 에이전트 프로그램으로, 각 서버에서 데몬으로 실행되면서 중앙 Leebalso 서버로부터 명령을 전달받아 수행하는 Java 기반 원격 관리 에이전트입니다.

## 목차
- [프로젝트 개요](#프로젝트-개요)
- [주요 특징](#주요-특징)
- [시스템 요구사항](#시스템-요구사항)
- [필수 라이브러리](#필수-라이브러리)
- [빌드 방법](#빌드-방법)
- [시스템 아키텍처](#시스템-아키텍처)
- [프로젝트 구조](#프로젝트-구조)
- [설정 방법](#설정-방법)
- [실행 방법](#실행-방법)
- [실행 흐름](#실행-흐름)
- [명령 처리 방식](#명령-처리-방식)
- [통신 방식](#통신-방식)
- [지원 운영체제](#지원-운영체제)

## 프로젝트 개요

MwManger는 분산 환경의 서버 관리를 자동화하기 위한 에이전트 프로그램입니다. 중앙 Leebalso 서버의 지시에 따라 다양한 작업을 수행하며, 실시간 명령 수신 및 결과 전송을 지원합니다.

**버전**: 0000.0008.0005
**타입**: JAVAAGENT

## 시스템 요구사항

- **JDK**: 1.8 (Java 8) 이상
- **메모리**: 최소 256MB
- **디스크**: 최소 100MB
- **네트워크**: Leebalso 서버 및 Kafka 브로커 접근 가능

## 필수 라이브러리

프로젝트는 다음 외부 라이브러리에 의존합니다 (JDK 1.8 호환):

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| Apache HttpClient | 4.5.14 | HTTP/HTTPS 통신 |
| Apache Kafka Client | 2.8.2 | Kafka 메시징 |
| BouncyCastle | 1.70 | TLS 1.2 지원 (AIX) |
| JSON Simple | 1.1.1 | JSON 처리 |
| Apache Commons Codec | 1.15 | 인코딩 유틸리티 |
| SLF4J | 1.7.36 | 로깅 (Kafka 의존성) |

자세한 의존성 정보는 [DEPENDENCIES.md](DEPENDENCIES.md) 참조

## 빌드 방법

### Maven 사용

```bash
# 의존성 포함 실행 가능 JAR 생성
mvn clean package

# 생성된 파일
target/mwmanger-0000.0008.0005-jar-with-dependencies.jar
```

### Gradle 사용

```bash
# Fat JAR 생성
gradle fatJar

# 생성된 파일
build/libs/mwmanger-all-0000.0008.0005.jar
```

## 주요 특징

- **자동 에이전트 등록**: 최초 실행 시 중앙 서버에 자동 등록
- **다중 통신 채널**: HTTP/HTTPS 및 Apache Kafka를 통한 명령 수신
- **비동기 명령 처리**: ThreadPool을 이용한 동시 다발적 명령 처리
- **보안 통신**: TLS 1.2 지원, JWT 기반 인증(Access Token, Refresh Token)
- **자동 토큰 갱신**: Access Token 만료 시 자동 갱신
- **확장 가능한 구조**: 플러그인 방식의 Order 및 AgentFunction 추가 가능
- **크로스 플랫폼**: Windows, Linux, AIX, HP-UX 지원

## 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                   Leebalso Server                       │
│  (Central Command Server + Kafka Broker)                │
└────────────────┬────────────────────┬───────────────────┘
                 │                    │
         HTTP/HTTPS (Polling)    Kafka (Push)
                 │                    │
┌────────────────┴────────────────────┴───────────────────┐
│                    MwManger Agent                        │
│  ┌────────────┐  ┌────────────┐  ┌────────────────┐   │
│  │  PreWork   │→ │ FirstWork  │→ │   MainWork     │   │
│  │ (등록/승인) │  │ (초기화)    │  │ (명령 수신/처리)│   │
│  └────────────┘  └────────────┘  └────────────────┘   │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Command Receivers                                │  │
│  │  - HTTP Poller (MainWork.suckCommands)           │  │
│  │  - Kafka Consumer (MwConsumerThread)             │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Order Processor (OrderCallerThread Pool)        │  │
│  │  ┌────────────┐  ┌────────────┐  ┌───────────┐  │  │
│  │  │ExeShell    │  │ReadFile    │  │ExeAgentFunc│ │  │
│  │  │DownloadFile│  │ExeScript   │  │ExeText    │  │  │
│  │  └────────────┘  └────────────┘  └───────────┘  │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Agent Functions                                  │  │
│  │  - JmxStatFunc (JMX 통계)                         │  │
│  │  - SSLCertiFunc (SSL 인증서)                      │  │
│  │  - DownloadNUnzipFunc (파일 다운로드/압축해제)     │  │
│  │  - SuckSyperFunc (Syper 데이터 수집)              │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Result Senders                                   │  │
│  │  - HTTP POST (Common.httpPOST)                   │  │
│  │  - Kafka Producer (MwProducer)                   │  │
│  └──────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

## 프로젝트 구조

```
mwmanger/
├── MwAgent.java                 # 메인 진입점
├── PreWork.java                 # 에이전트 등록 및 승인 처리
├── FirstWork.java               # Kafka 연결 등 초기화
├── MainWork.java                # 메인 루프 (명령 수신 및 처리)
├── OrderCallerThread.java       # 명령 실행 스레드
├── ShutdownThread.java          # 종료 처리
│
├── common/
│   ├── Config.java              # 설정 관리 (Singleton)
│   └── Common.java              # HTTP 통신 유틸리티
│
├── order/                       # 명령 실행 모듈
│   ├── Order.java               # 추상 Order 클래스
│   ├── OrderCaller.java         # Order 동적 로딩 및 실행
│   ├── ExeShell.java            # 쉘 스크립트 실행
│   ├── ExeScript.java           # 스크립트 실행
│   ├── ExeText.java             # 텍스트 실행
│   ├── ExeAgentFunc.java        # Agent Function 실행
│   ├── ReadFile.java            # 파일 읽기 (추상)
│   ├── ReadPlainFile.java       # 일반 파일 읽기
│   ├── ReadFullPathFile.java   # 전체 경로 파일 읽기
│   ├── DownloadFile.java        # 파일 다운로드
│   └── GetRefreshToken.java     # Refresh Token 갱신
│
├── agentfunction/               # 에이전트 기능 모듈
│   ├── AgentFunc.java           # AgentFunc 인터페이스
│   ├── AgentFuncFactory.java    # Factory 패턴
│   ├── HelloFunc.java           # Hello World 예제
│   ├── JmxStatFunc.java         # JMX 통계 수집
│   ├── SSLCertiFunc.java        # SSL 인증서 정보
│   ├── SSLCertiFileFunc.java    # SSL 인증서 파일
│   ├── DownloadNUnzipFunc.java  # 파일 다운로드 및 압축해제
│   └── SuckSyperFunc.java       # Syper 데이터 수집
│
├── kafka/
│   ├── MwConsumerThread.java    # Kafka Consumer (명령 수신)
│   ├── MwProducer.java          # Kafka Producer (결과 전송)
│   └── MwHealthCheckThread.java # Kafka 상태 모니터링
│
└── vo/
    ├── CommandVO.java           # 명령 VO
    ├── ResultVO.java            # 결과 VO
    ├── RawCommandsVO.java       # 원시 명령 VO
    └── MwResponseVO.java        # HTTP 응답 VO
```

## 설정 방법

### agent.properties 파일 생성

에이전트 루트 디렉토리에 `agent.properties` 파일을 생성합니다:

```properties
# 서버 설정
server_url=https://leebalso-server.example.com
get_command_uri=/api/v1/command/getCommands
post_agent_uri=/api/v1/agent

# 인증 토큰 (Refresh Token)
token=YOUR_REFRESH_TOKEN_HERE

# 명령 확인 주기 (초 단위)
command_check_cycle=60

# Kafka 브로커 주소 (선택 사항, BOOT 명령으로 동적 설정 가능)
kafka_broker_address=kafka-broker.example.com:9092

# 호스트 및 사용자 식별 환경 변수
host_name_var=HOSTNAME
user_name_var=USER

# 로그 설정
log_dir=/var/log/mwagent
log_level=INFO
```

### 설정 항목 설명

- **server_url**: Leebalso 중앙 서버 URL
- **get_command_uri**: 명령 조회 API 엔드포인트
- **post_agent_uri**: 에이전트 등록 API 엔드포인트
- **token**: 인증용 Refresh Token (최초 발급 필요)
- **command_check_cycle**: 명령 폴링 주기 (초)
- **kafka_broker_address**: Kafka 브로커 주소 (옵션)
- **host_name_var**: 호스트명을 가져올 환경 변수명
- **user_name_var**: 사용자명을 가져올 환경 변수명
- **log_dir**: 로그 파일 저장 경로
- **log_level**: 로그 레벨 (SEVERE, WARNING, INFO, FINE, FINEST)

## 실행 방법

### Fat JAR 실행 (권장)

Maven 또는 Gradle로 빌드한 경우:

```bash
# Maven으로 빌드한 경우
java -jar target/mwmanger-0000.0008.0005-jar-with-dependencies.jar

# Gradle으로 빌드한 경우
java -jar build/libs/mwmanger-all-0000.0008.0005.jar
```

### Classpath 직접 지정

```bash
java -cp ".:lib/*" mwmanger.MwAgent
```

### 백그라운드 실행

```bash
# Fat JAR 실행
nohup java -jar mwmanger-all.jar > /dev/null 2>&1 &

# Classpath 지정 실행
nohup java -cp ".:lib/*" mwmanger.MwAgent > /dev/null 2>&1 &
```

### 서비스 등록 (systemd 예제)

`/etc/systemd/system/mwagent.service`:

```ini
[Unit]
Description=MwManger Agent Service
After=network.target

[Service]
Type=simple
User=mwagent
WorkingDirectory=/opt/mwagent
# Fat JAR 실행 (권장)
ExecStart=/usr/bin/java -jar /opt/mwagent/mwmanger-all.jar
# 또는 Classpath 지정
# ExecStart=/usr/bin/java -cp ".:lib/*" mwmanger.MwAgent
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable mwagent
sudo systemctl start mwagent
sudo systemctl status mwagent
```

## 실행 흐름

### 1. PreWork 단계 (PreWork.java)

1. **noticeStart()**: 서버에 시작 알림 및 BOOT 명령 수신
2. **에이전트 등록 상태 확인**:
   - 등록되지 않음 (-1): `registerMe()` 호출하여 자동 등록
   - 승인 대기 중 (-2): 승인될 때까지 대기
   - 정상 (>0): 다음 단계로 진행

### 2. FirstWork 단계 (FirstWork.java)

1. **BOOT 명령 처리**:
   - Kafka 브로커 주소 설정
   - Kafka Consumer 스레드 시작 (명령 수신)
   - Kafka Health Check 스레드 시작
   - Kafka Producer 초기화 (결과 전송)
2. **Refresh Token 적용**: `applyRefreshToken()`

### 3. MainWork 단계 (MainWork.java)

무한 루프로 다음 작업 수행:

1. **suckCommands()**: HTTP GET으로 서버에서 명령 조회
2. **Access Token 만료 확인**: 401 응답 시 자동 갱신
3. **명령 처리**:
   - 각 명령의 `command_class` 추출
   - `OrderCallerThread` 생성 및 ExecutorService로 비동기 실행
4. **대기**: `command_check_cycle` 시간만큼 Sleep

### 4. 명령 실행 (OrderCallerThread.java)

1. **OrderCaller.executeOrder()**:
   - Reflection으로 Order 클래스 동적 로딩
   - Order 객체 생성 및 명령 전달
   - `execute()` 메서드 실행
   - 결과 전송: `sendResults()`

## 명령 처리 방식

### Order 클래스 구조

모든 명령은 `Order` 추상 클래스를 상속하며, 다음 메서드를 구현합니다:

- **convertCommand(JSONObject)**: JSON 명령을 CommandVO로 변환
- **execute()**: 명령 실행 로직 (추상 메서드)
- **sendResults()**: 결과를 서버 또는 Kafka로 전송

### 명령 타입 (Order 구현체)

| 클래스 | 설명 |
|--------|------|
| **ExeShell** | 쉘 스크립트 실행 (bash, ksh, cmd) |
| **ExeScript** | 스크립트 파일 실행 |
| **ExeText** | 텍스트 형식 명령 실행 |
| **ExeAgentFunc** | AgentFunction 호출 (플러그인 방식) |
| **ReadFile** | 파일 내용 읽기 |
| **ReadPlainFile** | 일반 파일 읽기 |
| **ReadFullPathFile** | 전체 경로로 파일 읽기 |
| **DownloadFile** | 서버에서 파일 다운로드 |
| **GetRefreshToken** | Refresh Token 갱신 |

### Agent Function (플러그인)

`ExeAgentFunc` Order를 통해 호출되는 확장 기능:

| 함수 | 설명 |
|------|------|
| **HelloFunc** | 테스트용 Hello World |
| **JmxStatFunc** | JMX 통계 정보 수집 |
| **SSLCertiFunc** | SSL 인증서 정보 조회 |
| **SSLCertiFileFunc** | SSL 인증서 파일 읽기 |
| **DownloadNUnzipFunc** | 파일 다운로드 및 압축 해제 |
| **SuckSyperFunc** | Syper 시스템 데이터 수집 |

### 명령 JSON 형식 예제

```json
{
  "command_id": "CMD-12345",
  "command_class": "ExeShell",
  "repetition_seq": 1,
  "target_file_name": "backup.sh",
  "target_file_path": "/scripts/",
  "additional_params": "-v --force",
  "result_receiver": "SERVER",
  "target_object": "t_backup_results",
  "result_hash": ""
}
```

### 파라미터 치환 기능

명령의 `target_file_path`, `target_file_name`, `additional_params`에서 환경 변수를 참조할 수 있습니다:

```
<<JAVA_HOME>>/bin/java
→ /usr/lib/jvm/java-11/bin/java
```

## 통신 방식

### 1. HTTP/HTTPS 통신

**인증 방식**: Bearer Token (JWT)

#### Access Token & Refresh Token

- **Refresh Token**: 장기 유효, `agent.properties`에 저장
- **Access Token**: 단기 유효, Refresh Token으로 자동 갱신
- 401 응답 시 자동으로 `Common.updateToken()` 호출

#### API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/v1/security/refresh` | Access Token 갱신 |
| POST | `/api/v1/agent` | 에이전트 등록 |
| GET | `/api/v1/command/getCommands/{agent_id}` | 명령 조회 (폴링) |
| GET | `/api/v1/command/getCommands/{agent_id}/{version}/{type}/BOOT` | 시작 알림 및 BOOT 명령 |
| GET | `/api/v1/agent/getRefreshToken/{agent_id}` | Refresh Token 조회 |
| POST | `/api/v1/command/result` | 명령 실행 결과 전송 |

#### TLS 설정

- TLS 1.2 프로토콜 사용
- 모든 인증서 신뢰 (Self-signed 포함)
- 호스트명 검증 비활성화
- AIX에서는 BouncyCastle Security Provider 사용

### 2. Kafka 통신

#### Consumer (명령 수신)

- **Topic**: `t_{agent_id}` (예: `t_server01_user01_J`)
- **Group ID**: `g_{agent_id}`
- 메시지 형식: JSON (command 객체)

#### Producer (결과 전송)

- **Topic**: 명령의 `target_object` 필드 값
- **Key**: `agent_id`
- 메시지 형식: JSON (result 객체)

#### Health Check

- **Topic**: `t_agent_health`
- 에이전트 상태 모니터링용

### 결과 전송 방식 선택

`result_receiver` 필드로 제어:

- **SERVER**: HTTP POST로만 전송
- **KAFKA**: Kafka로만 전송
- **SERVER_N_KAFKA**: 양쪽 모두 전송

## 지원 운영체제

| OS | 쉘 | 확인됨 |
|----|-----|--------|
| **Windows** | cmd | ✓ |
| **Linux** | bash | ✓ |
| **AIX** | ksh + BouncyCastle | ✓ |
| **HP-UX** | ksh | ✓ |

## Agent ID 생성 규칙

```
{hostname}_{username}_J
```

예: `server01_deploy_J` (J는 JAVAAGENT를 의미)

## 로그

로그 파일은 `log_dir` 설정 경로에 생성됩니다:

```
mwagent.0.0.log
mwagent.0.1.log
...
```

- 로그 파일 크기: 1MB
- 최대 파일 수: 10개 (순환)

## 종료 처리

`ShutdownThread`가 JVM 종료 Hook으로 등록되어 있어 Graceful Shutdown을 보장합니다.

## 보안 고려사항

1. **Refresh Token 보호**: `agent.properties` 파일 권한을 600으로 설정
2. **HTTPS 사용**: 중앙 서버와의 통신 시 HTTPS 권장
3. **Kafka 보안**: 필요 시 SASL/SSL 설정 추가
4. **명령 검증**: 악의적인 명령 실행 방지를 위한 화이트리스트 관리 권장

## 확장 방법

### 새로운 Order 추가

1. `order/` 디렉토리에 `Order` 클래스 상속
2. `execute()` 메서드 구현
3. 서버에서 해당 클래스명으로 명령 전송

예:
```java
package mwmanger.order;

public class CustomOrder extends Order {
    public CustomOrder(JSONObject command) {
        super(command);
    }

    public int execute() {
        // 구현
        return 1;
    }
}
```

### 새로운 AgentFunc 추가

1. `agentfunction/` 디렉토리에 `AgentFunc` 인터페이스 구현
2. `AgentFuncFactory`에 case 추가
3. `ExeAgentFunc` Order로 호출

## 테스트

### 단위 테스트 실행

프로젝트는 JUnit 5 기반의 단위 테스트를 포함합니다:

```bash
# Maven으로 테스트 실행
mvn test

# Gradle로 테스트 실행
gradle test
```

### 테스트 커버리지

- **VO 클래스**: CommandVO, ResultVO
- **유틸리티**: Common (escape, fillResult, makeOneResultArray)
- **Order 클래스**: 공통 기능 (replaceParam, getHash)
- **Factory 클래스**: AgentFuncFactory

자세한 테스트 정보는 [src/test/java/mwmanger/README_TESTS.md](src/test/java/mwmanger/README_TESTS.md) 참조

### 테스트 프레임워크

- JUnit 5 (5.8.2)
- Mockito (3.12.4)
- AssertJ (3.21.0)

## 문의 및 지원

프로젝트 관련 문의사항이나 이슈는 프로젝트 관리자에게 연락하시기 바랍니다.

---

**Last Updated**: 2025-01-23
**Version**: 0000.0008.0005
**Test Framework**: JUnit 5.8.2
