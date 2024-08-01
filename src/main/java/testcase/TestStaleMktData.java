package testcase;

/** To test stale market data:
 * set config to no auto-fill
 * run MdServer and RefAPI, get some prices
 * kill MdServer; observe first email
 * restart MdServer; observe second email
 */
public class TestStaleMktData extends MyTestCase {
	public void test() {
		
	}
}
