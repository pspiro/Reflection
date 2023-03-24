package testcase;

import junit.framework.TestCase;
import reflection.Config;

public class TestConfigSheet extends TestCase {
	static Config config = new Config();
	
	static {
		try {
			config.readFromSpreadsheet("Dev-config");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void testFireblocksKeys() {
		config.testApiKeys();
	}

}
