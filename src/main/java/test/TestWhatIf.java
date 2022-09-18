package test;

import static test.TestErrors.sendData;

import java.util.HashMap;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import junit.framework.TestCase;
import reflection.RefCode;

public class TestWhatIf extends TestCase {
	// what-if
	
	// missing conid
	public void testWhatIf1() throws Exception {
		String data = "{ 'msg': 'checkorder', 'side': 'buy', 'quantity': '100', 'price': '83' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( "Param 'conid' is missing", text);
	}

	// missing side
	public void testWhatIf2() throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'quantity': '100', 'price': '83' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( "Param 'side' is missing", text);
	}

	// missing quantity
	public void testWhatIf3() throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'side': 'buy', 'price': '83' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( "Param 'quantity' is missing", text);
	}

	// negative quantity 
	public void testWhatIf35() throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'side': 'buy', 'quantity': '-100', 'price': '83' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( "quantity must be positive", text);
	}

	// missing price
	public void testWhatIf4() throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'side': 'buy', 'quantity': '100' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( "Param 'price' is missing", text);
	}

	// negative price 
	public void testWhatIf5() throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '-83' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( "price must be positive", text);
	}

	// price too low 
	public void testWhatIf6() throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '30' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_PRICE.toString(), ret);
		assertEquals( "Order price is too low", text);
	}


	// insufficient liquidity; in order to test this, you need to disabled the max dollar amt check
//	public void testWhatIf7() throws Exception {
//		double price = TestOrder.curPrice + 1;
//		
//		String data = String.format( "{ 'msg': 'checkorder', 'conid': '8314', 'side': 'buy', 'quantity': '1000000', 'price': '%s' }", price); 
//		HashMap<String, Object> map = sendData( data);
//		String ret = (String)map.get( "code");
//		String text = (String)map.get( "text");
//		assertEquals( RefCode.REJECTED.toString(), ret);
//		assertEquals( "Insufficient liquidity in brokerage account", text);
//	}

	public void testMaxAmtBuy()  throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'side': 'buy', 'quantity': '200', 'price': '133' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}

	public void testMaxAmtSell()  throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'side': 'sell', 'quantity': '200', 'price': '133' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		assertEquals( RefCode.ORDER_TOO_LARGE.toString(), ret);
	}
	
	public void testFracSize()  throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'side': 'buy', 'quantity': '1.5', 'price': '129' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		assertEquals( RefCode.OK.toString(), ret);
	}
	
	public void testFracSize2()  throws Exception {  // rounded 
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'side': 'buy', 'quantity': '.4', 'price': '129' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		assertEquals( RefCode.OK.toString(), ret);
	}
	
	public void testZeroShares()  throws Exception {
		String data = "{ 'msg': 'checkorder', 'conid': '8314', 'side': 'buy', 'quantity': '0', 'price': '129' }"; 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.INVALID_REQUEST.toString(), ret);
		assertEquals( "quantity must be positive", text);
	}
	

	// successful buy what-if 
	public void testWhatIf0() throws Exception {
		double price = TestOrder.curPrice + 2;
		
		String data = String.format( "{ 'msg': 'checkorder', 'conid': '8314', 'side': 'buy', 'quantity': '100', 'price': '%s' }", price); 
		HashMap<String, Object> map = sendData( data);
		String ret = (String)map.get( "code");
		String text = (String)map.get( "text");
		assertEquals( RefCode.OK.toString(), ret);
		assertEquals( null, text);
	}

	public static void main(String[] args) {
//		Result result = JUnitCore.runClasses(TestWhatIf.class);
	}
	
}
