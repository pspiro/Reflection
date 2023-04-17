package testcase;

import java.util.Date;
import java.util.HashMap;

import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.MyTransaction;
import reflection.RefCode;
import reflection.RefException;
import reflection.Util;
import tw.util.S;

// this should be before open, after close, and on days that the exchange is closed
public class TestOutsideHours extends TestCase {
	static int QQQ = 320227571;
	static int IBM = 8314;
	
	@SuppressWarnings("deprecation")   // run this test again with a different time zone
	public void testExchHours() throws RefException {
		String str = "20220807:CLOSED;"
				+ "20220916:0900-20220916:1600;"
				+ "20220918:0930-20220918:1600";
		
		String tz = "America/New_York";
		
		assertTrue(  Util.inside( date(2022, 9, 16,  9, 00), IBM, str, tz) );
		assertTrue(  Util.inside( date(2022, 9, 16, 12, 00), IBM, str, tz) );
		assertFalse( Util.inside( date(2022, 9, 15, 12, 00), IBM, str, tz) );
		assertFalse( Util.inside( date(2022, 9, 17, 12, 00), IBM, str, tz) );
		assertFalse( Util.inside( date(2022, 9, 16,  8, 59), IBM, str, tz) );
		assertFalse( Util.inside( date(2022, 9, 16, 16, 00), IBM, str, tz) );
		assertFalse( Util.inside( date(2022, 9, 16, 16, 10), IBM, str, tz) );
	}

	private Date date(int year, int month, int day, int hr, int min) {
		return new Date( year - 1900, month - 1, day, hr, min);
	}

	public static MyJsonObject testHours( int conid, String time) throws Exception {
		double price = TestOrder.curPrice + 5;
		if (conid == QQQ) price = 318*1.05;
				
		String format = "{ 'msg': 'checkorder', 'conid': '%s', 'side': 'buy', 'quantity': '1', 'price': '%s', 'wallet': '0x747474', 'cryptoid': 'abcd', 'simtime': '%s' }";
		String data = String.format( format, conid, price, time); 
		return TestOrder.post( data);
	}

	/** These tests have to be run on a day that the exchange is open, i.e. not Saturday */
	public void testStk0()  throws Exception {
		MyJsonObject map = testHours( IBM, "3:59");

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		assertEquals( RefCode.EXCHANGE_CLOSED.toString(), ret);
		assertEquals( MyTransaction.exchangeIsClosed, text);
	}
	
	public void testStk1()  throws Exception {
		MyJsonObject map = testHours( IBM, "4:00");

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		S.out( "testStk1 " + text);
		assertEquals( RefCode.OK.toString(), ret);
	}
	
	public void testStk2() throws Exception {
		MyJsonObject map = testHours( IBM, "10:00");

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		S.out( "testStk2 " + text);
		assertEquals( RefCode.OK.toString(), ret);
	}
	
	public void testStk3() throws Exception {
		MyJsonObject map = testHours( IBM, "19:59");

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		S.out( "testStk3 " + text);
		assertEquals( RefCode.OK.toString(), ret);
	}
	
	public void testStk4() throws Exception {
		MyJsonObject map = testHours( IBM, "20:00");

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		S.out( "testStk4 " + text);
		assertEquals( RefCode.EXCHANGE_CLOSED.toString(), ret);
		assertEquals( MyTransaction.exchangeIsClosed, text);
	
	}
	
	public void testEtf0()  throws Exception {
		MyJsonObject map = testHours( QQQ, "3:59");

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		S.out(text);
		assertEquals( RefCode.EXCHANGE_CLOSED.toString(), ret);
		assertEquals( MyTransaction.exchangeIsClosed, text);
	}
	
	public void testEtf1()  throws Exception {
		MyJsonObject map = testHours( QQQ, "4:00");  // this will fail on the weekend

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		S.out( "testEtf1 " + text);
		assertEquals( RefCode.OK.toString(), ret);
	}
	
	public void testEtf2() throws Exception {
		MyJsonObject map = testHours( QQQ, "10:00");

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		S.out( "testEtf2 " + text);
		assertEquals( RefCode.OK.toString(), ret);
	}
	
	public void testEtf3() throws Exception {
		MyJsonObject map = testHours( QQQ, "19:59");

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		S.out( "testEtf3 " + text);
		assertEquals( RefCode.OK.toString(), ret);
	}
	
	public void testEtf4() throws Exception {
		MyJsonObject map = testHours( QQQ, "20:00");

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		S.out( "testEtf4 " + text);
		assertEquals( RefCode.OK.toString(), ret);
	}	

	public void testEtf5() throws Exception {
		MyJsonObject map = testHours( QQQ, "23:59");

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		S.out( "testEtf4 " + text);
		assertEquals( RefCode.OK.toString(), ret);
	}	

	public void testEtf6() throws Exception {
		MyJsonObject map = testHours( QQQ, "03:29");

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		S.out( "testEtf4 " + text);
		assertEquals( RefCode.OK.toString(), ret);
	}	

}
