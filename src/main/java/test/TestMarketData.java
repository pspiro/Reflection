package test;

import java.io.IOException;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.MarketDataType;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.TopMktDataAdapter;
import com.ib.controller.ConnectionAdapter;

import reflection.Config;
import tw.util.S;

public class TestMarketData extends ConnectionAdapter {
	private final ApiController m_controller = new ApiController( this, null, null);

	public static void main(String[] args) throws IOException, Exception {
		new TestMarketData();
	}

	TestMarketData() throws Exception {
		Config config = Config.readFrom("Dt-config");
		m_controller.connect(config.twsOrderHost(), config.twsOrderPort(), 9384, null);
	}

	@Override public void onConnected() {
		Contract c = new Contract();
		//c.symbol("AAPL");
		c.currency("USD");
		c.exchange("IBEOS");
		c.secType("STK");
		c.conid(265598);

		m_controller.reqContractDetails(c, list -> {
			try {
				m_controller.reqMktDataType(MarketDataType.DELAYED);
				ContractDetails item = list.get(0);
				c.conid(item.conid() );
				m_controller.reqTopMktData(c, null, false, false, new TopMktDataAdapter() {
					@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
						S.out( "%s %s", tickType, price);
					}
				});
			}
			catch( Exception e) {
				S.out( e.getMessage() );
			}
		});
	}
}
