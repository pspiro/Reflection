package testcase;

import org.json.simple.JsonObject;

import reflection.RefCode;
import tw.util.S;

/** This is to test stock splits as configured in the spreadsheet. 
 * 
 * You must have prices for these stocks and you must have this in the spreadsheet:
 * 
WHR	Whirlpool	13751	0x60cffed8e229140d365da97b907d408a5e9ffa85	Y	2023-07-11	
WMT	Walmart		13824	0x23854c97059f0234fc31b268ae8c18af26cf5c8c	Y	2023-07-12	
XOM	ExxonMobile	13977	0x17d8498eacc52e056a388bc2cbe98e88304d31b9	Y	2023-07-11
YUM	Yum! Brands	3206042	0x017982d464feec7696c0787c69f82b39280d390c	Y	2023-07-10
 * 
 */
public class TestSplitDates extends MyTestCase {
//	13751
//	13824
//	13977
//	3206042
	
	private JsonObject createOrder(String side, double qty, double offset, int conid) throws Exception {
		double price = 100;  // where to get this? pas
		S.out( "price is %s", price);

		JsonObject obj = TestOrder.createOrderWithPrice(side, qty, price * 1.05);
		obj.put( "conid", conid);
		return obj;
	}

	
	public void testOk1() throws Exception {
		JsonObject obj = createOrder("BUY", 10, 2, 13751);
		JsonObject map = postOrderToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		S.out( "OK1 " + text);
		assertEquals( RefCode.OK.toString(), ret);
	}

	public void testPreSplit() throws Exception {
		JsonObject obj = createOrder("BUY", 10, 2, 13824);
		JsonObject map = postOrderToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		S.out( "Pre split " + text);
		assertEquals( RefCode.PRE_SPLIT.toString(), ret);
	}

	public void testOk2() throws Exception {
		JsonObject obj = createOrder("BUY", 10, 2, 13977);
		JsonObject map = postOrderToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		S.out( "OK2 " + text);
		assertEquals( RefCode.OK.toString(), ret);
	}

	public void testPostSplit() throws Exception {
		JsonObject obj = createOrder("BUY", 10, 2, 3206042);
		JsonObject map = postOrderToObj(obj);
		String ret = map.getString( "code");
		String text = map.getString("message");
		S.out( "Post split " + text);
		assertEquals( RefCode.POST_SPLIT.toString(), ret);
	}

}
