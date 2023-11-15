package test;

import java.io.IOException;

import com.ib.client.Contract;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.TopMktDataAdapter;
import com.ib.controller.ConnectionAdapter;

import redis.MktDataServer;
import reflection.Config;
import reflection.Stocks;
import tw.util.S;

/** This program requests market data for all the tickers on SMART and OVERNIGHT
 *  and prints it out
 */
public class TestMarketData extends ConnectionAdapter {
	private final ApiController m_controller = new ApiController( this, null, null);

	public static void main(String[] args) throws IOException, Exception {
		new TestMarketData();
	}

	TestMarketData() throws Exception {
		//Config config = Config.readFrom("Dt-config");
		m_controller.connect("localhost", 7393, 7373, "");
	}

	@Override public void onConnected() {
		try {
			_onConnected();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void _onConnected() throws Exception {
		Stocks stocks = Config.ask().readStocks();
		
		for( reflection.Stock stock : stocks) {
			Contract c = new Contract();
			c.symbol(stock.symbol() );
			c.currency("USD");
			c.exchange(MktDataServer.Smart);
			c.secType("STK");
			c.conid(stock.conid());
	
			m_controller.reqTopMktData(c, null, false, false, new TopMktDataAdapter() {
				@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
					S.out( "%s %s %s Smart", c.symbol(), tickType, price);
				}
			});

			c.exchange(MktDataServer.Overnight);
	
			m_controller.reqTopMktData(c, null, false, false, new TopMktDataAdapter() {
				@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
					S.out( "%s %s %s Overnight", c.symbol(), tickType, price);
				}
			});
		}
	}
}
