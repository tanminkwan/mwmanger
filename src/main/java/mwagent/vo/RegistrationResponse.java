package mwagent.vo;

/**
 * Agent 등록 응답 데이터
 */
public class RegistrationResponse {

    private final boolean success;
    private final long returnCode;
    private final String message;

    public RegistrationResponse(boolean success, long returnCode, String message) {
        this.success = success;
        this.returnCode = returnCode;
        this.message = message;
    }

    public static RegistrationResponse success() {
        return new RegistrationResponse(true, 1, "Registration successful");
    }

    public static RegistrationResponse failure(long returnCode, String message) {
        return new RegistrationResponse(false, returnCode, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public long getReturnCode() {
        return returnCode;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "RegistrationResponse{" +
                "success=" + success +
                ", returnCode=" + returnCode +
                ", message='" + message + '\'' +
                '}';
    }
}
