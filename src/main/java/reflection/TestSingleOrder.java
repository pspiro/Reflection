package reflection;

import com.ib.client.Contract;
import com.ib.client.OrderType;
import com.ib.client.SingleOrder;
import com.ib.client.Types.Action;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;
import com.ib.controller.ConnectionAdapter;

import common.Util;
import tw.util.S;

public class TestSingleOrder {
	static Config config;
	private ApiController conn;

	public static void main(String[] args) throws Exception {
		config = Config.read();

		new TestSingleOrder().testSingleBuyOrder();
	}

	Prices prices = new Prices();
	
	public void testSingleBuyOrder() {
		conn = new ApiController( new ConnectionAdapter() {
			@Override public void onRecNextValidId(int id) {
				Util.execute( () -> testSingleBuyPlace( this) );
			}
		});
		
		S.out( "connecting");
		conn.connect( config.twsOrderHost(), config.twsOrderPort(), 35, "");
	}
	
	int i = Util.rnd.nextInt( 5000);
	int m_permId;
	
	private void testSingleBuyPlace(ConnectionAdapter connectionAdapter) {
		S.out( "connected; testing single buy order");
		
		SingleOrder ord = new SingleOrder( 
				conn,
				prices, 
				SingleOrder.Type.Night, 
				"test",
				"test " + i,
				(order, permId, action, filled, avgFillPrice) -> {
					S.out( "child ONE updated  permId=%s  action=%s  filled=%s  price=%s",
							permId, action, filled, avgFillPrice);
					m_permId = permId;
				});
		
		ord.o().action(Action.Buy);
		ord.o().orderType( OrderType.LMT);
		ord.o().roundedQty( 1);
		ord.o().lmtPrice( 169);
		ord.o().tif( TimeInForce.GTC);
		ord.o().outsideRth(true);
		
		try {
			S.out( "***Place order");
			ord.placeOrder( contract(), null);
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
				Util.execute( () -> testSingleBuyRestore() );
			}
		});
		S.out( "***reconnect");
		conn.connect( config.twsOrderHost(), config.twsOrderPort(), 35, "");
	}

	// exec in sep. thread
	void testSingleBuyRestore() {
		try {
			S.out( "***reconnected; testing restore single from live");
			
			// get live order maps
			var orderIdMap = conn.reqLiveOrderMap();
			var orderRefMap = MarginStore.getOrderRefMap(orderIdMap);
			
			SingleOrder ord = new SingleOrder( 
					conn, 
					prices, 
					SingleOrder.Type.Night, 
					"test",
					"test " + i,
					(order, permId, action, filled, avgFillPrice) -> {
						S.out( "child TWO updated  permId=%s  action=%s  filled=%s  price=%s",
								permId, action, filled, avgFillPrice);
					} );
			
			ord.placeOrder( contract(), orderRefMap);
			S.sleep( 1000);
			S.out( "***canceling order");
			ord.cancel();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Contract contract() {
		Contract contract = new Contract();
		contract.conid( 8314);
		contract.exchange( "SMART");
		return contract;
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
				(order, permId, action, filled, avgFillPrice) -> {
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
			ord.placeOrder( contract, null);
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
