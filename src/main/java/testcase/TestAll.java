package testcase;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import junit.framework.TestCase;
import testcase.web3.TestRusd;
import testcase.web3.TestSendEth;
import testcase.web3.TestStockToken;

// NOTE: static variables are shared across tests

@RunWith(Suite.class)
@Suite.SuiteClasses({
	// tests that require prices
//	TestFbOrders.class,
//	TestKyc.class,
//	TestOrder.class,
	TestOrderNoAutoFill.class,
	TestPartialFill.class,
	TestPaxos.class,
	TestPrices.class,

//	TestBackendMsgs.class,
//	TestCheckIdentity.class,
//	TestConfig.class,
//	TestErrors.class,
//	TestFaqs.class,
//	TestFireblocks.class,
//	TestGetCryptoTrans.class,
//	TestGetPositions.class,
	TestGtable.class,
//	TestHookServer.class,
//	TestHttpClient.class,
//	TestMktDataServer.class,
//	TestOnramp.class,
	TestOutsideHours.class,
	TestPanic.class,
//	TestPositionTracker.class,
//	TestProfile.class,  // fails due to us not requiring the pan and aadhaar anymore
//	TestRedeem.class,
//	TestRusd.class,
//	TestSendEth.class,
	TestSignup.class,
//	TestSiwe.class,
	TestSplitDates.class,
	TestSql.class,
	TestStockToken.class,
	TestStrings.class,
//	TestSwap.class,
	TestUnwindOrder.class,
	TestUserTokMgr.class,
	TestWallet.class
})
public class TestAll extends TestCase {
}
