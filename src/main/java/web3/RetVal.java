package web3;

import org.json.simple.JsonObject;

import tw.util.S;

public abstract class RetVal {
	public abstract String id();  // FB ID or transaction hash

	/** This blocks for up to 63 seconds. For Refblocks, it's really more like
	 *  "waitForReceipt()" */
	public abstract String waitForHash() throws Exception;

	/** this is a more accurate name */
	public final String waitForReceipt() throws Exception {
		return waitForHash();
	}

	public void displayHash() throws Exception {
		S.out( waitForHash() );
	}

	public static class NewRetVal extends RetVal {
		private String m_hash;
		private NodeInstance m_node;
		private String m_from;
		private String m_to;
		private String m_data;

		public NewRetVal(String hash, NodeInstance node, String from, String to, String data) {
			m_hash = hash;
			m_node = node;
			m_from = from;
			m_to = to;
			m_data = data;
		}

		@Override public String id() {
			return m_hash;
		}
		
		/** wait for receipt */  // could move this into NodeInstance
		@Override public String waitForHash() throws Exception {
			S.out( "waiting for transaction receipt");
			
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
				S.out( "got receipt in %s sec", (System.currentTimeMillis() - start) / 1000.);

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
