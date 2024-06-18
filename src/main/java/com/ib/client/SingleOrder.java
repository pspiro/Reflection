package com.ib.client;

import org.json.simple.JsonObject;

import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IOrderHandler;

import reflection.Prices;
import tw.util.S;

/** One order of a pair on DualOrder. This might not be needed
 *  We could maybe just add two IB Order's to DualOrder */
public class SingleOrder implements IOrderHandler {
	public enum Type { Day, Night };  // you could combine this with TradingHours.Session

	public interface SingleParent {
		void onStatusUpdated(Type session, int filled);  // change to Consumer<Type>  
	}

	private final Order m_order = new Order();  // this is downloaded; could be null
	private final Type m_session;
	private final SingleParent m_parent;
	private final Prices m_prices;  // could be null
	private double m_filled;
	private double m_avgPrice;
	private OrderStatus m_status = OrderStatus.Unknown;
	private Runnable m_listener;
	private String m_name;
	
	/** Called for new order, not submitted yet. Id will be set
	 *  when order is submitted.  
	 * @param json 
	 * @param night */
	public SingleOrder(Type session, Prices prices, String name, SingleParent parent) {
		m_session = session;
		m_parent = parent;
		m_prices = prices;
		m_name = (name + "/" + session).toUpperCase();
	}

	public Order o() {
		return m_order;
	}
	
	private void out( String format, Object... params) {
		S.out( m_name + " " + format, params);
	}

	JsonObject toJson() {
		return common.Util.toJson(
				"filled", m_filled,
				"avgPrice", m_avgPrice);
	}

	public void placeOrder(ApiController conn, Contract contract) throws Exception {
		common.Util.require( m_order.status() == OrderStatus.Unknown, "order should be Inactive");

		// we must simulate stop-limit orders on Overnight exchange (stop orders won't work as market orders are not supported)
		if (m_session == Type.Night && m_order.orderType() == OrderType.STP_LMT) {
			out( "Simulating stop order");

			// called when the prices are updated
			m_listener = () -> onPriceChanged( conn, contract);

			common.Util.require( m_prices != null, "must pass prices for sim stop order");
			m_prices.addListener( m_listener);
		}
		else {
			conn.placeOrder( contract, m_order, this);
			out( "Placed order with id %s", m_order.orderId() );
		}
	}
			
	/** triggers when the BID is <= trigger price; somewhat dangerous */
	private void onPriceChanged(ApiController conn, Contract contract) {
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
				conn.placeOrder( contract, m_order, this);
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
		
		String description = filled.toDouble() > m_filled
				? filled.toDouble() >= m_order.roundedQty() ? "filled" : "traded"
				: "status"; 

		m_filled = filled.toDouble();
		m_avgPrice = avgFillPrice;
		m_status = status;

		out( "margin order %s %s  id=%s  status=%s  session=%s  filled=%s  remaining=%s  avgPrice=%s", 
				m_name, description, m_order.orderId(), status, m_session, filled, remaining, avgFillPrice);
		
		m_parent.onStatusUpdated( m_session, (int)m_filled);
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

	public void cancel(ApiController conn) {
		if (m_order != null && m_status.canCancel() ) {
			conn.cancelOrder( m_order.orderId(), "", status -> 
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

	public void tick(boolean open) {
		if (m_order != null) {
			if (m_order.status() == OrderStatus.Unknown && m_filled == 0) {
				out( "Submitting single order");
			}
		}
	}

	/** Return dollar amount spent. Spending is negative.
	 *  Ideally we would subtract out the IB commissions as well */
	public double getBalance() {
		return -m_filled * m_avgPrice;
	}

	public double filled() {
		return m_filled;
	}

	public boolean isComplete() {
		return m_status.isComplete();
	}
}
