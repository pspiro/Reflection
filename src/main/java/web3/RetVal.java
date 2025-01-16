package web3;

import java.math.BigInteger;

import org.json.simple.JsonObject;

import common.Alerts;
import tw.util.MyException;
import tw.util.S;

public abstract class RetVal {
	protected boolean m_crucial;  // if marked crucial, we will send an alert if we timeout while waiting for the receipt

	public abstract String hash();

	/** This blocks for up to 63 seconds. */
	public abstract String waitForReceipt() throws Exception;

	public final void displayHash() throws Exception {
		S.out( waitForReceipt() );
	}

	/** Used with NodeInstance.callSigned() */
	public static class NodeRetVal extends RetVal {
		private String m_hash;
		private NodeInstance m_node;
		private String m_from; // don't need this, can get it from the receipt when it comes
		private String m_to; // don't need this, can get it from the receipt when it comes
		private String m_data;
		private long m_gasLimit;
		private BigInteger m_nonce;
		private BigInteger m_amtToSend;
		
		public BigInteger nonce() { return m_nonce; }

		/** you could remove 'from' and 'to' as params and get them from the receipt (except it may never come)
		 * @param gasLimit 
		 * @param nonce */
		public NodeRetVal(String hash, NodeInstance node, String from, String to, BigInteger amtToSend, String data, long gasLimit, BigInteger nonce) {
			m_hash = hash;
			m_node = node;
			m_from = from;
			m_to = to;
			m_data = data;
			m_gasLimit = gasLimit;
			m_nonce = nonce;
			m_amtToSend = amtToSend;
		}

		@Override public String hash() {
			return m_hash;
		}
		
		/** wait for receipt; this could be improved to make sure we don't wait for too many
		 *  at the same time to avoid excessive threads and http calls */
		@Override public String waitForReceipt() throws Exception {
			S.out( "  waiting for transaction receipt");
			
			long start = System.currentTimeMillis();
			JsonObject receipt = null;
			
			for (int i = 0; i < 15 && receipt == null; i++) {
				try {
					S.sleep( 5000);
					receipt = m_node.getReceipt( m_hash);
				} 
				catch (Exception e) {
					S.out( "received error while waiting for receipt - " + e.getMessage() );
				}
			}
			
			// timeout?
			if (receipt == null) {
				// this could mean:
				// * duplicate nonce / nonce too low; would be solved w/ retry or nonce tracking
				// * skipped nonce; resolved w/ retry; should never happen w/ nonce tracking
				// * low gas price on this transaction; will get replaced by the next transaction
				// * we ran out of gas after transaction was submitted; 'pending' nonce is increased then decreased
				// we might want to try to get revert reason here; a nonce error
				// won't show up and a gas error would. pas
				
				String body = String.format( "hash=%s  nonce=%s  chain=%s", m_hash, m_nonce, m_node.chainId() );
				if (m_crucial) {
					Alerts.alert( "RefAPI", "SOME CRUCIAL CALL TIMED OUT, CHECK LOGS", body);
				}
				throw new MyException( "Timed out while waiting for receipt  " + body);
			}
			else {
				S.out( "  got receipt in %s sec", (System.currentTimeMillis() - start) / 1000.);

				if (receipt.getString( "status").equals( "0x1") ) {
					return m_hash;
				}

				// is guaranteed to throw an exception
				m_node.getRevertReason( m_from, m_to, m_amtToSend, m_data, m_gasLimit, receipt.getString( "blockNumber") );
				throw new Exception(); // never comes here
			}						
		}
	}

	/** we should send an alert if we timeout waiting for receipt */
	public RetVal crucial() {
		m_crucial = true;
		return this;
	}
}
