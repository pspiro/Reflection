package reflection;

import com.ib.client.DualOrder;
import com.ib.client.Types.Action;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;
import com.ib.controller.ConnectionAdapter;

import common.Util;
import tw.util.S;

public class TestDualOrder extends ConnectionAdapter {
	ApiController controller = new ApiController(this);
	App app = new App();
	private TradingHours m_tradingHours;

	/* you have status and you have events; status you can get from the order:
	 * filled size, avg price
	 */

	public static void main(String[] args) throws Exception {
		new TestDualOrder().run();
	}


	void run() throws Exception {
		S.out( "reading config");
		Config config = Config.ask( "Dt");
		
		m_tradingHours = new TradingHours(controller, config);
		app.config( config, m_tradingHours);

		S.out( "connecting");
		controller.connect( config.twsOrderHost(), config.twsOrderPort(), 34, "");
	}
	
	@Override public void onConnected() {
		m_tradingHours.startQuery();
	}

	@Override public void onRecNextValidId(int id) {
		Util.executeAndWrap( () -> {
			{
				// place order to both exchanges
				DualOrder o = new DualOrder();
				o.action(Action.Buy);
				o.quantity( 1);
				o.lmtPrice( 160);
				o.outsideRth(true);
				o.ocaGroup( Util.uid(5) );
				o.tif( TimeInForce.GTC);
				o.placeOrder( controller, 8314);
	
				MarginOrder t = new MarginOrder( o);
	
				MarginOrderMgr mgr = new MarginOrderMgr();
				mgr.add( t);
	
				mgr.write();
			}

			S.sleep( 5000);
			S.out( "-----Restoring orders");
			MarginOrderMgr mgr2 = new MarginOrderMgr();
			mgr2.restore(app, controller, () -> {
				S.out( "-----restored Orders");
				mgr2.display();
	
				S.out( "sleep 5");
				S.sleep( 10000);
				S.out( "  done");
				//mgr2.cancel(controller);
			});
		});
	}
}
