package com.ib.client;

import java.util.HashMap;
import java.util.function.Consumer;

import org.json.simple.JsonObject;

import com.ib.client.Types.Action;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IOrderHandler;
import com.ib.controller.ApiController.LiveOrder;

import reflection.Prices;
import tw.util.S;

/** One order of a pair on DualOrder. This might not be needed
 *  We could maybe just add two IB Order's to DualOrder */
public class SingleOrder implements IOrderHandler {
	public enum Type { Day, Night };  // you could combine this with TradingHours.Session

	public interface SingleParent {
		void onStatusUpdated(SingleOrder order, int permId, Action action, int filled, double avgFillPrice);  
	}

	private final Type m_session;
	private final SingleParent m_parent;
	private final Prices m_prices;  // could be null
	private String m_name;
	
	// order and order status; comes from LiveOrder
	private Order m_order = new Order();  // could be replaced in placeOrder()
	private OrderStatus m_status = OrderStatus.Unknown;
	private int m_filled;
	private double m_avgPrice;

	private Consumer<Prices> m_listener; // access to this must be synchronized
	private ApiController m_conn;
	private String m_key;
	
	/** Called for new order, not submitted yet. Id will be set
	 *  when order is submitted.
	 *  @parent is last for ease of call  
	 * @param json 
	 * @param night 
	 * @throws Exception */
	public SingleOrder(
			ApiController conn, 
			Prices prices, 
			Type session, 
			String name, 
			String key,
			SingleParent parent) {
		
		m_conn = conn;
		m_session = session;
		m_parent = parent;
		m_prices = prices;
		m_name = (name + "/" + session).toUpperCase();
		m_key = key;
	}
	
	private void out( String format, Object... params) {
		S.out( m_name + " " + format, params);
	}

	JsonObject toJson() {
		return common.Util.toJson(
				"filled", m_filled,
				"avgPrice", m_avgPrice);
	}

	public synchronized void placeOrder( Contract contract, HashMap<String, LiveOrder> liveOrders) throws Exception {
		common.Util.require( m_order.status() == OrderStatus.Unknown, "order should be Inactive");

		// we must simulate stop-limit orders on Overnight exchange (stop orders won't work as market orders are not supported)
		if (m_session == Type.Night && m_order.orderType() == OrderType.STP_LMT) {
			out( "Simulating stop order");

			// called when the prices are updated
			m_listener = prices -> onPriceChanged( contract);

			common.Util.require( m_prices != null, "must pass prices for sim stop order");
			m_prices.addListener( m_listener);
		}
		else {
			LiveOrder liveOrder = liveOrders != null ? liveOrders.get( m_key) : null;

			// if there is aready a live IB order, sync to it; otherwise
			// place a new order
			if (liveOrder != null) {
				m_order = liveOrder.order();
				m_status = liveOrder.status();
				m_filled = liveOrder.filled();
				m_avgPrice = liveOrder.avgPrice();
				
				m_conn.listenTo( m_order, this);

				S.out( "Restored SingleOrder id=%s  key=%s  permId=%s", 
						m_order.orderId(), m_key, m_order.permId() );
			}
			else {
				m_order.orderRef( m_key);
				m_conn.placeOrder( contract, m_order, this);
				out( "Placed new SingleOrder id=%s  key=%s", m_order.orderId(), m_key);
			}
		}
	}
			
	/** triggers when the BID is <= trigger price; somewhat dangerous */
	private void onPriceChanged( Contract contract) {
		if (m_prices.bid() <= m_order.auxPrice() && m_listener != null) {
			out( "Simulated stop order has triggered  %s", m_prices);
			stopListening();
			
			m_order.orderType( OrderType.LMT);
			m_order.stopPrice( Double.MAX_VALUE);

			// lmt price should have been set already, but we can set it here 5% below the ask
			if (m_order.lmtPrice() == Double.MAX_VALUE) {
				m_order.lmtPrice( m_prices.ask() * .95);  // we could set this here or we could use the lmt price already set on the order; this way works for both stop and stop_lmt
			}

			try {
				m_conn.placeOrder( contract, m_order, this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override public void orderState(OrderState orderState) {
		// ignore this, we don't care
	}
	
	@Override public void onRecOrderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice, int permId,
			int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
		
		m_filled = filled.toInt();
		m_avgPrice = avgFillPrice;
		m_status = status;

		out( "SingleOrder name=%s  id=%s  status=%s  session=%s  filled=%s  remaining=%s  avgPrice=%s", 
				m_name, m_order.orderId(), status, m_session, filled, remaining, avgFillPrice);
		
		m_parent.onStatusUpdated( this, permId, m_order.action(), filled.toInt(), avgFillPrice);
	}
	
	@Override public void onRecOrderError(int errorCode, String errorMsg) {
	}

	/** Set the IB order and listen for updates IF the permId matches */
	public void setOrder(ApiController conn, Order order) {
//		if (m_orderId == order.orderId() && m_order == null) {
//			out( "Restoring order with orderId %s", m_orderId);
//			m_order = order;
//			conn.listenTo( m_order, this);
//		}
	}

	/** For debugging only */
	@Override public String toString() {
		return m_order != null
				? String.format( "orderId=%s  side=%s  qty=%s  price=%s",
					m_order.orderId(), m_order.action(), m_order.roundedQty(), m_order.lmtPrice() )
				: String.format( "orderId=%s  (order is null)", m_order.orderId() );
	}

	public void cancel() {
		if (m_order != null && m_status.canCancel() ) {
			m_conn.cancelOrder( m_order.orderId(), "", status -> 
				out( "Canceled order  orderId=%s  status=%s", m_order.orderId(), status) );
		}
		
		stopListening();
	}

	private void stopListening() {
		if (m_prices != null) {
			m_prices.removeListener( m_listener);
		}
		m_listener = null;
	}

	/** Return dollar amount spent. Spending is negative.
	 *  Ideally we would subtract out the IB commissions as well */
	public double getBalance() {
		return -m_filled * m_avgPrice;
	}

	public int filled() {
		return m_filled;
	}

	public boolean isComplete() {
		return m_status.isComplete();
	}

	public Order o() {
		return m_order;
	}

}
