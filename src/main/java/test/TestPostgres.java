package test;

import common.Util;
import reflection.Config;
import tw.util.S;



/** Just test that you can connect to the database. */
public class TestPostgres {
	static Config m_config;

	static {
		try {
//			m_config = Config.ask();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		S.out( Util.fmtTime(1811563292425L) );
		

	}

}
