package reflection;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.simple.JsonArray;

import com.ib.client.Types.TimeInForce;

import common.Alerts;
import common.Util;
import fireblocks.Accounts;
import fireblocks.Busd;
import fireblocks.Fireblocks;
import fireblocks.Rusd;
import junit.framework.TestCase;
import positions.MoralisServer;
import redis.ConfigBase;
import reflection.MySqlConnection.SqlCommand;
import reflection.MySqlConnection.SqlQuery;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.google.NewSheet.Book;
import tw.google.NewSheet.Book.Tab;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.google.Secret;
import tw.util.S;

public class Config extends ConfigBase {
	
	protected GTable m_tab;

	// user experience parameters
	private double minOrderSize;  // in dollars
	private double maxOrderSize; // max buy amt in dollars
	private double minBuySpread;  // as pct of price
	private double minSellSpread;  // as pct of price

	// needed by back-end server
	private double buySpread;  // as pct of price
	private double sellSpread;  // as pct of price

	// program parameters
	private double nonKycMaxOrderSize;
	private String twsOrderHost;  // TWS is listening on this host
	private int twsOrderPort;  // TWS is listening on this port
	//private String refApiHost; // not currently used; everyone listens on 0.0.0.0
	private int refApiPort;  // port for RefAPI to listen on
	private long orderTimeout = 7000;  // order timeout in ms
	private long timeout = 7000;  // all other messages timeout 
	private long reconnectInterval = 5000;  // when we lost connection with TWS
	private long recentPrice;
	private String postgresUrl;
	private String postgresExtUrl;  // external URL, used by Monitor
	private String postgresUser;
	private String postgresPassword;
	private String symbolsTab;  // tab name where symbols are stored
	private String backendConfigTab;
	private double commission;
	private boolean autoFill;  // approve all orders without placing them on the exchange; for paper trading only
	private int redisQueryInterval;  // mdserver query interval
	private double minTokenPosition; // minimum token position to display in portfolio section
	private int siweTimeout; // max time between issuedAt field and now
	private int sessionTimeout; // session times out after this amount of inactivity
	private String errorCodesTab;  // valid values are yes, no, random
	private TimeInForce tif;
	private String fbAdmins;
	private Allow allowTrading = Allow.All;  // won't be returned in getConfig message
	private boolean allowRedemptions;
	private String m_emailUsername;
	private String m_emailPassword;
	private int threads;
	private int myWalletRefresh;  // "My Wallet" panel refresh interval
	private double fbLookback;
	private String mdsConnection;
	private double minPartialFillPct;  // min pct for partial fills
	private String alertEmail;
	private String fbStablecoin;
	private String blockchainExplorer;
	private double maxAutoRedeem;
	private int hookServerPort;
	private String hookServerUrl;
	private String hookServerChain;

	// Fireblocks
	protected boolean useFireblocks;
	private String fireblocksApiKey;
	private String fireblocksPrivateKey;
	private String moralisPlatform;  // lower case
	private String platformBase;
	private int fbServerPort;
	private int fbPollIingInterval;
	private Busd m_busd;
	private Rusd m_rusd;

	public long recentPrice() { return recentPrice; }
	public Allow allowTrading() { return allowTrading; }
	public int myWalletRefresh() { return myWalletRefresh; }
	public int fbPollIingInterval() { return fbPollIingInterval; }
	
	public boolean useFireblocks() { return useFireblocks; }
	
	public boolean autoFill() { return autoFill; }

	public double minOrderSize() { return minOrderSize; }
	public double maxOrderSize() { return maxOrderSize; }
	
	public double minSellSpread() { return minSellSpread; }
	public double minBuySpread() { return minBuySpread; }
	
	public String postgresUser() { return postgresUser; }
	public String postgresUrl() { return postgresUrl; }

	public long timeout() {  return timeout;  }
	public long orderTimeout() { return orderTimeout; }
	public long reconnectInterval() { return reconnectInterval; }

	public String twsOrderHost() { return twsOrderHost; }
	public int twsOrderPort() { return twsOrderPort; }

	//public String refApiHost() { return refApiHost; }
	public int refApiPort() { return refApiPort; }
	public double commission() { return commission; }
	
	/** @return RUSD address lower case */
	public String rusdAddr() { 
		return m_rusd.address(); 
	}
	
