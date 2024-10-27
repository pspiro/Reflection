package test;

import java.io.IOException;

import com.ib.client.Contract;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.TopMktDataAdapter;
import com.ib.controller.ConnectionAdapter;

import redis.MdConfig;
import redis.MdServer;
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
		MdConfig config = new MdConfig();
		config.readFromSpreadsheet("Dt-config");
		
		m_controller.connect(config.twsMdHost(), config.twsMdPort(), 7373, "");
	}

	@Override public void onConnected() {
		try {
			_onConnected();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void _onConnected() throws Exception {
		Contract c = new Contract();
		c.symbol("INDL");
		
		c.currency("USD");
		c.exchange(MdServer.Smart);
		c.secType("STK");

		m_controller.reqTopMktData(c, null, false, false, new TopMktDataAdapter() {
			@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
				S.out( "%s %s %s Smart", c.symbol(), tickType, price);
			}
		});
	}

	void reqAll() throws Exception {
		Config config = Config.ask();
		Stocks stocks = config.chain().stocks();
		
		for( reflection.Stock stock : stocks) {
			Contract c = new Contract();
			c.symbol(stock.symbol() );
			c.currency("USD");
			c.exchange(MdServer.Smart);
			c.secType("STK");
			c.conid(stock.conid());
	
			m_controller.reqTopMktData(c, null, false, false, new TopMktDataAdapter() {
				@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
					S.out( "%s %s %s Smart", c.symbol(), tickType, price);
				}
			});

			c.exchange(MdServer.Overnight);
	
			m_controller.reqTopMktData(c, null, false, false, new TopMktDataAdapter() {
				@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
					S.out( "%s %s %s Overnight", c.symbol(), tickType, price);
				}
			});
		}
	}
}
