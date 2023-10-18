package testcase;

import org.json.simple.JsonObject;

import tw.util.S;

public class testrej extends TestOrder {
	public void testRej() throws Exception {
		S.out("testrej");
		JsonObject obj = TestOrder.createOrder2( "BUY", .1, 162.84);
		obj.put("conid", "11017");
		
		
		// this won't work because you have to 
		//obj.remove("noFireblocks"); // let the fireblocks go through so we can test the crypto_transaction
		
		JsonObject map = postOrderToObj(obj);
		S.out( "%s %s", cli.getRefCode(), cli.getMessage() );
	}

}
