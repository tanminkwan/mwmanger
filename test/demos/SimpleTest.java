import mwmanger.vo.CommandVO;
import mwmanger.vo.ResultVO;
import mwmanger.common.Common;

/**
 * 간단한 수동 테스트 실행기
 * Maven/Gradle 없이 테스트 가능
 */
public class SimpleTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  MwManger Agent - Simple Test Runner");
        System.out.println("========================================");
        System.out.println();

        // VO 테스트
        testCommandVO();
        testResultVO();

        // Common 유틸리티 테스트
        testCommonEscape();
        testCommonFillResult();

        // 결과 출력
        System.out.println();
        System.out.println("========================================");
        System.out.println("  Test Results");
        System.out.println("========================================");
        System.out.println("Passed: " + testsPassed);
        System.out.println("Failed: " + testsFailed);
        System.out.println("Total:  " + (testsPassed + testsFailed));
        System.out.println();

        if (testsFailed == 0) {
            System.out.println("✓ ALL TESTS PASSED!");
        } else {
            System.out.println("✗ SOME TESTS FAILED!");
            System.exit(1);
        }
    }

    // CommandVO 테스트
    private static void testCommandVO() {
        System.out.println("[TEST] CommandVO getter/setter");
        try {
            CommandVO cmd = new CommandVO();
            cmd.setCommandId("CMD-123");
            cmd.setHostName("server01");
            cmd.setTargetFileName("test.sh");

            assert cmd.getCommandId().equals("CMD-123") : "CommandId 불일치";
            assert cmd.getHostName().equals("server01") : "HostName 불일치";
            assert cmd.getTargetFileName().equals("test.sh") : "TargetFileName 불일치";

            pass("CommandVO getter/setter works correctly");
        } catch (AssertionError e) {
            fail("CommandVO getter/setter", e.getMessage());
        } catch (Exception e) {
            fail("CommandVO getter/setter", e.toString());
        }
    }

    // ResultVO 테스트
    private static void testResultVO() {
        System.out.println("[TEST] ResultVO getter/setter");
        try {
            ResultVO result = new ResultVO();
            result.setResult("Success");
            result.setHostName("server01");
            result.setOk(true);

            assert result.getResult().equals("Success") : "Result 불일치";
            assert result.getHostName().equals("server01") : "HostName 불일치";
            assert result.isOk() == true : "isOk 불일치";

            pass("ResultVO getter/setter works correctly");
        } catch (AssertionError e) {
            fail("ResultVO getter/setter", e.getMessage());
        } catch (Exception e) {
            fail("ResultVO getter/setter", e.toString());
        }
    }

    // Common.escape() 테스트
    private static void testCommonEscape() {
        System.out.println("[TEST] Common.escape()");
        try {
            // 백슬래시 테스트
            String result = Common.escape("path\\to\\file");
            assert result.equals("path\\\\to\\\\file") : "백슬래시 escape 실패";

            // 따옴표 테스트
            result = Common.escape("say \"hello\"");
            assert result.equals("say \\\"hello\\\"") : "따옴표 escape 실패";

            // 개행 테스트
            result = Common.escape("line1\nline2");
            assert result.equals("line1\\nline2") : "개행 escape 실패";

            // 탭 테스트
            result = Common.escape("col1\tcol2");
            assert result.equals("col1\\tcol2") : "탭 escape 실패";

            // 빈 문자열 테스트
            result = Common.escape("");
            assert result.equals("") : "빈 문자열 escape 실패";

            // null 테스트
            result = Common.escape(null);
            assert result == null : "null escape 실패";

            pass("Common.escape() handles all special characters correctly");
        } catch (AssertionError e) {
            fail("Common.escape()", e.getMessage());
        } catch (Exception e) {
            fail("Common.escape()", e.toString());
        }
    }

    // Common.fillResult() 테스트
    private static void testCommonFillResult() {
        System.out.println("[TEST] Common.fillResult()");
        try {
            ResultVO result = new ResultVO();
            result.setResult("test result");

            CommandVO command = new CommandVO();
            command.setHostName("server01");
            command.setTargetFileName("test.txt");
            command.setTargetFilePath("/data/");

            ResultVO filled = Common.fillResult(result, command);

            assert filled.getHostName().equals("server01") : "HostName 불일치";
            assert filled.getTargetFileName().equals("test.txt") : "TargetFileName 불일치";
            assert filled.getTargetFilePath().equals("/data/") : "TargetFilePath 불일치";
            assert filled.getResult().equals("test result") : "Result 불일치";

            pass("Common.fillResult() fills result correctly");
        } catch (AssertionError e) {
            fail("Common.fillResult()", e.getMessage());
        } catch (Exception e) {
            fail("Common.fillResult()", e.toString());
        }
    }

    // 테스트 통과
    private static void pass(String message) {
        System.out.println("  ✓ PASS: " + message);
        testsPassed++;
    }

    // 테스트 실패
    private static void fail(String testName, String reason) {
        System.out.println("  ✗ FAIL: " + testName);
        System.out.println("    Reason: " + reason);
        testsFailed++;
    }
}
