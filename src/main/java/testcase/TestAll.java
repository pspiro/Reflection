package testcase;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import junit.framework.TestCase;


@RunWith(Suite.class)
@Suite.SuiteClasses({
	   TestBackendMsgs.class,
	   TestConfig.class,
	   TestConfigSheet.class,
	   TestErrors.class,
	   TestFaqs.class,
	   TestFireblocks.class,
	   TestGetPositions.class,
	   TestGtable.class,
	   TestOrder.class,
	   TestOutsideHours.class,
	   TestPrices.class,
	   TestProfile.class,
	   TestRedeem.class,
	   TestSiwe.class,
	   TestStrings.class,
	   TestTwoOrdersDifUser.class,
	   TestTwoOrdersSameUser.class,	   
	   TestUnwindOrder.class,
	   TestWallet.class,
//	   TestMyRedis.class,
//	   TestOne.class,
})
public class TestAll extends TestCase {
}
