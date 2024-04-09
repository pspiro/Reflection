package testcase;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import junit.framework.TestCase;

// NOTE: static variables are shared across tests

@RunWith(Suite.class)
@Suite.SuiteClasses({
//	   TestAllow.class,
//	   TestBackendMsgs.class,
//	   TestConfig.class,
//	   TestConfigSheet.class,
//	   TestEmail.class,
//	   TestErrors.class,
//	   TestFaqs.class,
//	   TestFireblocks.class,
	   TestFbOrders.class, // failed
//	   TestGetCryptoTrans.class,
//	   TestGetPositions.class,
//	   TestGtable.class,
//	   TestHttpClient.class,
//	   TestKyc.class,
//	   TestLog.class,
//	   TestMktDataServer.class,
//	   TestOrder.class,
	   TestOrderNoAutoFill.class,
	   TestOutsideHours.class,  // all pass except TestOutsideHours
	   TestPanic.class,
//	   TestPartialFill.class,  // Important, must pass this!
//	   TestPositionTracker.class,
//	   TestPrices.class,
//	   TestProfile.class,
//	   TestRedeem.class,
//	   TestSignup.class,
	   TestSiwe.class,
	   TestSplitDates.class,
//	   TestSmartRusd.class,
//	   TestSmartToken.class,
//	   TestSql.class,
	   TestUserTokMgr.class,
	   TestSwap.class,
//	   TestStrings.class,
	   TestUnwindOrder.class,  // not done
//	   TestWallet.class,
	   
	   
//	   TestHookServer.class, // WARNING: running HookServer will kill the hooks in live system 
})
public class TestAll extends TestCase {
}
