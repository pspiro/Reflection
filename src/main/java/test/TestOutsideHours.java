package test;

import java.util.Date;
import java.util.HashMap;

import junit.framework.TestCase;
import reflection.MyTransaction;
import reflection.RefCode;
import reflection.RefException;
import reflection.Util;
import tw.util.S;

// this should be before open, after close, and on days that the exchange is closed
public class TestOutsideHours extends TestCase {
	public void testCheckOrder() throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '83', 'wallet': '0x747474',  }"; 
		HashMap<String, Object> map = TestErrors.sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.EXCHANGE_CLOSED.toString(), ret);
		assertEquals( MyTransaction.exchangeIsClosed, text);
	}

	public void testOrder() throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '83', 'wallet': '0x747474', 'cryptoid': 'abcd', 'simtime': ' }"; 
		HashMap<String, Object> map = TestErrors.sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.EXCHANGE_CLOSED.toString(), ret);
		assertEquals( MyTransaction.exchangeIsClosed, text);
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

	public HashMap<String, Object> testHours( int conid, String time) throws Exception {
		String format = "{ 'msg': 'order', 'conid': '%s', 'side': 'buy', 'quantity': '100', 'price': '%s', 'wallet': '0x747474', 'cryptoid': 'abcd', 'simtime': '%s' }";
		String data = String.format( format, conid, TestOrder.curPrice + 5, time); 
		return TestErrors.sendData( data);
	}

	/** These tests have to be run on a day that the exchange is open, i.e. not Saturday */
	public void testStk0()  throws Exception {
		HashMap<String, Object> map = testHours( 8314, "3:59");

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		assertEquals( RefCode.EXCHANGE_CLOSED.toString(), ret);
		assertEquals( MyTransaction.exchangeIsClosed, text);
	}
	
	public void testStk1()  throws Exception {
		HashMap<String, Object> map = testHours( 8314, "4:00");

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		assertEquals( RefCode.OK.toString(), ret);
	}
	
	public void testStk2() throws Exception {
		HashMap<String, Object> map = testHours( 8314, "10:00");

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		assertEquals( RefCode.OK.toString(), ret);
	}
	
	public void testStk3() throws Exception {
		HashMap<String, Object> map = testHours( 8314, "20:00");

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		assertEquals( RefCode.OK.toString(), ret);
	}
	
	public void testStk4() throws Exception {
		HashMap<String, Object> map = testHours( 8314, "20:01");

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		assertEquals( RefCode.EXCHANGE_CLOSED.toString(), ret);
		assertEquals( MyTransaction.exchangeIsClosed, text);
	
	}
}
	
/*	public void testETFLiquid()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '83', 'wallet': '0x747474', 'cryptoid': 'abcd', 'simtime': ' }"; 
		HashMap<String, Object> map = TestErrors.sendData( data);

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		assertEquals( RefCode.OK.toString(), ret);
	}
	
	public void testETFIlliquid()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '83', 'wallet': '0x747474', 'cryptoid': 'abcd', 'simtime': ' }"; 
		HashMap<String, Object> map = TestErrors.sendData( data);

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		assertEquals( RefCode.OK.toString(), ret);
	}
	
	public void testETFVeryIlliquid()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '83', 'wallet': '0x747474', 'cryptoid': 'abcd', 'simtime': ' }"; 
		HashMap<String, Object> map = TestErrors.sendData( data);

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");

		assertEquals( RefCode.OK.toString(), ret);
	}
	
	public void testETFOutside()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '83', 'wallet': '0x747474', 'cryptoid': 'abcd', 'simtime': ' }"; 
		HashMap<String, Object> map = TestErrors.sendData( data);

		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.EXCHANGE_CLOSED.toString(), ret);
		assertEquals( MyTransaction.exchangeIsClosed, text);
	}
	
	// test: stock inside, stock outside, ETF inside, ETF after hours, ETF outside
	

}
*/