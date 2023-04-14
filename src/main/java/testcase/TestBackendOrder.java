package testcase;

import org.json.simple.JSONObject;

import com.moonstoneid.siwe.SiweMessage;

import http.MyHttpClient;
import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.Prices;
import reflection.RefCode;
import reflection.SiweUtil;
import reflection.Util;
import tw.util.S;

public class TestBackendOrder extends TestCase {
	static String wallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	static String cookie;
	static double curPrice = 135.75; // Double.valueOf( jedis.hget("8314", "last") );
	
	static {
		try {
			signIn(wallet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// missing cryptoId
	public void testOrder1() throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '100', 'price': '83', 'wallet_public_key': '0xb016711702D3302ceF6cEb62419abBeF5c44450e' }"; 
		MyJsonObject map = sendData( data);
		String ret = map.getString( "code");
		String text = map.getString( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( text, "Param 'cryptoid' is missing");
	}

	// missing walletId
	public void testOrder2() throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '100', 'price': '83', 'cryptoId': '0x0xb016711702D3302ceF6cEb62419abBeF5c44450e83' }";
		MyJsonObject map = sendData( data);
		String ret = map.getString( "code");
		String text = map.getString( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( text, "Param 'wallet_public_key' is missing");
	}
	
	// reject order; price too low
	public void testOrder3() throws Exception {
		String data = orderData( -1);
		MyJsonObject map = sendData( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		S.out( code + " " + text);
		S.out( RefCode.INVALID_PRICE + " " + Prices.TOO_LOW);
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_LOW, text);
	}
	
	// sell order price to high
	public void testOrder35() throws Exception {
		String data = orderData( 1, "SELL", "pricetoohigh");
		MyJsonObject map = sendData( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		assertEquals( RefCode.INVALID_PRICE.toString(), code);
		assertEquals( Prices.TOO_HIGH, text);
	}
	
	// reject order; price too high; IB won't accept it
	public void testOrder4() throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '100', 'price': '150', 'wallet_public_key': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testmaxamtbuy' }";		
		MyJsonObject map = sendData( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		S.out( "testOrder4: %s", map);
		assertEquals( RefCode.REJECTED.toString(), code);
		assertEquals( "Reason unknown", text);
	}
	
	// fill order buy order
	public void testOrder98() throws Exception {
		String data = orderData( 3);
		MyJsonObject map = sendData( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		String filled = map.getString( "filled");
		assertEquals( RefCode.OK.toString(), code);
		assertEquals( null, text);
		assertEquals( "100", filled);
	}
	
	// fill order sell order
	public void testOrder99() throws Exception {
		String data = orderData( -3, "sell", "testorder99");
		MyJsonObject map = sendData( data);
		String code = map.getString( "code");
		String text = map.getString( "text");
		String filled = map.getString( "filled");
		assertEquals( RefCode.OK.toString(), code);
		assertEquals( null, text);
		assertEquals( "100", filled);
	}
	
	public void testMaxAmtBuy()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '200', 'price': '138', 'wallet_public_key': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testmaxamtbuy' }";
		MyJsonObject map = sendData( data);
		String ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testMaxAmtSell()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'sell', 'quantity': '200', 'price': '138', 'wallet_public_key': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testmaxamtsell' }"; 
		MyJsonObject map = sendData( data);
		String ret = map.getString( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testFracShares()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '1.5', 'price': '138', 'wallet_public_key': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testfracshares' }"; 
		MyJsonObject map = sendData( data);
		String ret = map.getString( "code");
		String text = map.getString( "text");
		S.out( "testFracShares: %s: %s", ret, text);
		assertEquals( RefCode.OK.toString(), ret);
	}

	public void testSmallOrder()  throws Exception {  // no order should be submitted to exchange
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '.4', 'price': '138', 'wallet_public_key': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testfracshares' }"; 
		MyJsonObject map = sendData( data);
		String ret = map.getString( "code");
		assertEquals( RefCode.OK.toString(), ret);
	}

	public void testZeroShares()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '0', 'price': '138', 'wallet_public_key': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testfracshares' }"; 
		MyJsonObject map = sendData( data);
		String ret = map.getString( "code");
		String text = map.getString( "text");
		assertEquals( "Quantity must be positive", text);
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
	}
	
	public void testPartialFill()  throws Exception {
		String data = "{ 'msg': 'order', 'conid': '8314', 'action': 'buy', 'quantity': '3', 'price': '138', 'wallet_public_key': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'cryptoid': 'testfracshares' }"; 
		MyJsonObject map = sendData( data);
		String ret = map.getString( "code");
		String text = map.getString( "text");
		S.out( "testPartialFill: %s", map);
		assertEquals( RefCode.PARTIAL_FILL.toString(), ret);
		assertEquals( "2 shares filled", text);
	}

	
	static String orderData(double offset) {
		return orderData( offset, "buy", "0x0xb016711702D3302ceF6cEb62419abBeF5c44450e");
	}
	
	static String orderData(double offset, String side, String cryptoId) {
		return String.format( "{ 'conid': '8314', 'action': '%s', 'quantity': '100', 'price': '%s', 'cryptoid': '%s', 'wallet_public_key': '0xb016711702D3302ceF6cEb62419abBeF5c44450e', 'tds': 1.11 }",
				side, Double.valueOf( curPrice + offset), cryptoId );
	}
	
	static MyJsonObject sendData( String data) throws Exception {
		signIn(wallet);
		
		MyHttpClient cli = new MyHttpClient( "localhost", 8383);
		cli.addHeader("Cookie", cookie).post( "/api/reflection-api/order", Util.toJson(data) );
		return cli.readMyJsonObject();
	}

	private static void signIn(String address) throws Exception {
		MyHttpClient cli = new MyHttpClient("localhost", 8383);
		
		// send siwe/init
		cli.get("/siwe/init");
		assertEquals( 200, cli.getResponseCode() );
		String nonce = cli.readMyJsonObject().getString("nonce");

		SiweMessage siweMsg = new SiweMessage.Builder(
				"Reflection.trading", 
				address, 
				"http://localhost", 
				"1",	// version 
				5,      // chainId 
				nonce,
				Util.isoNow() )
				.statement("Sign in to Reflection.")
				.build();
		
		JSONObject signedMsgSent = new JSONObject();
		signedMsgSent.put( "signature", "102268");
		signedMsgSent.put( "message", SiweUtil.toJsonObject(siweMsg) );

		// send siwe/signin
		cli = new MyHttpClient("localhost", 8383);
		cli.post("/siwe/signin", signedMsgSent.toString() );
		assertEquals( 200, cli.getResponseCode() );
		
		cookie = cli.getHeaders().get("set-cookie");
	}

	// current stock price
}
