package web3;

import common.Util;
import fireblocks.Accounts;
import fireblocks.FbErc20;
import fireblocks.Fireblocks;
import positions.MoralisServer;
import reflection.Config;
import testcase.MyTestCase;
import tw.util.S;

/** Migrate from Fireblocks to Refblocks.
 *  Dev2 should have new keys and addresses for owner, refWallet, and admin */
public class Migrate {
	static Config oldDev;
	static Config newDev;

	public static void main(String[] args) throws Exception {
		oldDev = Config.ask( "Dev");
		newDev = Config.ask( "Dev2");

		//migrate();

		// transfer matic for RefWallet 
		//transferMatic( 
		newDev.matic().transfer( newDev.ownerKey(), newDev.admin1Addr(), .03)
			.waitForHash();

//		S.out( "check owner and refwallet on scanner; check crypto tab on Monitor");
//		S.out( "rusd: " + newDev.blockchainAddress( newDev.rusdAddr() ) );
//		S.out( "stock: " + newDev.blockchainAddress( newDev.readStocks().getAnyStockToken().address() ) );
	}
	
	static void migrate() throws Exception {
		// set new owner on stocks
		Util.forEach( oldDev.readStocks(), stock -> {
			S.out( "updating owner on stock " + stock.symbol() );
		
			FbErc20 tok = new FbErc20( stock.getSmartContractId(), 18, "name");
			tok.setOwner( Accounts.instance.getId( "Owner"), newDev.ownerAddr() )
				.waitForHash();
		});

		// set new owner on RUSD
		oldDev.rusd().setOwner( oldDev.ownerKey(), newDev.ownerAddr() )
				.waitForHash();
		
		// transfer balance from old owner to new owner
		transferMatic( oldDev.ownerKey(), oldDev.ownerAddr(), newDev.ownerAddr() );
		
		// set new admin on RUSD; this is sufficient to run the RefAPI
		// with RefBlocks, but some things from Monitor won't work;
		// could be done as a phase 1 as Fireblocks would continue to work
		newDev.rusd().addOrRemoveAdmin( newDev.ownerKey(), newDev.admin1Addr(), true)
				.waitForHash();
		
		// update the RefWallet; even after this is done, the
		// Fireblocks should continue to work since the Fireblocks admin is still there on RUSD
		newDev.rusd().setRefWallet( newDev.ownerKey(), newDev.rusdAddr() )
				.waitForHash();

		// transfer matic for RefWallet 
		transferMatic( oldDev.refWalletKey(), oldDev.refWalletAddr(), newDev.refWalletAddr() );

		// let RefWallet give approval to RUSD
		newDev.giveApproval()
				.waitForHash();
		
		// transfer balance from old admin to new admin; no need to wait
		transferMatic( "Admin1", oldDev.admin1Addr(), newDev.admin1Addr() ); 
	}

	/** Transfer matic and wait for it to appear */
	private static void transferMatic( String sourceKey, String sourceAddr, String destAddr) throws Exception {
		// transfer balance from old owner to new owner
		Fireblocks.transfer(
				Accounts.instance.getId(sourceKey), 
				destAddr, 
				Fireblocks.platformBase,
				.03, //MoralisServer.getNativeBalance( sourceAddr) - .01, 
				"transfer matic to new owner")
			.waitForHash();

		// wait for balance to register or the next transaction will fail due to insufficient gas
		MyTestCase.waitFor(60, () -> MoralisServer.getNativeBalance( destAddr) > 0);
	}
}
