package reflection;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

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
	private double m_close;
	private long m_time;  // ms since epoch

	public double bid() { return m_bid; }
	public double ask() { return m_ask; }
	public double last() { return m_last; }
	public double close() { return m_close; }
	public long time() { return m_time; }

	public Prices(Map<String, String> map) {
		m_bid = getDouble(map, "bid");
		m_ask = getDouble(map, "ask");
		m_last = getDouble(map, "last");
		m_close = getDouble(map, "close");
		m_time = getLong(map, "time");
	}
	
	/** Used only by NULL prices. */
	public Prices() {
	}

	private long getLong(Map<String, String> map, String key) {
		String val = map.get(key);
		return val != null ? Long.valueOf( val) : 0;
	}

	double getDouble( Map<String, String> map, String key) {
		String val = map.get(key);
		return val != null ? Double.valueOf( val) : 0;
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
		return validBid() ? m_bid : validLast() ? m_last - .05 : validClose() ? m_close : 0;
	}

	/** Used for display on the Watch List */
	double anyAsk() {
		return validAsk() ? m_ask : validLast() ? m_last + .05 : validClose() ? m_close : 0;
	}

	boolean validLast() {
		return m_last > 0;
	}
	
	boolean validClose() {
		return m_close > 0;
	}
	
	public boolean hasSomePrice() {
		return validBid() || validAsk();
	}

	public boolean hasAnyPrice() {
		return validBid() || validAsk() || validLast() || validClose();
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
		S.out( "conid=%s  bid=%s  ask=%s  last=%s  close=%s",
				conid, m_bid, m_ask, m_last, m_close);	
	}

	private final static double NEAR = .005; // one half percent
	private final static long MIN = 60000; // one minute
	private final static long RECENT = MIN * 5; // five minutes
	
	/** Make sure bid or ask is near a recent last when order size is rounded to zero */
	public boolean hasNear(boolean buy) {
		return 
				m_bid <= m_ask &&  // bid/ask should not be crossed 
				System.currentTimeMillis() - m_time <= RECENT &&  // last price is recent 
				(buy 
						? (m_last - m_ask) / m_last <= NEAR   // for a buy order, make sure the ask is not very low
						: (m_bid - m_last) / m_last <= NEAR); // for a sell order, make sure the bid is not very high 
	}

//	static DateFormat fmt = new SimpleDateFormat("M/d K:m:s");
//	public static void main(String[] args) throws ParseException {
//		String str = "09/01 14:57:23";
//		Date t = fmt.parse(str);
//		S.out(t);
//		1693595099113
//	}
}
