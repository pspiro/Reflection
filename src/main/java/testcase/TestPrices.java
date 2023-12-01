package testcase;

import org.json.simple.JsonObject;

public class TestPrices extends MyTestCase{
	
	static double curPrice = TestOrder.curPrice;
	static double offset = .7;
	
	public void testOne() throws Exception {
		
		JsonObject json = cli().get( "/api/get-price/265598").readJsonObject();
		json.display();
		double bid = json.getDouble( "bid");
		assertTrue( bid >= curPrice - offset && bid <= curPrice + offset);
		
		double ask = json.getDouble( "ask");
		assertTrue( ask >= curPrice - offset && ask <= curPrice + offset);
		
		assertTrue (bid < ask);
	}

	public void testShowPrices() throws Exception {
		//cli().get( "/api/?msg=getallprices").readJsonObject().display();
	}
	
}
