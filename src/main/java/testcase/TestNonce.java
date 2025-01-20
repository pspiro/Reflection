package testcase;

import java.math.BigInteger;

import chain.Chain;
import common.Util;
import tw.util.S;
import web3.Busd;
import web3.NodeInstance;
import web3.Nonce;
import web3.Param;
import web3.Param.Address;
import web3.Param.BigInt;
import web3.RetVal;
import web3.RetVal.NodeRetVal;
import web3.Rusd;

/* what we see is this:
 * The vast majority of the time, the pending nonce increases before the receipt comes.
 * But at least once, when the transaction was mined very quickly, I saw it increase AFTER the receipt.
 * 
 * If a second transaction is sent w/ same nonce before first transaction is mined,
 * the second transaction times out; no receipt and no error! Why no error?!
 * (If a second transaction is sent exactly same as first, it has the same hash code and is a dup)
 * 
 * I will say:
 * - transactions for the same wallet must be mined serially (???)
 * - when a transaction is mined and we get receipt, we can safely increment the nonce ourselves
 * 
 * Q: when is it safe to increment the nonce?
 * Is it safe after the pending nonce increases? Would it ever increase and then still not get mined?
 * According to chat, the 'pending' nonce can increase, and then decrease.
 * Therefore, using the pending nonce is not safe. 
 * 
 * I propose that once the 'latest' nonce increases OR you recieve a receipt, it is safe to continue
 * To simplify, just wait for receipt. Or, even if the initial call succeeds, it seems the transaction
 * is always mined, so you need only wait for initial call. BUT WHAT ABOUT LOW GAS??? 
 * 
 * I now propose: wait for receipt; then, EITHER (a) increase your own nonce OR call getNonce wait
 * for it to increase before returning from the serialized chain
 * 
 * If you start seeing pending nonce increase and then decrease, which we don't know what would cause it,
 * switch to 'latest' nonce
 * 
 * For an assertion failure during blockchain execution, the transaction is still mined.
 * Not sure how to produce a transaction that goes to pending and then is not mined;
 * that would be good to test.
 * 
 * So, for a single wallet, if you are not concerned about gas, you can increment the nonce
 * yourself and carry on as soon as the initial transaction is accepted. If you want to be
 * foolproof, you must wait for either a receipt or timeout 
 *  
 * Possible solutions:
 * For the first transaction, query the nonce, try the transaction;
 * if it fails with nonce too low, try again.
 *  
 * 1. wait for receipt, then wait another little while, but we don't know how long
 * 2. wait for nonce increase, then 
 * 
 * Open Issues:
 * 
 * First query, query nonce; get wrong, too low nonce
 * submit order; get duplicate nonce error
 * Solution: try again; the second time it should work
 * 
 * Second query comes before first trans is mined and/or before
 * nonce of first trans is reflected in nonce query
 * Solution: do your own nonce management, OR
 * wait for trans to be mined; that leads to 90% success rate, then remaining
 * 10, if you get error due to wrong nonce returned, you could retry
 * 
 * On the first transaction, should we use latest or pending?
 * In a normal sitch, pending updates a little sooner, so pending is better.
 * But for too-low gas, pending is stuck, so latest is better.
 * If a wallet runs out of gas, pending would be incremented and then decremented; during that brief period, latest is better, but you can ignore this
 * Sometimes a node gets left behind; I saw this with pending (on Polygon), I assume same is true of Latest
 * 
 * 1. At startup, when you know there is no prior transaction, use latest. It's safer--if there is a stuck
 * transaction, this will clear it.
 * 2. Submit a transaction. If you get a receipt, increment your own counter; it's more reliable.
 * (But what if you mess up and send a transaction from another app???)
 * 3. If no receipt: REQUERY LATEST, IT FIXES EVERYTHING or ALWAYS use max
 * a. Nonce too low: only if trans submitted by another app; requery latest, OR take max(latest, localNonce)
 * b. Skipped a transaction: impossible, requery latest might work, might not
 * c. Gas price too low? requery for latest nonce, stuck xaction will get replaced
 * d. Ran out of gas? you're screwed, get more gas!!! 
 * 
 * Do we have to wait for the receipt? Let's say no. Just increment local nonce and go.
 * Always query latest after no transactions for 60 sec, OR if there is a timeout, just like we had before which worked good. 
 * a. Nonce too low: only if trans submitted by another app; requery latest, OR take max(latest, localNonce)
 * b. Skipped a transaction: impossible, requery latest might work, might not
 * c. Gas price too low? requery for latest nonce, stuck xaction will get replaced
 * d. Ran out of gas? you're screwed, get more gas!!!
 * 
 * SOLUTION
 * 1. Track your own nonce to avoid those intermitant error, get faster service;
 * 2. reset after 60 seconds or receipt timeout
 * 3. YOU MUST INCREMENT NONCE IF INITIAL SUBMIT FAILS WITH 'seen that!'
 * 
 * 
 */
