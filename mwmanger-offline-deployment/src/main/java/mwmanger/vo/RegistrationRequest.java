package mwmanger.vo;

/**
 * Agent 등록 요청 데이터
 */
public class RegistrationRequest {

    private final String agentId;
    private final String agentType;
    private final String installationPath;
    private final String hostId;

    public RegistrationRequest(String agentId, String agentType,
                              String installationPath, String hostId) {
        this.agentId = agentId;
        this.agentType = agentType;
        this.installationPath = installationPath;
        this.hostId = hostId;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAgentType() {
        return agentType;
    }

    public String getInstallationPath() {
        return installationPath;
    }

    public String getHostId() {
        return hostId;
    }

    @Override
    public String toString() {
        return "RegistrationRequest{" +
                "agentId='" + agentId + '\'' +
                ", agentType='" + agentType + '\'' +
                ", installationPath='" + installationPath + '\'' +
                ", hostId='" + hostId + '\'' +
                '}';
    }
}
