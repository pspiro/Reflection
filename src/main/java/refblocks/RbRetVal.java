package refblocks;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketTimeoutException;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.response.EmptyTransactionReceipt;
import org.web3j.utils.RevertReasonExtractor;

import common.Util;
import refblocks.Refblocks.DelayedTrp;
import tw.util.S;
import web3.RetVal;

/** It's starting to feel like this is a lot of work to return only a few seconds
 *  earlier than we would if we waited for the receipt to come.
 *  
 *  This class is redundant with NodeRetVal; we only need one of them
 *  
 *  If there is an error, we re-play the message at the end of the correct block
 *  and capture the message text; it is only mostly accurate. */
public class RbRetVal extends RetVal {
	private TransactionReceipt m_receipt;  // could be a real receipt or an empty receipt
	private Web3j m_web3j;
	private Function m_function;  // the function that was called on the smart contract; we need this to display error text; could be null

	/** If we already have the real receipt
	 *  @param function could be null */
	public RbRetVal( TransactionReceipt receipt, Web3j web3j, Function function) {
		m_receipt = receipt;
		m_web3j = web3j;
		m_function = function;  // could be null
	}
	
	/** Return transaction hash; we should have this even if we only have the temporary receipt. */
	public String hash() {
		return m_receipt != null ? m_receipt.getTransactionHash() : "";
	}

	/** Wait for receipt or return it hash if we already have it */
	public String waitForReceipt() throws Exception {
		Util.require( m_receipt != null, "null receipt");  // maybe you didn't 

		if (m_receipt instanceof EmptyTransactionReceipt) { 
	    	S.out( "  waiting for transaction receipt for %s, polling every %s ms",
	    			hash(), Refblocks.PollingInterval);
			
	    	m_receipt = waitForReceipt( m_receipt.getTransactionHash() ); // only web3j is needed, which contract has
			
	    	S.out( "  received transaction receipt: " + Refblocks.toString( m_receipt) );

	    	// some error occurred?
		    if ( m_receipt != null && !m_receipt.isStatusOK() ) {
				String error = String.format( "Transaction failed  method=%s  hash=%s  status=%s  reason=%s",
						getFunctionName(),
	   					m_receipt.getTransactionHash(),
	   					m_receipt.getStatus(),
	   					getReason( m_receipt) );

				S.out( "  " + error);
				throw new TransactionException( error);  				
	        }
		}
		
		return m_receipt.getTransactionHash();
	}
	
	private String getFunctionName() {
		return m_function != null ? m_function.getName() : "?";
	}

	/** If we have the function, replay the failed message to get the reason */ 
	private String getReason(TransactionReceipt receipt) {
		try {
			return m_function != null
				? RevertReasonExtractor.retrieveRevertReason( // you could switch and use NodeInstance instead
					receipt, 
					FunctionEncoder.encode(m_function),
					m_web3j, BigInteger.ZERO)
				: "unknown, function not provided";
		} catch (IOException e) {
			return "Could not get reason - " + e.getMessage();
		}
	}
	
	/** wait for the transaction receipt with polling;
	 *  this can timeout; try twice */
	public TransactionReceipt waitForReceipt(String hash) throws Exception {
		try {
			// try once
			return new DelayedTrp( m_web3j).reallyWait( hash);
		}
		catch( SocketTimeoutException e) {
			// try twice; second failing throws the exception
			S.out( "WARNING: waiting for receipt http call timed out; trying again"); // not the same as the whole wait-for-receipt process timing out
			return new DelayedTrp( m_web3j).reallyWait( hash);
		}
	}


}
