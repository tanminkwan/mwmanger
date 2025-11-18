/**
 * 패키지 없는 직접 테스트
 * 실제 테스트 데이터 보여주기
 */
public class DirectTest {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  MwManger 테스트 데이터 시연");
        System.out.println("========================================");
        System.out.println();

        // 테스트 데이터 예시 1: 명령 데이터
        System.out.println("[테스트 데이터 1] 명령(Command) 데이터");
        System.out.println("------------------------------------------");
        System.out.println("실제 서버에서 올 때:");
        System.out.println("  {");
        System.out.println("    \"command_id\": \"CMD-2025-01-23-0001\",");
        System.out.println("    \"host_name\": \"prod-server-01\",");
        System.out.println("    \"target_file_name\": \"production_backup.sh\"");
        System.out.println("  }");
        System.out.println();
        System.out.println("테스트할 때는:");
        System.out.println("  {");
        System.out.println("    \"command_id\": \"CMD-123\",         ← 간단하게!");
        System.out.println("    \"host_name\": \"server01\",         ← 짧게!");
        System.out.println("    \"target_file_name\": \"test.sh\"    ← 알아보기 쉽게!");
        System.out.println("  }");
        System.out.println();

        // 테스트 데이터 예시 2: 특수문자 테스트
        System.out.println("[테스트 데이터 2] 특수문자 escape 테스트");
        System.out.println("------------------------------------------");

        String test1 = "path\\to\\file";
        String test2 = "say \"hello\"";
        String test3 = "line1\nline2";
        String test4 = "";
        String test5 = null;

        System.out.println("테스트 케이스 1: \"" + test1 + "\"");
        System.out.println("  → 백슬래시가 있는 경로");
        System.out.println();

        System.out.println("테스트 케이스 2: \"" + test2 + "\"");
        System.out.println("  → 따옴표가 있는 문자열");
        System.out.println();

        System.out.println("테스트 케이스 3: 개행이 있는 문자열");
        System.out.println("  → \"line1\\nline2\"");
        System.out.println();

        System.out.println("테스트 케이스 4: \"" + test4 + "\"");
        System.out.println("  → 빈 문자열 (경계값 테스트)");
        System.out.println();

        System.out.println("테스트 케이스 5: " + test5);
        System.out.println("  → null 값 (예외 케이스)");
        System.out.println();

        // 테스트 데이터 예시 3: 다양한 시나리오
        System.out.println("[테스트 데이터 3] 다양한 시나리오");
        System.out.println("------------------------------------------");
        System.out.println("시나리오 A: 정상 케이스");
        System.out.println("  CommandId: \"CMD-123\"");
        System.out.println("  HostName: \"server01\"");
        System.out.println("  Result: 성공 예상 ✓");
        System.out.println();

        System.out.println("시나리오 B: 빈 값 테스트");
        System.out.println("  CommandId: \"\"");
        System.out.println("  HostName: \"\"");
        System.out.println("  Result: 기본값 확인용");
        System.out.println();

        System.out.println("시나리오 C: null 값 테스트");
        System.out.println("  CommandId: null");
        System.out.println("  HostName: null");
        System.out.println("  Result: null 처리 확인용");
        System.out.println();

        // 실제 테스트 동작 시뮬레이션
        System.out.println("[실제 테스트 동작]");
        System.out.println("------------------------------------------");
        System.out.println("1. Given (준비) - 테스트 데이터 생성");
        System.out.println("   TestData data = new TestData();");
        System.out.println("   data.setCommandId(\"CMD-123\");  ← 이게 테스트 데이터!");
        System.out.println("   data.setHostName(\"server01\");   ← 이것도!");
        System.out.println();

        System.out.println("2. When (실행) - 메서드 호출");
        System.out.println("   String result = data.getCommandId();");
        System.out.println();

        System.out.println("3. Then (검증) - 결과 확인");
        System.out.println("   assert result.equals(\"CMD-123\");");
        System.out.println("   → \"CMD-123\"이 나오면 성공! ✓");
        System.out.println();

        // 실제 값 확인
        System.out.println("========================================");
        System.out.println("  실제 Java 객체로 확인해보기");
        System.out.println("========================================");

        TestCommand cmd = new TestCommand();
        cmd.commandId = "CMD-123";
        cmd.hostName = "server01";
        cmd.targetFile = "test.sh";

        System.out.println("생성된 테스트 데이터:");
        System.out.println("  - commandId: " + cmd.commandId);
        System.out.println("  - hostName: " + cmd.hostName);
        System.out.println("  - targetFile: " + cmd.targetFile);
        System.out.println();

        System.out.println("이 데이터들을 사용해서 테스트합니다!");
        System.out.println();

        System.out.println("========================================");
        System.out.println("  테스트 완료!");
        System.out.println("========================================");
        System.out.println("이제 테스트 데이터가 무엇인지 이해되셨나요?");
        System.out.println("- 테스트 데이터 = 테스트할 때 쓰는 가짜 값");
        System.out.println("- 실제 서버 데이터 대신 우리가 만든 값");
        System.out.println("- 간단하고 예측 가능한 값 사용");
    }

    // 간단한 테스트용 클래스
    static class TestCommand {
        String commandId;
        String hostName;
        String targetFile;
    }
}
