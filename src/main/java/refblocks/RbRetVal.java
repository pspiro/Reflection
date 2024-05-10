package refblocks;

import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.response.EmptyTransactionReceipt;

import refblocks.Refblocks.DelayedTrp;
import tw.util.S;
import web3.RetVal;

public class RbRetVal extends RetVal {
	private DelayedTrp m_trp;
	private TransactionReceipt m_receipt;

	/** If we already have the real receipt */
	RbRetVal( TransactionReceipt receipt) {
		m_receipt = receipt;
	}
	
	/** If we need to wait for the receipt; we have just a EmptyTransactionReceipt most likely 
	 * @param receipt */
	public RbRetVal(DelayedTrp trp, TransactionReceipt receipt) {
		m_trp = trp;
		m_receipt = receipt;
	}
	
	/** Return transaction hash; we should have this even if we only have the temporary receipt. */
	public String id() {
		return m_receipt != null ? m_receipt.getTransactionHash() : "";
	}

	/** Wait for receipt or return it hash if we already have it */
	public String waitForHash() throws Exception {
		if (m_trp != null) {
	    	S.out( "waiting for transaction receipt for %s, polling every %s ms",
	    			id(), Refblocks.PollingInterval);
			
	    	m_receipt = m_trp.reallyWait();
			
	    	S.out( "Received transaction receipt: " + Refblocks.toString( m_receipt) );
			
	    	checkReceipt( m_receipt);
		}
		return m_receipt.getTransactionHash();
	}
	
	/** This blocks for up to 2 min */
	public void waitForCompleted() throws Exception {
		waitForHash();
	}

	/** this code was copied from Contract.executeTransaction() 
	 * @throws TransactionException */
	private void checkReceipt(TransactionReceipt receipt) throws TransactionException {
	    if (!(receipt instanceof EmptyTransactionReceipt)
                && receipt != null
                && !receipt.isStatusOK()) {

	    	throw new TransactionException( String.format(
	    			"Transaction %s has failed with status %s and reason %s",
   					m_receipt.getTransactionHash(),
   					m_receipt.getStatus(),
   					m_receipt.getRevertReason()
   					) );
        }
	    // to get the real reason, call this 
	    // String revertReason = RevertReasonExtractor.retrieveRevertReason(transactionReceipt, data, web3j, weiValue);
	}

}
