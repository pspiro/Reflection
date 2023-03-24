package fireblocks;

public class RetVal {
	private String m_id;

	RetVal(String id) {
		m_id = id;
	}
	
	String id() {
		return m_id;
	}
	
	void waitForHash() throws Exception {
		Fireblocks.getTransHash(m_id, 60);
	}
}
