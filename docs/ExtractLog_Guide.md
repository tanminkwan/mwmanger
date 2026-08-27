# ExtractLog 기능 사용자 가이드

`ExtractLog`는 분산 서버 환경에서 특정 로그 파일 내의 지정된 시간대 및 키워드를 포함하는 로그 블록(스택트레이스 포함)을 추출하고, 동일한 메시지가 몇 번 발생했는지 집계해 주는 기능입니다.

## 1. 개요
* **클래스명**: `mwagent.order.ExtractLog`
* **주요 기능**:
  * 특정 시간대(`start` ~ `end`) 필터링
  * 원하는 에러 키워드 다중 필터링 지원 (정규식 지원)
  * Multi-line 로그(예: Java Exception StackTrace)를 하나의 블록으로 인식하여 추출
  * 중복된 로그 블록은 최초 1회만 본문 텍스트를 전송하고 **발생 횟수(count)**를 함께 반환하여 네트워크 페이로드 감소 및 분석 편의 제공

---

## 2. 파라미터 설정 가이드

서버나 카프카(Kafka)를 통해 에이전트(Agent)로 `ExtractLog` 명령을 보낼 때, 아래와 같이 CommandVO 규격에 맞춰 파라미터를 전송해야 합니다.

| 파라미터명 | 설명 | 예시 |
|---|---|---|
| `target_object` | 실행할 클래스명 | `mwagent.order.ExtractLog` |
| `target_file_path` | 로그 파일이 위치한 디렉토리 절대 경로 | `/var/log/app/` |
| `target_file_name` | 타겟 로그 파일명 | `server.log` |
| `additional_params` | 추출 옵션 정보 (JSON 형태의 문자열로 인코딩하여 전송) | (하단 참조) |

### 2.1 `additional_params` 상세 스펙
`additional_params`는 JSON 형식의 데이터를 문자열(String)로 이스케이프 처리하여 전송해야 합니다. 파싱 전 순수 JSON 구조는 다음과 같습니다.

```json
{
  "file": "/var/log/app/server.log",
  "start": "2026.08.25 14:04:39",
  "end": "2026.08.25 14:04:43",
  "dateRegex": "\\[(\\d{4}\\.\\d{2}\\.\\d{2} \\d{2}:\\d{2}:\\d{2})\\](?:\\s*\\[[^\\]]*\\]){1,2}",
  "keywords": ["Exception", "Fail"],
  "abbreviatePrefix": "\tat ",
  "charset": "EUC-KR"
}
```

* **`file`** (선택): 대상 로그 파일의 절대 경로와 파일명이 합쳐진 전체 경로입니다. 이 값이 존재하면 기존 `target_file_path` 및 `target_file_name` 설정값을 무시하고 이 경로의 파일을 읽어옵니다.
* **`start`** (필수): 조회 시작 시각 (형식: `yyyy.MM.dd HH:mm:ss`)
* **`end`** (필수): 조회 종료 시각 (형식: `yyyy.MM.dd HH:mm:ss`)
* **`dateRegex`** (필수): 각 로그 라인의 선두에서 날짜/시간을 추출하기 위한 정규식입니다.
  * **주의사항**: 반드시 날짜/시간을 캡처하는 하나의 **캡처 그룹 `()`**을 포함해야 합니다.
  * 예시: 로그가 `[2026.08.25 14:04:39] [ERROR] [APP-001] 메시지` 형태일 때, 스레드명(`[APP-001]`)과 같이 매번 달라지는 부분을 무시하고 같은 에러 메시지로 묶음 처리(그룹핑)하고 싶다면 정규식을 `\\[(\\d{4}\\.\\d{2}\\.\\d{2} \\d{2}:\\d{2}:\\d{2})\\](?:\\s*\\[[^\\]]*\\]){1,2}` 와 같이 설정하여 타임스탬프 뒤의 최대 두 개의 `[]` 블록까지 매칭시킵니다.
* **`keywords`** (선택): 필터링할 키워드 목록(배열)입니다.
  * 대소문자를 구분하지 않고 검색합니다.
  * 배열 내 하나의 문자열로 정규식 OR 조건을 넣는 것도 가능합니다. (예: `["Exception|Fail"]`)
  * 키워드 조건을 넣지 않거나 빈 배열(`[]`)을 넣으면 지정된 시간 범위 안의 **모든 로그**를 추출합니다.
* **`abbreviatePrefix`** (선택): 반복되는 로그 구문을 축약하기 위한 접두사 문자열입니다. (예: `"\tat "`) 스택트레이스 등 해당 문자열로 시작하는 라인이 연속으로 반복될 경우, 첫 라인과 마지막 라인만 표시하고 중간은 `"..."`으로 축약하여 로그의 길이를 줄입니다.
* **`charset`** (선택): 로그 파일을 읽을 때 사용할 인코딩 방식입니다. (예: `EUC-KR`, `MS949`, `UTF-8` 등). 입력하지 않으면 기본값인 `UTF-8`이 사용됩니다. AIX나 Windows 등에서 인코딩 불일치로 한글이 깨질 경우 이 값을 명시하여 해결할 수 있습니다.

---

## 3. Command Request JSON 예시

API 또는 Kafka 메시지로 명령을 발송할 때의 페이로드 예시입니다. `additional_params`의 겹따옴표(`"`)를 이스케이프(`\"`) 해야 함에 주의하세요.

```json
{
  "command_id": "CMD-LOG-1001",
  "target_object": "mwagent.order.ExtractLog", 
  "target_file_path": "/var/log/app/",
  "target_file_name": "server.log",
  "additional_params": "{\"file\":\"/var/log/app/server.log\",\"start\":\"2026.08.25 14:04:39\",\"end\":\"2026.08.25 14:04:43\",\"dateRegex\":\"\\\\[(\\\\d{4}\\\\.\\\\d{2}\\\\.\\\\d{2} \\\\d{2}:\\\\d{2}:\\\\d{2})\\\\](?:\\\\s*\\\\[[^\\\\]]*\\\\]){1,2}\",\"keywords\":[\"Exception\", \"Fail\"],\"abbreviatePrefix\":\"\\tat \",\"charset\":\"EUC-KR\"}",
  "result_receiver": "SERVER"
}
```

---

## 4. 실행 결과 (Result) 

명령이 정상적으로 수행되면, Agent는 `ResultVO` 객체를 통해 `result_text` 필드에 **JSON Array 문자열** 형식으로 추출 결과를 반환합니다.

### 4.1 반환값(`result_text`) 형식

```json
[
  {
    "text": "[2026.08.25 14:04:39] [ERROR] [Thread-1] ...\njava.lang.NullPointerException\n\tat com.example.MyService.doWork(MyService.java:42)",
    "count": 5
  },
  {
    "text": "[2026.08.25 14:04:41] [ERROR] [Thread-2] ...\njava.sql.SQLException: Access denied for user",
    "count": 2
  }
]
```

* **`text`**: 첫 번째 줄(타임스탬프)부터 스택트레이스 끝까지 이르는 하나의 "로그 블록" 전체 텍스트입니다. (최초 발생한 메시지의 텍스트가 저장됩니다)
* **`count`**: 해당 시간대 안에서 동일한 패턴(타임스탬프 이후 본문 내용 기준)의 에러가 발생한 총 횟수입니다.

> **참고**: 조건에 만족하는 로그가 없거나 에러가 발생한 경우, 결과 배열이 빈 배열 `[]` 로 리턴되거나 "Error: ..." 형태의 문자열이 리턴될 수 있습니다. (예: 파일이 없을 경우 `Error: NoSuchFileException`)
