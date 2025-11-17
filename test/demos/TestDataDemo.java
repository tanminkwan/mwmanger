/**
 * Test Data Demonstration
 * Shows what test data is and how it works
 */
public class TestDataDemo {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  MwManger Test Data Demo");
        System.out.println("========================================");
        System.out.println();

        // Example 1: Command Test Data
        System.out.println("[TEST DATA Example 1] Command Data");
        System.out.println("------------------------------------------");
        System.out.println("Real Production Data:");
        System.out.println("  {");
        System.out.println("    command_id: \"CMD-2025-01-23-0001\"");
        System.out.println("    host_name: \"prod-server-01\"");
        System.out.println("    target_file: \"production_backup.sh\"");
        System.out.println("  }");
        System.out.println();
        System.out.println("Test Data (Fake Data for Testing):");
        System.out.println("  {");
        System.out.println("    command_id: \"CMD-123\"         <-- Simple!");
        System.out.println("    host_name: \"server01\"         <-- Short!");
        System.out.println("    target_file: \"test.sh\"        <-- Easy to read!");
        System.out.println("  }");
        System.out.println();

        // Example 2: Creating Test Data
        System.out.println("[TEST DATA Example 2] Creating Test Data");
        System.out.println("------------------------------------------");

        // This is test data!
        SimpleCommand testCmd = new SimpleCommand();
        testCmd.commandId = "CMD-123";          // <-- TEST DATA
        testCmd.hostName = "server01";          // <-- TEST DATA
        testCmd.targetFile = "test.sh";         // <-- TEST DATA
        testCmd.repetitionSeq = 5;              // <-- TEST DATA

        System.out.println("Created test data object:");
        System.out.println("  commandId: " + testCmd.commandId);
        System.out.println("  hostName: " + testCmd.hostName);
        System.out.println("  targetFile: " + testCmd.targetFile);
        System.out.println("  repetitionSeq: " + testCmd.repetitionSeq);
        System.out.println();

        // Example 3: Testing with Test Data
        System.out.println("[TEST DATA Example 3] Testing Process");
        System.out.println("------------------------------------------");
        System.out.println("Step 1: GIVEN - Create test data");
        System.out.println("  SimpleCommand cmd = new SimpleCommand();");
        System.out.println("  cmd.commandId = \"CMD-123\";  // This is TEST DATA!");
        System.out.println();

        System.out.println("Step 2: WHEN - Execute method");
        System.out.println("  String result = cmd.commandId;");
        System.out.println();

        System.out.println("Step 3: THEN - Verify result");
        String actualResult = testCmd.commandId;
        boolean testPassed = actualResult.equals("CMD-123");
        System.out.println("  Expected: \"CMD-123\"");
        System.out.println("  Actual:   \"" + actualResult + "\"");
        System.out.println("  Result:   " + (testPassed ? "PASS!" : "FAIL!"));
        System.out.println();

        // Example 4: Different Types of Test Data
        System.out.println("[TEST DATA Example 4] Different Scenarios");
        System.out.println("------------------------------------------");

        System.out.println("Scenario A: Normal case");
        SimpleCommand normalCmd = new SimpleCommand();
        normalCmd.commandId = "CMD-123";
        normalCmd.hostName = "server01";
        System.out.println("  CommandId: " + normalCmd.commandId);
        System.out.println("  HostName: " + normalCmd.hostName);
        System.out.println("  Expected: Success");
        System.out.println();

        System.out.println("Scenario B: Empty string (boundary test)");
        SimpleCommand emptyCmd = new SimpleCommand();
        emptyCmd.commandId = "";
        emptyCmd.hostName = "";
        System.out.println("  CommandId: \"" + emptyCmd.commandId + "\" (empty)");
        System.out.println("  HostName: \"" + emptyCmd.hostName + "\" (empty)");
        System.out.println("  Expected: Check default behavior");
        System.out.println();

        System.out.println("Scenario C: Null value (edge case)");
        SimpleCommand nullCmd = new SimpleCommand();
        nullCmd.commandId = null;
        nullCmd.hostName = null;
        System.out.println("  CommandId: " + nullCmd.commandId);
        System.out.println("  HostName: " + nullCmd.hostName);
        System.out.println("  Expected: Handle null gracefully");
        System.out.println();

        // Example 5: Special Characters Test Data
        System.out.println("[TEST DATA Example 5] Special Characters");
        System.out.println("------------------------------------------");
        String testData1 = "path\\to\\file";
        String testData2 = "say \"hello\"";
        String testData3 = "line1\nline2";

        System.out.println("Test Data 1: \"" + testData1 + "\"");
        System.out.println("  Purpose: Test backslash handling");
        System.out.println();

        System.out.println("Test Data 2: " + testData2);
        System.out.println("  Purpose: Test quote handling");
        System.out.println();

        System.out.println("Test Data 3: (with newline)");
        System.out.println("  Purpose: Test newline handling");
        System.out.println();

        // Summary
        System.out.println("========================================");
        System.out.println("  Summary: What is Test Data?");
        System.out.println("========================================");
        System.out.println("1. Test Data = FAKE values for testing");
        System.out.println("2. Used instead of real server data");
        System.out.println("3. Simple and predictable");
        System.out.println("4. Safe (won't affect production)");
        System.out.println("5. Fast (no network calls)");
        System.out.println();

        System.out.println("Examples of Test Data:");
        System.out.println("  - \"CMD-123\" instead of \"CMD-2025-01-23-0001\"");
        System.out.println("  - \"server01\" instead of \"prod-server-01\"");
        System.out.println("  - \"test.sh\" instead of \"production_backup.sh\"");
        System.out.println();

        System.out.println("Now you understand test data!");
    }

    static class SimpleCommand {
        String commandId;
        String hostName;
        String targetFile;
        int repetitionSeq;
    }
}
