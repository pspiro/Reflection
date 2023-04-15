package testcase;

import http.MyHttpClient;
import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.Prices;
import reflection.RefCode;
import reflection.Util;
import tw.util.S;

public class TestOrder extends TestCase {
	static String wallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	static double curPrice = 135.75; // Double.valueOf( jedis.hget("8314", "last") );

	/** This version includes the cookie */
	static MyJsonObject sendData( String data) throws Exception {
		MyHttpClient cli = new MyHttpClient( "localhost", 8383);
		cli.addHeader("Cookie", Cookie.cookie).post( "/", Util.toJson(data) );
		return cli.readMyJsonObject();
	}
	
	// missing cryptoId
	public void testOrder1() throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '83', 'wallet': '0xb016711702D3302ceF6cEb62419abBeF5c44450e' }"; 
		MyJsonObject map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( text, "Param 'cryptoid' is missing");
	}

	// missing walletId
	public void testMissingWallet() throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '83', 'cryptoId': '0x838383' }";
		MyJsonObject map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( text, "Param 'wallet' is missing");
	}
	
	// reject order; price too low
	public void testPriceTooLow() throws Exception {
		String data = orderData( -1);
		MyJsonObject map = sendData( data);
		String code = (String)map.get( "code");
		String text = (String)map.get( "text");
		S.out( code + " " + text);
		S.out( RefCode.INVALID_PRICE + " " + Prices.TOO_LOW);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_LOW, text);
	}
	
	// sell order price to high
	public void testPriceTooHigh() throws Exception {
		String data = orderData( 1, "SELL", "pricetoohigh");
		MyJsonObject map = sendData( data);
		String code = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_HIGH, text);
	}
	
	// reject order; price too high; IB won't accept it
	public void testOrder4() throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '150', 'wallet': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testmaxamtbuy' }";		
		MyJsonObject map = sendData( data);
		String code = (String)map.get( "code");
		String text = (String)map.get( "text");
		S.out( "testOrder4: %s", map);
		assertEquals( RefCode.REJECTED.toString(), code);
		assertEquals( "Reason unknown", text);
	}
	
	// fill order buy order
	public void testFillBuy() throws Exception {
		String data = orderData( 3);
		MyJsonObject map = sendData( data);
		String code = (String)map.get( "code");
		String text = (String)map.get( "text");
		String filled = (String)map.get( "filled");
		assertEquals( RefCode.OK.toString(), code);
		assertEquals( null, text);
		assertEquals( "100", filled);
	}
	
	// fill order sell order
	public void testFillSell() throws Exception {
		String data = orderData( -3, "sell", "testorder99");
		MyJsonObject map = sendData( data);
		String code = (String)map.get( "code");
		String text = (String)map.get( "text");
		String filled = (String)map.get( "filled");
		assertEquals( RefCode.OK.toString(), code);
		assertEquals( null, text);
		assertEquals( "100", filled);
	}
	
	public void testMaxAmtBuy()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '200', 'price': '138', 'wallet': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testmaxamtbuy' }";
		MyJsonObject map = sendData( data);
		String ret = (String)map.get( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testMaxAmtSell()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'sell', 'quantity': '200', 'price': '138', 'wallet': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testmaxamtsell' }"; 
		MyJsonObject map = sendData( data);
		String ret = (String)map.get( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testFracShares()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '1.5', 'price': '138', 'wallet': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testfracshares' }"; 
		MyJsonObject map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		S.out( "testFracShares: %s: %s", ret, text);
		assertEquals( RefCode.OK.toString(), ret);
	}

	public void testSmallOrder()  throws Exception {  // no order should be submitted to exchange
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '.4', 'price': '138', 'wallet': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testfracshares' }"; 
		MyJsonObject map = sendData( data);
		String ret = (String)map.get( "code");
		assertEquals( RefCode.OK.toString(), ret);
	}

	public void testZeroShares()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '0', 'price': '138', 'wallet': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testfracshares' }"; 
		MyJsonObject map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( "Quantity must be positive", text);
	}
	
	public void testPartialFill()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '3', 'price': '138', 'wallet': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testfracshares' }"; 
		MyJsonObject map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		S.out( "testPartialFill: %s", map);
		assertEquals( RefCode.PARTIAL_FILL.toString(), ret);
		assertEquals( "2 shares filled", text);
	}

	
	static String orderData(double offset) {
		return orderData( offset, "buy", "0x8383");
	}
	
	public static String orderData(double offset, String side, String cryptoId) {
		return String.format( "{ 'msg': 'order', 'conid': '8314', 'side': '%s', 'quantity': '100', 'price': '%s', 'cryptoid': '%s', 'wallet': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'tds': 1.11 }",
				side, Double.valueOf( curPrice + offset), cryptoId );
	}
}
