package testcase;

import common.Util;

public class TestMail {
	public static void main(String[] args) throws Exception {
		final String username = "josh@reflection.trading";
		final String password = "KyvuPRpi7uscVE@";
		Util.sendEmail(username, password, "Reflect", "peteraspiro@gmail.com", "sub", "text"); 
	}
}
