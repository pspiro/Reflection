package reflection;

import com.ib.client.DualOrder;
import com.ib.client.Types.Action;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;
import com.ib.controller.ConnectionAdapter;

import common.Util;
import tw.util.S;

public class TestDualOrder extends ConnectionAdapter {
//	static public class App {
//		TradingHours m_tradingHours;
//		private Stocks m_stocks;
//
//		public void config(Config config, TradingHours tradingHours) throws Exception {
//			m_stocks = config.readStocks();
//			m_tradingHours = tradingHours;
//		}
//
//		Stock getStock( int conid) throws RefException {
//			return m_stocks.stockMap().get( conid);
//		}
//
//
//	}

	ApiController controller = new ApiController(this);
	private TradingHours m_tradingHours;

	/* you have status and you have events; status you can get from the order:
	 * filled size, avg price
	 */

	public static void main(String[] args) throws Exception {
		new TestDualOrder().run();
	}

	void run() throws Exception {
		S.out( "reading config");
		Config config = Config.read();
		
//		m_tradingHours = new TradingHours(controller, config);
//		app.config( config, m_tradingHours);

		S.out( "connecting");
		controller.connect( config.twsOrderHost(), config.twsOrderPort(), 34, "");
	}
	
	@Override public void onConnected() {
	}

	@Override public void onRecNextValidId(int id) {
		S.out( "received next valid id");
		Util.executeAndWrap( () -> {
			
				// place order to both exchanges
				DualOrder ord = new DualOrder( filled -> S.out( "dual order completed filled=%s", filled), controller);
				ord.action(Action.Buy);
				ord.quantity( 1);
				ord.lmtPrice( 160);
				ord.outsideRth(true);
				ord.ocaGroup( Util.uid(5) );
				ord.placeOrder( controller, 8314);
		});
	}
}
