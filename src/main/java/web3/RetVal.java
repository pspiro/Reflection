package web3;

import org.json.simple.JsonObject;

import tw.util.S;

public abstract class RetVal {
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

		/** you could remove 'from' and 'to' as params and get them from the receipt */
		public NodeRetVal(String hash, NodeInstance node, String from, String to, String data) {
			m_hash = hash;
			m_node = node;
			m_from = from;
			m_to = to;
			m_data = data;
		}

		@Override public String hash() {
			return m_hash;
		}
		
		/** wait for receipt */  // could move this into NodeInstance
		@Override public String waitForReceipt() throws Exception {
			S.out( "  waiting for transaction receipt");
			
			long start = System.currentTimeMillis();
			JsonObject receipt = null;
			
			for (int i = 0; i < 24 && receipt == null; i++) {
				try {
					S.sleep( 5000);
					receipt = m_node.getReceipt( m_hash);
				} 
				catch (Exception e) {
					S.out( "received error while waiting for receipt - " + e.getMessage() );
				}
			}
			
			if (receipt == null) {
				throw new Exception( "Timed out while waiting for receipt for " + m_hash);
			}
			else {
				S.out( "  got receipt in %s sec", (System.currentTimeMillis() - start) / 1000.);

				if (receipt.getString( "status").equals( "0x1") ) {
					return m_hash;
				}
				
				m_node.getRevertReason( m_from, m_to, m_data, receipt.getString( "blockNumber") );
				
				S.out( "could not get revert reason");
				throw new Exception();
			}						
		}
	}
}
