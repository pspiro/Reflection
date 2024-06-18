package reflection;

import com.ib.client.Contract;
import com.ib.client.DualOrder;
import com.ib.client.OrderType;
import com.ib.client.SingleOrder;
import com.ib.client.Types.Action;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;
import com.ib.controller.ConnectionAdapter;

import common.Util;
import reflection.TradingHours.Session;
import tw.util.S;

public class TestDualOrder extends ConnectionAdapter {
	ApiController conn = new ApiController(this);
	private TradingHours m_tradingHours;

	public static void main(String[] args) throws Exception {
		new TestDualOrder().run();
	}

	void run() throws Exception {
		S.out( "reading config");
		Config config = Config.read();

		S.out( "connecting");
		conn.connect( config.twsOrderHost(), config.twsOrderPort(), 34, "");
	}
	
	@Override public void onConnected() {
	}

	@Override public void onRecNextValidId(int id) {
		S.out( "received next valid id");
		Util.executeAndWrap( () -> {
//			testSingleOrder();
			
			testDualOrder();
		});
	}

	private void testDualOrder() {
		S.out( "testing dual order");

		Prices prices = new Prices();
		
		// place order to both exchanges
		DualOrder ord = new DualOrder( 
				conn, 
				(filled, order) -> S.out( "dual order completed filled=%s", filled),
				"TEST",
				prices);
		
		ord.action(Action.Sell);
		ord.orderType( OrderType.STP);
		ord.quantity( 1);
		ord.stopPrice( 169);
		ord.outsideRth(true);

		try {
			ord.placeOrder( 8314);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Util.executeIn(2000, () -> {
			S.out( "updating no trigger");
			
			prices.update( Util.toJson( 
					"ask", 171,
					"bid", 170) 
					);
			S.sleep( 1000);
			
			S.out( "updating should trigger");
			prices.update( Util.toJson( 
					"ask", 170,
					"bid", 169) 
					);
			
			S.sleep( 5000);
		});

	}

	public void testSingleOrder() {
		S.out( "testing single order");
		
		Prices prices = new Prices();

		SingleOrder ord = new SingleOrder( SingleOrder.Type.Night, prices, "test", type -> {
			S.out( "order status changed");
		});
		ord.o().action(Action.Sell);
		ord.o().orderType( OrderType.STP);
		ord.o().roundedQty( 1);
		ord.o().stopPrice( 169);
		ord.o().tif( TimeInForce.DAY);
		ord.o().outsideRth(true);
		
		Contract contract = new Contract();
		contract.conid( 8314);
		contract.exchange( Session.Overnight.toString() );

		try {
			ord.placeOrder( conn, contract);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Util.executeIn(2000, () -> {
			S.out( "updating no trigger");
			
			prices.update( Util.toJson( 
					"ask", 171,
					"bid", 170) 
					);
			S.sleep( 1000);
			
			S.out( "updating should trigger");
			prices.update( Util.toJson( 
					"ask", 170,
					"bid", 169) 
					);
			
			S.sleep( 5000);
		});
	}
}
