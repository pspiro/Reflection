package reflection;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.ArrayList;

import fireblocks.Accounts;
import fireblocks.Busd;
import fireblocks.Fireblocks;
import fireblocks.Rusd;
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
	private String twsOrderHost;  // TWS is listening on this host
	private int twsOrderPort;  // TWS is listening on this port
	private String refApiHost = "0.0.0.0"; // host for RefAPI to listen on
	private int refApiPort = 8383;  // port for RefAPI to listen on
	private long orderTimeout = 7000;  // order timeout in ms
	private long timeout = 7000;  // all other messages timeout 
	private long reconnectInterval = 5000;  // when we lost connection with TWS
	private String postgresUrl;
	private String postgresUser;
	private String postgresPassword;
	private String symbolsTab;  // tab name where symbols are stored
	private String backendConfigTab;
	private String redisHost;
	private int redisPort;
	private double commission;
	private String fireblocksApiKey;
	private String fireblocksPrivateKey;	
	private String mintHtml;
	private String mintBusd;
	private String mintEth;
	private boolean approveAll;  // approve all orders without placing them on the exchange; for paper trading only
	int redisQueryInterval;
	
	// Fireblocks
	private boolean useFireblocks;
	private String rusdAddr;
	private String busdAddr;
	private int rusdDecimals;
	private int busdDecimals;
	private int stockTokenDecimals;
	
	public int rusdDecimals() { return rusdDecimals; }
	public int busdDecimals() { return busdDecimals; }
	public int stockTokenDecimals() { return stockTokenDecimals; }
	
	public boolean useFireblocks() { return useFireblocks; }
	
	public boolean approveAll() { return approveAll; }
	public String redisHost() { return redisHost; }
	public int redisPort() { return redisPort; }

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

	public String refApiHost() { return refApiHost; }
	public int refApiPort() { return refApiPort; }
	public double commission() { return commission; }
	public String rusdAddr() { return rusdAddr; }
	public String busdAddr() { return busdAddr; }

	public void readFromSpreadsheet(String tabName) throws Exception {
		GTable tab = new GTable( NewSheet.Reflection, tabName, "Tag", "Value");
		
		// user experience parameters
		this.buySpread = tab.getDouble( "buySpread");
		this.sellSpread = tab.getDouble( "sellSpread");
		this.minBuySpread = tab.getDouble( "minBuySpread");   // should be changed to read table without formatting. pas
		this.minSellSpread = tab.getDouble( "minSellSpread");
		this.maxBuyAmt = tab.getDouble( "maxBuyAmt");
		this.maxSellAmt = tab.getDouble( "maxSellAmt");

		// TWS connection
		this.twsOrderHost = tab.get( "twsOrderHost");
		this.twsOrderPort = tab.getRequiredInt( "twsOrderPort");
		this.reconnectInterval = tab.getRequiredInt( "reconnectInterval");
		this.orderTimeout = tab.getRequiredInt( "orderTimeout");
		this.timeout = tab.getRequiredInt( "timeout");

		// market data
		this.redisHost = tab.get( "redisHost");
		this.redisPort = tab.getRequiredInt( "redisPort");
		this.redisQueryInterval = tab.getRequiredInt("redisQueryInterval");

		// listen here
		this.refApiHost = tab.getRequiredString( "refApiHost");
		this.refApiPort = tab.getRequiredInt( "refApiPort");
		
		// database
		this.postgresUrl = tab.get( "postgresUrl");
		this.postgresUser = tab.get( "postgresUser");
		this.postgresPassword = tab.get( "postgresPassword");

		// additional data
		this.symbolsTab = tab.getRequiredString( "symbolsTab");
		this.backendConfigTab = tab.get( "backendConfigTab");
		this.commission = tab.getDouble( "commission");
		this.fireblocksApiKey = tab.getRequiredString("fireblocksApiKey"); 
		this.fireblocksPrivateKey = tab.getRequiredString("fireblocksPrivateKey");
		this.mintHtml = tab.getRequiredString("mintHtml");
		this.mintBusd = tab.getRequiredString("mintBusd");
		this.mintEth = tab.getRequiredString("mintEth");
		this.approveAll = tab.getBoolean("approveAll");
		
		// Fireblocks
		this.useFireblocks = tab.getBoolean("useFireblocks");
		if (useFireblocks) {
			this.rusdAddr = tab.getRequiredString("rusdAddr");
			this.busdAddr = tab.getRequiredString("busdAddr");
			this.rusdDecimals = tab.getRequiredInt("rusdDecimals");
			this.busdDecimals = tab.getRequiredInt("busdDecimals");
			this.stockTokenDecimals = tab.getRequiredInt("stockTokenDecimals");
		}
		
		require( buySpread > 0 && buySpread < .05, "buySpread");
		require( sellSpread > 0 && sellSpread <= .021, "sellSpread");  // stated max sell spread of 2% in the White Paper 
		require( minBuySpread > 0 && minBuySpread < .05 && minBuySpread < buySpread, "minBuySpread");
		require( minSellSpread > 0 && minSellSpread < .05 && minSellSpread < sellSpread, "minSellSpread");
		require( maxBuyAmt > 0 && maxBuyAmt < 100000, "maxBuyAmt");
		require( maxSellAmt > 0 && maxSellAmt < 100000, "maxSellAmt");
		require( reconnectInterval >= 1000 && reconnectInterval <= 60000, "reconnectInterval");
		require( orderTimeout >= 1000 && orderTimeout <= 20000, "orderTimeout");
		require( timeout >= 1000 && timeout <= 20000, "timeout");
		require( S.isNotNull( backendConfigTab), "backendConfigTab config is missing" );
		
		// update Fireblocks static keys
		Fireblocks.setKeys( fireblocksApiKey, fireblocksPrivateKey);
	}
	
	private void require( boolean v, String parameter) throws Exception {
		if (!v) {
			throw new Exception( String.format( "Config parameter %s is missing or invalid", parameter) );
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

	/** Populate google sheet from database. */
	void pullBackendConfig(MySqlConnection database) throws Exception {
		Main.require( S.isNotNull( backendConfigTab), RefCode.UNKNOWN, "'backendConfigTab' setting missing from Reflection configuration");
		
		// read from google sheet
		Tab tab = NewSheet.getTab( NewSheet.Reflection, backendConfigTab);

		ResultSet res = database.queryNext( "select * from system_configurations");
		for (int i = 1; i <= res.getMetaData().getColumnCount(); i++) {
			insertOrUpdate( tab, res.getMetaData().getColumnLabel(i), res.getString(i), "", "1");
		}
		
		ResultSet res2 = database.query( "select * from configurations");
		while (res2.next() ) {
			String tag = res2.getString("key");
			String value = res2.getString("value");
			String description = res2.getString("description");
			
			insertOrUpdate( tab, tag, value, description, "2");
		}

		//validateBackendConfig( table);
	}
	
	private void insertOrUpdate(Tab tab, String tag, String value, String description, String type) throws Exception {
		ListEntry row = tab.findRow( "Tag", tag);
		if (row == null) {
			row = tab.newListEntry();
			row.setValue( "Tag", tag);
			row.setValue( "Value", value);
			row.setValue( "Description", description);
			row.setValue( "Type", type);
			row.insert();
		}
		else {
			row.setValue( "Value", value);
			row.setValue( "Description", description);
			row.setValue( "Type", type);
			row.update();
		}
	}

	void pushBackendConfig(MySqlConnection database) throws Exception {
		// read from google sheet
		Tab tab = NewSheet.getTab( NewSheet.Reflection, backendConfigTab);

		//validateBackendConfig( table);

		// build the sql string for type 1 config
		StringBuilder sql = new StringBuilder( "update system_configurations set ");
		boolean first = true;

		for (ListEntry row : tab.fetchRows() ) {
			String tag = row.getValue( "Tag");
			String value = row.getValue( "Value");
			String type = row.getValue("Type");
			
			// don't update database with null values; do that manually if needed
			// this is to protect against wiping out config values by mistake
			if (S.isNull( tag) || S.isNull( value) ) {
				continue;
			}
			
			if ("1".equals( type) ) {
				if (S.isNotNull( tag) && S.isNotNull( value) ) {
					if (!first) {
						sql.append( ",");
					}

					String formatStr = Util.isNumeric( value) ? "%s=%s" : "%s='%s'";
					sql.append( String.format( formatStr, tag, value) );
					first = false;
				}
				
			}
			else if ("2".equals( type) ) {
				String description = row.getValue( "Description");
				insertOrUpdate( database, tag, value, description);
				
			}
			else if (S.isNotNull( tag) ) {
				Main.require( false, RefCode.UNKNOWN, "Invalid type on backend config tab");
			}
		}		

		// update the system_configurations table (type 1)
		S.out( sql);
		database.execute( sql.toString() );
	}

	static String[] configColumnNames = { "key", "value", "description" };

	/** Update configurations table. 
	 * @throws Exception */
	private void insertOrUpdate(MySqlConnection db, String tag, String value, String description) throws Exception {
		ResultSet res = db.query( "select * from configurations where key='%s'", tag);
		if (res.next() ) {
			db.execute( String.format( "update configurations set value='%s', description='%s' where key='%s'",
					Util.dblQ(value), Util.dblQ(description), tag) );
		}
		else {
			db.insert("configurations", configColumnNames, tag, value, description);
		}
	}
	

	private void validateBackendConfig(GTable t) throws Exception {
		require( t, "min_order_size", 1, 100); 
		require( t, "max_order_size", 0, 20000);
		require( t, "non_kyc_max_order_size", 0, 20000);
		require( t, "price_refresh_interval", 5, 60);
		require( t, "commission", 0, 5);
		require( t, "buy_spread", .001, .05 );
		require( t, "sell_spread", .001, .05 );
		
		require( t.getDouble( "buy_spread") > minBuySpread, "buy_spread");
		require( t.getDouble( "sell_spread") > minSellSpread, "sell_spread");
	}

	private void require(GTable t, String param, double lower, double upper) throws Exception {
		double value = t.getDouble( param);
		require( value >= lower && value <= upper, param);
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

	public String symbolsTab() {
		return symbolsTab;
	}

	public String mintHtml() {
		return mintHtml;
	}
	public String mintBusd() {
		return mintBusd;
	}
	public String mintEth() {
		return mintEth;
	}

	/** This causes a dependency that we might not want to have. */
	public Rusd newRusd() {
		return new Rusd( rusdAddr, rusdDecimals, stockTokenDecimals);
	}

	public Busd newBusd() {
		return new Busd( busdAddr, 6);
	}

	public int redisQueryInterval() {
		return redisQueryInterval;
	}
}
