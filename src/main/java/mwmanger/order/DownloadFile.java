package mwmanger.order;

import static mwmanger.common.Config.getConfig;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import org.json.simple.JSONObject;

import mwmanger.common.Common;
import mwmanger.common.SecurityValidator;
import mwmanger.vo.MwResponseVO;
import mwmanger.vo.ResultVO;


public class DownloadFile extends Order {

	public DownloadFile(JSONObject command) {
		super(command);
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

		ResultVO rv = new ResultVO();
		rv.setOk(false);
		rv.setHostName(getConfig().getHostName());
		rv.setTargetFileName(commandVo.getTargetFileName());

		String baseDir = System.getProperty("user.dir");
		String targetFilePath = commandVo.getTargetFilePath();

		boolean isAbsolutePath = false;
		if (commandVo.getAdditionalParamsJson() != null) {
			Object obj = commandVo.getAdditionalParamsJson().get("is_absolute_path");
			if (obj instanceof Boolean) {
				isAbsolutePath = (Boolean) obj;
			} else if (obj instanceof String) {
				isAbsolutePath = Boolean.parseBoolean((String) obj);
			}
		}

		// Security validation: check path traversal (configurable, default ON)
		String targetFileName = commandVo.getTargetFileName();
		if (getConfig().isSecurityPathTraversalCheck()) {
			if (isAbsolutePath) {
				// For absolute path, check for path traversal patterns
				if (targetFilePath.contains("..")) {
					getConfig().getLogger().severe("Security: Path traversal detected in absolute targetFilePath: " + targetFilePath);
					rv.setResult("Error:SecurityException - Invalid absolute path");
					return rv;
				}
			} else {
				if (!SecurityValidator.isValidPath(baseDir, targetFilePath)) {
					getConfig().getLogger().severe("Security: Path traversal detected in targetFilePath: " + targetFilePath);
					rv.setResult("Error:SecurityException - Invalid path");
					return rv;
				}
			}

			// Security validation: check filename
			if (!SecurityValidator.isValidFilename(targetFileName)) {
				getConfig().getLogger().severe("Security: Invalid filename: " + targetFileName);
				rv.setResult("Error:SecurityException - Invalid filename");
				return rv;
			}
		}

		String file_location;
		if (isAbsolutePath) {
			file_location = targetFilePath;
		} else if (getConfig().isSecurityPathTraversalCheck()) {
			try {
				file_location = SecurityValidator.getValidatedPath(baseDir, targetFilePath);
			} catch (SecurityException e) {
				getConfig().getLogger().severe("Security: " + e.getMessage());
				rv.setResult("Error:SecurityException - " + e.getMessage());
				return rv;
			}
		} else {
			file_location = baseDir + File.separator + targetFilePath;
		}

		if (!file_location.endsWith(File.separator)) {
			file_location += File.separator;
		}

		// Check if file exists and rename if necessary before downloading
		File existingFile = new File(file_location + targetFileName);
		if (existingFile.exists()) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd.HHmmss");
			String timestamp = sdf.format(new Date());
			String newFileName = file_location + targetFileName + "_" + timestamp;
			File destFile = new File(newFileName);
			if (existingFile.renameTo(destFile)) {
				getConfig().getLogger().info("Existing file renamed to: " + newFileName);
			} else {
				getConfig().getLogger().warning("Failed to rename existing file: " + targetFileName);
			}
		}

		String url = getConfig().getServer_url()
        		+ "/api/v1/agent/download/" + getConfig().getAgent_type()
        		+ "/" + targetFileName;

        MwResponseVO mwrv = Common.httpFileDownload(url, getConfig().getAccess_token(), file_location);

		rv.setTargetFilePath(file_location);

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
