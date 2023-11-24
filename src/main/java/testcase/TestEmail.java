package testcase;

import tw.util.IStream;

public class TestEmail extends MyTestCase {
	public void test() throws Exception {
//		m_config.sendEmail("peteraspiro@gmail.com", "test subject", "test body", false); 

		//String html = new IStream("c:/temp/file.html").readAll();
		String orig = new IStream("c:/temp/file.html").readAll();
		String to = "peterspiro@reflection.trading";
		
		m_config.sendEmail(to, "from Eurodns", orig, true);
		
//		Auth.auth().getMail().send(
//				"peter", 
//				"peteraspiro@gmail.com", 
//				to, 
//				"from gmail", 
//				orig, 
//				"html"
//			);
	}
}
