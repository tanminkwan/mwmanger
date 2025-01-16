package mwmanger.common;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import mwmanger.vo.CommandVO;
import mwmanger.vo.MwResponseVO;
import mwmanger.vo.ResultVO;

public class Common {
	
	private static CloseableHttpClient httpClient = null;
	private static CloseableHttpClient httpsClient = null;

	public static ArrayList<ResultVO> makeOneResultArray(ResultVO rv, CommandVO command){
		ArrayList<ResultVO> rvs = new ArrayList<ResultVO>();
		rv.setHostName(command.getHostName());
		rv.setTargetFileName(command.getTargetFileName());
		rv.setTargetFilePath(command.getTargetFilePath());
		rvs.add(rv);
		return rvs;
	}

	public static ResultVO fillResult(ResultVO rv, CommandVO command){
		rv.setHostName(command.getHostName());
		rv.setTargetFileName(command.getTargetFileName());
		rv.setTargetFilePath(command.getTargetFilePath());
		return rv;
	}

	private static CloseableHttpClient getHttpClient(String url){
		
		if(httpsClient==null || httpClient==null)createHttpsClient();
		
		if (url.toLowerCase().startsWith("https")) {
			return httpsClient;
		}else{
			return httpClient;
		}		    	
	}
	
    public static void createHttpsClient() {
    	
    	// 0. TLSv1.2 용 Security Provider 선택
    	if(Config.getOs().equalsIgnoreCase("AIX")){
    		Security.addProvider(new BouncyCastleProvider());
    	}
    		
	    // 1. SSLContext 생성 : 모든 인증서를 신뢰하도록 설정    		
	    SSLContext sslContext = null;
		try {
			sslContext = SSLContexts.custom()
						.setProtocol("TLSv1.2")
						.loadTrustMaterial(null, (certificate, authType) -> true)
						.build();
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
        	Config.getLogger().log(Level.SEVERE, e.getMessage(), e);
			e.printStackTrace();
		}
	    	
	    // 2. SSLConnectionScoketFactory 생성 : 호스트네임 검증 비활설화
	    SSLConnectionSocketFactory sslScoketFactory = new SSLConnectionSocketFactory(
	    				sslContext,
	    				NoopHostnameVerifier.INSTANCE // 호스트네임 검증 비활성화
	    			);
	    	
	    // 3. CloseableHttpClient 생성: 커스텀 SSL socker factory 사용
	    httpsClient = HttpClients.custom()
	                .setSSLSocketFactory(sslScoketFactory)
	                .build();
	    
	    // 4. http 
	    httpClient = HttpClients.createDefault();
    	
    }

    public static MwResponseVO httpPOST(String uri, String token, String data) {
    	
    	MwResponseVO mrvo = new MwResponseVO();
    	
        try {
			
        	HttpClient httpClient = getHttpClient(uri);

			HttpPost request = new HttpPost(uri);
			request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer "+token);
	        request.setEntity(new StringEntity(data));

			HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            
            mrvo.setStatusCode(response.getStatusLine().getStatusCode());
            
            if (entity != null) {
            	
            	String value = EntityUtils.toString(entity);
    			
                JSONParser jsonPar = new JSONParser();
                JSONObject jsonObj = null;
                
                try {
                	jsonObj = (JSONObject) jsonPar.parse(value);
                }catch(ParseException e){
                	Config.getLogger().warning("JSON Parsing Error  data : "+value);
                } 
                
                mrvo.setResponse(jsonObj);
                
            }
        	
        } catch(IOException e){
        	Config.getLogger().warning("HTTP execution failed" + " : " + uri);
        	mrvo.setStatusCode(-104);
        }catch(Exception e){
        	Config.getLogger().log(Level.WARNING, e.getMessage(), e);
        	mrvo.setStatusCode(-105);
        }
        
        return mrvo;
        
    }
    
    public static MwResponseVO httpGET(String uri, String token) {
    	
    	MwResponseVO mrvo = new MwResponseVO();
    	
        try {
			
        	HttpClient httpClient = getHttpClient(uri);
            
        	HttpGet request = new HttpGet(uri);
            request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer "+token);

			HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            
            mrvo.setStatusCode(response.getStatusLine().getStatusCode());
            
            if (entity != null) {
            	
            	String value = EntityUtils.toString(entity);
    			
                JSONParser jsonPar = new JSONParser();
                JSONObject jsonObj = null;
                
                try {
                	jsonObj = (JSONObject) jsonPar.parse(value);
                }catch(ParseException e){
                	Config.getLogger().warning("JSON Parsing Error  data : "+value);
                } 
                
                mrvo.setResponse(jsonObj);
                
            }
        	
        } catch(IOException e){
        	Config.getLogger().warning("HTTP execution failed" + " : " + uri);
        	mrvo.setStatusCode(-110);
        }catch(Exception e){
        	Config.getLogger().log(Level.WARNING, e.getMessage(), e);
        	mrvo.setStatusCode(-111);
        }
        
        return mrvo;
        
    }

