package fireblocks;

import web3.RetVal;

public class FbRetVal extends RetVal {
	private String m_id;

	public FbRetVal(String id) {
		m_id = id;
	}
	
	@Override public String id() {
		return m_id;
	}
	
	/** This blocks for up to 2 min */
	@Override public String waitForHash() throws Exception {
		// take your pick
		Fireblocks.waitForHash(m_id, 60, 2000);
		Fireblocks.waitForStatus(m_id, "COMPLETED");  // throw an exception if not completed. pas
		throw new Exception();
	}
}
