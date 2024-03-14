package testcase;

public class TestEmail extends MyTestCase {
	public void test() throws Exception {
		
		String to = "peter@reflection.trading";
		
		m_config.sendEmail(to, "this is a test", "<p>abc<br>def</p>");
		
	}
}
