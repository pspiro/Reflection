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
	TestBackendMsgs.class,
	TestCheckIdentity.class,
	TestConfig.class,
	TestErrors.class,
	TestFaqs.class,
	TestFbOrders.class,
	TestFireblocks.class,
	TestGetCryptoTrans.class,
	TestGetPositions.class,
	TestGtable.class,
	TestHookServer.class,
	TestHttpClient.class,
	TestKyc.class,
	TestLog.class,
	TestMktDataServer.class,
	TestOnramp.class,
	TestOrder.class,
	TestOrderNoAutoFill.class,
	TestOutsideHours.class,
	TestPanic.class,
	TestPartialFill.class,
	TestPaxos.class,
	TestPositionTracker.class,
	TestPrices.class,
	TestProfile.class,
	TestRedeem.class,
	TestRusd.class,
	TestSendEth.class,
	TestSignup.class,
	TestSiwe.class,
	TestSplitDates.class,
	TestSql.class,
	TestStockToken.class,
	TestStrings.class,
	TestSwap.class,
	TestUnwindOrder.class,
	TestUserTokMgr.class,
	TestWallet.class
})
public class TestAll extends TestCase {
}
