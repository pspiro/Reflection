//package fireblocks;
//
//import common.Util;
//import positions.Wallet;
//import reflection.Config;
//import reflection.Stocks;
//import tw.util.S;
//import web3.Rusd;
//import web3.StockToken;
//
///** This tests two wallets at the same time which will cause both 
// *  Admin1 and Amin2 accounts to be used */
//public class TestTwoAdmins {
//	static final String user1 = "0x614EF3Ebe43314fa73515b155e319E34C8CA348b";
//	static final String user2 = "0x4b92327b67A5F1DB38032277B3d46BC4D0e3D05b";
//	
//	public static void main(String[] args) throws Exception {
//		Config config = Config.ask();
//		Rusd rusd = config.rusd();
//		
//		Stocks stocks = config.readStocks();
//		StockToken token = stocks.getStockByConid(265598).getToken();
//		
//		S.out( "user1 balance: %s", new Wallet(user1).getBalance(rusd.address()));
//		S.out( "user2 balance: %s", new Wallet(user2).getBalance(rusd.address()));
//
//		rusd.burnRusd( user1, 4, token).waitForCompleted();  // change this to 1!!!
//		rusd.burnRusd( user2, 3, token).waitForCompleted();  // change this to 1!!!
//		
//		S.out( "user1 balance: %s", new Wallet(user1).getBalance(rusd.address()));
//		S.out( "user2 balance: %s", new Wallet(user2).getBalance(rusd.address()));
//		
//	}
//	public static void mainn(String[] args) throws Exception {
//		Config config = Config.ask();
//		Rusd rusd = config.rusd();
//		
//		Stocks stocks = config.readStocks();
//		StockToken token = stocks.getStockByConid(265598).getToken();
//
//		
//		S.out( "user1 balance: %s", new Wallet(user1).getBalance(rusd.address()));
//		S.out( "user2 balance: %s", new Wallet(user2).getBalance(rusd.address()));
//
//		Util.executeAndWrap( () -> {
//			rusd.mintRusd( user1, 1, token).waitForCompleted();
//			rusd.buyStockWithRusd( user1, 1, token, 1).waitForCompleted();
//			rusd.sellStockForRusd( user1, 1, token, 1).waitForCompleted();
//			rusd.burnRusd( user1, 1, token).waitForCompleted();  // change this to 1!!!
//		});
//		
//		Util.executeAndWrap( () -> {
//			rusd.mintRusd( user2, 1, token).waitForCompleted();
//			rusd.buyStockWithRusd( user2, 1, token, 1).waitForCompleted();
//			rusd.sellStockForRusd( user2, 1, token, 1).waitForCompleted();
//			rusd.burnRusd( user2, 1, token).waitForCompleted();  // change this to 1!!!
//		});
//		
//		S.out( "user1 balance: %s", new Wallet(user1).getBalance(rusd.address()));
//		S.out( "user2 balance: %s", new Wallet(user2).getBalance(rusd.address()));
//		
//	}
//}
//
