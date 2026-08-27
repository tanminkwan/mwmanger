package test.demos;

import mwagent.order.ExtractLog;
import mwagent.vo.ResultVO;
import mwagent.common.Config;
import org.json.simple.JSONObject;

public class TestExtractLog {
    public static void main(String[] args) {
        System.out.println("--- Starting TestExtractLog ---");
        try {
            java.util.logging.Logger consoleLogger = java.util.logging.Logger.getLogger("Hennry");
            consoleLogger.setLevel(java.util.logging.Level.INFO);
            Config.getConfig().setLogger(consoleLogger);
            Config.getConfig().setHostName("localhost");
        } catch (Exception e) {
            System.err.println("Config init failed, but continuing...");
        }

        JSONObject params = new JSONObject();
        params.put("file", "/home/hennry/projects/mwmanger/tmp/test_dummy.log");
        params.put("start", "2026.08.27 09:00:00");
        params.put("end", "2026.08.27 11:00:00");
        params.put("dateRegex", "\\[(\\d{4}\\.\\d{2}\\.\\d{2} \\d{2}:\\d{2}:\\d{2})\\](?:\\s*\\[[^\\]]*\\]){1,2}");
        params.put("abbreviatePrefix", "\tat ");
        params.put("charset", "UTF-8");
        
        JSONObject cmdObj = new JSONObject();
        cmdObj.put("command_id", "test-cmd-01");
        cmdObj.put("target_object", "mwagent.order.ExtractLog");
        cmdObj.put("target_file_path", "/wrong/path/");
        cmdObj.put("target_file_name", "wrong.log");
        cmdObj.put("result_hash", "");
        cmdObj.put("additional_params", params.toJSONString());
        
        System.out.println("Command: " + cmdObj.toJSONString());
        
        try {
            ExtractLog order = new ExtractLog(cmdObj);
            int res = order.execute();
            
            System.out.println("\nExecution result code: " + res);
            ResultVO resultVO = order.getResultVo();
            System.out.println("Is OK: " + resultVO.isOk());
            System.out.println("Result String: \n" + resultVO.getResult());
            
            java.nio.file.Files.write(java.nio.file.Paths.get("/home/hennry/projects/mwmanger/tmp/test_result.json"), resultVO.getResult().getBytes());
            System.out.println("Result saved to /home/hennry/projects/mwmanger/tmp/test_result.json");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
