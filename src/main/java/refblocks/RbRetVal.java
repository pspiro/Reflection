package refblocks;

import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.response.EmptyTransactionReceipt;

import fireblocks.RetVal;
import refblocks.Refblocks.DelayedTrp;
import tw.util.S;

public class RbRetVal extends RetVal {
	private TransactionReceipt m_receipt;
	private DelayedTrp m_trp;

	/** If we already have the receipt */
	RbRetVal( TransactionReceipt receipt) {
		m_receipt = receipt;
	}
	
	/** If we need to wait for the receipt */
	public RbRetVal(DelayedTrp trp) {
		m_trp = trp;
	}

	/** Wait for receipt or return it hash if we already have it */
	public String waitForHash() throws Exception {
		if (m_trp != null) {
			m_receipt = m_trp.reallyWait();
			S.out( "Received receipt: " + this);
			checkReceipt( m_receipt);
		}
		return m_receipt.getTransactionHash();
	}
	
	/** This blocks for up to 2 min */
	public void waitForCompleted() throws Exception {
		throw new Exception();
	}

	/** This blocks for up to 2 min */
	public void waitForStatus(String status) throws Exception {
		throw new Exception();
	}
	
	@Override public String toString() {
		return m_receipt != null ? Refblocks.toString( m_receipt) : "";
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
