package testcase;

import common.Util;

public class TestMail extends MyTestCase {
	public void test() throws Exception {
		final String username = "josh@reflection.trading";
		final String password = "KyvuPRpi7uscVE@";
		Util.sendEmail(username, password, "Reflect", "peteraspiro@gmail.com", "test subject", "test body"); 
	}
}
