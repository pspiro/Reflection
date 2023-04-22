package reflection;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.json.simple.JSONObject;

import fireblocks.Busd;
import fireblocks.Fireblocks;
import fireblocks.Rusd;
import junit.framework.TestCase;
import redis.clients.jedis.Jedis;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.google.Secret;
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
	private String mintHtml;
	private String mintBusd;
	private String mintEth;
	private boolean autoFill;  // approve all orders without placing them on the exchange; for paper trading only
	private int redisQueryInterval;
	private double minTokenPosition; // minimum token position to display in portfolio section
	private int siweTimeout; // max time between issuedAt field and now
	private int sessionTimeout; // session times out after this amount of inactivity
	private boolean produceErrors;
	
	// Fireblocks
	protected boolean useFireblocks;
	private String fireblocksApiKey;
	private String fireblocksPrivateKey;
	private String moralisPlatform;
	private String platformBase;
	protected String rusdAddr;
	private String busdAddr;
	private int rusdDecimals;
	private int busdDecimals;
	private GTable m_tab;
	
	public int rusdDecimals() { return rusdDecimals; }
	public int busdDecimals() { return busdDecimals; }
	
	public boolean useFireblocks() { return useFireblocks; }
	
	public boolean autoFill() { return autoFill; }
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
	public double minTokenPosition() { return minTokenPosition; }
	public boolean produceErrors() { return produceErrors; }
	
	public static Config readFrom(String tab) throws Exception {
		Config config = new Config();
		config.readFromSpreadsheet("Desktop-config");
		return config;
	}

	public void readFromSpreadsheet(String tabName) throws Exception {
		m_tab = new GTable( NewSheet.Reflection, tabName, "Tag", "Value");
		
		// user experience parameters
		this.buySpread = m_tab.getDouble( "buySpread");
		this.sellSpread = m_tab.getDouble( "sellSpread");
		this.minBuySpread = m_tab.getDouble( "minBuySpread");   // should be changed to read table without formatting. pas
		this.minSellSpread = m_tab.getDouble( "minSellSpread");
		this.maxBuyAmt = m_tab.getDouble( "maxBuyAmt");
		this.maxSellAmt = m_tab.getDouble( "maxSellAmt");
		this.minTokenPosition = m_tab.getDouble("minTokenPosition");

		// TWS connection
		this.twsOrderHost = m_tab.get( "twsOrderHost");
		this.twsOrderPort = m_tab.getRequiredInt( "twsOrderPort");
		this.reconnectInterval = m_tab.getRequiredInt( "reconnectInterval");
		this.orderTimeout = m_tab.getRequiredInt( "orderTimeout");
		this.timeout = m_tab.getRequiredInt( "timeout");

		// market data
		this.redisHost = m_tab.get( "redisHost");
		this.redisPort = m_tab.getRequiredInt( "redisPort");
		this.redisQueryInterval = m_tab.getRequiredInt("redisQueryInterval");

		// listen here
		this.refApiHost = m_tab.getRequiredString( "refApiHost");
		this.refApiPort = m_tab.getRequiredInt( "refApiPort");
		
		// database
		this.postgresUrl = m_tab.get( "postgresUrl");
		this.postgresUser = m_tab.get( "postgresUser");
		this.postgresPassword = m_tab.get( "postgresPassword");

		// additional data
		this.symbolsTab = m_tab.getRequiredString( "symbolsTab");
		this.backendConfigTab = m_tab.get( "backendConfigTab");
		this.commission = m_tab.getDouble( "commission");
		this.mintHtml = m_tab.getRequiredString("mintHtml");
		this.mintBusd = m_tab.getRequiredString("mintBusd");
		this.mintEth = m_tab.getRequiredString("mintEth");
		this.autoFill = m_tab.getBoolean("autoFill");
		this.siweTimeout = m_tab.getRequiredInt("siweTimeout");
		this.sessionTimeout = m_tab.getRequiredInt("sessionTimeout");
		this.produceErrors = m_tab.getBoolean("produceErrors");
		
		// Fireblocks
		this.useFireblocks = m_tab.getBoolean("useFireblocks");
		if (useFireblocks) {
			this.rusdAddr = m_tab.getRequiredString("rusdAddr"); 
			this.busdAddr = m_tab.getRequiredString("busdAddr");
			this.platformBase = m_tab.getRequiredString("platformBase");
			this.moralisPlatform = m_tab.getRequiredString("moralisPlatform");
			this.rusdDecimals = m_tab.getRequiredInt("rusdDecimals");
			this.busdDecimals = m_tab.getRequiredInt("busdDecimals");
			this.fireblocksApiKey = m_tab.getRequiredString("fireblocksApiKey"); 
			this.fireblocksPrivateKey = m_tab.getRequiredString("fireblocksPrivateKey");

			// the fireblocks keys could contain the actual keys, or they could
			// contain the paths to the google secrets containing the keys
			if (fireblocksApiKey.startsWith("projects/") ) {
				fireblocksApiKey = Secret.readValue( fireblocksApiKey);
				fireblocksPrivateKey = Secret.readValue( fireblocksPrivateKey);
			}

			// update Fireblocks static keys
			Fireblocks.setKeys( fireblocksApiKey, fireblocksPrivateKey, platformBase, moralisPlatform);
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
	}
	
	protected void require( boolean v, String parameter) throws Exception {
		if (!v) {
			throw new Exception( String.format( "Config parameter %s is missing or invalid", parameter) );
		}
	}

	public JSONObject toJson() throws Exception {
		ArrayList<Object> list = new ArrayList<Object>();
		
		for (Field field : Config.class.getDeclaredFields() ) {
			Object obj = field.get(this);
			if (obj != null && isPrimitive(obj.getClass()) ) {
				list.add( field.getName() );
				list.add(obj);
			}
		}
		
		return Util.toJsonMsg( list.toArray() );
	}

	private boolean isPrimitive(Class clas) {
		return clas == String.class || clas == Integer.class || clas == Double.class || clas == Long.class;
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
				Main.require( false, RefCode.UNKNOWN, "Invalid value in Type column (missing entry?) on backend config tab");
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

	/** This causes a dependency that we might not want to have. 
	 * @throws Exception */
	public Rusd rusd() throws Exception {
		return new Rusd( rusdAddr, rusdDecimals);
	}

	public Busd busd() {
		return new Busd( busdAddr, busdDecimals);
	}

	public int redisQueryInterval() {
		return redisQueryInterval;
	}
	
	public void setRusdAddress(String address) throws Exception {
		rusdAddr = address;
		m_tab.put( "rusdAddr", rusdAddr);
	}
	
	public void setBusdAddress(String address) {
		busdAddr = address;
		m_tab.put( "busdAddr", busdAddr);
	}

	static class RefApiConfig extends Config {
		
		public void readFromSpreadsheet(String tabName) throws Exception {
			super.readFromSpreadsheet(tabName);
			
			if (useFireblocks) {
				require(S.isNotNull( this.rusdAddr), "rusdAddr");
			}
		}
	}
	
	public void testApiKeys() {
		TestCase.assertTrue( useFireblocks);
		
		TestCase.assertEquals( "bbce654d-08da-2216-5b20-bed4deaad1be", fireblocksApiKey);
		
		TestCase.assertEquals( 
				"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",
				fireblocksPrivateKey);
		
	}

	public void dump() {
		S.out( "dumping config");
		S.out( m_tab);
	}

	public int siweTimeout() {
		return siweTimeout;
	}

	public long sessionTimeout() {
		return sessionTimeout;
	}
	
	Jedis createJedis() {
		return redisPort == 0 ? new Jedis(redisHost) : new Jedis(redisHost, redisPort);
	}
	
	public MySqlConnection sqlConnection() throws SQLException {
		return new MySqlConnection().connect( postgresUrl, postgresUser, postgresPassword );
	}
}
