package reflection;

import com.ib.client.Contract;
import com.ib.client.DualOrder;
import com.ib.client.DualOrder.DualParent;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.SingleOrder;
import com.ib.client.Types.Action;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;
import com.ib.controller.ConnectionAdapter;

import common.Util;
import tw.util.S;

public class TestDualOrder {
	static Config config;
	private ApiController conn;

	public static void main(String[] args) throws Exception {
		config = Config.read();

		new TestDualOrder().testDualOrder();
	}

	Prices prices = new Prices();
	
	public void testDualOrder() {
		conn = new ApiController( new ConnectionAdapter() {
			@Override public void onRecNextValidId(int id) {
				Util.execute( () -> testDualBuyPlace( this) );
			}
		});
		
		S.out( "connecting");
		conn.connect( config.twsOrderHost(), config.twsOrderPort(), 35, "");
	}
	
	int i = Util.rnd.nextInt( 5000);
	int m_permId;
	
	private void testDualBuyPlace(ConnectionAdapter connectionAdapter) {
		S.out( "connected; testing single buy order");
		
		DualOrder ord = new DualOrder( 
				conn,
				prices, 
				"test",
				"test " + i,
				8314, 
				new DualParent() {
					@Override public void onStatusUpdated(DualOrder order, OrderStatus status, int permId, Action action,
							int filled, double price) {
						S.out( "child ONE updated  permId=%s  action=%s  filled=%s  price=%s  status=%s",
								permId, action, filled, price, status);
						m_permId = permId;
					}
					@Override public void out(String string, Object... params) {
						S.out( string, params);
					}
				});
		
		ord.action(Action.Buy);
		ord.orderType( OrderType.LMT);
		ord.quantity( 1);
		ord.lmtPrice( 169);
		ord.tif( TimeInForce.GTC);
		ord.outsideRth(true);
		
		try {
			S.out( "***Place order");
			ord.placeOrder( 8314);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		S.sleep( 4000);
		S.out( "***disconnect");
		conn.disconnect();
		conn = null;
		S.sleep( 2000);
		
		conn = new ApiController( new ConnectionAdapter() {
			@Override public void onRecNextValidId(int id) {
				Util.execute( () -> testBuyRestore() );
			}
		});
		S.out( "***reconnect");
		conn.connect( config.twsOrderHost(), config.twsOrderPort(), 35, "");
	}

	// exec in sep. thread
	void testBuyRestore() {
		try {
			S.out( "***reconnected; testing restore single from live");
			
			// get live order maps
			var orderIdMap = conn._reqLiveOrderMap();
			var orderRefMap = MarginStore.getOrderRefMap(orderIdMap);
			
			DualOrder ord = new DualOrder( 
					conn, 
					prices, 
					"test",
					"test " + i,
					8314,
					new DualParent() {
						@Override public void onStatusUpdated(DualOrder order, OrderStatus status, int permId, Action action,
								int filled, double price) {
							S.out( "child ONE updated  permId=%s  action=%s  filled=%s  price=%s  status=%s",
									permId, action, filled, price, status);
							m_permId = permId;
						}
						@Override public void out(String string, Object... params) {
							S.out( string, params);
						}
					});
			
			ord.placeOrder( 8314);
			S.sleep( 1000);
			S.out( "***canceling order");
			ord.cancel();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void testSingleStopOrder() {
		S.out( "testing trigger single stop order");
		
		Prices prices = new Prices();

		SingleOrder ord = new SingleOrder(
				conn,
				prices, 
				SingleOrder.Type.Night, 
				"test",
				"testId",
				8314,
				(order, status, permId, action, filled, avgFillPrice) -> {
					S.out( "child updated  permId=%s  action=%s  filled=%s  price=%s",
							permId, action, filled, avgFillPrice);
				});
		
		ord.o().action(Action.Sell);
		ord.o().orderType( OrderType.STP);
		ord.o().roundedQty( 1);
		ord.o().stopPrice( 169);
		ord.o().tif( TimeInForce.DAY);
		ord.o().outsideRth(true);
		
		Contract contract = new Contract();
		contract.conid( 8314);
		contract.exchange( "OVERNIGHT");

		try {
			ord.placeOrder();
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
