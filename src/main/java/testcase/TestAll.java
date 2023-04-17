package testcase;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import junit.framework.TestCase;


@RunWith(Suite.class)
@Suite.SuiteClasses({
	   TestBackendOrder.class,
	   TestBackendMsgs.class,
	   TestConfig.class,
	   TestConfigSheet.class,
	   TestErrors.class,
	   TestFireblocks.class,
	   TestGetPositions.class,
	   TestGtable.class,
	   TestMyRedis.class,
	   TestOrder.class,
	   TestOutsideHours.class,
	   TestPrices.class,
	   TestRedeem.class,
	   TestSiwe.class,
	   TestUnwindOrder.class,
	   TestWhatIf.class,
})
public class TestAll extends TestCase {
}
