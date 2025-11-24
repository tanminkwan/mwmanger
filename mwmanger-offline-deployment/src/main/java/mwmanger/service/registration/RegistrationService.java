package mwmanger.service.registration;

import static mwmanger.common.Config.getConfig;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;

import mwmanger.common.Common;
import mwmanger.vo.MwResponseVO;
import mwmanger.vo.RegistrationRequest;
import mwmanger.vo.RegistrationResponse;

/**
 * Agent 등록을 담당하는 서비스
 * PreWork.registerMe() 로직을 모듈화
 */
public class RegistrationService {

    private final Logger logger;

    public RegistrationService() {
        this.logger = getConfig().getLogger();
    }

    /**
     * Agent를 서버에 등록
     *
     * @param request 등록 요청 데이터
     * @return 등록 응답
     */
    @SuppressWarnings("unchecked")
    public RegistrationResponse register(RegistrationRequest request) {

        logger.info("Attempting to register agent: " + request.getAgentId());

        String path = getConfig().getPost_agent_uri();

        // 요청 JSON 생성
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("agent_id", request.getAgentId());
        jsonObj.put("agent_type", request.getAgentType());
        jsonObj.put("installation_path", request.getInstallationPath());
        jsonObj.put("host_id", request.getHostId());

        logger.fine("Registration request: " + jsonObj.toString());

        // HTTP POST 요청
        MwResponseVO mwrv = Common.httpPOST(path, getConfig().getAccess_token(), jsonObj.toString());

        // 응답 처리
        if (mwrv.getResponse() != null) {

            long rtn = (Long) mwrv.getResponse().get("return_code");

            if (rtn < 0) {
                String message = (String) mwrv.getResponse().get("message");
                logger.severe(String.format("Registration failed: [%d] [%s]", rtn, message));
                return RegistrationResponse.failure(rtn, message);
            }

            logger.info("Agent registered successfully: " + request.getAgentId());
            return RegistrationResponse.success();

        } else {
            logger.severe("Registration failed: No response from server");
            return RegistrationResponse.failure(-2, "No response from server");
        }
    }

    /**
     * 현재 설정으로부터 RegistrationRequest 생성
     *
     * @return RegistrationRequest
     */
    public RegistrationRequest createRegistrationRequest() {
        return new RegistrationRequest(
                getConfig().getAgent_id(),
                getConfig().getAgent_type(),
                System.getProperty("user.dir"),
                getConfig().getHostName()
        );
    }
}
