package test;

import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.client.Types.Action;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IOrderHandler;
import com.ib.controller.ApiController.TopMktDataAdapter;
import com.ib.controller.ConnectionAdapter;

import common.Util;
import tw.util.S;

public class TestApi extends ConnectionAdapter {
	ApiController m_controller = new ApiController( this, null, null);
	
	public static void main(String[] args) {
		new TestApi().run();
	}
	
	void run() {
		//m_controller.connect("localhost", 9395, 838, null);
		m_controller.connect("34.125.231.254", 7498, 838, null);
	}

	@Override
	public void onConnected() {
	}
	
	@Override
	public void onRecNextValidId(int id) {
		//placeOrder();
		reqMktData();
	}
	
	void reqMktData() {
		Contract c = new Contract();
		c.conid(8314);
		c.exchange("SMART");
		
		m_controller.reqTopMktData(c, null, false, false, new TopMktDataAdapter() {
			public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
				S.out( "%s %s", tickType, price);
			}
		});

		Util.executeIn( 10000, () -> System.exit(0) );
	}
	
	void placeOrder() {
		Contract c = new Contract();
		c.conid(8314);
		c.exchange("SMART");
		
		Order o = new Order();
		o.action(Action.Buy);
		o.roundedQty(1);
		o.lmtPrice(186);
		o.transmit(true);
		o.outsideRth(true);
		o.orderRef("ZZZZZZZZ");
		
		try {
			m_controller.placeOrder(c, o, new IOrderHandler() {
				
				@Override
				public void orderState(OrderState orderState) {
					S.out("state: " + orderState);
				}
				
				@Override
				public void onRecOrderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice, int permId,
						int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
					S.out("status: " + status);
				}
				
				@Override
				public void onRecOrderError(int errorCode, String errorMsg) {
					S.out("%s - %s", errorCode, errorMsg);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		Util.executeIn( 6000, () -> System.exit(0) );
	}

}
