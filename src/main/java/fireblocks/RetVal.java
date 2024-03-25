package fireblocks;

import reflection.Config;

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
	
	/** This blocks for up to 2 min */
	public RetVal waitForCompleted() throws Exception {
		return waitForStatus("COMPLETED");
	}
	
	/** This blocks for up to 2 min */
	public RetVal waitForStatus(String status) throws Exception {
		Fireblocks.waitForStatus(m_id, status);
		return this;
	}
	
	public static void main(String[] args) throws Exception {
		Config.ask();
		new RetVal( "98115961-d3cf-48dd-9686-2b31e0aabac3").waitForCompleted();
	}
}
