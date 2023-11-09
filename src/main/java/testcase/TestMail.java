package testcase;

import reflection.Config;

public class TestMail extends MyTestCase {
	public void test() throws Exception {
		Config.readFrom("Dt-config").sendEmail("peteraspiro@gmail.com", "test subject", "test body"); 
	}
}
