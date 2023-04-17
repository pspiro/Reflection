package reflection;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.json.simple.JSONObject;

import com.ib.client.Order;
import com.ib.client.Types.Action;

import tw.util.S;

public class Prices {
	public static final Prices NULL = new Prices();
	
	public static String TOO_LOW = "Your order was not filled because the price was too low; try refreshing the token price and resubmitting the order"; // // this is displayed to user
	public static String TOO_HIGH = "Your order was not filled because the price was too high; try refreshing the token price and resubmitting the order";
	private static SimpleDateFormat timeFmt = new SimpleDateFormat( "MM/dd HH:mm:ss");
	
	private double m_bid;
	private double m_ask;
	private double m_last;
	private double m_close;
	private String m_time;

	public double bid() { return m_bid; }
	public double ask() { return m_ask; }
	public double last() { return m_last; }
	public double close() { return m_close; }

	public Prices(Map<String, String> map) {
		m_bid = getDouble(map, "bid");
		m_ask = getDouble(map, "ask");
		m_last = getDouble(map, "last");
		m_close = getDouble(map, "close");
		m_time = map.get("time");
	}
	
	/** Used only by NULL prices. */
	public Prices() {
		m_time = "";
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
		if (order.action() == Action.BUY) {
			Main.require( validAsk(), RefCode.NO_PRICES, "No ask price");
			Main.require( orderPrice >= m_ask, RefCode.INVALID_PRICE, TOO_LOW);  // this is displayed to user
		}
		else {
			Main.require( validBid(), RefCode.NO_PRICES, "No bid price");
			Main.require( orderPrice <= m_bid, RefCode.INVALID_PRICE, TOO_HIGH);  // this is displayed to user
		}
	}

	public JSONObject toJson(int conid) throws RefException {
		return Util.toJsonMsg( "bid", anyBid(), "ask", anyAsk() );
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
}
