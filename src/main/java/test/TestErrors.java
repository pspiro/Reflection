package test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import http.MyHttpClient;
import junit.framework.TestCase;
import reflection.RefCode;
import reflection.RefException;
import reflection.Util;
import tw.util.S;

public class TestErrors extends TestCase {
	String text = "text";
	String code = "code";
	
	
	public void testMalformedJson() throws Exception {
		String data = "{ 'nomsg': 'nodata', }";
		
		HashMap<String, Object> map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.get( code) );
		assertEquals( "Param 'msg' is missing", map.get( text) ); 
	}
	
	public void testMissingMsg() throws Exception {
		String data = "{ 'nomsg': 'nodata' }";
		
		HashMap<String, Object> map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.get( code) );
		assertEquals( "Param 'msg' is missing", map.get( text) ); 
	}
	
	public void testInvalidMsg() throws Exception {
		String data = "{ 'msg': 'notamsg' }";
		HashMap<String, Object> map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.get( code) );
		assertStartsWith( "Param 'msg' has invalid value", map.get( text) ); 
	}
	
	public static void assertStartsWith(String expected, Object actual) {
		assertEquals( expected, actual.toString().substring( 0, expected.length() ) );
	}

	public void testJson2() throws Exception {
		String data = "{ 'msg': 'value' ";
		HashMap<String, Object> map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.get( code) );
		assertTrue( ((String)(map.get( text))).startsWith( "Error parsing json") );
	}
	
	// invalid conid
	public void testCheckHours() throws Exception {
		String data = "{ 'msg': 'checkhours', 'conid': '-5' }";
		HashMap<String, Object> map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.get( code) );
		assertEquals( "Param 'conid' must be positive integer", map.get( text) ); 
	}
	
	// missing conid
	public void testCheckHours2() throws Exception {
		String data = "{ 'msg': 'checkhours', 'conidd': '33' }";
		HashMap<String, Object> map = sendData( data);
		assertEquals( RefCode.INVALID_REQUEST.toString(), map.get( code) );
		assertEquals( "Param 'conid' is missing", map.get( text) ); 
	}
	
	// no such conid
	public void testCheckHours3() throws Exception {
		String data = "{ 'msg': 'checkhours', 'conid': '83' }";
		HashMap<String, Object> map = sendData( data);
		assertEquals( RefCode.NO_SUCH_STOCK.toString(), map.get( code) );
		assertEquals( "No contract details found for conid 83", map.get( text) ); 
	}
	
	// all valid
	public void testCheckHours4() throws Exception {
		String data = "{ 'msg': 'checkhours', 'conid': '8314' }";
		HashMap<String, Object> map = sendData( data);
		String hours = (String)map.get( "hours");
		assertTrue( hours.equals( "liquid") || hours.equals( "illiquid") || hours.equals( "closed") );
	}
	
	
	

	
	static String prod = "34.148.125.2";
	static String local = "localhost";
	
	static String host = prod;
	
	static MyHttpClient cli() throws Exception {
		return new MyHttpClient( host, 8383);
	}
	
	static HashMap<String, Object> sendData( String data) throws Exception {
		MyHttpClient cli = cli();
		cli.post( data.replaceAll( "\\'", "\"") );
		return cli.readJsonMap();
	}
	
	static HashMap<String, Object> sendAndReceive( String filename) throws Exception {
		MyHttpClient cli = cli();
		cli.writeFile( filename);
		return cli.readJsonMap();
	}

	@SuppressWarnings("deprecation")   // run this test again with a different time zone
	public void testExchHours() throws RefException {
		String str = "20220807:CLOSED;"
				+ "20220916:0900-20220916:1600;"
				+ "20220918:0930-20220918:1600";
		
		String tz = "America/New_York";
		
		assertTrue(  Util.inside( date(2022, 9, 16,  9, 00), 8314, str, tz) );
		assertTrue(  Util.inside( date(2022, 9, 16, 12, 00), 8314, str, tz) );
		assertFalse( Util.inside( date(2022, 9, 15, 12, 00), 8314, str, tz) );
		assertFalse( Util.inside( date(2022, 9, 17, 12, 00), 8314, str, tz) );
		assertFalse( Util.inside( date(2022, 9, 16,  8, 59), 8314, str, tz) );
		assertFalse( Util.inside( date(2022, 9, 16, 16, 00), 8314, str, tz) );
		assertFalse( Util.inside( date(2022, 9, 16, 16, 10), 8314, str, tz) );
	}

	private Date date(int year, int month, int day, int hr, int min) {
		return new Date( year - 1900, month - 1, day, hr, min);
	}
	
	public static void main(String[] args) {
		S.out( new SimpleDateFormat( "yyyymmdd").getTimeZone().getID() );
		
	}
	
} 	
