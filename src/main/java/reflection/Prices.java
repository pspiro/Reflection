package reflection;

import java.util.Random;

import com.ib.client.Decimal;
import com.ib.client.Order;
import com.ib.client.TickType;
import com.ib.client.Types.Action;

import tw.util.S;

class Prices {
	private double m_bid;
	private double m_ask;
	private double m_last;
	private double m_close;
	private Decimal m_bidSize;  // not being used for anything
	private Decimal m_askSize;

	public double bid() { return m_bid; }
	public double ask() { return m_ask; }

	public void tick(TickType tickType, double price, Decimal size) {
		switch( tickType) {
			case CLOSE:
			case DELAYED_CLOSE:
				m_close = price; break;
			case LAST:
			case DELAYED_LAST:
				m_last = price; break;
			case BID:
			case DELAYED_BID:
				m_bid = price; break;
			case ASK:
			case DELAYED_ASK:
				m_ask = price; break;
			case BID_SIZE:
			case DELAYED_BID_SIZE:
				m_bidSize = size; break;
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

	public Json toJson(int conid) throws RefException {
		Main.require( m_bidSize !=null && m_askSize != null, RefCode.INVALID_PRICE, "conid %s has null sizes", conid); 
		return Util.toJsonMsg("bid", m_bid, "bidSize", m_bidSize.toInteger(), "ask", m_ask, "askSize", m_askSize.toInteger() );
//		return Util.toJsonMsg("bid", m_bid, "ask", m_ask);
	}
	
	/** Used for display on the Watch List */
	double anyBid() {
		return validBid() ? m_bid : validLast() ? m_last - .02 : 0;
	}

	/** Used for display on the Watch List */
	double anyAsk() {
		return validAsk() ? m_ask : validLast() ? m_last + .02 : 0;
	}

	boolean validLast() {
		return m_last > 0;
	}
	
	public boolean hasSomePrice() {
		return validBid() || validAsk();
	}

	public boolean hasAnyPrice() {
		return validBid() || validAsk() || validLast();
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


	
	/** This is called only when simulating prices and trading, never in production.
	 *  Adjust the prices up or down by from .0001 to 0015 
	 *  This should cause some orders to be rejected. */
	static Random rnd = new Random();
	static Decimal defSize = Decimal.get( 100.0);
	public void adjustPrices() {
		double adj = (rnd.nextInt(15) + 1) / 10000.0;
		double adj2 = rnd.nextBoolean() ? 1 + adj : 1 - adj;
		
		m_bid *= adj2;
		m_ask *= adj2;
	}

	/** Called in simulated mode only. */
	public void setInitialPrices() {
		tick( TickType.BID, 53.14, null);
		tick( TickType.ASK, 53.42, null);
		tick( TickType.BID_SIZE, 0., Decimal.get( 100) );
		tick( TickType.ASK_SIZE, 0., Decimal.get( 100) );
	}
}
