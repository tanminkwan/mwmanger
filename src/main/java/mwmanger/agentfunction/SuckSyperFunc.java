package mwmanger.agentfunction;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import mwmanger.common.Common;
import static mwmanger.common.Config.getConfig;
import mwmanger.vo.CommandVO;
import mwmanger.vo.ResultVO;

public class SuckSyperFunc implements AgentFunc {

	private Connection conn = null;
	private Statement stmt = null;

	@Override
	public ArrayList<ResultVO> exeCommand(CommandVO command) throws Exception {
	
    	String db_driver = "";
    	String db_uri = "";
    	String db_username = "";
    	String db_password = "";
    	
    	ResultVO rv = new ResultVO();
    	rv.setOk(false);

    	try{
    		
    		JSONParser jsonPar = new JSONParser();
    		JSONObject jsonObj = (JSONObject) jsonPar.parse(command.getTargetFilePath());
        
    		db_driver = (String)jsonObj.get("db_driver");
    		db_uri = (String)jsonObj.get("db_uri");
    		db_username = (String)jsonObj.get("db_username");
    		db_password = (String)jsonObj.get("db_password");
    		
    	}catch(Exception e){
    		getConfig().getLogger().log(Level.SEVERE, e.getMessage(), e);
    		rv.setResult("params parsing error");
		}
    	
    	try{
    		Class.forName(db_driver);
    		conn = DriverManager.getConnection(db_uri, db_username, db_password);
    		stmt = conn.createStatement();
    	}catch(ClassNotFoundException e){
    		getConfig().getLogger().log(Level.SEVERE, e.getMessage(), e);    		
    		rv.setResult("Driver class not found");
    	}catch(SQLException e){
    		getConfig().getLogger().log(Level.SEVERE, e.getMessage(), e);    		
    		rv.setResult("SQLException occured 1");
    	}

    	if(!conn.equals(null)){
    		conn.close();
    	}

    	ArrayList<ResultVO> rvs = gatherContents();
    	
    	if(rvs.isEmpty()){
    		rv.setResult("SQLException occured 2");
    		return Common.makeOneResultArray(rv, command);
    	}else{
    		return rvs;
    	}
    	
	}
	
    private ArrayList<ResultVO> gatherContents() {
    	
    	ArrayList<ResultVO> rvs = new ArrayList<ResultVO>();
    	
    	String select = "SELECT HOSTNAME, FILEPATH, FILENM, FILECONTENTS FROM CFGFILE";
    	String where = "where REGTIME <> '00000000000000'";
    	String sql = select + " " + where;
    	String host_name   = "";
    	String file_path   = "";
    	String file_name   = "";
    	Blob file_contents;
    	String contents = "";
    	
    	try{
        	ResultSet rs = stmt.executeQuery(sql);
        	
        	while(rs.next()){
        		
        		host_name   = rs.getNString("HOSTNAME");
        		file_path   = rs.getNString("FILEPATH");
        		file_name   = rs.getNString("FILENM");
        		file_contents = rs.getBlob("FILECONTENTS");
        		byte[] buff = file_contents.getBytes(1l, (int)file_contents.length());
        		contents = new String(buff);
        		
        		ResultVO rv = new ResultVO();
        		rv.setHostName(host_name);
        		rv.setTargetFileName(file_name);
        		rv.setTargetFilePath(file_path);
        		rv.setResult(contents);
        		
        		rvs.add(rv);
        	}
        	
        	rs.close();
        	
    	}catch(SQLException e){
    		getConfig().getLogger().log(Level.WARNING, e.getMessage(), e);
    		return null;
    	}
    	
    	return rvs;
    }


}
