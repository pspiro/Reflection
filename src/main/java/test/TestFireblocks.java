package test;

import java.math.BigInteger;

import fireblocks.Accounts;
import fireblocks.Fireblocks;
import fireblocks.Rusd;
import fireblocks.StockToken;
import fireblocks.Transactions;
import json.MyJsonObject;
import junit.framework.TestCase;
import reflection.Config;

public class TestFireblocks extends TestCase {
	static Config config = new Config();
	static Rusd rusd;
	
	String ge =       "0x7abc82771a6afa4d0d56045cf09cb1deaedb3cc2";
	String userAddr = "0xAb52e8f017fBD6C7708c7C90C0204966690e7Fc8"; // Testnet Test1 account (id=1)	
	String myWallet = "0xb016711702D3302ceF6cEb62419abBeF5c44450e";
	String myStock =  "0x0b55eeb4a4d9a709b1144b6991c463e9ff10648d"; // deployed with RUSD w/ two RefWallets

	static {
		try {
			config.readFromSpreadsheet("Dev-config");
			Accounts.instance.read();  // could do lazy init
			rusd = config.newRusd();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static String fix( String str) {
		return str.replaceAll( "\\'", "\"");
	}
	
	public void testEncode() throws Exception {
		String[] types = { "string", "address", "uint256", "uint256" };
		Object[] vals = {
				"hello",
				myWallet,
				3,
				new BigInteger("4")
		};
		
		assertEquals( 
				"0000000000000000000000000000000000000000000000000000000000000080000000000000000000000000b016711702D3302ceF6cEb62419abBeF5c44450e00000000000000000000000000000000000000000000000000000000000000030000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000568656c6c6f000000000000000000000000000000000000000000000000000000",
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
	

	public void testToStablecoin() throws Exception {
//		String[] types = { "uint256" };
//		Object[] vals = { Rusd.toStablecoin( 34.01112) }; // it gets rounded to three decimal places
//		assertEquals( 
//				"000000000000000000000000000000000000000000000000000000000206f7dc",
//				Fireblocks.encodeParameters( types, vals) );
//
//		Object[] vals2 = { Rusd.toStablecoin( 34.0111) };
//		S.out( Fireblocks.encodeParameters( types, vals2) );
//		assertEquals( 
//				"000000000000000000000000000000000000000000000001d7ff584d4fcf8000",
//				Fireblocks.encodeParameters( types, vals2) );
		
	}
	
	
//	public void testBuyStockWithBusd() throws Exception {
//	// first you must approve the transaction, which has to be signed by the user,
//	// or create a test user wallet account in Fireblocks, which makes more sense
//	// you must have BUSD in the user account and base currency (e.g. BNB) in the RefWallet 
//	
//	// let user wallet approve RUSD to spend BUSD; user wallet must have some BNB in it
//	double amt = 155.55;
//	String id1 = Busd.approveToSpendBusd( Rusd.userAcctId, Rusd.rusdAddr, amt);
//	MyJsonObject approveTrans = Fireblocks.getTransaction( id1);
//	approveTrans.display("'approve' transaction");
//	
//	// let refWallet call RUSD.buy()
//	String id2 = Rusd.buyStock(Rusd.userAddr, Rusd.busdAddr, amt, ge, 4.5);
//	Fireblocks.getTransaction( id2).display("buy stock with BUSD");
//	
//	assertEquals( 66, Fireblocks.getTransHash(id2,60).length() );
//}
//
}
