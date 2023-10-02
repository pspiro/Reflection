package fireblocks;

public class RetVal {
	private String m_id;

	public RetVal(String id) {
		m_id = id;
	}
	
	public String id() {
		return m_id;
	}
	
	/** This blocks for up to 63 seconds */
	public String waitForHash() throws Exception {
		return Fireblocks.waitForHash(m_id, 60, 2000);
	}
	
	/** This blocks for up to 90 seconds */
	public void waitForCompleted() throws Exception {
		waitForStatus("COMPLETED");
	}
	
	/** This blocks for up to 90 seconds */
	public void waitForStatus(String status) throws Exception {
		Fireblocks.waitForStatus(m_id, status);
	}
}
