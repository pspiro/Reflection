package refblocks;

import java.io.IOException;
import java.math.BigInteger;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.response.EmptyTransactionReceipt;
import org.web3j.utils.RevertReasonExtractor;

import common.Util;
import tw.util.S;
import web3.RetVal;

/** It's starting to feel like this is a lot of work to return only a few seconds
 *  earlier than we would if we waited for the receipt to come.
 *  
 *  If there is an error, we re-play the message at the end of the correct block
 *  and capture the message text; it is only mostly accurate. */
public class RbRetVal extends RetVal {
	private TransactionReceipt m_receipt;  // could be a real receipt or an empty receipt
	private Function m_function;  // the function that was called on the smart contract; we need this to display error text
	private Refblocks m_blocks;

	/** If we already have the real receipt */
	public RbRetVal( TransactionReceipt receipt, Refblocks blocks) {
		m_receipt = receipt;
		m_blocks = blocks;
	}
	
	RbRetVal( TransactionReceipt receipt, Function function) {
		m_receipt = receipt;
		m_function = function;
	}
	
	/** Return transaction hash; we should have this even if we only have the temporary receipt. */
	public String id() {
		return m_receipt != null ? m_receipt.getTransactionHash() : "";
	}

	/** Wait for receipt or return it hash if we already have it */
	public String waitForReceipt() throws Exception {
		Util.require( m_receipt != null, "null receipt");  // maybe you didn't 

		if (m_receipt instanceof EmptyTransactionReceipt) { 
	    	S.out( "waiting for transaction receipt for %s, polling every %s ms",
	    			id(), Refblocks.PollingInterval);
			
	    	m_receipt = m_blocks.waitForReceipt( m_receipt.getTransactionHash() );
			
	    	S.out( "received transaction receipt: " + Refblocks.toString( m_receipt) );
			
	    	checkReceipt( m_receipt);
		}
		
		return m_receipt.getTransactionHash();
	}
	
	/** this code was copied from Contract.executeTransaction() 
	 * @throws TransactionException */
	protected void checkReceipt(TransactionReceipt receipt) throws TransactionException {
	    if (!(receipt instanceof EmptyTransactionReceipt)
                && receipt != null
                && !receipt.isStatusOK() ) {

			String error = String.format( "Transaction failed  method=%s  hash=%s  status=%s  reason=%s",
					getFunctionName(),
   					m_receipt.getTransactionHash(),
   					m_receipt.getStatus(),
   					getReason( receipt) );
   							
   			throw new TransactionException( error);  				
        }
	}
	
	private String getFunctionName() {
		return m_function != null ? m_function.getName() : "?";
	}

	/** If we have the function, replay the failed message to get the reason */ 
	private String getReason(TransactionReceipt receipt) {
		try {
			return m_function != null
				? RevertReasonExtractor.retrieveRevertReason( 
					receipt, 
					FunctionEncoder.encode(m_function),
					m_blocks.web3j(), BigInteger.ZERO)
				: "unknown, function not provided";
		} catch (IOException e) {
			return "Could not get reason - " + e.getMessage();
		}
	}

}
