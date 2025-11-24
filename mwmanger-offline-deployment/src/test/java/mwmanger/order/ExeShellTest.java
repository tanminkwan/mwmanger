package mwmanger.order;

import static mwmanger.common.Config.getConfig;
import static org.assertj.core.api.Assertions.*;

import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mwmanger.vo.ResultVO;

/**
 * ExeShell 테스트
 *
 * ⚠️ 보안 경고: 이 클래스는 Command Injection 위험이 있습니다.
 * 현재 테스트는 기본 동작을 검증하며, 향후 보안 리팩토링 시
 * 보안 검증 테스트로 확장되어야 합니다.
 *
 * TODO: Phase 2에서 보안 강화 후 다음 테스트 추가:
 * - Command Injection 방어 테스트
 * - Command Whitelist 검증
 * - Malicious input 거부 테스트
 */
class ExeShellTest {

    private ExeShell exeShell;
    private JSONObject testCommand;

    @BeforeEach
    void setUp() {
        // Initialize Config for testing
        getConfig().setLogger(Logger.getLogger("TestLogger"));
        getConfig().setOs("LINUX");
        getConfig().setHostName("test-host");

        // Given: 테스트용 명령어 JSON 생성
        testCommand = new JSONObject();
        testCommand.put("command_id", "CMD-001");
        testCommand.put("repetition_seq", 1L);
        testCommand.put("host_name", "test-host");
        testCommand.put("user_name", "test-user");
        testCommand.put("additional_params", "");
        testCommand.put("target_file_path", "test/");
        testCommand.put("target_file_name", "test.sh");
        testCommand.put("result_hash", "");
        testCommand.put("result_receiver", "SERVER");
        testCommand.put("target_object", "test-object");

        exeShell = new ExeShell(testCommand);
    }

    @Test
    void constructor_ShouldInitializeWithCommand() {
        // Then
        assertThat(exeShell).isNotNull();
        assertThat(exeShell.getCommandVo()).isNotNull();
    }

    @Test
    void execute_ShouldReturnOne() {
        // When
        int result = exeShell.execute();

        // Then
        assertThat(result).isEqualTo(1);
    }

    @Test
    void execute_ShouldSetResultVO() {
        // When
        exeShell.execute();

        // Then
        ResultVO resultVo = exeShell.getResultVo();
        assertThat(resultVo).isNotNull();
        assertThat(resultVo.getHostName()).isEqualTo("test-host");
        assertThat(resultVo.getTargetFileName()).isEqualTo("test.sh");
    }

    @Test
    void constructor_WithAdditionalParams_ShouldIncludeInCommand() {
        // Given
        JSONObject commandWithParams = new JSONObject();
        commandWithParams.put("command_id", "CMD-002");
        commandWithParams.put("repetition_seq", 1L);
        commandWithParams.put("host_name", "test-host");
        commandWithParams.put("user_name", "test-user");
        commandWithParams.put("additional_params", "param1 param2");
        commandWithParams.put("target_file_path", "test/");
        commandWithParams.put("target_file_name", "test.sh");
        commandWithParams.put("result_hash", "");
        commandWithParams.put("result_receiver", "SERVER");
        commandWithParams.put("target_object", "test-object");

        // When
        ExeShell shellWithParams = new ExeShell(commandWithParams);
        shellWithParams.execute();

        // Then
        assertThat(shellWithParams.getCommandVo().getAdditionalParams())
            .isEqualTo("param1 param2");
    }

    @Test
    void execute_WithEmptyAdditionalParams_ShouldSucceed() {
        // Given
        testCommand.put("additional_params", "");

        // When
        ExeShell shell = new ExeShell(testCommand);
        int result = shell.execute();

        // Then
        assertThat(result).isEqualTo(1);
    }

    /**
     * 보안 테스트 - 향후 Phase 2에서 활성화
     *
     * 현재는 주석 처리: Command Injection 방어 로직이 아직 구현되지 않음
     *
     * Phase 2 작업 시 다음 테스트들을 활성화하고 구현:
     * 1. Whitelist 기반 명령어 검증
     * 2. Shell metacharacter 차단
     * 3. ProcessBuilder 사용으로 전환
     */

    // @Test
    // void execute_WithMaliciousCommand_ShouldThrowSecurityException() {
    //     // Given
    //     testCommand.put("additional_params", "; rm -rf /");
    //     ExeShell maliciousShell = new ExeShell(testCommand);
    //
    //     // When & Then
    //     assertThatThrownBy(() -> maliciousShell.execute())
    //         .isInstanceOf(SecurityException.class)
    //         .hasMessageContaining("Command injection detected");
    // }

    // @Test
    // void execute_WithPipeInParams_ShouldThrowSecurityException() {
    //     // Given
    //     testCommand.put("additional_params", "| cat /etc/passwd");
    //     ExeShell maliciousShell = new ExeShell(testCommand);
    //
    //     // When & Then
    //     assertThatThrownBy(() -> maliciousShell.execute())
    //         .isInstanceOf(SecurityException.class);
    // }

    // @Test
    // void execute_WithCommandNotInWhitelist_ShouldFail() {
    //     // Given
    //     testCommand.put("target_file_name", "dangerous.exe");
    //     ExeShell nonWhitelistedShell = new ExeShell(testCommand);
    //
    //     // When & Then
    //     assertThatThrownBy(() -> nonWhitelistedShell.execute())
    //         .isInstanceOf(SecurityException.class)
    //         .hasMessageContaining("not in whitelist");
    // }
}
