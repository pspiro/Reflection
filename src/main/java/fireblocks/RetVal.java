package fireblocks;

import tw.util.S;

public class RetVal {
	private String m_id;

	RetVal(String id) {
		m_id = id;
	}
	
	String id() {
		return m_id;
	}
	
	/** This blocks for up to 63 seconds */
	public String waitForHash() throws Exception {
		S.out( "  waiting for blockchain hash...");
		return Fireblocks.getTransHash(m_id, 60);
	}
}