/** A collection of tests to generate edge cases. Not desiged to 'pass' or 'fail' */
public class TestNonce extends MyTestCase {
	/** This test does fail sometimes. If the receipt comes very quickly,
	 *  there may be some nodes that don't have the updated value yet.
	 *  In fact, a node can be 2-3 nonces and 30 seconds behind!
	 *  
	 *  Pending fails on Polygon;
	 *  On Sepolia, Pending and Latest both work; latest waits until the last possible second but always comes in on time
	 *  
	 *  Mostly, the correct nonce is being reported slightly before the receipt comes.
	 *  
	 *  Note also that before the receipt, the pending may go, up, down, then
	 *  up again as the query for nonce is routed to different nodes.
	 *  
	 *  Tested on Sepolia AND Polygon */
	public void testAlwaysIncremented() throws Exception {
		S.out( "test always incremented");
		
		startShowNonces( chain().params().admin1Addr() );
		
		for (int i = 0; i < 20; i++) {
			var retval = chain().rusd().mintRusd(NodeInstance.prod, 1, chain().getAnyStockToken() );
			var nonce = ((NodeRetVal)retval).nonce();
			retval.waitForReceipt();
			var newNonce = chain().node().getNonceLatest( chain().params().admin1Addr() );
			S.out( "submitted=%s  isNow=%s", nonce, newNonce);
			Util.require( newNonce.equals( nonce.add( BigInteger.ONE) ), "not incremented yet");
		}		
	}
	
	public void testCancelStuck() {
//		chain().rusd().mintRusd(NodeInstance.prod, 1, chain().getAnyStockToken() );
		
	}
		
	
	/** works great, now deal w/ timeouts */
	public void test3() throws Exception {
		for (int i = 0; i < 3; i++) {
			Util.executeAndWrap( () -> {			
				chain().rusd().mintRusd(NodeInstance.prod, 1, chain().getAnyStockToken() )
					.waitForReceipt();
			});
		}
		S.sleep( 50000);
	}

	public void testQuicklyReplace() throws Exception {
		S.out( "test quickly replace");
		
		startShowNonces( chain().params().admin1Addr() );
		
		// succeeds
		Util.executeAndWrap( () -> {
			chain().rusd().mintRusd(NodeInstance.prod, 1, chain().getAnyStockToken() )
				.waitForReceipt();
		});

		// could succeed but most likely fails with 'replacement transaction underpriced'
		// replacement transaction must be at least 10% higher gas fee than original
		Util.executeAndWrap( () -> {
			S.sleep( 2000);
			chain().rusd().mintRusd(NodeInstance.prod, 2, chain().getAnyStockToken() )
				.waitForReceipt();
		});
		
		S.sleep( 30000);
	}
	
