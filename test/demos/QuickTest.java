import mwmanger.vo.CommandVO;
import mwmanger.vo.ResultVO;

/**
 * 초간단 테스트 (의존성 없음)
 */
public class QuickTest {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  MwManger Agent - Quick Test");
        System.out.println("========================================");
        System.out.println();

        int passed = 0;
        int failed = 0;

        // Test 1: CommandVO
        System.out.println("[TEST 1] CommandVO - 기본 동작");
        try {
            CommandVO cmd = new CommandVO();

            // 테스트 데이터 설정
            cmd.setCommandId("CMD-123");
            cmd.setRepetitionSeq(5L);
            cmd.setHostName("server01");
            cmd.setTargetFileName("test.sh");
            cmd.setTargetFilePath("/scripts/");

            // 검증
            if (!cmd.getCommandId().equals("CMD-123")) {
                throw new Exception("CommandId 불일치!");
            }
            if (cmd.getRepetitionSeq() != 5L) {
                throw new Exception("RepetitionSeq 불일치!");
            }
            if (!cmd.getHostName().equals("server01")) {
                throw new Exception("HostName 불일치!");
            }
            if (!cmd.getTargetFileName().equals("test.sh")) {
                throw new Exception("TargetFileName 불일치!");
            }
            if (!cmd.getTargetFilePath().equals("/scripts/")) {
                throw new Exception("TargetFilePath 불일치!");
            }

            System.out.println("  ✓ PASS - 모든 getter/setter 정상 동작");
            System.out.println("    - CommandId: " + cmd.getCommandId());
            System.out.println("    - HostName: " + cmd.getHostName());
            System.out.println("    - FileName: " + cmd.getTargetFileName());
            passed++;
        } catch (Exception e) {
            System.out.println("  ✗ FAIL - " + e.getMessage());
            failed++;
        }
        System.out.println();

        // Test 2: CommandVO toString
        System.out.println("[TEST 2] CommandVO - toString()");
        try {
            CommandVO cmd = new CommandVO();
            cmd.setCommandId("CMD-456");
            cmd.setHostName("server02");

            String str = cmd.toString();
            if (!str.contains("CMD-456")) {
                throw new Exception("toString에 CommandId 없음!");
            }
            if (!str.contains("server02")) {
                throw new Exception("toString에 HostName 없음!");
            }
            if (!str.contains("CommandVO")) {
                throw new Exception("toString에 클래스명 없음!");
            }

            System.out.println("  ✓ PASS - toString() 정상 동작");
            System.out.println("    Output: " + str);
            passed++;
        } catch (Exception e) {
            System.out.println("  ✗ FAIL - " + e.getMessage());
            failed++;
        }
        System.out.println();

        // Test 3: CommandVO 기본값
        System.out.println("[TEST 3] CommandVO - 기본값 확인");
        try {
            CommandVO cmd = new CommandVO();

            if (!cmd.getCommandId().equals("")) {
                throw new Exception("CommandId 기본값이 빈 문자열이 아님!");
            }
            if (cmd.getRepetitionSeq() != 0L) {
                throw new Exception("RepetitionSeq 기본값이 0이 아님!");
            }
            if (cmd.getHostName() != null) {
                throw new Exception("HostName 기본값이 null이 아님!");
            }

            System.out.println("  ✓ PASS - 기본값 정상");
            System.out.println("    - CommandId: \"\" (빈 문자열)");
            System.out.println("    - RepetitionSeq: 0");
            System.out.println("    - HostName: null");
            passed++;
        } catch (Exception e) {
            System.out.println("  ✗ FAIL - " + e.getMessage());
            failed++;
        }
        System.out.println();

        // Test 4: ResultVO
        System.out.println("[TEST 4] ResultVO - 기본 동작");
        try {
            ResultVO result = new ResultVO();

            // 테스트 데이터 설정
            result.setTargetFileName("output.txt");
            result.setHostName("server03");
            result.setTargetFilePath("/data/");
            result.setResult("Success");
            result.setResultHash("hash123");
            result.setOk(true);

            // 검증
            if (!result.getTargetFileName().equals("output.txt")) {
                throw new Exception("TargetFileName 불일치!");
            }
            if (!result.getHostName().equals("server03")) {
                throw new Exception("HostName 불일치!");
            }
            if (!result.getResult().equals("Success")) {
                throw new Exception("Result 불일치!");
            }
            if (!result.isOk()) {
                throw new Exception("isOk가 false!");
            }

            System.out.println("  ✓ PASS - 모든 getter/setter 정상 동작");
            System.out.println("    - FileName: " + result.getTargetFileName());
            System.out.println("    - HostName: " + result.getHostName());
            System.out.println("    - Result: " + result.getResult());
            System.out.println("    - IsOk: " + result.isOk());
            passed++;
        } catch (Exception e) {
            System.out.println("  ✗ FAIL - " + e.getMessage());
            failed++;
        }
        System.out.println();

        // Test 5: ResultVO 기본값
        System.out.println("[TEST 5] ResultVO - 기본값 확인");
        try {
            ResultVO result = new ResultVO();

            if (!result.getTargetFileName().equals("")) {
                throw new Exception("TargetFileName 기본값이 빈 문자열이 아님!");
            }
            if (!result.getResult().equals("")) {
                throw new Exception("Result 기본값이 빈 문자열이 아님!");
            }
            if (result.isOk()) {
                throw new Exception("isOk 기본값이 false가 아님!");
            }

            System.out.println("  ✓ PASS - 기본값 정상");
            System.out.println("    - TargetFileName: \"\" (빈 문자열)");
            System.out.println("    - Result: \"\" (빈 문자열)");
            System.out.println("    - IsOk: false");
            passed++;
        } catch (Exception e) {
            System.out.println("  ✗ FAIL - " + e.getMessage());
            failed++;
        }
        System.out.println();

        // 결과 요약
        System.out.println("========================================");
        System.out.println("  Test Results");
        System.out.println("========================================");
        System.out.println("✓ Passed: " + passed);
        System.out.println("✗ Failed: " + failed);
        System.out.println("  Total:  " + (passed + failed));
        System.out.println();

        if (failed == 0) {
            System.out.println("🎉 ALL TESTS PASSED!");
            System.out.println();
            System.out.println("테스트 데이터가 제대로 동작합니다:");
            System.out.println("- CommandVO의 모든 필드 저장/조회 성공");
            System.out.println("- ResultVO의 모든 필드 저장/조회 성공");
            System.out.println("- 기본값들이 정확하게 설정됨");
        } else {
            System.out.println("❌ SOME TESTS FAILED!");
            System.exit(1);
        }
    }
}
