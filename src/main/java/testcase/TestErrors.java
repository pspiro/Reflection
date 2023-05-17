package testcase;

import http.MyHttpClient;
import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.RefCode;
import reflection.Util;
import tw.util.S;

public class TestErrors extends TestCase {
	String message = "message";
	String code = "code";
	
	static String prod = "34.125.38.193";
	static String local = "localhost";
	static String host = local;
	
	/** This version does not include the cookie */
	public static MyJsonObject sendData( String data) throws Exception {
		MyHttpClient cli = new MyHttpClient( host, 8383);
		cli.post( Util.toJson( data) );
		return cli.readMyJsonObject();
	}
	
	public void testMalformedJson() throws Exception {
		String data = "{ 'nomsg': 'nodata', }";
		
		MyJsonObject map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.getString(code) );
		assertEquals( "The 'msg' parameter is missing or the URI path is invalid", map.getString(message) ); 
	}
	
	public void testMissingMsg() throws Exception {
		String data = "{ 'nomsg': 'nodata' }";
		
		MyJsonObject map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.getString(code) );
		assertEquals( "The 'msg' parameter is missing or the URI path is invalid", map.getString(message) ); 
	}
	
	public void testInvalidMsg() throws Exception {
		String data = "{ 'msg': 'notamsg' }";
		MyJsonObject map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.getString(code) );
		assertStartsWith( "Param 'msg' has invalid value", map.getString(message) ); 
	}
	
	public static void assertStartsWith(String expected, Object actual) {
		assertEquals( expected, actual.toString().substring( 0, expected.length() ) );
	}

	public void testJson2() throws Exception {
		String data = "{ 'msg': 'value' ";
		MyJsonObject map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.getString(code) );
		assertTrue( ((String)(map.getString(message))).startsWith( "Error parsing json") );
	}
	
} 	
