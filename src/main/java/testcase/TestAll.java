package testcase;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import junit.framework.TestCase;

// NOTE: static variables are shared across tests

@RunWith(Suite.class)
@Suite.SuiteClasses({
//	TestApprove.class,
//	TestBackendMsgs.class,
//	TestCheckIdentity.class,
//	TestConfig.class,
//	TestErrors.class,
//	TestFaqs.class,
//	TestFbOrders.class,
//	TestFireblocks.class,
//	TestGetCryptoTrans.class,
//	TestGetPositions.class,
//	TestGtable.class,
//	TestHookServer.class,
//	TestHttpClient.class,
//	TestKyc.class,
//	TestMktDataServer.class,
//	TestNodeServer.class,
//	TestOnramp.class,
//	TestOrder.class,
	TestOrderNoAutoFill.class,
	TestOutsideHours.class,
	TestPanic.class,			// fails, ok
	TestPartialFill.class,		// fails, ok
//	TestPositionTracker.class,
//	TestPrices.class,
//	TestProfile.class,			// fails because we don't require correct email code
	TestRedeem.class,
//	TestRefblocks.class,  // could add more tests here
//	TestReqPositionsMap.class,
//	TestReward.class,
//	TestSignup.class,
//	TestSiwe.class,
//	TestSplitDates.class,		// ignoring for now
//	TestSql.class,
	TestStaleMktData.class,
	TestStream.class,
	TestStrings.class,
	TestSwap.class,
	TestUnwindOrder.class,
	TestUserTokMgr.class,
	TestWallet.class,
})
public class TestAll extends TestCase {
}
