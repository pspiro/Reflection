package reflection;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.function.Consumer;

import org.json.simple.JsonObject;

import com.ib.client.Order;

import common.Util;
import tw.util.S;

public class Prices {
	//public static final Prices NULL = new Prices();
	
	public static String TOO_LOW = "Your order was not filled because the price was too low; try refreshing the token price and resubmitting the order"; // // this is displayed to user
	public static String TOO_HIGH = "Your order was not filled because the price was too high; try refreshing the token price and resubmitting the order";
	public static String NO_PRICE = "There is no %s price in the market. Please try your order again later.";
	
	private static SimpleDateFormat timeFmt = new SimpleDateFormat( "MM/dd HH:mm:ss");
	
	private double m_bid;
	private double m_ask;
	private double m_last;
	private long m_time;  // ms since epoch; time of most recent price, could be bid, ask, or last

	private final ArrayList<Consumer<Prices>> m_listeners = new ArrayList<>();

	public double bid() { return m_bid; }
	public double ask() { return m_ask; }
	public double last() { return m_last; }
	public long time() { return m_time; }

	public Prices(JsonObject obj) {
		update( obj);
	}
	
	/** Used only by NULL prices. */
	public Prices() {
	}

	public void update(JsonObject obj) {
		m_bid = obj.getDouble("bid");
		m_ask = obj.getDouble("ask");
		m_last = obj.getDouble("last");
		m_time = obj.getLong("time");

		// notify all listeners that the price has changed;
		// make a copy to avoid ConcurrentMod error because 
		// the listener may remove itself during processing
		new ArrayList<Consumer<Prices>>( m_listeners)
			.forEach( listener -> listener.accept( this) );
	}

	/** Used by simulated stop orders */
	public void addListener( Consumer<Prices> listener) {
		m_listeners.add( listener);
	}

	/** note listener could be null */
	public void removeListener(Consumer<Prices> listener) {
		m_listeners.remove( listener);
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

	/** If we have ask and last, return the smaller; if we have ask only
	 *  return that. Last only doesn't help us because it could be old */
	public double askMark() {
		return validLast() ? Math.min( m_ask, m_last) : m_ask;
	}

	/** Must have valid bid and ask; check before calling.
	 *  @return last price bounded by bid/ask, or midpoint if no last 
	 * @throws Exception */
	public double markPrice() throws Exception {
		Util.require( validBid() && validAsk(), "No valid prices for mark price");
		return validLast() ? S.between( m_last, m_bid, m_ask) : (m_bid + m_ask) / 2.;
	}

	/** for debug and S.out() only */
	@Override public String toString() {
		return S.format( "bid=%s  ask=%s  last=%s  time=%s",
				m_bid, m_ask, m_last, Util.hhmmss.format( m_time) );
	}

//	static DateFormat fmt = new SimpleDateFormat("M/d K:m:s");
//	public static void main(String[] args) throws ParseException {
//		String str = "09/01 14:57:23";
//		Date t = fmt.parse(str);
//		S.out(t);
//		1693595099113
//	}
	
	static final double dailyVol = .02;
	static final double secVol = dailyVol / Math.sqrt(28800);
	
	public void fakeInit() {
		m_last = Util.rnd.nextDouble(20, 500);
		m_bid = .998 * m_last;
		m_ask = 1.002 * m_last;
		
		Util.executeEvery("ticker", 0, 1000, () -> {
			double mult = Util.rnd.nextBoolean() ? 1. + secVol : 1. - secVol;
			m_last *= mult;
			m_bid = m_last * .998;
			m_ask = m_last * 1.002;
		});
	}

}
