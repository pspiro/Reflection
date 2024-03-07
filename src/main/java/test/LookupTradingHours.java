package test;

import java.io.IOException;

import com.ib.client.Contract;
import com.ib.controller.ApiController;
import com.ib.controller.ConnectionAdapter;

import common.Util;
import reflection.Config;
import tw.util.S;

public class LookupTradingHours extends ConnectionAdapter {
	private final ApiController m_controller = new ApiController( this, null, null);

	public static void main(String[] args) throws IOException, Exception {
		new LookupTradingHours();
		S.sleep( 5000);
		
	}

	LookupTradingHours() throws Exception {
		Config config = Config.readFrom("Dt-config");
		m_controller.connect(config.twsOrderHost(), config.twsOrderPort(), 9284, null);
	}

	public void onRecNextValidId(int id) {
		Util.execute( () -> query() );
	}
	
	void query() {
		query( "SMART");
		query( "OVERNIGHT");
	}
	
	private void query( String exch) { 
		Contract c = new Contract();
		c.conid(8314);
		c.exchange(exch);
		
		m_controller.reqContractDetails(c, list -> {
			S.out( "Trading hours for %s: %s", exch, list.get(0).tradingHours() );
		});
	}
}
