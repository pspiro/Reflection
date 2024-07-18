package com.ib.client;

import java.util.HashMap;
import java.util.function.Consumer;

import com.ib.client.Types.Action;
import com.ib.client.Types.TimeInForce;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IOrderHandler;
import com.ib.controller.ApiController.LiveOrder;

import reflection.Prices;
import reflection.TradingHours.Session;
import tw.util.S;

/** One order of a pair on DualOrder. This might not be needed
 *  We could maybe just add two IB Order's to DualOrder */
public class SingleOrder implements IOrderHandler {
	public enum Type { Day, Night };  // you could combine this with TradingHours.Session

	public interface SingleParent {
		void onStatusUpdated(SingleOrder order, OrderStatus status, int permId, Action action, int filled, double avgFillPrice);  
	}

	private final ApiController m_conn;
	private final Prices m_prices;  // could be null
	private final Type m_session;
	private final String m_name;
	private final String m_key;
	private final int m_conid;
	private final SingleParent m_parent;
	
	// order and order status; comes from LiveOrder
	private Order m_order = new Order();  // could be replaced in placeOrder(); other than that, it does not change
	private Consumer<Prices> m_listener; // access to this must be synchronized
	
	/** Called for new order, not submitted yet. Id will be set when order is submitted.
	 *  @parent is last for ease of call */  
	public SingleOrder(
			ApiController conn,
			Prices prices, 
			Type session, 
			String name, 
			String key,
			int conid,
			SingleParent parent) {
		
		m_conn = conn;
		m_session = session;
		m_parent = parent;
		m_prices = prices;
		m_name = (name + "/" + session).toUpperCase();
		m_key = key;
		m_conid = conid;
		
		m_order.orderRef( m_key);
		m_order.tif( m_session == Type.Day ? TimeInForce.GTC : TimeInForce.DAY);
	}
	
	/** Called after reconnect */
	public void rehydrate(HashMap<String, LiveOrder> orderRefMap) {
		common.Util.ifff( orderRefMap.get( m_key), liveOrder -> {
			m_order = liveOrder.order();
			m_order.status( liveOrder.status() ); //remove. pas
			
			m_conn.listenTo( m_order.orderId(), this);

			out( "Restored SingleOrder id=%s  key=%s  permId=%s  status=%s", 
					m_order.orderId(), m_key, m_order.permId(), m_order.status() );
			
			// notify parent in case state has changed in some way
			// we don't need this because we are going to call MarginOrder.process() 
			// right after this method, plus we have already saved/update the live order
			// in the orderMap()
//			m_parent.onStatusUpdated( this, liveOrder.status(), m_order.permId(), m_order.action(), 
//					liveOrder.filled(), liveOrder.avgPrice() );
		});
	}

	/** Called periodically; the order should be working */ 
	synchronized void checkOrder( int quantity) {
		if (m_order.status().isActive() || m_listener != null) {
			// nothing to do
		}
		else {
			out( "WARNING: SingleOrder state is %s when it should be active", m_order.status() );
			m_order.roundedQty( quantity);
			try {
				placeOrder();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void checkCanceled() {
		if (m_order.status().isActive() ) {
			out( "WARNING: SingleOrder state is %s when it should be canceled");
			cancel();
		}
	}

	public synchronized void placeOrder() throws Exception {
		//common.Util.require( m_order.status() == OrderStatus.Unknown, "SingleOrder should be inactive");
	
		// simulate stop-limit orders on Overnight exchange (stop orders won't work as 
		// market orders are not supported)
		if (m_session == Type.Night && m_order.orderType() == OrderType.STP_LMT) {  // technically, if the order triggered, we should send it in even if it is now untriggered. pas
			if (m_listener == null) {
				out( "Simulating stop order");
				
				// listen for price changes
				common.Util.require( m_prices != null, "must pass prices for sim stop order");
				m_listener = prices -> onPriceChanged();
				m_prices.addListener( m_listener);
			}
			else {
				out( "ERROR: stop order is already listening");  // should never happen
			}
		}
		else {
			m_conn.placeOrder( contract(), m_order, this);
			m_order.status( OrderStatus.PendingSubmit);  // so we know the order was submitted, in case "check()" is called before we get a response from TWS"
			out( "Placed new SingleOrder id=%s  key=%s", m_order.orderId(), m_key);  // must come after placeOrder()
		}
	}

	/** Called when the price is modified. Submit or resubmit the order */ 
	public synchronized void resubmit() throws Exception {
		if (m_session == Type.Night && m_order.orderType() == OrderType.STP_LMT) {  // technically, if the order triggered, we should send it in even if it is now untriggered. pas
			if (m_listener == null) {
				out( "Simulating stop order");
				
				// listen for price changes
				common.Util.require( m_prices != null, "must pass prices for sim stop order");
				m_listener = prices -> onPriceChanged();
				m_prices.addListener( m_listener);
			}
			// else already listening, nothing to do
		}
		
		if (m_order.status().canModify() ) {
			m_conn.modifyOrder( contract(), m_order, this);
		}
		else if (m_order.status() == OrderStatus.Unknown) {
			m_conn.placeOrder( contract(), m_order, this);
		}
	}

	/** triggers when the BID is <= trigger price; somewhat dangerous */
	private void onPriceChanged() {
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
				m_conn.placeOrder( contract(), m_order, this);
			} catch (Exception e) {
				e.printStackTrace();  // what to do here? pas
			}
		}
	}
	
	@Override public void onRecOrderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice, int permId,
			int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
		
		m_order.status( status);

		out( "SingleOrder name=%s  id=%s  status=%s  session=%s  filled=%s  remaining=%s  avgPrice=%s", 
				m_name, m_order.orderId(), status, m_session, filled, remaining, avgFillPrice);
		
		m_parent.onStatusUpdated( this, status, permId, m_order.action(), filled.toInt(), avgFillPrice);
	}

	@Override public void orderState(OrderState orderState) {
		// ignore this, we don't care
	}
	
	@Override public void onRecOrderError(int errorCode, String errorMsg) {
		// ignore this, we don't care
	}

	public void cancel() {
		if (m_order != null && m_order.status().canModify() && m_conn.isConnected() ) {

			m_order.status( OrderStatus.PendingCancel);
			
			m_conn.cancelOrder( m_order.orderId(), "", status -> 
				out( "Canceled order  orderId=%s  permId=%s  status=%s", m_order.orderId(), m_order.permId(), status) );
		}
		
		stopListening();
	}

	private synchronized void stopListening() {
		if (m_prices != null) {
			m_prices.removeListener( m_listener);
		}
		m_listener = null;
	}

	public boolean isComplete() {
		return m_order.status().isComplete();
	}

	public Order o() {
		return m_order;
	}
	
	String name() {
		return m_name;
	}
	
	private void out( String format, Object... params) {
		S.out( m_name + " " + format, params);
	}
	
	private String exchange() {
		return m_session == Type.Day ? Session.Smart.toString() : Session.Overnight.toString();
	}
	
	private Contract contract() {
		return new Contract( m_conid, exchange() );
	}


}
