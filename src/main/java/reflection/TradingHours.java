package reflection;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.json.simple.JSONObject;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.controller.ApiController;

import tw.util.S;
import util.LogType;

public class TradingHours {
	public enum Session { Smart, Ibeos, None }

	static final int interval = 60 * 60 * 1000; // 1 hour
    static final DateFormat dateAndTime = new SimpleDateFormat( "MM/dd/yy HH:mm"); 
	static final int stockConid = 8314;    // IBM
	static final int etf24Conid = 756733;  // SPY

    private ApiController m_controller;
	private HashMap<String,ContractDetails> m_map = new HashMap<>();  // map exchange to trading hours
	private boolean m_started;
	
	public TradingHours(ApiController main) {
		m_controller = main;
	}

	/** This could get called multiple times if there is a disconnect/reconnect */
	public void startQuery() {
		if (!m_started) {
			m_started = true;

			S.out( "Starting trading hours query thread every one hour");
			
			Util.executeEvery( 0, interval, () -> {
				S.out( "Querying for trading hours now");
				query( stockConid, "SMART");
				query( etf24Conid, "IBEOS");
			});
		}
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
				Main.log( LogType.ERROR, e.getMessage() ); 
			}
		});
	}
	
    /** If simTime is set, return today's date and simTime, otherwise return now */
    private Date getNow(String simTime) throws ParseException {
		return S.isNull( simTime) 
				? new Date()
				: dateAndTime.parse( S.userDate(new Date()) + " " + simTime);
	}
	
	
	/** Check if we are inside trading hours. For ETF's, check smart; if that fails,
	 *  check IBEOS and change the exchange on the contract passed in to IBEOS. */
	boolean insideAnyHours( boolean is24Hour, String simTime) throws Exception {
		// if auto-fill is on, always return true, UNLESS simtime is passed
		// which means this is called by a test script
		if (Main.m_config.autoFill() && S.isNull(simTime) ) {
			return true;
		}
		
		Date now = getNow(simTime);

		boolean inside = insideHours( "SMART", now);
			
		if (!inside && is24Hour) {
			inside = insideHours( "IBEOS", now);
		}
		
		return inside;
	}

	/** Return true if now is inside trading hours OR liquid hours. */
	boolean insideHours( String exchange, Date now) throws Exception {  // change to return boolean
		ContractDetails deets;
		synchronized( m_map) {
			deets = m_map.get(exchange);
		}
		Util.require( deets != null, "Error: no contract details for exchange " + exchange); // this can happen at startup if the mkt data update thread starts before the trading hours have returned

		return inside( deets, deets.tradingHours(), now ) ||
			   inside( deets, deets.liquidHours(), now );
	}
	
	/** Return true if now is inside the specified hours */
	static boolean inside(ContractDetails deets, String hours, Date now) throws Exception {
		return Util.inside( now, deets.conid(), hours, deets.timeZoneId() );
	}

	/** We can update this to be more specific and consider conid if necessary */
	public Session getSession(Stock stock) throws Exception {
		Date now = new Date();
		
		return insideHours( "SMART", now)
				? Session.Smart
				: stock.is24Hour() && insideHours( "IBEOS", now) 
					? Session.Ibeos 
					: Session.None;
	}

	public JSONObject getHours() {
		JSONObject obj = new JSONObject();
		
		m_map.forEach( (item1, deets) -> {
			JSONObject inner = new JSONObject();
			inner.put( "Trading hours", deets.tradingHours() );
			inner.put( "Liquid hours", deets.liquidHours() );
			obj.put( item1, inner);
		});

		return obj;

	}
	
}
