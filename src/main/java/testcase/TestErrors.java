package testcase;

import org.json.simple.JsonObject;

import common.Util;
import http.MyHttpClient;
import reflection.RefCode;

public class TestErrors extends MyTestCase {
	String message = "message";
	String code = "code";
	
	static String prod = "34.125.38.193";
	static String local = "localhost";
	static String host = local;
	
	/** This version does not include the cookie */
	public static JsonObject sendData( String data) throws Exception {
		MyHttpClient cli = new MyHttpClient( host, port);
		cli.post( "/api", Util.easyJson( data) );
		return cli.readJsonObject();
	}
	
	public void testMalformedJson() throws Exception {
		String data = "{ 'nomsg': 'nodata', }";
		
		JsonObject map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.getString(code) );
		assertEquals( "The 'msg' parameter is missing or the URI path is invalid", map.getString(message) ); 
	}
	
	public void testMissingMsg() throws Exception {
		String data = "{ 'nomsg': 'nodata' }";
		
		JsonObject map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.getString(code) );
		assertEquals( "The 'msg' parameter is missing or the URI path is invalid", map.getString(message) ); 
	}
	
	public void testInvalidMsg() throws Exception {
		String data = "{ 'msg': 'notamsg' }";
		JsonObject map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.getString(code) );
		assertStartsWith( "Param 'msg' has an invalid value", map.getString(message) ); 
	}

	public void testJson2() throws Exception {
		String data = "{ 'msg': 'value' ";
		JsonObject map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.getString(code) );
		assertTrue( ((String)(map.getString(message))).startsWith( "Error parsing json") );
	}
	
} 	
