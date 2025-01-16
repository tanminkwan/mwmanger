package mwmanger.agentfunction;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import mwmanger.common.Common;
import mwmanger.common.Config;
import mwmanger.vo.CommandVO;
import mwmanger.vo.ResultVO;

public class SSLCertiFunc implements AgentFunc {

	@Override
	public ArrayList<ResultVO> exeCommand(CommandVO command) throws Exception {

		String param = command.getAdditionalParams();
		
		//String url = "https://" + param;
		
		//httpsGet(url);
		
		String[] dnp = getDomainNPort(param);
		
		String domain = dnp[0];
		String port = dnp[1];
				
		X509Certificate[] validCerts = checkSSLCertificate(domain, "127.0.0.1", Integer.parseInt(port));
		
		ResultVO rv = printValidCerts(param, validCerts);
		
		return Common.makeOneResultArray(rv, command);
	
	}
	
	public ResultVO printValidCerts(String domain, X509Certificate[] certs) {
		
		ResultVO rv = new ResultVO();
		rv.setOk(false);

    	StringBuilder json = new StringBuilder();
        json.append("{\"domain\":\"" + domain + "\",\"certs\":[");

        int i = 0;
		for(X509Certificate c:certs){
			i++;
			json.append("{");
			json.append("\"index\":\""+Integer.toString(i)+"\",");
			json.append("\"notafter\":\""+c.getNotAfter().toString()+"\",");
			json.append("\"notbefore\":\""+c.getNotBefore().toString()+"\",");
			json.append("\"serial\":\""+String.format("%032X", c.getSerialNumber())+"\",");
			json.append("\"issuer\":\""+c.getIssuerDN().getName()+"\",");
            json.append("\"subject\":\""+c.getSubjectDN().getName()+"\"");
            json.append("}");
            if(i!=certs.length){
            	json.append(",");
            }
		}
    
		json.append("]}");
		rv.setResult(json.toString());
		rv.setOk(true);

        return rv;
        
	}

	public String[] getDomainNPort(String param) {
		
		String domain;
		String port;
		
		if(param.contains(":")){
			String[] parts = param.split(":");

			domain = parts[0];
			port = parts[1];
		}
		else{
			domain = param;
			port = "443";
		}

		return new String[]{domain, port};
	}
	
	public X509Certificate[] checkSSLCertificate(String domain, String ip, int port) {
		
		List<X509Certificate> validCerts = new ArrayList<>();
		
		try{
			
			Security.addProvider(new BouncyCastleProvider());
			
			SSLContext context = SSLContext.getInstance("TLSv1.2");
			context.init(null, null, null);
			SSLSocketFactory factory = context.getSocketFactory();
			
			try(SSLSocket socket = (SSLSocket) factory.createSocket()){
				
				socket.connect(new InetSocketAddress(ip, port));
				SSLParameters sslParams = socket.getSSLParameters();
				//sslParams.setEndpointIdentificationAlgorithm("HTTPS");
				socket.setSSLParameters(sslParams);
				socket.startHandshake();
				
				SSLSession session = socket.getSession();
				Certificate[] certificates = session.getPeerCertificates();
				
				for(Certificate certificate : certificates ){
					
					
					if (certificate instanceof X509Certificate) {
						
						X509Certificate cert = (X509Certificate) certificate;
						
						if (isCertificateValidForDomain(cert, domain))validCerts.add(cert);
						
					}
				}
			}
			
		} catch (Exception e){
			e.printStackTrace();
			System.out.println("Failed to check SSL certificate: " + e.getMessage());
			return new X509Certificate[0];
		}
		
		return validCerts.toArray(new X509Certificate[0]);
		
	}
	
