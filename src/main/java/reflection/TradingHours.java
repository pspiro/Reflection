package reflection;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.json.simple.JsonObject;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.controller.ApiController;

import chain.Stocks.Stock;
import common.LogType;
import common.Util;
import redis.MdServer;
import reflection.Config.MultiChainConfig;
import tw.util.S;

public class TradingHours {
	public enum Session { Smart, Overnight, None }

	static final int interval = 60 * 60 * 1000; // 1 hour
    static final DateFormat dateAndTime = new SimpleDateFormat( "MM/dd/yy HH:mm"); 
	static final int ibm = 8314;    // IBM

    private ApiController m_controller;
	private HashMap<String,ContractDetails> m_map = new HashMap<>();  // map exchange to trading hours; there will be two entries, one for SMART, and one for OVERNIGHT
	private boolean m_started;
	private MultiChainConfig m_config; // could be null
	
	public TradingHours(ApiController main, MultiChainConfig config) {
		m_controller = main;
		m_config = config;
	}

	/** This could get called multiple times if there is a disconnect/reconnect */
	public void startQuery() {
		if (!m_started) {
			m_started = true;

			S.out( "Starting trading hours query thread every one hour");
			
			Util.executeEvery( 0, interval, () -> queryNow() );
		}
		else {
			queryNow();
		}
	}
	
	private void queryNow() {
		S.out( "Querying for trading hours now");
		query( ibm, "SMART");
		query( ibm, MdServer.Overnight);
	}

	/** Query for trading hours of conid on exchange */
	private void query(int conid, String exchange) {
		Contract contract = new Contract();
		contract.conid(conid);
		contract.exchange(exchange);
		
		m_controller.reqContractDetails(contract, list -> {
			try {
				Util.require( !list.isEmpty(), "ERROR: No contract details for " + contract.conid() );
				ContractDetails deets = list.get(0);
				synchronized(m_map) {
					S.out( "Mapping exchange hours for %s", exchange);
					m_map.put( exchange, deets);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				if ( m_config != null) {
					Util.wrap( () ->
						m_config.sqlCommand( conn -> conn.insertJson( "log", Util.toJson(
								"type", LogType.TRADING_HOURS_ERROR,
								"data", RefException.eToJson(e) ) ) ) );
				}
			}
		});
	}
	
    /** If simTime is set, return today's date and simTime, otherwise return now */
    private Date getNow(String simTime) throws ParseException {
		return S.isNull( simTime) 
				? new Date()
				: dateAndTime.parse( S.userDate(new Date()) + " " + simTime);
	}

    /** @return the ContractDetails for the specified exchange */
    private ContractDetails getDeets(String exchange) throws Exception {
		Util.require( S.isNotNull(exchange), "Error: null exchange"); // this can happen at startup if the mkt data update thread starts before the trading hours have returned
		ContractDetails deets;
		synchronized( m_map) {
			deets = m_map.get(exchange);
		}
		Util.require( deets != null, "Error: no contract details for exchange " + exchange); // this can happen at startup if the mkt data update thread starts before the trading hours have returned
		return deets;
    }
    
    /** Confirm that dates are null or start date is in the past and end date is in the future;
     *  if not, throw an exception; the dates are only set when there is a stock split */
    public void checkSplitDates(String simTime, String startDate, String endDate) throws Exception {
    	// catch the vast majority of checks; only stock splits will get past here
    	if (S.isNull(startDate) && S.isNull(endDate) ) {
    		return;
    	}
    	
		TimeZone zone = getDeets("SMART").getTimeZone();

		SimpleDateFormat yyyymmdd = new SimpleDateFormat( "yyyy-MM-dd");
		yyyymmdd.setTimeZone(zone);
		String today = yyyymmdd.format( getNow(simTime) );

		Main.require( S.isNull(startDate) || today.compareTo(startDate) >= 0, RefCode.PRE_SPLIT, "This contract has not started trading yet; it starts on %s", startDate); 
		Main.require( S.isNull(endDate) || today.compareTo(endDate) <= 0, RefCode.POST_SPLIT, "The stock has split and must be converted to the new post-split smart contract"); 
    }
    
	/** Check if we are inside trading hours. For ETF's, check smart; if that fails,
	 *  check IBEOS and change the exchange on the contract passed in to IBEOS;
	 *  Exact duplicate of the below.
	 *  @param run gets executed if we want to swtich to IBEOS
	 *  @param simTime can be null */
	Session getTradingSession( boolean is24Hour, String simTime) throws Exception {
		// if auto-fill is on, always return true, UNLESS simtime is passed
		// which means this is called by a test script
		if (!Main.m_config.isProduction() && S.isNull(simTime) ) {
			return Session.Smart;  // for testing only
		}
		
		Date now = getNow(simTime);

		if (insideHours( MdServer.Smart, now) ) {
			return Session.Smart;
		}
			
		if (is24Hour && insideHours( MdServer.Overnight, now) ) {
			return Session.Overnight;
		}
		
		return Session.None;
	}

	/** We can update this to be more specific and consider conid if necessary;
	 *  exact duplicate of the above */
	public Session getSession(Stock stock) throws Exception {
		Date now = new Date();
		
		return insideHours( "SMART", now)
				? Session.Smart
				: stock.rec().is24Hour() && insideHours( MdServer.Overnight, now) 
					? Session.Overnight 
					: Session.None;
	}

	/** Return true if now is inside trading hours OR liquid hours. */
	boolean insideHours( String exchange, Date now) throws Exception {  // change to return boolean
		ContractDetails deets = getDeets(exchange);

		return inside( deets, deets.tradingHours(), now ) ||
			   inside( deets, deets.liquidHours(), now );
	}
	
	/** Return true if now is inside the specified hours */
	static boolean inside(ContractDetails deets, String hours, Date now) throws Exception {
		return Util.inside( now, deets.conid(), hours, deets.timeZoneId() );
	}

	public JsonObject getHours() {
		JsonObject obj = new JsonObject();
		
		m_map.forEach( (item1, deets) -> {
			JsonObject inner = new JsonObject();
			inner.put( "Trading hours", deets.tradingHours() );
			inner.put( "Liquid hours", deets.liquidHours() );
			obj.put( item1, inner);
		});

		return obj;

	}
	
}