	public double minTokenPosition() { return minTokenPosition; }
	public String errorCodesTab() { return errorCodesTab; }  // yes, no, random
	public TimeInForce tif() { return tif; }
	public String fbAdmins() { return fbAdmins; }
	
	public static Config readFrom(String tab) throws Exception {
		Config config = new Config();
		config.readFromSpreadsheet(tab);
		S.out( "Using config tab %s", tab);
		return config;
	}

	public void readFromSpreadsheet(Book book, String tabName) throws Exception {
		readFromSpreadsheet( book.getTab(tabName) );
	}

	public void readFromSpreadsheet(String tabName) throws Exception {
		readFromSpreadsheet( NewSheet.getTab(NewSheet.Reflection, tabName) );
	}
	
	protected void readFromSpreadsheet(Tab tab) throws Exception {
		m_tab = new GTable( tab, "Tag", "Value", true);
		
		// user experience parameters
		this.buySpread = m_tab.getRequiredDouble( "buy_spread");
		this.sellSpread = m_tab.getRequiredDouble( "sell_spread");
		this.minBuySpread = m_tab.getRequiredDouble( "minBuySpread");   // should be changed to read table without formatting. pas
		this.minSellSpread = m_tab.getRequiredDouble( "minSellSpread");
		this.minOrderSize = m_tab.getRequiredDouble( "min_order_size");
		this.maxOrderSize = m_tab.getRequiredDouble( "max_order_size");
		this.minTokenPosition = m_tab.getRequiredDouble("minTokenPosition");
		this.commission = m_tab.getRequiredDouble( "commission");

		// TWS connection
		this.twsOrderHost = m_tab.get( "twsOrderHost");
		this.twsOrderPort = m_tab.getRequiredInt( "twsOrderPort");
		this.reconnectInterval = m_tab.getRequiredInt( "reconnectInterval");
		this.orderTimeout = m_tab.getRequiredInt( "orderTimeout");
		this.timeout = m_tab.getRequiredInt( "timeout");

		this.redisQueryInterval = m_tab.getRequiredInt("redisQueryInterval");

		// listen here
		//this.refApiHost = m_tab.getRequiredString( "refApiHost");
		this.refApiPort = m_tab.getRequiredInt( "refApiPort");
		
		// database
		this.postgresUrl = m_tab.get( "postgresUrl");
		this.postgresExtUrl = m_tab.get( "postgresExtUrl");
		this.postgresUser = m_tab.get( "postgresUser");
		this.postgresPassword = m_tab.get( "postgresPassword");

		// additional data
		this.symbolsTab = m_tab.getRequiredString( "symbolsTab");
		this.backendConfigTab = m_tab.get( "backendConfigTab");
		this.autoFill = m_tab.getBoolean("autoFill");
		this.siweTimeout = m_tab.getRequiredInt("siweTimeout");
		this.sessionTimeout = m_tab.getRequiredInt("sessionTimeout");
		this.errorCodesTab = m_tab.get("errorCodesTab");
		this.tif = Util.getEnum(m_tab.getOrDefault("tif", "IOC"), TimeInForce.values() );
		this.allowTrading = Util.getEnum(m_tab.getRequiredString("allowTrading"), Allow.values() );
		this.allowRedemptions = m_tab.getBoolean("allowRedemptions");
		this.nonKycMaxOrderSize = m_tab.getRequiredDouble("non_kyc_max_order_size");
		this.m_emailUsername = m_tab.getRequiredString("emailUsername");
		this.m_emailPassword = m_tab.getRequiredString("emailPassword");
		this.recentPrice = m_tab.getRequiredInt("recentPrice");
		this.threads = m_tab.getRequiredInt("threads");
		this.myWalletRefresh = m_tab.getRequiredInt("myWalletRefresh");
		this.fbLookback = m_tab.getRequiredDouble("fbLookback");
		this.mdsConnection = m_tab.getRequiredString("mdsConnection");
		this.minPartialFillPct = m_tab.getRequiredDouble("minPartialFillPct");
		this.alertEmail = m_tab.getRequiredString("alertEmail");
		this.maxAutoRedeem = m_tab.getRequiredDouble("maxAutoRedeem");
		this.hookServerPort = m_tab.getInt("hookServerPort");
		this.hookServerUrl = m_tab.getRequiredString("hookServerUrl");
		this.hookServerChain = m_tab.getRequiredString("hookServerChain");
		
		Alerts.setEmail( this.alertEmail);
		
		// Fireblocks
		this.platformBase = m_tab.getRequiredString("platformBase");
		this.useFireblocks = m_tab.getBoolean("useFireblocks");
		if (useFireblocks) {
			this.moralisPlatform = m_tab.getRequiredString("moralisPlatform").toLowerCase();
			this.fireblocksApiKey = m_tab.getRequiredString("fireblocksApiKey"); 
			this.fireblocksPrivateKey = m_tab.getRequiredString("fireblocksPrivateKey");
			this.fbServerPort = m_tab.getRequiredInt("fbServerPort");
			this.fbPollIingInterval = m_tab.getRequiredInt("fbPollIingInterval");
			this.fbAdmins = m_tab.getRequiredString("fbAdmins");
			this.fbStablecoin = m_tab.get("fbStablecoin");
			
			m_busd = new Busd( 
					m_tab.getRequiredString("busdAddr").toLowerCase(),
					m_tab.getRequiredInt("busdDecimals"),
					m_tab.getRequiredString ("busdName") );

			m_rusd = new Rusd(
					m_tab.getRequiredString("rusdAddr").toLowerCase(),
					m_tab.getRequiredInt("rusdDecimals") );
			
			// the fireblocks keys could contain the actual keys, or they could
			// contain the paths to the google secrets containing the keys
			if (fireblocksApiKey.startsWith("projects/") ) {
				fireblocksApiKey = Secret.readValue( fireblocksApiKey);
				fireblocksPrivateKey = Secret.readValue( fireblocksPrivateKey);
			}

			// update Fireblocks static keys and admins
			Fireblocks.setKeys( fireblocksApiKey, fireblocksPrivateKey, platformBase);
			Accounts.instance.setAdmins( fbAdmins);
			
			// update Moralis chain
			MoralisServer.chain = moralisPlatform;
		}
		
		if (isProduction() ) {
			this.blockchainExplorer = m_tab.getRequiredString("blockchainExplorer");
		}

		require( !autoFill || !isProduction(), "No auto-fill in production");
		require( buySpread > 0 && buySpread < .05, "buySpread");
		require( sellSpread > 0 && sellSpread <= .021, "sellSpread");  // stated max sell spread of 2% in the White Paper 
		require( minBuySpread > 0 && minBuySpread < .05 && minBuySpread < buySpread, "minBuySpread");
		require( minSellSpread > 0 && minSellSpread < .05 && minSellSpread < sellSpread, "minSellSpread");
		require( minOrderSize > 0 && minOrderSize <= 100, "minOrderSize");
		require( maxOrderSize > 0 && maxOrderSize <= 100000, "maxOrderSize");
		require( reconnectInterval >= 1000 && reconnectInterval <= 60000, "reconnectInterval");
		require( orderTimeout >= 1000 && orderTimeout <= 60000, "orderTimeout");
		require( timeout >= 1000 && timeout <= 20000, "timeout");
		require( S.isNotNull( backendConfigTab), "backendConfigTab config is missing" );
		require( tif == TimeInForce.DAY || tif == TimeInForce.IOC, "TIF is invalid");
	}
	
