package mwmanger.order;

import static mwmanger.common.Config.getConfig;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.json.simple.JSONObject;

import mwmanger.common.Common;
import mwmanger.kafka.MwProducer;
import mwmanger.vo.CommandVO;
import mwmanger.vo.MwResponseVO;
import mwmanger.vo.ResultVO;

public abstract class Order {

	public static String KAFKA = "KAFKA";
	public static String SERVER = "SERVER";
	public static String SERVER_N_KAFKA = "SERVER_N_KAFKA";

	CommandVO commandVo = new CommandVO();	
	ResultVO resultVo = new ResultVO();	
	ArrayList<ResultVO> resultVos = new ArrayList<ResultVO>();

	public Order(JSONObject command) {
		convertCommand(command);
	}

	public abstract int execute();

	public void sendResults() throws IOException{

		if(!resultVo.getResult().equals("")){
			getConfig().getLogger().fine("resultVo : "+resultVo.toString());
			sendResult(resultVo);
		}
		
		for(ResultVO rv : resultVos){
			getConfig().getLogger().fine("resultVo Array : "+rv.toString());
			sendResult(rv);
		}
	}

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
		commandVo.setHostName(getConfig().getHostName());

	}

	protected String replaceParam(String text) {

		if (text == null || text.isEmpty() || text.length() < 1)
			return text;

		int s = text.indexOf("<<");
		int e = text.indexOf(">>");

		if (s == -1 || e == -1 || s > e){
			return text;
		}			

		String env_val = text.substring(s + 2, e);
		String rtn_val = getConfig().getEnv().get(env_val);
		
		getConfig().getLogger().fine("Param in order :" + env_val + "=>" + rtn_val);
		
		String ctext = text.substring(0, s) + rtn_val + text.substring(e + 2);
		return  replaceParam(ctext);

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
		getConfig().getLogger().fine("sendResult commandVo : " + commandVo.toString());
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

		MwProducer.sendMessage(topic, getConfig().getAgent_id(), js);

		return 1;

	}

	private int send2Server(ResultVO rv) {

		String path = "/api/v1/command/result";
		String data = getJsonResult(false, rv);
		
		MwResponseVO mwrv = Common.httpPOST(path, getConfig().getAccess_token(), data);

		if (mwrv.getResponse() != null) {
			getConfig().getLogger().fine("sendPOST result:" + mwrv.getResponse().get("message").toString());
		} else {			
			getConfig().getLogger().warning("sendPOST Error");			
		}

		return 1;

	}

	@SuppressWarnings("unchecked")
	private String getJsonResult(boolean is4Kafka, ResultVO rv) {

		JSONObject jsonObj = new JSONObject();
		
		getConfig().getLogger().info("Agent_id : " + getConfig().getAgent_id());
		
		jsonObj.put("agent_id", getConfig().getAgent_id());
		jsonObj.put("command_id", commandVo.getCommandId());
		jsonObj.put("repetition_seq", Long.toString(commandVo.getRepetitionSeq()));
		jsonObj.put("key_value1", rv.getTargetFileName());
		jsonObj.put("host_id", rv.getHostName());
		jsonObj.put("is_normal", rv.isOk());
		jsonObj.put("key_value2", rv.getTargetFilePath());
		jsonObj.put("result_text", rv.getResult());
		jsonObj.put("result_hash", rv.getResultHash());
		jsonObj.put("aggregation_key", rv.getObjectAggregationKey());
		
		return jsonObj.toString();
		/*
		StringBuilder js = new StringBuilder();
		System.out.println("Agent_id : " + getConfig().getAgent_id());

		js.append("{");
		js.append("\"agent_id\":\"" + getConfig().getAgent_id() + "\",");
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
		*/		
	}

}