	/** if you hang up a transaction with low fee, the next transaction will have the
	 *  same nonce; if the transaction is the same, you will get 'already known'
	 *  if next transaction is different but still not priced properly, you will
	 *  get 'replacement transaction underpriced'
	 *  
	 *  To run this, hard-code fees to 1 in Fees
	 *  
	 * @throws Exception
	 */
	public void testLowGasPrice() throws Exception {
		startShowNonces( chain().params().admin1Addr() );

		try {
			chain().rusd().buyStockWithRusd(NodeInstance.prod, 1, chain().getAnyStockToken(), 1)
				.waitForReceipt();
		}
		catch( Exception e) {
			S.out( e.getMessage() );
		}
		
		S.sleep( 2000);
	}
	
	// wait for receipt is NOT always sufficient, 
	public void testFailDuringExecution() throws Exception {
		S.out( "test fail execution");
		startShowNonces( chain().params().admin1Addr() );

		try {
			chain().rusd().sellRusd(NodeInstance.prod, chain().busd(), 100000000)
				.waitForReceipt();
		}
		catch( Exception e) {
			S.out( e.getMessage() );
		}
		S.sleep( 2000);
	}

	/** fails with 'nonce too low' */
	public void testFailNonceTooLow() throws Exception {
		S.out( "test fail nonce too low");
		startShowNonces( chain().params().admin1Addr() );

		try {
			Nonce.plus = BigInteger.valueOf( -1);
			chain().rusd().mintRusd(NodeInstance.prod, 1, chain().getAnyStockToken() ) // << fails here with 'nonce too low: next nonce 1169, tx nonce 1168'
				.waitForReceipt();
		}
		catch( Exception e) {
			S.out( e.getMessage() );
		}
		S.sleep( 2000);
	}

	/** There's no error. The trans stays in the queue and then times out */ 
	public void testFailNonceTooHigh() throws Exception {
		S.out( "test fail");
		startShowNonces( chain().params().admin1Addr() );

		try {
			Nonce.plus = BigInteger.valueOf( 1);
			chain().rusd().mintRusd(NodeInstance.prod, 1, chain().getAnyStockToken() )
				.waitForReceipt();
		}
		catch( Exception e) {
			S.out( e.getMessage() );
		}
		S.sleep( 2000);
	}

	/** fails with 'insufficient funds for gas...' */
	public void testFailNoGas() throws Exception {
		S.out( "test fail no gas");
		String adminKey = Util.createPrivateKey();
		String adminAddr = Util.getAddress(adminKey);

		startShowNonces( adminAddr);

		try {
			sellRusd(chain(), adminKey, chain.busd(), 0, 500000) // fails here, never goes to pool
				.waitForReceipt();
		}
		catch( Exception e) {
			S.out( "ERROR: " + e.getMessage() );
		}
		S.sleep( 2000);
	}

	/** to see how much gas is really needed and used */
	// fails because new admin is not admin, you would have to add it
	public void testSucceedx() throws Exception {
		S.out( "test succeed");
		
		String adminKey = Util.createPrivateKey();
		String adminAddr = Util.getAddress(adminKey);

		// transfer enough gas for one
		BigInteger units = BigInteger.valueOf( 165000);
		double needed = Util.truncate( chain().node().queryFees().maxCost( units, BigInteger.ZERO) * 1.2, 5);
		S.out( "transferring " + needed + " matic to temp admin");
		chain().blocks().transfer(chain().params().admin1Key(), adminAddr, needed)
			.waitForReceipt();
		
		startShowNonces( adminAddr);

		sellRusd(chain(), adminKey, chain.busd(), 0, units.longValue() )
			.waitForReceipt();
		
		S.sleep( 2000);
	}

