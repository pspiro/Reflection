package testcase;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import junit.framework.TestCase;


@RunWith(Suite.class)
@Suite.SuiteClasses({
	   TestOrder.class,
	   TestBackendMsgs.class,
	   TestConfig.class,
	   TestConfigSheet.class,
	   TestErrors.class,
	   TestFaqs.class,
	   TestFireblocks.class,
	   TestGetPositions.class,
	   TestGtable.class,
//	   TestMyRedis.class,
//	   TestOne.class,
	   TestOutsideHours.class,
	   TestPrices.class,
	   TestRedeem.class,
	   TestSiwe.class,
	   TestUnwindOrder.class,
	   TestWallet.class,
})
public class TestAll extends TestCase {
}
