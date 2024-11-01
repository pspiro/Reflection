package testcase;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import junit.framework.TestCase;
import test.TestAlchemy;
import testcase.web3.TestBusd;
import testcase.web3.TestRusd;
import testcase.web3.TestSendEth;

// NOTE: static variables are shared across tests

@RunWith(Suite.class)
@Suite.SuiteClasses({
	// blockchain stuff
	
	
	TestAlchemy.class,
	TestApprove.class,
	TestBackendMsgs.class,
	TestCheckIdentity.class,
	TestConfig.class,
	TestErrors.class,
	TestFaqs.class,
	TestFaucet.class,
	TestFbOrders.class,
	TestFundWallet.class,
	TestGetCryptoTrans.class,
	TestGetPositions.class,
	TestGtable.class,
	TestHookServer.class,
	TestHttpClient.class,
	TestJson.class,
	TestKyc.class,
	TestMktDataServer.class,
	TestNode.class,
	TestOnramp.class,
	TestOrder.class,
	TestOrderNoAutoFill.class,
	TestOutsideHours.class,
	TestPanic.class,
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
	TestSwap.class,
	TestUnwindOrder.class,
	TestUserTokMgr.class,
	TestWallet.class,
	
	// these are in a separate folder; don't delete them
	TestBusd.class,
	TestRusd.class,
	TestSendEth.class,
})
public class TestAll extends TestCase {
}
