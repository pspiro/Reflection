package testcase;

import java.text.SimpleDateFormat;

import http.MyHttpClient;
import json.MyJsonArray;
import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.RefCode;
import reflection.Util;
import tw.util.S;

public class TestErrors extends TestCase {
	String text = "text";
	String code = "code";
	
	
	public void testMalformedJson() throws Exception {
		String data = "{ 'nomsg': 'nodata', }";
		
		MyJsonObject map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.get( code) );
		assertEquals( "Param 'msg' is missing", map.get( text) ); 
	}
	
	public void testMissingMsg() throws Exception {
		String data = "{ 'nomsg': 'nodata' }";
		
		MyJsonObject map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.get( code) );
		assertEquals( "Param 'msg' is missing", map.get( text) ); 
	}
	
	public void testInvalidMsg() throws Exception {
		String data = "{ 'msg': 'notamsg' }";
		MyJsonObject map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.get( code) );
		assertStartsWith( "Param 'msg' has invalid value", map.get( text) ); 
	}
	
	public static void assertStartsWith(String expected, Object actual) {
		assertEquals( expected, actual.toString().substring( 0, expected.length() ) );
	}

	public void testJson2() throws Exception {
		String data = "{ 'msg': 'value' ";
		MyJsonObject map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.get( code) );
		assertTrue( ((String)(map.get( text))).startsWith( "Error parsing json") );
	}
	
	// invalid conid
	public void testCheckHours() throws Exception {
		String data = "{ 'msg': 'checkhours', 'conid': '-5' }";
		MyJsonObject map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.get( code) );
		assertEquals( "Param 'conid' must be positive integer", map.get( text) ); 
	}
	
	// missing conid
	public void testCheckHours2() throws Exception {
		String data = "{ 'msg': 'checkhours', 'conidd': '33' }";
		MyJsonObject map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.get( code) );
		assertEquals( "Param 'conid' is missing", map.get( text) ); 
	}
	
	// no such conid
	public void testCheckHours3() throws Exception {
		String data = "{ 'msg': 'checkhours', 'conid': '83' }";
		MyJsonObject map = sendData( data);
		S.out( map);
		assertEquals( RefCode.NO_SUCH_STOCK.toString(), map.get( code) );
		assertEquals( "No contract details found for conid 83", map.get( text) ); 
	}
	
	// all valid
	public void testCheckHours4() throws Exception {
		String data = "{ 'msg': 'checkhours', 'conid': '8314' }";
		MyJsonObject map = sendData( data);
		String hours = (String)map.get( "hours");
		assertTrue( hours.equals( "liquid") || hours.equals( "illiquid") || hours.equals( "closed") );
	}
	
	
	

	
	static String prod = "34.125.38.193";
	static String local = "localhost";
	
	static String host = prod; //local;
	
	static MyHttpClient cli() throws Exception {
		return new MyHttpClient( host, 8383);
	}
	
	public static MyJsonObject sendData( String data) throws Exception {
		MyHttpClient cli = cli();
		cli.post( Util.toJson( data) );
		return cli.readMyJsonObject();
	}
	
	static MyJsonArray sendData2( String data) throws Exception {
		MyHttpClient cli = cli();
		cli.post( Util.toJson( data) );
		return cli.readMyJsonArray();
	}
	
	static MyJsonObject sendAndReceive( String filename) throws Exception {
		MyHttpClient cli = cli();
		cli.writeFile( filename);
		return cli.readMyJsonObject();
	}

	
	public static void main(String[] args) {
		S.out( new SimpleDateFormat( "yyyymmdd").getTimeZone().getID() );
		
	}
	
} 	
