package reflection;

import com.ib.client.Decimal;
import com.ib.client.Order;
import com.ib.client.TickType;
import com.ib.client.Types.Action;

import tw.util.S;

class Prices {
	private double m_bid;
	private double m_ask;
	private Decimal m_bidSize;
	private Decimal m_askSize;

	public void tick(TickType tickType, double price, Decimal size) {
		switch( tickType) {
			case BID:
			case DELAYED_BID:
				m_bid = price; break;
			case BID_SIZE:
			case DELAYED_BID_SIZE:
				m_bidSize = size; break;
			case ASK:
			case DELAYED_ASK:
				m_ask = price; break;
			case ASK_SIZE:
			case DELAYED_ASK_SIZE:
				m_askSize = size; break;
		default:
			// ignore
		}
	}
	
	/** No penny stocks */
	boolean validBid() {
		return m_bid >= 1;
	}
	
	/** No penny stocks */
	boolean validAsk() {
		return m_ask >= 1;
	}

	public String getString() {
		return String.format( "bid=%s bidSize=%s ask=%s askSize=%s", 
				m_bid, m_bidSize, m_ask, m_askSize);  // format w/ two dec. pas
	}

	public void checkOrderPrice(Order order, double orderPrice, Config config) throws RefException {
		if (order.action() == Action.BUY) {
			Main.require( validAsk(), RefCode.NO_PRICES, "No ask price");
			Main.require( orderPrice >= m_ask, RefCode.INVALID_PRICE, "\"Your order was not filled because the price was too low; try refreshing the token price and resubmitting the order");  // this is displayed to user
		}
		else {
			Main.require( validBid(), RefCode.NO_PRICES, "No bid price");
			Main.require( orderPrice <= m_bid, RefCode.INVALID_PRICE, "\"Your order was not filled because the price was too high; try refreshing the token price and resubmitting the order");  // this is displayed to user
		}
	}

	public Json toJson() {
		return Util.toJsonMsg("bid", m_bid, "bidSize", m_bidSize.toInteger(), "ask", m_ask, "askSize", m_askSize.toInteger() );
	}

	public boolean hasSomePrice() {
		return validBid() || validAsk();
	}

	public double midpoint() {
		if (validBid() && validAsk() ) {
			return (m_bid + m_ask) / 2;
		}
		if (validBid() ) {
			return m_bid;
		}
		if (validAsk() ) {
			return m_ask;
		}
		return 0;
	}

	/** Return midpoint formatted w/ two decimal places. */
	public String midpointStr() {
		return S.fmt3( midpoint() );
	}

	public double bid() {
		return m_bid;
	}

	public double ask() {
		return m_ask;
	}
}
