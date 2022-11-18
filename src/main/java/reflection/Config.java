package reflection;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Map.Entry;

import org.json.simple.JSONObject;

import reflection.Main.Mode;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;

public class Config {
	
	// user experience parameters
	private double maxBuyAmt = 15000; // max buy amt in dollars
	private double maxSellAmt = 15000; // max sell amt in dollars
	private double minBuySpread = .004;  // as pct of price
	private double minSellSpread = .004;  // as pct of price

	// needed by back-end server
	private double buySpread = .005;  // as pct of price
	private double sellSpread = .005;  // as pct of price
	
	// 
//	private double commission;
//	private int price_refresh_interval (integer)
	
//	min_order_size (float)  // get rid of these
//	max_order_size (float)  // // get rid of these
//	non_kyc_max_order_size (float)
//	commission (float)
	

	// program parameters
	private Mode mode = Mode.paper;  // paper or production
	private String twsOrderHost;  // TWS is listening on this host
	private int twsOrderPort;  // TWS is listening on this port
	private String twsMdHost;  // TWS is listening on this host
	private int twsMdPort;  // TWS is listening on this port
	private String refApiHost = "0.0.0.0"; // host for RefAPI to listen on
	private int refApiPort = 8383;  // port for RefAPI to listen on
	private long orderTimeout = 7000;  // order timeout in ms
	private long timeout = 7000;  // all other messages timeout 
	private long reconnectInterval = 5000;  // when we lost connection with TWS
	private String postgresUrl;
	private String postgresUser;
	private String postgresPassword;
	
	
	public double maxBuyAmt() { return maxBuyAmt; }
	public double maxSellAmt() { return maxSellAmt; }
	public double minSellSpread() { return minSellSpread; }
	public double minBuySpread() { return minBuySpread; }
	
	public String postgresPassword() { return postgresPassword; }
	public String postgresUser() { return postgresUser; }
	public String postgresUrl() { return postgresUrl; }

	public long timeout() {  return timeout;  }
	public long orderTimeout() { return orderTimeout; }
	public long reconnectInterval() { return reconnectInterval; }

	public String twsOrderHost() { return twsOrderHost; }
	public int twsOrderPort() { return twsOrderPort; }

	public String twsMdHost() { return twsMdHost; }
	public int twsMdPort() { return twsMdPort; }

	public Mode mode() {  return mode;  }
	public String refApiHost() { return refApiHost; }
	public int refApiPort() { return refApiPort; }

	public Config() { }
	
	public void readFromSpreadsheet(String tabName) throws Exception {
		GTable tab = new GTable( NewSheet.Reflection, tabName, "Tag", "Value");
		
		// user experience parameters
		this.buySpread = tab.getDouble( "buySpread");
		this.sellSpread = tab.getDouble( "sellSpread");
		this.minBuySpread = tab.getDouble( "minBuySpread");   // should be changed to read table without formatting. pas
		this.minSellSpread = tab.getDouble( "minSellSpread");
		this.maxBuyAmt = tab.getDouble( "maxBuyAmt");
		this.maxSellAmt = tab.getDouble( "maxSellAmt");

		// program parameters
		this.mode = S.getEnum( tab.get( "paperMode"), Mode.values() );

		this.twsOrderHost = tab.get( "twsOrderHost");
		this.twsOrderPort = tab.getInt( "twsOrderPort");

		this.twsMdHost = tab.get( "twsMdHost");
		this.twsMdPort = tab.getInt( "twsMdPort");

		this.refApiHost = tab.get( "refApiHost");
		this.refApiPort = tab.getInt( "refApiPort");
		
		this.postgresUrl = tab.get( "postgresUrl");
		this.postgresUser = tab.get( "postgresUser");
		this.postgresPassword = tab.get( "postgresPassword");
		
		
		this.reconnectInterval = tab.getInt( "reconnectInterval");
		this.orderTimeout = tab.getInt( "orderTimeout");
		this.timeout = tab.getInt( "timeout");
		
		require( buySpread > 0 && buySpread < .05, "buySpread");
		require( sellSpread > 0 && sellSpread <= .021, "sellSpread");  // stated max sell spread of 2% in the White Paper 
		require( minBuySpread > 0 && minBuySpread < .05 && minBuySpread < buySpread, "minBuySpread");
		require( minSellSpread > 0 && minSellSpread < .05 && minSellSpread < sellSpread, "minSellSpread");
		require( maxBuyAmt > 0 && maxBuyAmt < 100000, "maxBuyAmt");
		require( maxSellAmt > 0 && maxSellAmt < 100000, "maxSellAmt");
		require( reconnectInterval >= 1000 && reconnectInterval <= 60000, "reconnectInterval");
		require( orderTimeout >= 1000 && orderTimeout <= 20000, "orderTimeout");
		require( timeout >= 1000 && timeout <= 20000, "timeout");
	}
	
