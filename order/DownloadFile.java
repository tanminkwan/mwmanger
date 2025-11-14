package mwmanger.order;

import static mwmanger.common.Config.getConfig;

import java.io.File;
import java.util.logging.Level;

import org.json.simple.JSONObject;

import mwmanger.common.Common;
import mwmanger.vo.MwResponseVO;
import mwmanger.vo.ResultVO;


public class DownloadFile extends Order {

	public DownloadFile(JSONObject command) {
		super(command);
		// TODO Auto-generated constructor stub
	}

	public int execute() {

		try {
			
			resultVo = downloadFile();
			
		}catch (Exception e) {
			getConfig().getLogger().log(Level.WARNING, e.getMessage(), e);    		
		}	

		return 1;
	}

    private ResultVO downloadFile() {

        String url = getConfig().getServer_url() 
        		+ "/api/v1/agent/download/" + getConfig().getAgent_type() 
        		+ "/" + commandVo.getTargetFileName();
        
        String file_location = System.getProperty("user.dir") + File.separator + commandVo.getTargetFilePath();

        MwResponseVO mwrv = Common.httpFileDownload(url, getConfig().getAccess_token(), file_location);

		ResultVO rv = new ResultVO();

		rv.setOk(false);
		rv.setTargetFilePath(file_location);
		rv.setHostName(getConfig().getHostName());
		rv.setTargetFileName(commandVo.getTargetFileName());		
		
		int rtn_code = mwrv.getStatusCode();
		getConfig().getLogger().fine("file download response code :"+Integer.toString(rtn_code));

		if(rtn_code < 200 || rtn_code >= 300){
			
			rv.setResult("file download request error :["+Integer.toString(rtn_code)+"]");
			
		}else{

			String fileName = mwrv.getFileName();
				
	        if(fileName.equals("")){
	        	
	        	rv.setResult("No response Error");
	        	
	        }else{
	        	
	        	if(fileName.endsWith(".sh")){
	    			File f = new File(file_location + fileName);
	    			f.setExecutable(true, true);
	        	}
	        	
				rv.setOk(true);
				rv.setTargetFileName(fileName);
				rv.setResult("File download is completed");
	        	
			}
		}
		
		return rv;

    }

}
