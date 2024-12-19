package testcase.web3;

import common.Util;
import testcase.MyTestCase;
import tw.util.S;
import web3.Param;
import web3.Param.Address;
import web3.Param.BigInt;
import web3.Rusd;

public class TestPreRusd extends MyTestCase {
//	addOrRemoveAdmin(String ownerKey, String address, boolean add) throws Exception {
//	burnRusd(String address, double amt, StockToken anyStockToken) throws Exception {
//	buyStock(String userAddr, Stablecoin stablecoin, double stablecoinAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
//	buyStockWithRusd(String userAddr, double stablecoinAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
//	mintRusd(String address, double amt, StockToken anyStockToken) throws Exception {
//	mintStockToken(String address, StockToken stockToken, double amt) throws Exception {
//	sellRusd(String userAddr, Busd busd, double amt) throws Exception {
//	sellStockForRusd(final String userAddr, final double rusdAmt, StockToken stockToken, double stockTokenAmt) throws Exception {
//	setRefWallet( String ownerKey, String refWalletAddr) throws Exception {
//	swap( String userAddr, StockToken stockToBurn, StockToken stockToMint, double burnAmt, double mintAmt) throws Exception {
//	deploy( String ownerKey, String refWallet, String admin1) throws Exception {

	public void testPre1() throws Exception {
		String key = Util.createPrivateKey();
		String walletAddr = Util.getAddress(key);
		
		var tok = chain.getAnyStockToken();
		
		// fail, no RUSD to buy with
		not( () -> {
			S.out( "buying with RUSD, fail");
			chain.rusd().preBuyStock(walletAddr, chain.rusd(), 1, tok, 1);
		});

		// fail, no approval
		not( () -> {
			S.out( "buying with BUSD, fail");
			chain.rusd().preBuyStock(walletAddr, chain.busd(), 1, tok, 1);
		});

		// fail, no stock token
		not( () -> {
			S.out( "sell stock token, fail");
			chain.rusd().preSellStock(walletAddr, 1, tok, 1);
		});
	}
	
	public void testPre2() throws Exception {
		String key = Util.createPrivateKey();
		String walletAddr = Util.getAddress(key);
		
		var tok = chain.getAnyStockToken();

		// mint RUSD; buy stock will now pass
		S.out( "buy stock token:");
		chain.rusd().mintRusd(walletAddr, 2, tok ).waitForReceipt();
		chain.rusd().preBuyStock(walletAddr, chain.rusd(), 1, tok, 1);

		String newAdminKey = Util.createPrivateKey();
		String newAdminAddr = Util.getAddress(newAdminKey);
		
		Param[] params = {
				new Address( walletAddr),
				new Address( chain.rusd().address() ),
				new Address( tok.address() ),
				new BigInt( chain.rusd().toBlockchain( 1) ), 
				new BigInt( tok.toBlockchain( 1) )
		};

		// fail buy, new admin has no gas
		not( () -> {
			S.out( "fail buy, admin has no gas:");
			chain.node().preCheck( newAdminAddr, chain.rusd().address(), Rusd.buyStockKeccak, params, 500000);
		});
		
		// give it gas
		S.out( "give some gas to new admin:");
		chain.blocks().transfer(chain.params().admin1Key(), newAdminAddr, .04).waitForReceipt();

		// fail because it's invalid admin
		not( () -> {
			S.out( "fail buy, invalid admin:");
			chain.node().preCheck( newAdminAddr, chain.rusd().address(), Rusd.buyStockKeccak, params, 500000);
		});
		// even with gas it would fail since a

		// add new admin
		S.out( "add new admin:");
		chain.rusd().addOrRemoveAdmin( chain.params().ownerKey(), newAdminAddr, true).waitForReceipt();

		// succeeds
		S.out( "succeed:");
		chain.node().preCheck( newAdminAddr, chain.rusd().address(), Rusd.buyStockKeccak, params, 500000);

		S.out( "remove new admin");
		chain.rusd().addOrRemoveAdmin( chain.params().ownerKey(), newAdminAddr, false);
		
		// send back the gas (could fail, then we burned .04)
		S.out( "sending back gas from %s", newAdminKey);
		chain().blocks().transfer( newAdminKey, chain.params().admin1Addr(), .03);  // could fail, we don't know how much it will cost
		// I don't understand why this fails, it should be way more than enough to pay the gas fee
	}

}
