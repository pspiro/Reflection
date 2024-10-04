package fireblocks;

import common.Util;
import reflection.Config;
import tw.util.S;

public class MintRusd {
	static Config m_config;

	static {
		try {
			m_config = Config.ask();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 5; i++) {
			String str = Util.createFakeAddress();
			double amt = Util.round( Util.rnd.nextDouble( 300, 3000) );
			S.out( str + " " + amt);
			m_config.rusd().mintRusd( str, amt, m_config.readStocks().getAnyStockToken() );
		}
	}
}
