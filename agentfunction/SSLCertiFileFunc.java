package mwmanger.agentfunction;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.logging.Level;

import mwmanger.common.Common;
import mwmanger.vo.CommandVO;
import mwmanger.vo.ResultVO;
import static mwmanger.common.Config.getConfig;

public class SSLCertiFileFunc  implements AgentFunc {

	@Override
	public ArrayList<ResultVO> exeCommand(CommandVO command) throws Exception {

		String param = command.getAdditionalParams();
		
        InputStream is = null;
		BufferedReader br = null;

		String line;
		String result = "";
		boolean is_get = false;
		
    	ResultVO rv = new ResultVO();
    	rv.setOk(false);
    	
		try {
			
			String ext = "";
			int idx = param.lastIndexOf('.');
			if(idx >= 0){ ext = param.substring(idx+1); }
			
			if(ext.equals("pem")){
			
				br = new BufferedReader(new FileReader(param));
				while((line=br.readLine())!=null){
		
					if (line.contains("BEGIN CERTIFICATE")){
						is_get = true;
					}else if(line.contains("END CERTIFICATE")){
						result += line;
						break;
					}
					
					if(is_get == true){
						result += line + "\n";
					}
					
				}
				
				//System.out.println("[Cetri2]"+result);
				is = new ByteArrayInputStream(result.getBytes());
			}else{
				is = new FileInputStream(param);
			}
			
			CertificateFactory fact = CertificateFactory.getInstance("X.509");
			X509Certificate cer = (X509Certificate) fact.generateCertificate(is);	
			
	    	StringBuilder json = new StringBuilder();
	        
	        //System.out.println("getType : "+cer.getType());
			//System.out.println("getVersion : "+Integer.toString(cer.getVersion()));
			//System.out.println("getNotAfter : "+cer.getNotAfter().toString());
			//System.out.println("getNotBefore : "+cer.getNotBefore().toString());
			//System.out.println("getSubjectDN : "+cer.getSubjectDN().getName());
			//System.out.println("getSerialNumber : "+cer.getSerialNumber().toString());
			//System.out.println("getSerialNumber 16 : "+cer.getSerialNumber().toString(16));
			//System.out.println("getSerialNumber 16 z: "+String.format("%032X", cer.getSerialNumber()));
			//System.out.println("getIssuerDN : "+cer.getIssuerDN().getName());
	            
			json.append("{");
			json.append("\"certifile\":\"" + Common.escape(param) + "\",");
			json.append("\"notafter\":\""+cer.getNotAfter().toString()+"\",");
			json.append("\"notbefore\":\""+cer.getNotBefore().toString()+"\",");
			json.append("\"serial\":\""+String.format("%032X", cer.getSerialNumber())+"\",");
			json.append("\"issuer\":\""+cer.getIssuerDN().getName()+"\",");
	        json.append("\"subject\":\""+cer.getSubjectDN().getName()+"\"");
	        json.append("}");
	        rv.setResult(json.toString());
	        rv.setOk(true);
		}
        catch (CertificateException e){
        	getConfig().getLogger().log(Level.WARNING, e.getMessage(), e);
        	rv.setResult("CertificateException occured");
        }
        catch (FileNotFoundException e){
        	getConfig().getLogger().log(Level.WARNING, e.getMessage(), e);
        	rv.setResult("FileNotFoundException occured");
        }        
        finally {
            if (is != null) {
            	is.close();
            }
            if (br != null) {
            	br.close();
            }
        }
		
		return Common.makeOneResultArray(rv, command);
		
	}

}
