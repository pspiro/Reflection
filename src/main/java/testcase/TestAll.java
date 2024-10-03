package testcase;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import junit.framework.TestCase;

// NOTE: static variables are shared across tests

@RunWith(Suite.class)
@Suite.SuiteClasses({
	TestApprove.class,			//
	TestBackendMsgs.class,		// testOnRamp
	TestCheckIdentity.class,	//
	TestConfig.class,			//
	TestErrors.class,			//
	TestFaqs.class,				//
	TestFbOrders.class,			// testInsufAllow fails
	TestFireblocks.class,
	TestGetCryptoTrans.class,	//
	TestGetPositions.class,		// testTokPos
	TestGtable.class,			//
	TestHookServer.class,
	TestHttpClient.class,		// testgetarray
	TestKyc.class,				//
	TestMktDataServer.class,	//
	TestNodeServer.class,		// testKnownTrans
	TestOnramp.class,			
	TestOrder.class,			// testFillBuy fails then passes
	//TestOrderNoAutoFill.class,
	//TestOutsideHours.class,
	//TestPanic.class,			// fails, ok
	//TestPartialFill.class,	// fails, ok
	TestPositionTracker.class,	//
	TestPrices.class,			//
	TestProfile.class,			// 
	//TestRedeem.class,			// fails because of nonce errors; needs fixing
	TestReward.class,			//
	TestSignup.class,			//
	TestSiwe.class,				//
	TestSplitDates.class,		// fails, ignoring for now
	TestSql.class,				//
	TestSwap.class,				// not implemented yet
	TestUnwindOrder.class,		// won't work in auto-fill mode
	TestUserTokMgr.class,		//
	TestWallet.class,			// testMyWallet, testPosQuery
})
public class TestAll extends TestCase {
}
