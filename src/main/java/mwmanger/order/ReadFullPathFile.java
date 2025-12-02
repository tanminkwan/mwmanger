package mwmanger.order;

import static mwmanger.common.Config.getConfig;

import java.io.File;
import java.util.logging.Level;

import org.json.simple.JSONObject;
import mwmanger.common.SecurityValidator;

public class ReadFullPathFile extends ReadFile {

	// Allowed base directories for reading files
	private static final String[] ALLOWED_READ_PATHS = {
		System.getProperty("user.dir"),           // Agent working directory
		System.getProperty("java.io.tmpdir"),     // Temp directory
		"/var/log",                                // Log directory (Linux)
		"/opt",                                    // Application directory (Linux)
		"C:\\logs",                                // Log directory (Windows)
		"C:\\Program Files"                        // Application directory (Windows)
	};

	public ReadFullPathFile(JSONObject command) {
		super(command);
	}

	protected String getFileFullName() {
		String requestedPath = commandVo.getAdditionalParams();

		// Security validation: check for path traversal and allowed directories (configurable, default ON)
		if (getConfig().isSecurityPathTraversalCheck()) {
			if (!SecurityValidator.isValidAbsolutePath(requestedPath, ALLOWED_READ_PATHS)) {
				getConfig().getLogger().severe("Security: Path traversal or unauthorized path detected: " + requestedPath);
				return null;  // Will cause FileNotFoundException, handled by parent class
			}
		}

		return requestedPath;
	}

	protected String getFileName(){
		return commandVo.getTargetFileName();
	}

	protected String getFilePath(){
		return commandVo.getAdditionalParams();
	}

}
