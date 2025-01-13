package testcase;

import common.Util;
import tw.util.S;
import web3.NodeInstance;

/* what we see is this:
 * The vast majority of the time, the pending nonce increases before the receipt comes.
 * But at least once, when the transaction was mined very quickly, I saw it increase AFTER the receipt.
 * 
 * If a second transaction is sent w/ same nonce before first transaction is mined,
 * the second transaction times out; no receipt and no error! Why no error?!
 * (If a second transaction is sent exactly same as first, it has the same hash code and is a dup)
 * 
 * So it is possible that you could submit a transaction, wait for receipt, query nonce,
 * get same value, submit second transaction, and it will time out.
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
 * To simplify, just wait for receipt. If the 'latest' nonce increase comes first, then 
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
 * 
 * Possible solutions:
 * 1. wait for receipt, then wait another little while, but we don't know how long
 * 2. wait for nonce increase, then 
 */
public class TestNonce extends MyTestCase {
	// wait for receipt is always sufficient, 
	public void test2() throws Exception {
		Util.executeAndWrap( () -> {
			for (int i = 0; i < 10000; i++) {
				chain().showAdmin1Nonces();
				S.sleep( 300);
			}
		});

		try {
			chain().rusd().sellRusd(NodeInstance.prod, chain().busd(), 100000000).waitForReceipt();
		}
		catch( Exception e) {
			S.out( e.getMessage() );
		}
	}
	
	public void test() throws Exception {
		Util.executeAndWrap( () -> {
			for (int i = 0; i < 10000; i++) {
				chain().showAdmin1Nonces();
				S.sleep( 200);
			}
		});
		
		Util.executeAndWrap( () -> {
			S.sleep( 100);
			var wait = chain().rusd().mintRusd( NodeInstance.prod, 1, chain().getAnyStockToken() );
			S.out( "sent, waiting");
			wait.waitForReceipt();
			S.out( "received");
			chain().showAdmin1Nonces();
			S.sleep( 3000);
			System.exit(0);
		});

		Util.executeAndWrap( () -> {
			S.sleep( 500);
			var wait = chain().rusd().mintRusd( NodeInstance.prod, 2, chain().getAnyStockToken() );
			S.out( "sent, waiting");
			wait.waitForReceipt();
			S.out( "received");
			chain().showAdmin1Nonces();
			S.sleep( 3000);
			System.exit(0);
		});
		
		S.sleep( 50000);

	}
}
