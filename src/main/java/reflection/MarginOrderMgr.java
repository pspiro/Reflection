package reflection;

import java.util.ArrayList;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.ib.client.Contract;
import com.ib.client.Decimal;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.ILiveOrderHandler;

import common.Util;
import tw.util.S;

public class MarginOrderMgr {
	private ArrayList<MarginOrder> m_orders = new ArrayList<>();

	public void add(MarginOrder order) {
		m_orders.add( order);
	}

	void write() throws Exception {
		toJson().writeToFile( "marginorders");
	}
	
	private JsonObject toJson() {
		JsonArray ar = new JsonArray();
		m_orders.forEach( order -> ar.add( order.toJson() ) );
		
		return Util.toJson( "orders", ar);
	}

	/** run is called after all live orders have been restored */
	void restore(App main, ApiController conn, Runnable run) throws Exception {
		// read file and restore array of MarginOrder objects
		JsonObject.readFromFile("marginorders").getArray( "orders").forEach( order-> {
			try {
				m_orders.add( new MarginOrder( main, order) );
			}
			catch( Exception e) {
				S.out( "Caught exception when reading margin order from file");
				e.printStackTrace();
			}
		});
		
		conn.reqLiveOrders( new ILiveOrderHandler() {
			@Override public void orderStatus(int orderId, OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice,
					int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
				S.out( "RECEIVED open order status %s %s %s", status, permId, orderId);
			}
			
			@Override public void openOrder(Contract contract, Order order, OrderState orderState) {
				S.out( "RECEIVED open order %s %s", order.permId(), order.orderId() );
				setOrder( conn, order);
			}
			
			@Override public void openOrderEnd() {
				run.run();
			}
			
			@Override public void handle(int orderId, int errorCode, String errorMsg) {
				
			}
		});
	}

	private void setOrder(ApiController conn, Order ibOrder) {
		for (MarginOrder mo : m_orders) {
			mo.setOrder( conn, ibOrder);
		}
		
	}

	public void display() {
		m_orders.forEach( order -> order.display() );		
	}

	public void cancel(ApiController conn) {
		m_orders.forEach( order -> order.cancel(conn) );
	}

	public void tick() {
		//m_orders.forEach( order -> order.tick() );
	}


}
