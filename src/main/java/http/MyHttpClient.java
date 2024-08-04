package http;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import junit.framework.TestCase;
import reflection.RefCode;
import tw.util.S;

/** @deprecated, use MyClient
 *  Good for testing, don't use this in production, there are many things not handled. */
public class MyHttpClient {
	static String host = "http://localhost:8383";

	private ArrayList<String> m_reqHeaders = new ArrayList<String>();
	private HashMap<String,String> m_respHeaders = new HashMap<>();
	private int m_responseCode;
	private String m_data;
	
	public MyHttpClient() throws Exception {
	}
	
	/** @param uri may or not start with / 
	 * @return */
	public MyHttpClient get( String uri) throws Exception {
		if (!uri.startsWith("/") ) {
			uri = "/" + uri;
		}
		
		String url = String.format( "%s%s", host, uri);

		return process( MyClient.create( url)
			.addHeaders( m_reqHeaders)
			.query()
			);
	}

	public MyHttpClient post( String uri, String data) throws Exception {
		String url = String.format( "%s%s", host, uri);

		return process( MyClient.create( url, data)
				.addHeaders( m_reqHeaders)
				.query()
				);
		
	}

	private MyHttpClient process(HttpResponse<String> resp) {
		m_data = resp.body();
		m_responseCode = resp.statusCode(); 

		// NOTE that if the there are two headers with the same key,
		// we take the first one only
		resp.headers().map().forEach( (key,vals) -> {
			for (var val : vals) {
				m_respHeaders.put( key, val);
				break;
			}
		});
		
		return this;
	}
	
	public JsonObject readJsonObject() throws Exception {
		return JsonObject.parse( readString() );
	}
	
	public JsonArray readJsonArray() throws Exception {
		return JsonArray.parse( readString() );
	}
	
	public String readString() throws Exception {
		return m_data;
	}
	
	/** for sending */
	public MyHttpClient addHeader(String key, String val) {
		m_reqHeaders.add( String.format( "%s:%s", key, val) );
		return this;
	}

	public void dumpHeaders() {
	}

	public HashMap<String,String> getHeaders() throws Exception {
		return m_respHeaders;
	}

	public int getResponseCode() throws Exception {
		return m_responseCode;
	}

	/** The messages can change; it's better to create a custom code and use getCode() */
	public String getMessage() throws Exception {
		return readJsonObject().getString("message");
	}
	
	// let this return the enum
	public RefCode getRefCode() throws Exception {
		String code = readJsonObject().getString("code");
		return S.isNull( code) ? null : Util.getEnum( code, RefCode.values() );
	}

	public String getUId() throws Exception {
		return readJsonObject().getString("id");
	}

	public void assertResponseCode(int code) throws Exception {
		TestCase.assertEquals(code, getResponseCode() );
	}

	public JsonObject postToJson(String url, JsonObject json) throws Exception {
		return postToJson( url, json.toString() );
	}

	public JsonObject postToJson(String url, String data) throws Exception {
		post(url, data);
		return readJsonObject();
	}

	public JsonObject getToJson(String url) throws Exception {
		get(url);
		return readJsonObject();
	}
}
