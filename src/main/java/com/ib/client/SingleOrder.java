package com.ib.client;

import org.json.simple.JsonObject;

import com.ib.client.DualOrder.Type;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IOrderHandler;

import tw.util.S;

/** One order of a pair on DualOrder. This might not be needed
 *  We could maybe just add two IB Order's to DualOrder */
class SingleOrder implements IOrderHandler {
	private final Order m_order;  // this is downloaded; could be null
	private final Type m_session;
	private final DualOrder m_parent;
	private int m_orderId;  // this is serialized
	private double m_filled;
	private double m_avgPrice;
	private OrderStatus m_status = OrderStatus.Inactive;

	/** Called for new order, not submitted yet. Id will be set
	 *  when order is submitted.  
	 * @param json 
	 * @param night */
	public SingleOrder(Type session, DualOrder parent) {
		m_session = session;
		m_parent = parent;
		m_order = new Order();
	}

	JsonObject toJson() {
		return common.Util.toJson(
				"orderId", m_orderId,
				"filled", m_filled,
				"avgPrice", m_avgPrice);
	}

	public int orderId() {
		return m_orderId;
	}

	public Order o() {
		return m_order;
	}

	public void placeOrder(ApiController controller, Contract contract) throws Exception {
		common.Util.require( m_orderId == 0, "orderId should be zero");
		
		controller.placeOrder( contract, m_order, this);
		m_orderId = m_order.orderId();
		S.out( "Placed order %s order with id %s", m_session, m_orderId);
	}
			
	@Override public void orderState(OrderState orderState) {
		// ignore this, we don't care
	}
	
	@Override public void onRecOrderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice, int permId,
			int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {

		m_filled = filled.toDouble();
		m_avgPrice = avgFillPrice;
		m_status = status;

		S.out( "received order status %s %s %s %s %s", m_session, status, filled, remaining, avgFillPrice);
		m_parent.onRecOrderStatus( m_session);
	}
	
	@Override public void onRecOrderError(int errorCode, String errorMsg) {
	}

	/** Set the IB order and listen for updates IF the permId matches */
	public void setOrder(ApiController conn, Order order) {
//		if (m_orderId == order.orderId() && m_order == null) {
//			S.out( "Restoring order with orderId %s", m_orderId);
//			m_order = order;
//			conn.listenTo( m_order, this);
//		}
	}

	/** For debugging only */
	@Override public String toString() {
		return m_order != null
				? String.format( "orderId=%s  side=%s  qty=%s  price=%s",
					m_orderId, m_order.action(), m_order.roundedQty(), m_order.lmtPrice() )
				: String.format( "orderId=%s  (order is null)", m_orderId);
	}

	public void cancel(ApiController conn) {
		if (m_order != null) {
			conn.cancelOrder( m_orderId, "", status -> 
				S.out( "Canceled order orderId=%s  status=%s", m_orderId, status) );
		}
	}

	public void tick(boolean open) {
		if (m_order != null) {
			if (m_order.status() == OrderStatus.Inactive && m_filled == 0) {
				S.out( "Submitting single order");
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

	public boolean isWorking() {
		return m_status == OrderStatus.Inactive || m_status.isActive(); 
	}
}
