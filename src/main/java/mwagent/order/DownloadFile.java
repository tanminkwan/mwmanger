package mwagent.order;

import static mwagent.common.Config.getConfig;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.simple.JSONObject;

import mwagent.common.Common;
import mwagent.common.SecurityValidator;
import mwagent.vo.MwResponseVO;
import mwagent.vo.ResultVO;


/**
 * Handles file download requests from the server.
 * 
 * Supported additional_params (JSON):
 * - is_absolute_path: (Boolean) If true, targetFilePath is treated as an absolute path.
 * - backup_if_exists: (Boolean) If true, renames existing file with timestamp suffix before download.
 * - extract: (Boolean) If true and the downloaded file is a zip archive, it will be extracted to the target directory.
 * - chmod: (String) Optional octal permission string (e.g., "755") to apply to downloaded/extracted files (Non-Windows only).
 * - exe_filename: (String) Optional filename to execute after download/extract. On Windows, ".sh" is auto-replaced with ".bat".
 * - exe_params: (String) Optional parameters for the execution.
 * - download_url: (String) Optional external URL to download the file from.
 * 
 * Sample additional_params JSON:
 * {
 *   "is_absolute_path": false,
 *   "backup_if_exists": true,
 *   "extract": true,
 *   "chmod": "755",
 *   "exe_filename": "install.sh",
 *   "exe_params": "-v",
 *   "download_url": "https://example.com/file.zip"
 * }
 */
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
		boolean backupIfExists = false;
		boolean extract = false;
		String chmod = null;
		if (commandVo.getAdditionalParamsJson() != null) {
			Object obj = commandVo.getAdditionalParamsJson().get("backup_if_exists");
			if (obj instanceof Boolean) {
				backupIfExists = (Boolean) obj;
			} else if (obj instanceof String) {
				backupIfExists = Boolean.parseBoolean((String) obj);
			}

			Object extObj = commandVo.getAdditionalParamsJson().get("extract");
			if (extObj instanceof Boolean) {
				extract = (Boolean) extObj;
			} else if (extObj instanceof String) {
				extract = Boolean.parseBoolean((String) extObj);
			}

			Object chmodObj = commandVo.getAdditionalParamsJson().get("chmod");
			if (chmodObj instanceof String) {
				chmod = (String) chmodObj;
			}
		}

		String exeFilename = null;
		String exeParams = "";
		String downloadUrl = null;
		if (commandVo.getAdditionalParamsJson() != null) {
			Object exeFileObj = commandVo.getAdditionalParamsJson().get("exe_filename");
			if (exeFileObj instanceof String) {
				exeFilename = (String) exeFileObj;
			}
			Object exeParamObj = commandVo.getAdditionalParamsJson().get("exe_params");
			if (exeParamObj instanceof String) {
				exeParams = (String) exeParamObj;
			}
			Object urlObj = commandVo.getAdditionalParamsJson().get("download_url");
			if (urlObj instanceof String) {
				downloadUrl = (String) urlObj;
			}
		}

		File existingFile = new File(file_location + targetFileName);
		if (existingFile.exists() && backupIfExists) {
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

		String url;
		String token;
		if (downloadUrl != null && !downloadUrl.isEmpty()) {
			url = downloadUrl;
			token = null; // Don't send manager token to external URL
			getConfig().getLogger().info("Downloading from external URL: " + url);
		} else {
			url = getConfig().getServer_url()
					+ "/api/v1/agent/download/" + getConfig().getAgent_type()
					+ "/" + targetFileName;
			token = getConfig().getAccess_token();
		}

        MwResponseVO mwrv = Common.httpFileDownload(url, token, file_location);

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

				// Apply manual chmod if specified and not on Windows
				if (chmod != null && !getConfig().getOs().equals("WIN")) {
					applyChmod(file_location + fileName, chmod);
				}

				if (extract && fileName.toLowerCase().endsWith(".zip")) {
					try {
						unzipFile(new File(file_location + fileName), new File(file_location));
						getConfig().getLogger().info("File extracted: " + fileName);
						
						// If chmod is specified, apply it to all extracted files as well
						if (chmod != null && !getConfig().getOs().equals("WIN")) {
							applyChmod(file_location, chmod, true); // recursive for directory
						}
					} catch (IOException e) {
						getConfig().getLogger().log(Level.SEVERE, "Failed to extract zip file: " + fileName, e);
						rv.setResult("File download completed but extraction failed: " + e.getMessage());
						return rv;
					}
				}

				rv.setOk(true);
				rv.setTargetFileName(fileName);
				rv.setResult("File download is completed");

				// After all tasks, if exe_filename is provided, execute it
				if (exeFilename != null && !exeFilename.isEmpty()) {
					// Auto-replace .sh with .bat on Windows
					if (getConfig().getOs().equals("WIN") && exeFilename.toLowerCase().endsWith(".sh")) {
						String originalFilename = exeFilename;
						exeFilename = exeFilename.substring(0, exeFilename.length() - 3) + ".bat";
						getConfig().getLogger().info("Windows detected: Auto-replacing " + originalFilename + " with " + exeFilename);
					}

					getConfig().getLogger().info("Triggering execution of: " + exeFilename);
					
					try {
						JSONObject exeCmdJson = new JSONObject();
						exeCmdJson.put("command_id", commandVo.getCommandId());
						exeCmdJson.put("repetition_seq", commandVo.getRepetitionSeq());
						exeCmdJson.put("target_file_path", targetFilePath); 
						exeCmdJson.put("target_file_name", exeFilename);
						exeCmdJson.put("additional_params", exeParams);
						exeCmdJson.put("host_name", commandVo.getHostName());
						exeCmdJson.put("result_receiver", commandVo.getResultReceiver());
						exeCmdJson.put("target_object", commandVo.getTargetObject());
						
						ExeShell exeShell = new ExeShell(exeCmdJson);
						int exeRtn = exeShell.execute();
						
						if (exeRtn == 1) {
							ResultVO exeRv = exeShell.getResultVo();
							rv.setResult(rv.getResult() + " | Execution [" + exeFilename + "] result: " + exeRv.getResult());
						} else {
							rv.setResult(rv.getResult() + " | Execution [" + exeFilename + "] failed with code: " + exeRtn);
						}
					} catch (Exception e) {
						getConfig().getLogger().log(Level.WARNING, "Error during post-download execution", e);
						rv.setResult(rv.getResult() + " | Execution error: " + e.getMessage());
					}
				}

			}
		}

		return rv;

    }

	private void unzipFile(File zipFile, File destDir) throws IOException {
		if (!destDir.exists()) {
			destDir.mkdirs();
		}

		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				File outFile = new File(destDir, entry.getName());
				if (entry.isDirectory()) {
					if (!outFile.exists()) {
						outFile.mkdirs();
					}
				} else {
					File parentDir = outFile.getParentFile();
					if (!parentDir.exists()) {
						parentDir.mkdirs();
					}
					try (FileOutputStream fos = new FileOutputStream(outFile)) {
						byte[] buffer = new byte[4096];
						int len;
						while ((len = zis.read(buffer)) != -1) {
							fos.write(buffer, 0, len);
						}
					}
				}
				zis.closeEntry();
			}
		}
	}

	private void applyChmod(String path, String mode) {
		applyChmod(path, mode, false);
	}

	private void applyChmod(String path, String mode, boolean recursive) {
		try {
			String cmd = "chmod " + (recursive ? "-R " : "") + mode + " " + path;
			Runtime.getRuntime().exec(cmd);
			getConfig().getLogger().info("Applied permission: " + cmd);
		} catch (IOException e) {
			getConfig().getLogger().warning("Failed to apply chmod: " + e.getMessage());
		}
	}

}
