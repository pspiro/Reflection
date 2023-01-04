package test;

import static test.TestErrors.sendData;

import java.util.HashMap;

import junit.framework.TestCase;
import redis.clients.jedis.Jedis;
import reflection.Prices;
import reflection.RefCode;
import tw.util.S;

public class TestOrder extends TestCase {

	// missing cryptoId
	public void testOrder1() throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '83', 'wallet': '0x747474' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( text, "Param 'cryptoid' is missing");
	}

	// missing walletId
	public void testOrder2() throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '83', 'cryptoId': '0x838383' }";
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( text, "Param 'wallet' is missing");
	}
	
	// reject order; price too low
	public void testOrder3() throws Exception {
		String data = orderData( -1);
		HashMap<String, Object> map = sendData( data);
		String code = (String)map.get( "code");
		String text = (String)map.get( "text");
		S.out( code + " " + text);
		S.out( RefCode.INVALID_PRICE + " " + Prices.TOO_LOW);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_LOW, text);
	}
	
	// sell order price to high
	public void testOrder35() throws Exception {
		String data = orderData( 1, "SELL", "pricetoohigh");
		HashMap<String, Object> map = sendData( data);
		String code = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_HIGH, text);
	}
	
	// reject order; price too high; IB won't accept it
	public void testOrder4() throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '150', 'wallet': '8383', 'cryptoid': 'testmaxamtbuy' }";		
		HashMap<String, Object> map = sendData( data);
		String code = (String)map.get( "code");
		String text = (String)map.get( "text");
		String filled = (String)map.get( "filled");
		assertEquals( RefCode.REJECTED.toString(), code);
		assertEquals( "Reason unknown", text);
	}
	
	// fill order buy order
	public void testOrder98() throws Exception {
		String data = orderData( 3);
		HashMap<String, Object> map = sendData( data);
		String code = (String)map.get( "code");
		String text = (String)map.get( "text");
		String filled = (String)map.get( "filled");
		assertEquals( RefCode.OK.toString(), code);
		assertEquals( null, text);
		assertEquals( "100", filled);
	}
	
	// fill order sell order
	public void testOrder99() throws Exception {
		String data = orderData( -3, "sell", "testorder99");
		HashMap<String, Object> map = sendData( data);
		String code = (String)map.get( "code");
		String text = (String)map.get( "text");
		String filled = (String)map.get( "filled");
		assertEquals( RefCode.OK.toString(), code);
		assertEquals( null, text);
		assertEquals( "100", filled);
	}
	
	public void testMaxAmtBuy()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '200', 'price': '147', 'wallet': '8383', 'cryptoid': 'testmaxamtbuy' }";
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testMaxAmtSell()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'sell', 'quantity': '200', 'price': '147', 'wallet': '8383', 'cryptoid': 'testmaxamtsell' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testFracShares()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '1.5', 'price': '147', 'wallet': '8383', 'cryptoid': 'testfracshares' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		assertEquals( RefCode.OK.toString(), ret);
	}

	public void testSmallOrder()  throws Exception {  // no order should be submitted to exchange
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '.4', 'price': '147', 'wallet': '8383', 'cryptoid': 'testfracshares' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		assertEquals( RefCode.OK.toString(), ret);
	}

	public void testZeroShares()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '0', 'price': '147', 'wallet': '8383', 'cryptoid': 'testfracshares' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( "Quantity must be positive", text);
	}
	
	public void testPartialFill()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '3', 'price': '147', 'wallet': '8383', 'cryptoid': 'testfracshares' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		S.out( map);
		assertEquals( RefCode.PARTIAL_FILL.toString(), ret);
		assertEquals( "2 shares filled", text);
	}

	
	static String orderData(double offset) {
		return orderData( offset, "buy", "0x8383");
	}
	
	static String orderData(double offset, String side, String cryptoId) {
		return String.format( "{ 'msg': 'order', 'conid': '8314', 'side': '%s', 'quantity': '100', 'price': '%s', 'cryptoid': '%s', 'wallet': '0x747474' }",
				side, Double.valueOf( curPrice + offset), cryptoId );
	}
	
	// current stock price
	static Jedis jedis = new Jedis("34.125.38.193", 3001); 
	static double curPrice = Double.valueOf( jedis.hget("8314", "last") );
}
