package test;

import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.Types.Action;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IOrderHandler;
import com.ib.controller.ConnectionAdapter;

import reflection.Config;
import tw.util.S;

public class TestApi extends ConnectionAdapter {
	ApiController m_conn = new ApiController( this, null, null);
	
	public static void main(String[] args) {
		try {
			new TestApi().run(args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void run(String[] args) throws Exception {
		Config m_config = Config.ask();
		
		m_conn.connect( m_config.twsOrderHost(), m_config.twsOrderPort(), 776, "");
	}

	@Override
	public void onConnected() {
		S.out( "onConnected()");
	}
	
	@Override
	public void onRecNextValidId(int id) {
		try {
			placeOrder(id);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void placeOrder(int id) throws Exception {
		S.out( "rec valid id");
		
		Contract c = new Contract();
		c.conid( 274105);
		c.exchange( "OVERNIGHT");
		
		Order o = new Order();
		o.action(Action.Buy);
		o.roundedQty(1);
		o.lmtPrice(93);
		o.outsideRth(true);
		o.tif( TimeInForce.DAY);
		m_conn.placeOrder( c,  o,  new IOrderHandler() {
			
			@Override
			public void orderState(OrderState orderState) {
				S.out( "order state %s", orderState);
			}
			
			@Override
			public void onRecOrderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice, int permId,
					int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
				S.out( "order status %s", status);
				
			}
			
			@Override
			public void onRecOrderError(int errorCode, String errorMsg) {
				S.out( "order err %s %s", errorCode, errorMsg);
			}
		});
	}

	@Override
	public void onDisconnected() {
		S.out( "disconnected");
	}

	@Override
	public void error(Exception e) {
		e.printStackTrace();
		
	}

	@Override
	public void message(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
		S.out(errorMsg);
		
	}

	@Override
	public void show(String string) {
		S.out(string);
		
	}
}
