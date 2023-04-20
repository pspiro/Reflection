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
	static MyJsonObject post( String data) throws Exception {
		MyHttpClient cli = new MyHttpClient( "localhost", 8383);
		cli.addHeader("Cookie", Cookie.cookie).post( "/", Util.toJson(data) );
		return cli.readMyJsonObject();
	}
	
	// missing walletId
	public void testMissingWallet() throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '83' }";
		MyJsonObject map = post( data);
		String ret = map.getString( "code");
		String text = map.getString( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( text, "Param 'wallet' is missing");
	}
	
	// reject order; price too high; IB rejects it
	public void testBuyTooHigh() throws Exception {
		String data = orderData( 100, "BUY", wallet);
		MyJsonObject map = post( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		assertEquals( RefCode.REJECTED.toString(), code);  // test fails if autoFill is on
		assertEquals( "Reason unknown", text);
	}
	
	// reject order; buy price too low; RefAPI rejects it
	public void testBuyPriceTooLow() throws Exception {
		String data = orderData( -1);
		MyJsonObject map = post( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		S.out( code + " " + text);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_LOW, text);
	}
	
	// reject order; sell price too high; RefAPI rejects it
	public void testSellTooHigh() throws Exception {
		String data = orderData( 1, "SELL", wallet);		
		MyJsonObject map = post( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		S.out( "testOrder4: %s", map);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_HIGH, text);
	}
	
	// reject order; sell price too low; IB rejects it
	public void testSellPriceTooLow() throws Exception {
		String data = orderData( -30, "SELL", wallet);
		MyJsonObject map = post( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		assertEquals( RefCode.INVALID_PRICE.toString(), code);  // test fails if autoFill is on
		assertEquals( Prices.TOO_HIGH, text);
	}
	
	// fill order buy order
	public void testFillBuy() throws Exception {
		String data = orderData( 3);
		MyJsonObject map = post( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		assertEquals( RefCode.OK.toString(), code);
		double filled = map.getDouble( "filled");
		assertEquals( 100.0, filled);
	}
	
	// fill order sell order
	public void testFillSell() throws Exception {
		String data = orderData( -3, "sell", wallet);
		MyJsonObject map = post( data);
		String code = (String)map.getString( "code");
		String text = map.getString( "text");
		double filled = map.getDouble( "filled");
		assertEquals( RefCode.OK.toString(), code);
		assertEquals( 100.0, filled);
	}
	
	public void testMaxAmtBuy()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '200', 'price': '138', 'wallet': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testmaxamtbuy' }";
		MyJsonObject map = post( data);
		String ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testMaxAmtSell()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'sell', 'quantity': '200', 'price': '138', 'wallet': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testmaxamtsell' }"; 
		MyJsonObject map = post( data);
		String ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testFracShares()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '1.5', 'price': '138', 'wallet': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testfracshares' }"; 
		MyJsonObject map = post( data);
		String ret = map.getString( "code");
		String text = map.getString( "text");
		S.out( "testFracShares: %s: %s", ret, text);
		assertEquals( RefCode.OK.toString(), ret);
	}

	public void testSmallOrder()  throws Exception {  // no order should be submitted to exchange
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '.4', 'price': '138', 'wallet': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testfracshares' }"; 
		MyJsonObject map = post( data);
		String ret = map.getString( "code");
		assertEquals( RefCode.OK.toString(), ret);
	}

	public void testZeroShares()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'side': 'buy', 'quantity': '0', 'price': '138', 'wallet': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testfracshares' }"; 
		MyJsonObject map = post( data);
		String ret = map.getString( "code");
		String text = map.getString( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( "Quantity must be positive", text);
	}
	
	static String orderData(double offset) {
		return orderData( offset, "buy", wallet);
	}
	
	public static String orderData(double offset, String side, String wallet) {
		return String.format( "{ 'msg': 'order', 'currency': 'busd', 'conid': '8314', 'side': '%s', 'quantity': '100', 'price': '%s', 'wallet': '%s', 'tds': 1.11 }",
				side, Double.valueOf( curPrice + offset), wallet);
	}
}
