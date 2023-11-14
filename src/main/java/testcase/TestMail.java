package testcase;

import tw.util.IStream;

public class TestMail extends MyTestCase {
	public void test() throws Exception {
//		m_config.sendEmail("peteraspiro@gmail.com", "test subject", "test body", false); 

		String html = new IStream("c:/sync/reflection/email.html").readAll();
		m_config.sendEmail("peterspiro@reflection.trading", "html", html, true); 
	}
}