	protected void require( boolean v, String parameter) throws Exception {
		if (!v) {
			throw new Exception( String.format( "Config parameter %s is missing or invalid", parameter) );
		}
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

	public int threads() {
		return threads;
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
	
	public String symbolsTab() {
		return symbolsTab;
	}

	/** This causes a dependency that we might not want to have. 
	 * @throws Exception */
	public Rusd rusd() throws Exception {
		Util.require( m_rusd != null, "Fireblocks not set");
		return m_rusd;
	}

	public Busd busd() throws Exception {
		Util.require( m_busd != null, "Fireblocks not set");
		return m_busd;
	}

	/** mdserver query interval, called redisQueryInterval in config file */
	public int mdQueryInterval() {
		return redisQueryInterval;
	}
	
	/** Update spreadsheet */
	public void setRusdAddress(String address) throws Exception {
		m_tab.put( "rusdAddr", address);
	}
	
	/** Update spreadsheet */
	public void setBusdAddress(String address) {
		m_tab.put( "busdAddress", address);
	}

	static class RefApiConfig extends Config {
		
		public void readFromSpreadsheet(String tabName) throws Exception {
			super.readFromSpreadsheet(tabName);
			
			if (useFireblocks) {
				require(S.isNotNull( this.rusdAddr()), "rusdAddr");
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

	public MySqlConnection createConnection() throws SQLException {
		MySqlConnection conn = new MySqlConnection();
		conn.connect( postgresUrl, postgresUser, postgresPassword);
		return conn;
	}

	/** Connect, execute a command, then close the connection.
	 *  Since executions is delayed, don't use it to update data
	 *  that will be used by a subsequent operation */
	public void sqlCommand(SqlCommand command) throws Exception {
		try ( MySqlConnection conn = createConnection() ) {
			command.run(conn);
		}
	}

	/** Use this one to make a single query */
	public JsonArray sqlQuery(String sql, Object... params) throws Exception {
		try ( MySqlConnection conn = new MySqlConnection() ) {
			conn.connect( postgresUrl, postgresUser, postgresPassword);
			return conn.queryToJson(sql, params);
		}
	}

	/** Use this one to make multiple queries with a single connection */
	public JsonArray sqlQuery(SqlQuery query) throws Exception {
		try ( MySqlConnection conn = new MySqlConnection() ) {
			conn.connect( postgresUrl, postgresUser, postgresPassword);
			return query.run(conn);
		}
	}
	
	public String backendConfigTab() {
		return backendConfigTab;
	}

	public boolean isProduction() {
		return "polygon".equals(moralisPlatform);  
	}
	
	public double buySpread() {
		return buySpread;
	}

	public double sellSpread() {
		return sellSpread;
	}

	public enum Tooltip {
		rusdBalance,
		busdBalance,
		baseBalance,
		redeemButton,
		approveButton,
	}
	
	public String getTooltip(Tooltip tag) {
		return m_tab.get(tag.toString());
	}

	public int fbServerPort() {
		return fbServerPort;
	}
	
	public boolean allowRedemptions() {
		return allowRedemptions;		
	}
	
	public double nonKycMaxOrderSize() {
		return nonKycMaxOrderSize;
	}
	
	double getRequiredDouble(String key) throws Exception {
		return m_tab.getRequiredDouble(key);
	}

	/** don't throw an exception; it's usually not critical */
	public void sendEmail(String to, String subject, String text, boolean isHtml) {
		Util.wrap( () -> sendEmailEx( to, subject, text, isHtml) );
	}
	
	public void sendEmailEx(String to, String subject, String text, boolean isHtml) throws Exception {
		Util.sendEmail(m_emailUsername, m_emailPassword, "Reflection", to, subject, text, isHtml);
	}
	
	/** Used by test cases */
	public String getSetting(String key) {
		return m_tab.get(key);
	}
	
	/** Used by test cases */
	public void setSetting(String key, String val) {
		m_tab.put(key, val);
	}

	public static Config ask() throws Exception {
		return readFrom( Util.ask("Enter config tab name prefix") + "-config");
	}
	
	public String getTabName() {
		return m_tab.tabName();
	}

	/** RefAPI uses internal url; Monitor and java programs use external url 
	 * @throws Exception */ 
	public Config useExternalDbUrl() throws Exception {
		require( S.isNotNull(postgresExtUrl), "No external URL set");
		postgresUrl = postgresExtUrl;
		return this; // for chaining calls
	}

	public Stocks readStocks() throws Exception {
		Stocks stocks = new Stocks();
		stocks.readFromSheet(this);
		return stocks;
	}

	public double fbLookback() {
		return fbLookback;
	}

	public String mdsConnection() {
		return mdsConnection;
	}
	
	public double minPartialFillPct() {
		return minPartialFillPct;
	}
	
	public String fbStablecoin() {
		return fbStablecoin;
	}
	
	public String blockchainExplorer() {
		return blockchainExplorer;
	}
	
	public double maxAutoRedeem() {
		return maxAutoRedeem;
	}
	
	/** Pull native token from Fireblocks */
	public String nativeTokName() {
		return platformBase.split("_")[0];
	}
	
	public int hookServerPort() {
		return hookServerPort;
	}
	
	public String hookServerUrl() {
		return hookServerUrl;
	}
	
	public String hookServerChain() {
		return hookServerChain;
	}

	/** suffix used when creating Moralis WebHook */ 
	public String getHookNameSuffix() {
		return String.format( "%s-%s", hookServerChain, hookServerPort);
	}

}
