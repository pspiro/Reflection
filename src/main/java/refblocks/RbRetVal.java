package refblocks;

import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.response.EmptyTransactionReceipt;

import common.Util;
import tw.util.S;
import web3.RetVal;

/** It's starting to feel like this is a lot of work to return only a few seconds
 *  earlier than we would if we waited for the receipt to come */
public class RbRetVal extends RetVal {
	private TransactionReceipt m_receipt;  // could be a real receipt or an empty receipt

	/** If we already have the real receipt */
	RbRetVal( TransactionReceipt receipt) {
		m_receipt = receipt;
	}
	
	/** Return transaction hash; we should have this even if we only have the temporary receipt. */
	public String id() {
		return m_receipt != null ? m_receipt.getTransactionHash() : "";
	}

	/** Wait for receipt or return it hash if we already have it */
	public String waitForHash() throws Exception {
		Util.require( m_receipt != null, "null receipt");  // maybe you didn't 

		if (m_receipt instanceof EmptyTransactionReceipt) { 
	    	S.out( "waiting for transaction receipt for %s, polling every %s ms",
	    			id(), Refblocks.PollingInterval);
			
	    	m_receipt = Refblocks.waitForReceipt( m_receipt);
			
	    	S.out( "received transaction receipt: " + Refblocks.toString( m_receipt) );
			
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
                && !receipt.isStatusOK() ) {

	    	throw new TransactionException( String.format(
	    			"Transaction %s has failed with status %s and reason %s",
   					m_receipt.getTransactionHash(),
   					m_receipt.getStatus(),
   					m_receipt.getRevertReason()
   					) );
        }

	    // LEAVE THIS CODE
	    // use FunctionEncoder.encode(function) to get data
	    // to get the real reason, call this
	    // the problem is, we don't have the data which comes from:
	    // FunctionEncoder.encode(function)
	    // and we don't have the function; you could get it if you changed
	    // the code or added code to the generated classes e.g. refblocks.Rusd
	    
//	    String revertReason = RevertReasonExtractor.retrieveRevertReason(
//	    		receipt, 
//	    		data, 
//	    		web3j, 
//	    		weiValue);
	}

}
