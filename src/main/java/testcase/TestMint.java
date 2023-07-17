package testcase;

public class TestMint extends MyTestCase {
	public void test() throws Exception {
		cli().get("/mint/" + Cookie.wallet);
		assertEquals( 200, cli.getResponseCode() );
	}
}
