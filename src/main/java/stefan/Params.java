package stefan;

import java.text.ParseException;
import java.util.Date;
import java.util.regex.Pattern;

import org.json.simple.JsonObject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ib.client.OrderType;
import com.ib.client.Types.BarSize;
import com.ib.controller.ApiController.ApiParams;

import common.Util;
import tw.util.MyException;
import tw.util.S;

// or maybe config
@JsonIgnoreProperties(ignoreUnknown = true)  // extra fields in the json will be ignored
record Params( 
		String host,			// tws connection
		int port,				// tws connection
		int clientId,			// tws connection
		String stockList,		// stock list, comma-separated
		String startTime,		// candlestick start time
		String endTime,			// trading start time
		String closeTime,		// trading end time, close orders
		int candleSize,			// candle size in minutes
		double totalAmount,		// total dollar amount of all trades (sum of long and abs(short) )
		double tradeAmount,		// dollar amount of each buy order
		int minShares,			// min shares per order; if we can't afford this, no order
		String orderType,		// order type of buy order (mkt or lmt)
		double limitOffset,		// applied to limit price for buy limit order;
		double trailingPct,		// trailing amount of trailing stop order AS A PERCENT, not a decimal, e.g. 1=1%
		double lossLimitPct,	// added to buyLine to calculate lossLimitPrice
		double excessPct		// added to buyLine to calculate initial buy stop price aka priceBuffer
		) {
	
//	Params() {
//	}
	
	static String filename = "config.json";

	public OrderType ibOrderType() {
		return Util.getEnum( orderType, OrderType.values() );
	}

	public ApiParams getApiParams() {
		return new ApiParams( host, port, clientId);
	}

	public int duration() throws ParseException {
		return (int)(getEndAsDate().getTime() - getStartAsDate().getTime() ) / 1000;
	}

	public BarSize getBarSize() {
		if (candleSize == 1) {
			return BarSize._1_min;
		}
		return BarSize.valueOf( String.format( "_%s_mins", candleSize) );  
	}
	private void validateBarSize() throws Exception {
	
		try {
			getBarSize();
		}
		catch( Exception e) {
			throw new Exception( "The candle size is invalid; valid sizes are: 1, 2, 3, 5, 10, 15, 20, 30");
		}
	}
	
	public Date getStartAsDate() throws ParseException {
		return timeAsDate( startTime);
	}

	public Date getEndAsDate() throws ParseException {
		return timeAsDate( endTime);
	}
	
	public Date getCloseAsDate() throws ParseException {
		return timeAsDate( closeTime);
	}
	
	/** NOTE: chatgpt suggested a faster way to do this using LocalDateTime
	 *  @param time is in format hh:mm
	 *  @return date object for today with the specified time 
	 *  @throws ParseException */
	public Date timeAsDate( String time) throws ParseException {
		String today = Util.yToD.format( new Date() );
		String full = String.format( "%s %s:00", today, time);
		return Util.yToS.parse( full); 
	}
	
	public void validate() throws Exception {
		req( Util.isValidIpAddress( host), "IP address");
		req( port > 0, "port");
		req( clientId > 0, "client Id");
		req( S.isNotNull( stockList), "stock list");
		req( validTime( startTime), "candle start time");
		req( validTime( endTime), "candle end time");
		req( validTime( closeTime), "trading end time");
		req( getEndAsDate().compareTo( getStartAsDate() ) > 0, "starting and/or ending time");
		req( getCloseAsDate().compareTo( getEndAsDate() ) > 0, "ending and/or closing time");
		req( candleSize > 0, "candle size");
		req( totalAmount > 0 && totalAmount > tradeAmount, "total amount");
		req( tradeAmount > 0, "trade amount");
		req( minShares > 0, "minimum shares per order");
		req( Util.equals( orderType, "MKT", "LMT"), "order type");
		// add this back. pas req( orderType.equals( "MKT") || limitOffset >= 0, "limit price offset");
		req( trailingPct > 0, "trailing pct");
		req( excessPct > 0, "price buffer");
		req( lossLimitPct >= 0, "loss limit");
		
		validateBarSize();
	}
	
	private boolean validTime(String t) {
		return t != null && Pattern.matches( "[0-9]{1,2}:[0-9]{1,2}", t);
	}

	private void req(boolean valid, String field) throws Exception {
		if (!valid) throw new MyException( "The '%s' is invalid", field);
	}

	static Params read() throws Exception {
		return JsonObject.readFromFile(filename).toRecord( Params.class);
	}
	
	void write() throws Exception {
		JsonObject.toJson( this).writeToFile(filename);
	}
}