	private boolean isCertificateValidForDomain(X509Certificate cert, String domain) {
    	
    	if(domain.equals("*"))return true;
    	
        try {
            // Get subject alternative names (SANs) from the certificate
            Collection<List<?>> sanEntries = cert.getSubjectAlternativeNames();
            if (sanEntries != null) {
                for (List<?> sanEntry : sanEntries) {
                    if (sanEntry != null && sanEntry.size() >= 2) {
                        Object sanValue = sanEntry.get(1);
                        if (sanValue instanceof String && domain.equalsIgnoreCase((String) sanValue)) {
                            return true;
                        }
                    }
                }
            }

            // Check the common name (CN) in the subject if SANs are not present
            String subjectDN = cert.getSubjectX500Principal().getName();
            String cn = getCommonName(subjectDN);
			return matchesDomain(cn, domain);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String getCommonName(String subjectDN) {
        String[] dnComponents = subjectDN.split(",");
        for (String component : dnComponents) {
            component = component.trim();
            if (component.startsWith("CN=")) {
                return component.substring(3);
            }
        }
        return null;
    }

	private boolean matchesDomain(String certDomain, String domain) {
		if(certDomain.startsWith("*.")){
			String wildCardBase = certDomain.substring(2);
			return domain.endsWith(wildCardBase) && domain.split(".").length==certDomain.split(".").length;
		}
		return certDomain.equalsIgnoreCase(domain);
	}
    
	/*
    private void httpsGet(String strURL) throws Exception
    {
        URL url = null;
        HttpsURLConnection con = null;
        
        try {
            url = new URL(strURL);
            ignoreSsl();
            con = (HttpsURLConnection) url.openConnection();
            con.setConnectTimeout(3000);
            con.getInputStream();

        }
        catch (UnknownHostException e){
        	Config.getLogger().log(Level.WARNING, e.getMessage(), e);
        	rv.setResult("UnknownHostException occured");
        }
        catch (ConnectException e){
        	Config.getLogger().log(Level.WARNING, e.getMessage(), e);
        	rv.setResult("ConnectException occured");
        }
        catch (SocketTimeoutException e){
        	Config.getLogger().log(Level.WARNING, e.getMessage(), e);
        	rv.setResult("SocketTimeoutException occured");
        }        
        catch (IOException e) {
        } 
        finally {
            if (con != null) {
                con.disconnect();
            }
        }
        
    }
    
    private void ignoreSsl() throws Exception{
        HostnameVerifier hv = new HostnameVerifier() {
        public boolean verify(String urlHostName, SSLSession session) {
                return true;
            }
        };
        trustAllHttpsCertificates();
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
    }

    private void trustAllHttpsCertificates() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[1];
        TrustManager tm = new miTM();
        trustAllCerts[0] = tm;
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }
 
    class miTM implements TrustManager,X509TrustManager {

    	public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
 
        public boolean isServerTrusted(X509Certificate[] certs) {
            return true;
        }
 
        public boolean isClientTrusted(X509Certificate[] certs) {
            return true;
        }
 
        public void checkServerTrusted(X509Certificate[] certs, String authType)
                throws CertificateException {

        	StringBuilder json = new StringBuilder();
            json.append("{\"domain\":\"" + domain + "\",\"certs\":[");

            int i = 0;
    		for(X509Certificate c:certs){
    			i++;
    			//System.out.println("getType : "+c.getType());
    			//System.out.println("getVersion : "+Integer.toString(c.getVersion()));
    			//System.out.println("getNotAfter : "+c.getNotAfter().toString());
    			//System.out.println("getNotBefore : "+c.getNotBefore().toString());
    			//System.out.println("getSubjectDN : "+c.getSubjectDN().getName());
    			//System.out.println("getSerialNumber : "+c.getSerialNumber().toString());
    			//System.out.println("getSerialNumber 16 : "+c.getSerialNumber().toString(16));
    			//System.out.println("getSerialNumber 16 z: "+String.format("%032X", c.getSerialNumber()));
    			//System.out.println("getIssuerDN : "+c.getIssuerDN().getName());
                
    			json.append("{");
    			json.append("\"index\":\""+Integer.toString(i)+"\",");
    			json.append("\"notafter\":\""+c.getNotAfter().toString()+"\",");
    			json.append("\"notbefore\":\""+c.getNotBefore().toString()+"\",");
    			json.append("\"serial\":\""+String.format("%032X", c.getSerialNumber())+"\",");
    			json.append("\"issuer\":\""+c.getIssuerDN().getName()+"\",");
                json.append("\"subject\":\""+c.getSubjectDN().getName()+"\"");
                json.append("}");
                if(i!=certs.length){
                	json.append(",");
                }
    		}
        
    		json.append("]}");
    		rv.setResult(json.toString());
    		rv.setOk(true);

            return;
        }
 
        public void checkClientTrusted(X509Certificate[] certs, String authType)
        		throws CertificateException {
        	return;
        }
    }
	*/
}
