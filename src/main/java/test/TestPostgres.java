package test;

import reflection.Config;
import tw.util.S;

/** Just test that you can connect to the database. */
public class TestPostgres {
	static Config m_config;

	static {
		try {
			m_config = Config.read();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		String wal = "0xcb6c2EDBb986ef14B66E094787245350b69EA5Ec";
		S.out( "**approved=%s", m_config.getApprovedAmt() );
		S.out( "**rusdBal=%s", m_config.rusd().getPosition(wal));
		S.out( "**busdBal=%s", m_config.busd().getPosition(m_config.refWalletAddr()));
		
		S.out( "sending redemption request to succeed");
		m_config.rusd().sellRusd(wal, m_config.busd(), 3)
			.displayHash();
//		redeem();
		//assert200();
	}
}
