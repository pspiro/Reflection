package testcase;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import junit.framework.TestCase;

// NOTE: static variables are shared across tests

@RunWith(Suite.class)
@Suite.SuiteClasses({
	// blockchain stuff
	
//	TestAlchemy.class,
	TestFbOrders.class,
	TestHookServer.class,
	TestNode.class,
	TestSwap.class,
	
	TestApprove.class,
	TestBackendMsgs.class,
	TestCheckIdentity.class,
	TestConfig.class,
	TestErrors.class,
	TestFaqs.class,
	TestFaucet.class,
	TestFundWallet.class,
	TestGetCryptoTrans.class,
	TestGetPositions.class,
	TestGtable.class,
	TestHttpClient.class,
	TestJson.class,
	TestKyc.class,
	TestMktDataServer.class,
	TestOnramp.class,
	TestOrder.class,
	TestOrderNoAutoFill.class,
	TestOutsideHours.class,
	TestPanic.class,
	TestParams.class,
	TestPartialFill.class,
	TestPositionTracker.class,
	TestPrices.class,
	TestProfile.class,
	TestPwServer.class,
	TestRedeem.class,
	TestReqPositionsMap.class,
	TestReward.class,
	TestSignup.class,
	TestSiwe.class,
	TestSplitDates.class,
	TestSql.class,
	TestUnwindOrder.class,
	TestUserTokMgr.class,
	TestWallet.class
})
public class TestAll extends TestCase {
}
