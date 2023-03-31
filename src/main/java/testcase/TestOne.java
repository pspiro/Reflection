package testcase;

import static testcase.TestErrors.sendData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import http.MyHttpClient;
import junit.framework.TestCase;
import reflection.Prices;
import reflection.RefCode;
import tw.util.S;

public class TestOne extends TestCase {
	public void testPartialFill()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '3', 'price': '138', 'wallet_public_key': '8383', 'cryptoid': 'testfracshares' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		S.out( "testPartialFill: %s", map);
		assertEquals( RefCode.PARTIAL_FILL.toString(), ret);
		assertEquals( "2 shares filled", text);
	}

	
	static String orderData(double offset) {
		return orderData( offset, "buy", "0x8383");
	}
	
	static String orderData(double offset, String side, String cryptoId) {
		return String.format( "{ 'conid': '8314', 'action': '%s', 'quantity': '100', 'price': '%s', 'cryptoid': '%s', 'wallet': '0x747474', 'tds': 1.11 }",
				side, Double.valueOf( curPrice + offset), cryptoId );
	}
	
	static MyHttpClient cli() throws Exception {
		return new MyHttpClient( "localhost", 8383);
	}
	
	static HashMap<String, Object> sendData( String data) throws Exception {
		MyHttpClient cli = cli();
		cli.post( "/api/reflection-api/order", data.replaceAll( "\\'", "\"") );
		return cli.readJsonMap();
	}

	// current stock price
	static double curPrice = 135.75; // Double.valueOf( jedis.hget("8314", "last") );
}
