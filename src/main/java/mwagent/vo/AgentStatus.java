package mwagent.vo;

/**
 * Agent 등록 및 승인 상태를 나타내는 Enum
 */
public enum AgentStatus {

    /**
     * Agent가 서버에 등록되지 않음 (return_code: -1)
     */
    NOT_REGISTERED(-1, "Agent Not Exists"),

    /**
     * Agent가 등록되었으나 아직 승인되지 않음 (return_code: -2)
     */
    PENDING_APPROVAL(-2, "Not Approved Yet"),

    /**
     * Agent가 승인되어 정상 동작 가능 (return_code: >= 0)
     */
    APPROVED(0, "Approved"),

    /**
     * noticeStart API 호출 실패 (return_code: -10)
     */
    CONNECTION_ERROR(-10, "Connection Error"),

    /**
     * Agent 등록 실패 (return_code: -20)
     */
    REGISTRATION_ERROR(-20, "Registration Error"),

    /**
     * 기타 에러
     */
    UNKNOWN_ERROR(-99, "Unknown Error");

    private final long returnCode;
    private final String description;

    AgentStatus(long returnCode, String description) {
        this.returnCode = returnCode;
        this.description = description;
    }

    public long getReturnCode() {
        return returnCode;
    }

    public String getDescription() {
        return description;
    }

    /**
     * return code로부터 AgentStatus를 반환
     * @param returnCode API 응답의 return_code
     * @return AgentStatus
     */
    public static AgentStatus fromReturnCode(long returnCode) {
        if (returnCode == -1) {
            return NOT_REGISTERED;
        } else if (returnCode == -2) {
            return PENDING_APPROVAL;
        } else if (returnCode == -10) {
            return CONNECTION_ERROR;
        } else if (returnCode == -20) {
            return REGISTRATION_ERROR;
        } else if (returnCode >= 0) {
            return APPROVED;
        } else {
            return UNKNOWN_ERROR;
        }
    }

    public boolean isApproved() {
        return this == APPROVED;
    }

    public boolean needsRegistration() {
        return this == NOT_REGISTERED;
    }

    public boolean isPending() {
        return this == PENDING_APPROVAL;
    }

    public boolean isError() {
        return this == CONNECTION_ERROR ||
               this == REGISTRATION_ERROR ||
               this == UNKNOWN_ERROR;
    }
}