	/** second trans times out because there is insuf. gas after the first trans executes */
	public void testFailDualSend() throws Exception {
		S.out( "test succeed");
		
		String key = Util.createPrivateKey();
		String addr = Util.getAddress(key);
		
		S.out( "key=%s  addr=%s", key, addr);

		chain().blocks().transfer( chain().params().admin1Key(), addr, .002)
			.waitForReceipt();
		
		startShowNonces( addr);

		// should pass
		Util.executeAndWrap( () -> {		
			chain().blocks().transfer( key, chain().params().admin1Addr(), .0006)
				.waitForReceipt();
		});

		// FAILS AS IT SHOULD because of insuf. gas
		// NO RECEIPT EVER COMES because it is not mined 
		// **BUT** THE INITIAL CALL SUCCEEDS
		// IT IS ADDED TO MEMPOOL AND THEN REMOVED and discarded
		// Pending nonce DOES go to "2" and then goes back to "1"
		// Then it's gone, not pending, not in the queue, gone, with no error ever returned
		Util.executeAndWrap( () -> {
			S.sleep( 100);
			Nonce.plus = BigInteger.ONE;
			chain().blocks().transfer( key, chain().params().admin1Addr(), .0006) // << succeeds
				.waitForReceipt();  // << times out
		});
		
		S.sleep( 130000); // let the second one timeout; let's see if it gives a good error
	}

	/** trans nees more gas than allowed by limit;
	 *  fails with 'out of gas'; transaction is still mined */
	public void testFailLowLmt() throws Exception {
		S.out( "test fail low lmt");
		startShowNonces( chain().params().admin1Addr() );

		try {
			sellRusd(chain(), chain().params().admin1Key(), chain.busd(), 0, 30000) 
				.waitForReceipt();
		}
		catch( Exception e) {
			S.out( "ERROR: " + e.getMessage() );
		}
		chain().showAdmin1Nonces();
		S.sleep( 2000);
	}

	/** fails on initial call; never goes to pool */
	public void testFailVeryLowLmt() throws Exception {
		S.out( "test fail very low lmt");
		startShowNonces( chain().params().admin1Addr() );

		try {
			sellRusd(chain(), chain().params().admin1Key(), chain.busd(), 0, 10000) 
				.waitForReceipt();
		}
		catch( Exception e) {
			S.out( "ERROR: " + e.getMessage() );
		}
		chain().showAdmin1Nonces();
		S.sleep( 2000);
	}

	/** second transaction times out due to duplicate nonce */
	public void testTwoAtTheSameTime() throws Exception {
		S.out( "test two at the same time");
		startShowNonces( chain().params().admin1Addr() );
		
		Util.executeAndWrap( () -> {
			S.sleep( 100);
			chain().rusd().mintRusd( NodeInstance.prod, 1, chain().getAnyStockToken() )
				.waitForReceipt();
			S.out( "received");
			chain().showAdmin1Nonces();
			S.sleep( 2000);
			System.exit(0);
		});

		// send a different call with same nonce; initial call succeeds BUT transaction
		// is never mined, no receipt is returned; it probably stays in the queue and then is removed
		Util.executeAndWrap( () -> {
			S.sleep( 300);
			chain().rusd().mintRusd( NodeInstance.prod, 2, chain().getAnyStockToken() )
				.waitForReceipt();
			S.out( "received");
			chain().showAdmin1Nonces();
			S.sleep( 2000);
			System.exit(0);
		});
		
		S.sleep( 50000);
	}
	
	/** fails with non-zero amt on Sepolia due to bug in Sepolia RUSD */
	RetVal sellRusd(Chain chain, String admin1Key, Busd busd, double amt, long gasLmt) throws Exception {
		Param[] params = {
				new Address( NodeInstance.prod),
				new Address( busd.address() ), // stable addr
				new BigInt( busd.toBlockchain( amt) ), // stable amt 
				new BigInt( chain.rusd().toBlockchain( amt) ) // rusd amt
		};
		
		return chain.node().callSigned(
				admin1Key,
				chain.rusd().address(),
				Rusd.sellRusdKeccak,
				params,
				gasLmt);
	}
	
	/** query for nonces every 300 ms */
	private void startShowNonces(String adminAddr) throws Exception {
		Util.reqValidAddress(adminAddr);
		
		Util.executeAndWrap( () -> {
			while (true) {
				chain().blocks().showAllNonces(adminAddr);
				S.sleep( 1000);
			}
		});
	}
}
