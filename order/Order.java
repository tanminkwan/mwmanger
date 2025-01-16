package mwmanger.order;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.json.simple.JSONObject;

import mwmanger.common.Common;
import mwmanger.common.Config;
import mwmanger.kafka.MwProducer;
import mwmanger.vo.CommandVO;
import mwmanger.vo.MwResponseVO;
import mwmanger.vo.ResultVO;

public abstract class Order {

	//private String target_object_aggregation_key = "";
	// private boolean is_result_string = true;

	public static String KAFKA = "KAFKA";
	public static String SERVER = "SERVER";
	public static String SERVER_N_KAFKA = "SERVER_N_KAFKA";

	CommandVO commandVo = new CommandVO();
	
	ResultVO resultVo = new ResultVO();
	
	ArrayList<ResultVO> resultVos = new ArrayList<ResultVO>();

	protected void convertCommand(JSONObject command) {

		commandVo.setCommandId((String) command.get("command_id"));
		commandVo.setRepetitionSeq((Long) command.get("repetition_seq"));
		String tmp_target_file_name = (String) command.get("target_file_name");
		commandVo.setTargetFileName(replaceParam(tmp_target_file_name));
		String tmp_target_file_path = (String) command.get("target_file_path");
		commandVo.setTargetFilePath(replaceParam(tmp_target_file_path));
		commandVo.setResultHash((String) command.get("result_hash"));
		String tmp_additional_params = (String) command.get("additional_params");
		commandVo.setAdditionalParams(replaceParam(tmp_additional_params));
		commandVo.setResultReceiver((String) command.get("result_receiver"));
		commandVo.setTargetObject((String) command.get("target_object"));
		commandVo.setHostName(Config.getHostName());

	}

	public Order(JSONObject command) {
		convertCommand(command);
	}

	public abstract int execute();

	public void sendResults() throws IOException{

		if(!resultVo.getResult().equals("")){
			Config.getLogger().fine("resultVo : "+resultVo.toString());
			sendResult(resultVo);
		}
		
		for(ResultVO rv : resultVos){
			Config.getLogger().fine("resultVo Array : "+rv.toString());
			sendResult(rv);
		}
	}

	protected String replaceParam(String text) {

		if (text == null || text.isEmpty() || text.length() < 1)
			return text;

		String ctext = "";
		String env_val = "";
		String rtn_val = "";
		int s = text.indexOf("<<");
		int e = text.indexOf(">>");

		if (s == -1 || e == -1 || s > e)
			return text;

		env_val = text.substring(s + 2, e);
		rtn_val = Config.getEnv().get(env_val);
		System.out.println("Param in order :" + env_val + "=>" + rtn_val);
		ctext = text.substring(0, s) + rtn_val + text.substring(e + 2);
		ctext = replaceParam(ctext);
		return ctext;

	}

	protected String getHash(String content) throws NoSuchAlgorithmException {

		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		messageDigest.update(content.getBytes(Charset.forName("UTF-8")));
		byte[] byteHash = messageDigest.digest();

		StringBuilder sb = new StringBuilder();
		for (byte b : byteHash) {
			sb.append(String.format("%02X", b));
		}

		return sb.toString();

	}

	protected int sendResult(ResultVO rv) throws IOException {

		int rtn = 0;
		Config.getLogger().fine("sendResult commandVo : " + commandVo.toString());
		if (commandVo.getResultReceiver().equals(SERVER) || commandVo.getResultReceiver().equals(SERVER_N_KAFKA)) {
			rtn = send2Server(rv);
		}

		if (commandVo.getResultReceiver().equals(KAFKA) || commandVo.getResultReceiver().equals(SERVER_N_KAFKA)) {
			rtn = send2Kafka(commandVo.getTargetObject(), rv);
		}

		return rtn;

	}

	private int send2Kafka(String topic, ResultVO rv) throws IOException {

		String js = getJsonResult(true, rv);

		MwProducer.sendMessage(topic, Config.getAgent_id(), js);

		return 1;

	}

	private int send2Server(ResultVO rv) {

		String url = Config.getServer_url() + "/api/v1/command/result";
		String data = getJsonResult(false, rv);
		
		MwResponseVO mwrv = Common.httpPOST(url, Config.getAccess_token(), data);

		if (mwrv.getResponse() != null) {
			Config.getLogger().fine("sendPOST result:" + mwrv.getResponse().get("message").toString());
		} else {			
			Config.getLogger().warning("sendPOST Error");			
		}

		return 1;

	}

	private String getJsonResult(boolean is4Kafka, ResultVO rv) {

		StringBuilder js = new StringBuilder();
		System.out.println("Agent_id : " + Config.getAgent_id());

		js.append("{");
		js.append("\"agent_id\":\"" + Config.getAgent_id() + "\",");
		js.append("\"command_id\":\"" + commandVo.getCommandId() + "\",");
		js.append("\"repetition_seq\":" + Long.toString(commandVo.getRepetitionSeq()) + ",");
		js.append("\"key_value1\":\"" + Common.escape(rv.getTargetFileName()) + "\",");
		js.append("\"host_id\":\"" + rv.getHostName() + "\",");
		js.append("\"is_normal\":\"" + rv.isOk() + "\",");
		js.append("\"key_value2\":\"" + Common.escape(rv.getTargetFilePath()) + "\",");
		if (rv.isOk() || !is4Kafka) {
			js.append("\"result_text\":\"" + Common.escape(rv.getResult()) + "\",");
		} else {
			js.append("\"result_text\":" + rv.getResult() + ",");
		}
		js.append("\"result_hash\":\"" + rv.getResultHash() + "\",");
		js.append("\"aggregation_key\":\"" + rv.getObjectAggregationKey() + "\"");
		js.append("}");

		return js.toString();

	}

}