    public static MwResponseVO httpFileDownload(String uri, String token, String file_location) {
    	
    	MwResponseVO mrvo = new MwResponseVO();
    	
    	String fullname = "";
    	
        try {

        	HttpClient httpClient = getHttpClient(uri);

        	HttpGet request = new HttpGet(uri);
            request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer "+token);

			HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            
            int returnCode = response.getStatusLine().getStatusCode();
            
            mrvo.setStatusCode(returnCode);
            Config.getLogger().fine("File Download status code  " + returnCode);	
            
            if (returnCode >= 200 && returnCode < 300 && entity != null) {            	
            	
				String name = response.getFirstHeader("Content-Disposition").getValue();
				String filename = name.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");
				fullname = file_location + filename;
				FileOutputStream fos = new FileOutputStream(fullname);
				entity.writeTo(fos);
				fos.close();
                
				mrvo.setFileName(filename);
				mrvo.setFileLocation(file_location);
                
            }
            
		}catch(FileNotFoundException e){
        	Config.getLogger().log(Level.WARNING, e.getMessage(), e);
        	mrvo.setStatusCode(-101);
        } catch(IOException e){
        	Config.getLogger().log(Level.WARNING, e.getMessage(), e);
        	mrvo.setStatusCode(-100);
        } catch(Exception e){
        	Config.getLogger().log(Level.WARNING, e.getMessage(), e);
        	mrvo.setStatusCode(-102);
        }
        
        return mrvo;
        
    }

	public static long updateRefreshToken(){

        int rtn = 0;
        
		String uri =  Config.getServer_url() + "/api/v1/agent/getRefreshToken/" + Config.getAgent_id();
			
		Config.getLogger().fine("updateRefreshToken uri : "+uri);

		MwResponseVO mrvo = Common.httpGET(uri, Config.getAccess_token());

        if(mrvo.getStatusCode()!=200){
        	
        	Config.getLogger().severe("getRefreshToken response error : "+Integer.toString(mrvo.getStatusCode()));
            rtn = -1;
            
        }else if(mrvo.getResponse() != null) {
            	
            String refresh_token = (String)mrvo.getResponse().get("refresh_token");
            Config.getLogger().fine("refresh_token :"+refresh_token);
            Config.setRefresh_token(refresh_token);
            rtn = 1;
            
        }else{
        	
        	rtn = -2;
        	
        }
        
        return rtn;
    	
    }

	public static int applyRefreshToken(){

		long rtn = Common.updateRefreshToken();
	
		if(rtn<0){
			Config.getLogger().severe("Failed when getting a Refresh Token.");
			return -1;
		}

		rtn = Config.updateProperty("token", Config.getRefresh_token());

		if(rtn<0){
			Config.getLogger().severe("Failed when updating Refresh Token propery.");
			return -2;
		}
		
		return 1;
		
	}
	
	public static int updateToken() {

        int rtn = 0;
        
        String uri =  Config.getServer_url() + "/api/v1/security/refresh";
		Config.getLogger().fine("updateToken uri : "+uri);
		MwResponseVO mrvo = Common.httpPOST(uri, Config.getRefresh_token(), "");
		
		Config.getLogger().fine("updateToken response code : "+Integer.toString(mrvo.getStatusCode()));
            
        if (mrvo.getResponse() != null) {
            
        	String access_token = (String)mrvo.getResponse().get("access_token");
        	Config.setAccess_token(access_token);
            Config.getLogger().fine("access_token :"+access_token);
            rtn = 1;
            
        }else{
        	rtn = -1;
    	}
        
        return rtn;
    }

    public static String escape(String raw) {
        String escaped = raw;
        escaped = escaped.replace("\\", "\\\\");
        escaped = escaped.replace("\"", "\\\"");
        escaped = escaped.replace("\b", "\\b");
        escaped = escaped.replace("\f", "\\f");
        escaped = escaped.replace("\n", "\\n");
        escaped = escaped.replace("\r", "\\r");
        escaped = escaped.replace("\t", "\\t");

        return escaped;
    }

}
