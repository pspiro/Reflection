package fireblocks;

public class FbRetVal extends RetVal {
	private String m_id;

	public FbRetVal(String id) {
		m_id = id;
	}
	
	public String id() {
		return m_id;
	}
	
	/** This blocks for up to 63 seconds */
	@Override public String waitForHash() throws Exception {
		return Fireblocks.waitForHash(m_id, 60, 2000);
	}
	
	/** This blocks for up to 2 min */
	@Override public void waitForCompleted() throws Exception {
		Fireblocks.waitForStatus(m_id, "COMPLETED");  // throw an exception if not completed. pas
	}
	
	/** This blocks for up to 2 min */
	@Override public void waitForStatus(String status) throws Exception {
		Fireblocks.waitForStatus(m_id, status);
	}
}
