package testcase;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import junit.framework.TestCase;


@RunWith(Suite.class)
@Suite.SuiteClasses({
//	   TestBackendMsgs.class,
//	   TestConfig.class,
//	   TestConfigSheet.class,
//	   TestErrors.class,
//	   TestFaqs.class,
//	   TestFireblocks.class,
	   TestFbOrders.class,
//	   TestGetCryptoTrans.class,
//	   TestGetPositions.class,
//	   TestGtable.class,
//	   TestHttpClient.class,
	   TestKyc.class,
//	   TestLog.class,
//	   TestEmail.class,
	   TestMint.class,
	   TestMktDataServer.class,
//	   TestMyRedis.class,
	   TestOrder.class,
	   TestOrderNoAutoFill.class,
	   TestOutsideHours.class,  // all pass except TestOutsideHours
	   TestPanic.class,
//	   TestPositionTracker.class,
	   TestPrices.class,
	   TestProfile.class,
	   TestRedeem.class,
//	   TestSignup.class,
//	   TestSiwe.class,
	   TestSplitDates.class,
//	   TestSmartRusd.class,
//	   TestSmartToken.class,
//	   TestSwap.class,
//	   TestStrings.class,
	   TestTwoOrdersDifUser.class,
	   TestTwoOrdersSameUser.class,	   
	   TestUnwindOrder.class,
//	   TestWallet.class,
})
public class TestAll extends TestCase {
}
