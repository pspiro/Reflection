package web3;

import chain.Chain;
import chain.Chains;
import common.MyScanner;
import tw.util.S;

/** This actually works as of 8/19/24 on pulsechain */
public class CancelStuckTransaction {
	public static void main(String[] args) throws Exception {
		try (MyScanner s = new MyScanner() ) {
			String name = s.getString( "enter chain name: [Polygon]");
			name = S.isNull( name) ? "Polygon" : name;
			
			Chain chain = new Chains().readOne( name, false);

			String wallet = chain.params().ownerAddr();// admin1Addr();  // wallet that is stuck

			// show current nonces
			chain.blocks().showAllNonces( wallet);

			// show nonces of stuck transactions
			chain.node().showTrans( wallet);

			// you have to cancel the lowest nonce first, but then the others will go through
			// you might have to replace the others, not wait for a receipt, then cancel the lowest

			while( true) {
				String str = s.getString( "enter nonce to cancel: ");
				if (S.isNull( str) ) {
					break;
				}

				chain.blocks().cancelStuckTransaction( chain.params().admin1Key(), Integer.parseInt( str) );
			}
		}

	}

	//static void fillTheGap() {
}

/* There was a very annoying and hard to fix issue. When the admin1 wallet
ran out of gas, many transactions were submitted with increasing nonces.
They all failed, but they stayed in the 'queued' transactions list. But
only in Asia; they did not show up in the US. And only in the main RPC 
node.

These transactions could not be canceled because there was a nonce gap.

The solution was to switch to another RPC node (Moralis). I suppose each
node keeps it's own queue. Another solution which may have worked would
be to submit dummy transactions to fill the nonce gap, and then to keep
going to cancel the queued transactions.

I must wonder if I can ever switch back to the main RPC node.

The bad transactions eventually cleared out, after several hours, after
a 're-indexing', I think

Today, on 12/5/24, after an hours-long conversation with ChatGPT, I have reach
total clarity on the mempool and different states of a transaction,
and also what happened yesterday when orders got stuck and couldn't be unstuck.

Submit an order where you have insufficient gas
The order will be approved and go to the mempool and then be rejected
Submit another order right away. There will be a nonce gap, so it will sit in the queue,
as will all subsequent orders. You must submit a dummy order with the canceled nonce
to free up the other orders stuck in the queue. But those order will then go in;
if you don't want that, you must cancel those orders first, before filling in the gap.

You might not see the orders in the queue if you query for it because those orders
are not propogated and you never know which node your query is going to be sent to.

So, anytime you don't get a receipt, you must assume that there is a nonce gap and the 
transaction is in the queue. You must decide if you want it to go in or not when
the gap is filled; probably not, so you should cancel the transaction.

When canceling a transaction, you might want to up the gas because I saw an error
saying 'insufficient gas for replacement transaction.'

The only thing I'm not sure about is this: I think the India queries were returning
different latest and pending nonces than NY; that shouldn't be possible.
What could cause that? It eventually cleared up and re-synced.

So, should you use pending or latest when getting the nonce?
Why would they be different?
A valid trans was just submitted and hasn't gone in yet; then you want to use the pending nonce.
A transaction is stuck in the mempool; it needs to be removed OR replaced; use the latest nonce

It seems that for TRANSFERS, it will pick up insuf. gas prior to adding to the mempool,
but for other ERC20 transactions, it does not.

The only foolproof way to do this is to submit one order at a time and wait for the receipt for each one.
When the receipt comes, increase the nonce OR query for it; at that point you might as well query
for it since fasterTM won't be helping you;
if no receipt comes...it means nonce gap OR gas price too low; you can figure out which one
by looking for the order in the mempool ('pending' state); if it's there, it's low gas and
must be fixed or canceled; if it's not there, it's in the queue meaning there is a nonce gap
either cancel it, or don't, and fill the gap

 */
