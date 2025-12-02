package mwmanger.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class Config {

	private static final Config INSTANCE = new Config();

	private String agent_version = readVersionFromManifest();
	private String agent_type = "JAVAAGENT";

	/**
	 * Read version from MANIFEST.MF (Implementation-Version)
	 * Falls back to DEV version if not found (e.g., running from IDE)
	 */
	private static String readVersionFromManifest() {
		try {
			Package pkg = Config.class.getPackage();
			String version = pkg.getImplementationVersion();
			if (version != null && !version.isEmpty()) {
				return version;
			}
		} catch (Exception e) {
			// Ignore and use fallback
		}
		// Fallback for development/IDE environment
		return "0000.0000.0000-DEV";
	}
	private String hostName = "";
	private String userName = "";
	private String server_url = "";
	private String get_command_uri = "";
	private String post_agent_uri = "";
	private String agent_id = "";
	private String access_token = "";
	private String refresh_token = "";
	private String kafka_broker_address = "";
	private long command_check_cycle = 60;

	// mTLS Configuration
	private boolean use_mtls = false;
	private String client_keystore_path = "";
	private String client_keystore_password = "";
	private String truststore_path = "";
	private String truststore_password = "";

	// Security Configuration
	private boolean security_command_injection_check = false;
	private boolean security_path_traversal_check = true;

	private Logger logger;
	
	private String os = "";	

	private Map<String, String> env = Collections.emptyMap();

	public static Config getConfig(){
		return INSTANCE;
	}
	
	public String getKafka_broker_address() {
		return kafka_broker_address;
	}
	public void setKafka_broker_address(String kafka_broker_address) {
		this.kafka_broker_address = kafka_broker_address;
	}

	public Logger getLogger() {
		return logger;
	}
	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public Map<String, String> getEnv() {
		return env;
	}
	public void setEnv(Map<String, String> env) {
		this.env = env;
	}
	public String getOs() {
		return os;
	}
	public void setOs(String os) {
		this.os = os;
	}

	public String getAgent_version() {
		return agent_version;
	}
	public String getAgent_type() {
		return agent_type;
	}

	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getServer_url() {
		return server_url;
	}
	public void setServer_url(String server_url) {
		this.server_url = server_url;
	}
	public String getGet_command_uri() {
		return get_command_uri;
	}
	public void setGet_command_uri(String get_command_uri) {
		this.get_command_uri = get_command_uri;
	}
	public String getPost_agent_uri() {
		return post_agent_uri;
	}
	public void setPost_agent_uri(String post_agent_uri) {
		this.post_agent_uri = post_agent_uri;
	}
	public String getAgent_id() {
		return agent_id;
	}
	public void setAgent_id(String agent_id) {
		this.agent_id = agent_id;
	}
	public String getAccess_token() {
		return access_token;
	}
	public void setAccess_token(String access_token) {
		this.access_token = access_token;
	}
	public String getRefresh_token() {
		return refresh_token;
	}
	public void setRefresh_token(String refresh_token) {
		this.refresh_token = refresh_token;
	}	
	public long getCommand_check_cycle() {
		return command_check_cycle;
	}
	public void setCommand_check_cycle(long command_check_cycle) {
		this.command_check_cycle = command_check_cycle;
	}

	// mTLS getters and setters
	public boolean isUseMtls() {
		return use_mtls;
	}
	public void setUseMtls(boolean use_mtls) {
		this.use_mtls = use_mtls;
	}
	public String getClientKeystorePath() {
		return client_keystore_path;
	}
	public void setClientKeystorePath(String client_keystore_path) {
		this.client_keystore_path = client_keystore_path;
	}
	public String getClientKeystorePassword() {
		return client_keystore_password;
	}
	public void setClientKeystorePassword(String client_keystore_password) {
		this.client_keystore_password = client_keystore_password;
	}
	public String getTruststorePath() {
		return truststore_path;
	}
	public void setTruststorePath(String truststore_path) {
		this.truststore_path = truststore_path;
	}
	public String getTruststorePassword() {
		return truststore_password;
	}
	public void setTruststorePassword(String truststore_password) {
		this.truststore_password = truststore_password;
	}

	// Security Configuration getters/setters
	public boolean isSecurityCommandInjectionCheck() {
		return security_command_injection_check;
	}
	public void setSecurityCommandInjectionCheck(boolean security_command_injection_check) {
		this.security_command_injection_check = security_command_injection_check;
	}
	public boolean isSecurityPathTraversalCheck() {
		return security_path_traversal_check;
	}
	public void setSecurityPathTraversalCheck(boolean security_path_traversal_check) {
		this.security_path_traversal_check = security_path_traversal_check;
	}

    public long setConfig() {

		Properties prop = new Properties();
		
		String host_name_var = "";
		String user_name_var = "";
		
		int rtn = 0;
		
		try{
			
			FileReader in = new FileReader("agent.properties");
			prop.load(in);
			
			in.close();
			
			int command_check_cycle = Integer.parseInt(prop.getProperty("command_check_cycle", "60"));
			String get_command_uri = prop.getProperty("get_command_uri");
			String post_agent_uri = prop.getProperty("post_agent_uri");
			setRefresh_token(prop.getProperty("token"));
			host_name_var = prop.getProperty("host_name_var");
			user_name_var = prop.getProperty("user_name_var");
			String log_dir = prop.getProperty("log_dir", System.getProperty("user.dir"));
			String log_level = prop.getProperty("log_level", "INFO");
			
			setServer_url(prop.getProperty("server_url"));
			setCommand_check_cycle(command_check_cycle);
			setGet_command_uri(get_command_uri);
			setPost_agent_uri(post_agent_uri);
			setKafka_broker_address(prop.getProperty("kafka_broker_address", ""));

			// mTLS Configuration
			setUseMtls(Boolean.parseBoolean(prop.getProperty("use_mtls", "false")));
			setClientKeystorePath(prop.getProperty("client.keystore.path", ""));
			setClientKeystorePassword(prop.getProperty("client.keystore.password", ""));
			setTruststorePath(prop.getProperty("truststore.path", ""));
			setTruststorePassword(prop.getProperty("truststore.password", ""));

			// Security Configuration (default: command injection check OFF, path traversal check ON)
			setSecurityCommandInjectionCheck(Boolean.parseBoolean(prop.getProperty("security.command_injection_check", "false")));
			setSecurityPathTraversalCheck(Boolean.parseBoolean(prop.getProperty("security.path_traversal_check", "true")));

			//Create Logger
			Logger logger = Logger.getLogger("Hennry");
			logger.setLevel(Level.parse(log_level));
			FileHandler fh = new FileHandler(log_dir + File.separator + "mwagent.%u.%g.log", 1024*1024, 10, true);
			
			SimpleFormatter sf = new SimpleFormatter();
			fh.setFormatter(sf);
			logger.addHandler(fh);
			
			setLogger(logger);
			getLogger().info("Logger is activated.");
			
			//Get access token
			rtn = Common.updateToken();
			
			if(rtn < 0){
				getLogger().severe("Update Token error occurred.");
				System.exit(0);
			}
			
			getLogger().info("Access Token is updated.");
			
		}catch(IOException e){
        	getLogger().log(Level.SEVERE, e.getMessage(), e);
			return -1;
		}catch(IllegalArgumentException e){
        	getLogger().log(Level.SEVERE, e.getMessage(), e);
			return -2;
		}
		
    	Map<String, String> env = System.getenv();
    	for (String envName : env.keySet()){
    		if(envName.equals(host_name_var)){
    			setHostName(env.get(envName));
    		}else if(envName.equals(user_name_var)){
    			setUserName(env.get(envName));
    		} 
    	}
    	
    	setEnv(env);
    	
    	String agent_id = getHostName() + "_" + getUserName() + "_J";
    	setAgent_id(agent_id);
    	
    	getLogger().info(String.format("hostName:%s, userName:%s, get_command_uri:%s, command_check_cycle:%d", getHostName(), getUserName(), getGet_command_uri(), getCommand_check_cycle()));
		
		String os = System.getProperty("os.name").toLowerCase();
		
		getLogger().info(String.format("OS : %s %n", os));
		
		if (os.contains("win")){
			setOs("WIN");
		}else if (os.contains("aix")){
			setOs("AIX");
		}else if (os.contains("linux")){
			setOs("LINUX");
		}else if (os.contains("hp-ux")){
			setOs("HPUX");
		}
		
		getLogger().info(String.format("OS in Config : %s %n", getOs()));
		
		return 1;
		
    }
    
    public long updateProperty(String item, String value){
    	
    	try{
    		
    		Properties prop = new Properties();
			FileReader in = new FileReader("agent.properties");
			prop.load(in);
			in.close();
			
			prop.setProperty(item, value);
			
			FileOutputStream out = new FileOutputStream("agent.properties");
			prop.store(out, null);
			out.close();
			
    		//String content = FileUtils.readFileToString(new File("agent.properties"),"UTF-8");
			//String newContent = content.replaceAll(item+"=[A-Za-z0-9-_=.]*",item+"="+value);
			//File path = new File("agent.properties");
			//FileUtils.writeStringToFile(path, newContent, "UTF-8");
    	
    	}catch(IOException e){
			getLogger().log(Level.SEVERE, e.getMessage(), e);
			return -1;
		}
    	
		return 1;

    }
    
}
