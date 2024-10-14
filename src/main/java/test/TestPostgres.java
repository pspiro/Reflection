package test;

import reflection.Config;
import tw.util.S;
import web3.NodeInstance;



/** Just test that you can connect to the database. */
public class TestPostgres {
	static Config m_config;

	static {
		try {
			m_config = Config.ask();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception {
		S.out( m_config.busd().getApprovedAmt( NodeInstance.prod, m_config.rusdAddr() ) );

	}

}
