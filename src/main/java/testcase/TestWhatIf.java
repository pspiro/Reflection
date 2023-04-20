package testcase;

import http.MyHttpClient;
import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.Prices;
import reflection.RefCode;
import reflection.Util;

public class TestWhatIf extends TestCase {
	// what-if
	
	// missing conid
	public void testMissingConid() throws Exception {
		String data = "{ 'msg': 'checkorder', 'action': 'buy', 'quantity': '100', 'price': '83' }"; 
		MyJsonObject map = post( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( "Param 'conid' is missing", text);
	}

	// missing side
	public void testMissingAction() throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'quantity': '100', 'price': '83' }"; 
		MyJsonObject map = post( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( "Param 'action' is missing", text);
	}

	// missing quantity
	public void testMissingQty() throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'action': 'buy', 'price': '83' }"; 
		MyJsonObject map = post( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( "Param 'quantity' is missing", text);
	}

	// negative quantity 
	public void testWhatIf35() throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'action': 'buy', 'quantity': '-100', 'price': '83' }"; 
		MyJsonObject map = post( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( "Quantity must be positive", text);
	}

	// missing price
	public void testMissingPrice() throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'action': 'buy', 'quantity': '100' }"; 
		MyJsonObject map = post( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( "Param 'price' is missing", text);
	}

	// negative price 
	public void testNegativePrice() throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'action': 'buy', 'quantity': '100', 'price': '-83' }"; 
		MyJsonObject map = post( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( "Price must be positive", text);
	}

	// price too low 
	public void testPriceTooLow() throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'action': 'buy', 'quantity': '100', 'price': '30' }"; 
		MyJsonObject map = post( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_PRICE.toString(), ret);
		assertEquals( Prices.TOO_LOW, text);
	}


	// insufficient liquidity; in order to test this, you need to disabled the max dollar amt check
//	public void testWhatIf7() throws Exception {
//		double price = curPrice + 1;
//		
//		String data = String.format( "{ 'msg': 'checkorder', 'conid': '8314', 'action': 'buy', 'quantity': '1000000', 'price': '%s' }", price); 
//		MyJsonObject map = sendData( data);
//		String ret = (String)map.get( "code");
//		String text = (String)map.get( "text");
//		assertEquals( RefCode.REJECTED.toString(), ret);
//		assertEquals( "Insufficient liquidity in brokerage account", text);
//	}

	public void testMaxAmtBuy()  throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'action': 'buy', 'quantity': '200', 'price': '133' }"; 
		MyJsonObject map = post( data);
		String ret = (String)map.get( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testMaxAmtSell()  throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'action': 'sell', 'quantity': '200', 'price': '133' }"; 
		MyJsonObject map = post( data);
		String ret = (String)map.get( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}
	
	public void testFracSize()  throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'action': 'buy', 'quantity': '1.5', 'price': '147' }"; 
		MyJsonObject map = post( data);
		String ret = (String)map.get( "code");
		assertEquals( RefCode.OK.toString(), ret);
	}
	
	public void testFracSize2()  throws Exception {  // rounded 
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'action': 'buy', 'quantity': '.4', 'price': '147' }"; 
		MyJsonObject map = post( data);
		String ret = (String)map.get( "code");
		assertEquals( RefCode.OK.toString(), ret);
	}
	
	public void testZeroShares()  throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'action': 'buy', 'quantity': '0', 'price': '147' }"; 
		MyJsonObject map = post( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( "Quantity must be positive", text);
	}
	
	static double curPrice = 138;
	
	// successful buy what-if 
	public void testWhatIfSuccess() throws Exception {
		double price = curPrice + 2;
		
		String data = String.format( "{ 'msg': 'checkorder', 'conid': '8314', 'action': 'buy', 'quantity': '100', 'price': '%s' }", price); 
		MyJsonObject map = post( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.OK.toString(), ret);
	}

	public static void main(String[] args) {
//		Result result = JUnitCore.runClasses(TestWhatIf.class);
	}

	static MyJsonObject post(String data) throws Exception {
		MyHttpClient cli = new MyHttpClient( "localhost", 8383);
		cli.post( "/api/reflection-api/check-order", Cookie.addCookie( Util.toJson(data) ) );
		return cli.readMyJsonObject();
	}

}