	private void require( boolean v, String parameter) throws Exception {
		if (!v) {
			throw new Exception( String.format( "Config parameter %s is invalid", parameter) );
		}
	}
	


	public Json toJson() throws Exception {
		ArrayList<Object> list = new ArrayList<Object>();
		
		for (Field field : Config.class.getDeclaredFields() ) {
			list.add( field.getName() );
			list.add( field.get( this) );
		}
		
		return Util.toJsonMsg( list.toArray() );
	}

	public Json readBackendConfig(String tabName) throws Exception {
		JSONObject whole = new JSONObject();
		
		Tab tab = NewSheet.getTab( NewSheet.Reflection, "Config");
		ListEntry[] rows = tab.fetchRows(false);  // no formatting
		
		for (ListEntry row : rows) {
			if ("Y".equals( row.getValue("Backend") ) ) {
				String tag = row.getValue( "Tag");
				String val = row.getValue( "Value");
				if (tag != null && val != null) {
					Double dval = tryDouble( val);
					if (dval != null) {
						whole.put( tag, dval); // this causes it to not have quotation marks in the JSON
					}
					else {
						whole.put( tag, val);  // this will have quotation marks
					}
				}
			}
		}
		
		return new Json( whole);
	}
	
	private Double tryDouble(String val) {
		try {
			return Double.valueOf( val);
		}
		catch( Exception e) {
			return null;
		}
	}

	/** Populate google sheet from database. */
	void pullBackendConfig(MySqlConnection database) throws Exception {
		// read from google sheet
		GTable table = new GTable( NewSheet.Reflection, "Backend-config", "Tag", "Value");

		ResultSet res = database.queryNext( "select * from system_configurations");
		for (int i = 1; i <= res.getMetaData().getColumnCount(); i++) {
			table.put( res.getMetaData().getColumnLabel(i), res.getString(i) );
		}

		validateBackendConfig( table);
	}
	
	void pushBackendConfig(MySqlConnection database) throws Exception {
		// read from google sheet
		GTable table = new GTable( NewSheet.Reflection, "Backend-config", "Tag", "Value");

		validateBackendConfig( table);

		// build the sql string
		StringBuilder sql = new StringBuilder( "update system_configurations set ");
		boolean first = true;
		for (Entry<String, String> entry : table.entrySet() ) {
			String tag = entry.getKey();
			String val = entry.getValue();
			
			if (S.isNotNull( tag) && S.isNotNull( val) ) {
				if (!first) {
					sql.append( ",");
				}

				String formatStr = Util.isNumeric( val) ? "%s=%s" : "%s='%s'";
				sql.append( String.format( formatStr, tag, val) );
				first = false;
			}
		}
		
		S.out( sql);
		database.execute( sql.toString() );
	}

	private void validateBackendConfig(GTable t) throws Exception {
		require( t, "min_order_size", 1, 100); 
		require( t, "max_order_size", 0, 20000);
		require( t, "non_kyc_max_order_size", 0, 20000);
		require( t, "price_refresh_interval", 5, 60);
		require( t, "commission", 0, 5);
//		require( t, "buy_commission", 0, 5 );
//		require( t, "sell_commission", 0, 5 );
		require( t, "buy_spread", .001, .05 );
		require( t, "sell_spread", .001, .05 );
		
		require( t.getDouble( "buy_spread") > minBuySpread, "buy_spread");
		require( t.getDouble( "sell_spread") > minSellSpread, "sell_spread");
	}

	private void require(GTable t, String param, double lower, double upper) throws Exception {
		double val = t.getDouble( param);
		require( val >= lower && val <= upper, param);
	}

	public int threads() {
		return 10;
	}

	public void pullFaq(MySqlConnection database) throws Exception {
		//Tab tab = NewSheet.getTab( NewSheet.Reflection, "FAQ");
		//ResultSet res = database.query( "select * from frequently_asked_questions");
		
		GTable table = new GTable( NewSheet.Reflection, "FAQ", "Question", "Answer");

		ResultSet res = database.query( "select * from frequently_asked_questions");
		
		while (res.next() ) {
			table.put( 
					res.getString("question"), 
					res.getString("answer")
			);
		}
	}
	
	public void pushFaq(MySqlConnection db) throws Exception {
		db.startTransaction();
		
		db.delete("delete from frequently_asked_questions");
		
		int id = 1;
		
		Tab tab = NewSheet.getTab( NewSheet.Reflection, "FAQ");
		for (ListEntry row : tab.fetchRows() ) {
			String q = row.getValue("Question");
			String a = row.getValue("Answer");
			
			if (S.isNotNull(q) && S.isNotNull(a) ) {
				db.insert("frequently_asked_questions", id++, q, a, true);
			}
		}
		
		db.commit();
		
	}
}
