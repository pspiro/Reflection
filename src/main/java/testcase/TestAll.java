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
	   TestOrderNoAutoFill.class,
	   TestOutsideHours.class,
	   TestPanic.class,
	   TestPrices.class,
	   TestProfile.class,
	   TestRedeem.class,
	   TestSiwe.class,
	   TestSplitDates.class,
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
