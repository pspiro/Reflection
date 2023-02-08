package test;

import java.math.BigInteger;

import fireblocks.Busd;
import fireblocks.Deploy;
import fireblocks.Fireblocks;
import fireblocks.Rusd;
import fireblocks.StockToken;
import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.RefException;
import tw.util.S;

public class TestFireblocks extends TestCase {
	static String fix( String str) {
		return str.replaceAll( "\\'", "\"");
	}
	
	public void testEncode() throws Exception {
		String[] types = { "string", "address", "uint256", "uint256" };
		Object[] vals = {
				"hello",
				Rusd.rusdAddr,
				3,
				new BigInteger("4")
		};
		
		assertEquals( 
				"0000000000000000000000000000000000000000000000000000000000000080000000000000000000000000dd9b1982261f0437aff1d3fec9584f86ab4f819700000000000000000000000000000000000000000000000000000000000000030000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000568656c6c6f000000000000000000000000000000000000000000000000000000",
				Fireblocks.encodeParameters( types, vals) );
	}
	
	public void testStringToBytes() {
		assertEquals( "414243", Fireblocks.stringToBytes("ABC") );		
	}
	
	public void testToJson() throws Exception {
		MyJsonObject obj = Fireblocks.toJsonObject( 
				Fireblocks.toJson("{ 'name': '%s', 'age': %s }", "peter", 6)
			);
		
		assertEquals( "peter", obj.getString("name") );
		assertEquals( 6, obj.getInt("age") );
	}
	
	public void testToStockToken() throws Exception {
		String[] types = { "uint256" };
		Object[] vals = { Rusd.toStockToken(34.01111) };
		assertEquals( 
			"000000000000000000000000000000000000000000000000000000000206f778", // it gets rounded to three decimal places
			Fireblocks.encodeParameters( types, vals) );
	}

	public void testToStablecoin() throws Exception {
		String[] types = { "uint256" };
		Object[] vals = { Rusd.toStablecoin( Rusd.rusdAddr, 34.0111) }; // it gets rounded to three decimal places
		assertEquals( 
				"000000000000000000000000000000000000000000000000000000000206f778",
				Fireblocks.encodeParameters( types, vals) );

		Object[] vals2 = { Rusd.toStablecoin( Rusd.busdAddr, 34.0111) };
		S.out( Fireblocks.encodeParameters( types, vals2) );
		assertEquals( 
				"000000000000000000000000000000000000000000000001d7ff584d4fcf8000",
				Fireblocks.encodeParameters( types, vals2) );
		
	}
	
	public static final String ge  = "0x7abc82771a6afa4d0d56045cf09cb1deaedb3cc2";
	
//	public void testBuyStockWithBusd() throws Exception {
//		Fireblocks.setTestVals();
//		// first you must approve the transaction, which has to be signed by the user,
//		// or create a test user wallet account in Fireblocks, which makes more sense
//		// you must have BUSD in the user account and base currency (e.g. BNB) in the RefWallet 
//		
//		// let user wallet approve RUSD to spend BUSD; user wallet must have some BNB in it
//		double amt = 155.55;
//		String id1 = Busd.approveToSpendBusd( Rusd.userAcctId, Rusd.rusdAddr, amt);
//		MyJsonObject approveTrans = Fireblocks.getTransaction( id1);
//		approveTrans.display("'approve' transaction");
//		
//		// let refWallet call RUSD.buy()
//		String id2 = Rusd.buyStock(Rusd.userAddr, Rusd.busdAddr, amt, ge, 4.5);
//		Fireblocks.getTransaction( id2).display("buy stock with BUSD");
//		
//		assertEquals( 66, Fireblocks.getTransHash(id2,60).length() );
//	}
//
	public void testBuyStockWithRusd() throws Exception {
		Fireblocks.setTestVals();
		
		// let refWallet call RUSD.buy()
		String id2 = Rusd.buyStock(Rusd.userAddr, Rusd.rusdAddr, 3.0, ge, 4.5);
		Fireblocks.getTransaction( id2).display("buy stock with RUSD");

		assertEquals( 66, Fireblocks.getTransHash(id2,60).length() );
	}

	public void testSellStock() throws Exception {
		Fireblocks.setTestVals();

		// let refWallet call RUSD.buy()
		String id2 = Rusd.sellStock(Rusd.userAddr, Rusd.rusdAddr, 99.991, ge, 4.555);
		Fireblocks.getTransaction(id2).display("sell stock for RUSD");

		assertEquals( 66, Fireblocks.getTransHash(id2,60).length() );
	}


}	
