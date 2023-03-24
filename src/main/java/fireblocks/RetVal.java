package fireblocks;

public class RetVal {
	private String m_id;

	RetVal(String id) {
		m_id = id;
	}
	
	String id() {
		return m_id;
	}
	
	/** This blocks for up to 63 seconds */
	String waitForHash() throws Exception {
		return Fireblocks.getTransHash(m_id, 60);
	}
}
