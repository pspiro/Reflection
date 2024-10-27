package testcase;
import common.Util;
import tw.util.S;

public class TestApprove extends MyTestCase {
	
	public void testApprove() throws Exception {
		int n = Util.rnd.nextInt( 10000) + 10;

		// let Owner approve RUSD to spend BUSD
		S.out( "testing approve");
		
		m_config.busd().approve( m_config.ownerKey(), m_config.rusdAddr(), n)
				.displayHash();

		// wait for it to be reflected in wallet
		waitFor( 30, () -> {
			double appr = m_config.busd().getAllowance( m_config.ownerAddr(), m_config.rusdAddr() );
			S.out( "approved %s", appr);
			return appr == n;  
		});
	}
}
