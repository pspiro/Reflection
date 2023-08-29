package testcase;

import org.json.simple.JsonObject;

import reflection.Allow;
import reflection.RefCode;
import tw.google.GTable;
import tw.google.NewSheet;

/** Test panic (allowTrading) and blacklist */
public class TestPanic extends MyTestCase {

	public void testPanic() throws Exception {
		//cli().get("/?msg=seedprices");

		GTable tab = new GTable(NewSheet.Reflection, "Dt-config", "Tag", "Value");
		
		tab.put( "allowTrading", Allow.Sell.toString() );
		cli().get("/?msg=refreshconfig");
		assertEquals( RefCode.OK, cli.getRefCode() );
		
		JsonObject obj = TestOrder.createOrder( "BUY", 10, 3);
		postOrderToObj(obj);
		assertEquals( RefCode.TRADING_HALTED.toString(), cli.getRefCode().toString() );

		obj = TestOrder.createOrder( "SELL", 10, 3);
		postOrderToObj(obj);
		assertNotEquals( RefCode.TRADING_HALTED.toString(), cli.getRefCode().toString() );
		
		tab.put( "allowTrading", Allow.None.toString() );
		cli().get("/?msg=refreshconfig");
		assertEquals( RefCode.OK, cli.getRefCode() );
		
		obj = TestOrder.createOrder( "BUY", 10, 3);
		postOrderToObj(obj);
		assertEquals( RefCode.TRADING_HALTED.toString(), cli.getRefCode().toString() );

		obj = TestOrder.createOrder( "SELL", 10, 3);
		postOrderToObj(obj);
		assertEquals( RefCode.TRADING_HALTED.toString(), cli.getRefCode().toString() );

		// put it back to all
		tab.put( "allowTrading", Allow.All.toString() );
		cli().get("/?msg=refreshconfig");
		assertEquals( RefCode.OK, cli.getRefCode() );
		
		obj = TestOrder.createOrder( "BUY", 10, 3);
		postOrderToObj(obj);
		assertNotEquals( RefCode.TRADING_HALTED.toString(), cli.getRefCode().toString() );

		obj = TestOrder.createOrder( "SELL", 10, 3);
		postOrderToObj(obj);
		assertNotEquals( RefCode.TRADING_HALTED.toString(), cli.getRefCode().toString() );
		
	}
	
	// test blacklist
	public static String buy  = "0x000000000000000000000000000000000000000a";
	public static String sell = "0x000000000000000000000000000000000000000b";
	public static String all  = "0x000000000000000000000000000000000000000c";
	public static String none = "0x000000000000000000000000000000000000000d";
	
	/** Test pausing a stock */
	public void testPauseStock() {
		//write this
	}
	
	/** Test blacklisting a wallet */
	public void testBlacklist() throws Exception {
		JsonObject obj;
		JsonObject map;
		
		obj = TestOrder.createOrder("BUY", 10, 2);
		obj.put("wallet_public_key", buy);
		map = postOrderToObj(obj);
		assertNotEquals( RefCode.ACCESS_DENIED.toString(), map.getString( "code") );

		obj = TestOrder.createOrder("SELL", 10, 2);
		obj.put("wallet_public_key", buy);
		map = postOrderToObj(obj);
		assertEquals( RefCode.ACCESS_DENIED.toString(), map.getString( "code") );
		
		obj = TestOrder.createOrder("BUY", 10, 2);
		obj.put("wallet_public_key", sell);
		map = postOrderToObj(obj);
		assertEquals( RefCode.ACCESS_DENIED.toString(), map.getString( "code") );

		obj = TestOrder.createOrder("SELL", 10, 2);
		obj.put("wallet_public_key", sell);
		map = postOrderToObj(obj);
		assertNotEquals( RefCode.ACCESS_DENIED.toString(), map.getString( "code") );
		
		obj = TestOrder.createOrder("BUY", 10, 2);
		obj.put("wallet_public_key", all);
		map = postOrderToObj(obj);
		assertNotEquals( RefCode.ACCESS_DENIED.toString(), map.getString( "code") );

		obj = TestOrder.createOrder("SELL", 10, 2);
		obj.put("wallet_public_key", all);
		map = postOrderToObj(obj);
		assertNotEquals( RefCode.ACCESS_DENIED.toString(), map.getString( "code") );
		
		obj = TestOrder.createOrder("BUY", 10, 2);
		obj.put("wallet_public_key", none);
		map = postOrderToObj(obj);
		assertEquals( RefCode.ACCESS_DENIED.toString(), map.getString( "code") );

		obj = TestOrder.createOrder("SELL", 10, 2);
		obj.put("wallet_public_key", none);
		map = postOrderToObj(obj);
		assertEquals( RefCode.ACCESS_DENIED.toString(), map.getString( "code") );
	}
	
	

}
