package reflection;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.simple.JsonObject;

import com.ib.client.Order;

import common.Util;
import tw.util.S;

public class Prices {
	public static final Prices NULL = new Prices();
	
	public static String TOO_LOW = "Your order was not filled because the price was too low; try refreshing the token price and resubmitting the order"; // // this is displayed to user
	public static String TOO_HIGH = "Your order was not filled because the price was too high; try refreshing the token price and resubmitting the order";
	public static String NO_PRICE = "There is no %s price in the market. Please try your order again later.";
	
	private static SimpleDateFormat timeFmt = new SimpleDateFormat( "MM/dd HH:mm:ss");
	
	private double m_bid;
	private double m_ask;
	private double m_last;
	private long m_time;  // ms since epoch; time of most recent price, could be bid, ask, or last

	public double bid() { return m_bid; }
	public double ask() { return m_ask; }
	public double last() { return m_last; }
	public long time() { return m_time; }

	public Prices(JsonObject obj) {
		m_bid = obj.getDouble("bid");
		m_ask = obj.getDouble("ask");
		m_last = obj.getDouble("last");
		m_time = obj.getLong("time");
	}
	
	/** Used only by NULL prices. */
	public Prices() {
	}

	public String getFormattedTime() {
		return S.isNull( m_time)
				? ""
				: timeFmt.format( new Date( Long.valueOf( m_time) ) );
	}
	
	/** No penny stocks */
	boolean validBid() {
		return m_bid >= 1;
	}
	
	/** No penny stocks */
	boolean validAsk() {
		return m_ask >= 1;
	}

	public void checkOrderPrice(Order order, double orderPrice, Config config) throws RefException {
		if (order.isBuy() ) {
			Main.require( validAsk(), RefCode.NO_PRICES, NO_PRICE, "ask");
			Main.require( orderPrice >= m_ask, RefCode.INVALID_PRICE, TOO_LOW);  // this is displayed to user
		}
		else {
			Main.require( validBid(), RefCode.NO_PRICES, NO_PRICE, "bid");  // this should not happen because if the exchange is open, there should be a bid, and if the exchange is closed, they would receive an error telling them so; but, it happens in testing with autoFill
			Main.require( orderPrice <= m_bid, RefCode.INVALID_PRICE, TOO_HIGH);  // this is displayed to user
		}
	}

	/** Returns bid/ask only */
	public JsonObject toJson(int conid) throws RefException {
		return Util.toJson( "bid", anyBid(), "ask", anyAsk() );
	}
	
	/** Used for display on the Watch List */
	double anyBid() {
		return validBid() ? m_bid : validLast() ? m_last - .05 : 0;
	}

	/** Used for display on the Watch List */
	double anyAsk() {
		return validAsk() ? m_ask : validLast() ? m_last + .05 : 0;
	}

	boolean validLast() {
		return m_last > 0;
	}
	
	public boolean hasSomePrice() {
		return validBid() || validAsk();
	}

	public boolean hasAnyPrice() { // seems useless. pas
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
		return S.fmt2d( midpoint() );
	}

	public void dump(int conid) {
		S.out( "conid=%s  bid=%s  ask=%s  last=%s",
				conid, m_bid, m_ask, m_last);	
	}

//	static DateFormat fmt = new SimpleDateFormat("M/d K:m:s");
//	public static void main(String[] args) throws ParseException {
//		String str = "09/01 14:57:23";
//		Date t = fmt.parse(str);
//		S.out(t);
//		1693595099113
//	}
}
