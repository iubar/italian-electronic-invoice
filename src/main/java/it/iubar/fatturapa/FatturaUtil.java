package it.iubar.fatturapa;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.apache.commons.codec.binary.Base64;

public class FatturaUtil {
	
	public static final String DATA_TAG = "data";
	public static final String CODE_TAG = "code";
	public static final String RESPONSE_TAG = "response";
	public static final String XML_TAG = "xml";
	public static final String ERROR_TAG = "error";
	
	public static final String USER_PARAM = "user";
	public static final String TIMESTAMP_PARAM = "ts";
	public static final String SIGNATURE_PARAM = "hash";
	
	private static String API_KEY = setApi();
	private static String USER = "user@user.it";
	
	public static String getUser() {
		return USER;
	}

	public static void setUser(String user) {
		USER = user;
	}
	
	public static String getApi(){
		return API_KEY;
	}
	
	private static String setApi(){
		Properties prop = new Properties();
		InputStream input = null;
		
		try{
			input = new FileInputStream("src/main/resources/apikey.ini");
			prop.load(input);
			return prop.getProperty("apikey");
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static String getResponse(String url){
		try {
			Client client = Client.create();
			WebResource webResource = client.resource(url);
			
			ClientResponse response = webResource.queryParam(USER_PARAM, getUser()).queryParam(TIMESTAMP_PARAM, getTimeStamp()).queryParam(SIGNATURE_PARAM, getSignature()).accept("application/json").get(ClientResponse.class);
			
			if (response.getStatus() != 200) {
				System.out.println("Code: " + response.getStatus());
			}
			return response.getEntity(String.class);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Deprecated
	//Use getInfo Instead
	public static String getXmlString(String url) throws Exception{
		JSONObject jsonObject = new JSONObject(FatturaUtil.getResponse(url));
		JSONObject data = jsonObject.getJSONObject(DATA_TAG);
		return data.getString(XML_TAG);
	}
	
	public static String getInfo(String url, String tag){
		JSONObject jsonObject = new JSONObject(FatturaUtil.getResponse(url));
		if(tag.equalsIgnoreCase(XML_TAG)|| tag.equalsIgnoreCase(ERROR_TAG)){
			JSONObject data = jsonObject.getJSONObject(DATA_TAG);
			return data.getString(tag);
		}else if(tag.equalsIgnoreCase(CODE_TAG)){
			return String.valueOf(jsonObject.getInt(CODE_TAG));
		}else if(tag.equalsIgnoreCase(DATA_TAG)){
			JSONObject data = jsonObject.getJSONObject(tag);
			return data.toString();
		}
		return jsonObject.getString(tag);
	}
	
	public static Document getXmlDocument(String url) throws Exception{
		
		String data = FatturaUtil.getResponse(url);
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	   	return builder.parse(new InputSource(new StringReader(data)));
	}
	
	private static String getPayLoad(){
		return FatturaUtil.getUser() + getTimeStamp() + FatturaUtil.getApi();
	}
	
	private static String getTimeStamp(){
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date());
	}

	public static String getSignature() {
		String payload = FatturaUtil.getPayLoad();
		String algo = "HmacSHA256";
		String keyString = FatturaUtil.getApi();
		try{
			Mac sha256_HMAC = Mac.getInstance(algo);
		     SecretKeySpec secret_key = new SecretKeySpec(keyString.getBytes(), algo);
		     sha256_HMAC.init(secret_key);
		     String hash = Base64.encodeBase64String(sha256_HMAC.doFinal(payload.getBytes()));
		     return hash;
		}catch (Exception e){
			System.out.println("Error");
			return null;
		}
	}
}