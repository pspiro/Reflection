package reflection;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.simple.JsonArray;

import com.ib.client.Types.TimeInForce;

import common.Alerts;
import common.Util;
import fireblocks.Accounts;
import fireblocks.FbBusd;
import fireblocks.FbMatic;
import fireblocks.FbRusd;
import fireblocks.Fireblocks;
import junit.framework.TestCase;
import positions.MoralisServer;
import redis.ConfigBase;
import refblocks.RbBusd;
import refblocks.RbMatic;
import refblocks.RbRusd;
import refblocks.Refblocks;
import reflection.MySqlConnection.SqlCommand;
import reflection.MySqlConnection.SqlQuery;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.google.NewSheet.Book;
import tw.google.NewSheet.Book.Tab;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.google.Secret;
import tw.util.S;
import web3.Busd;
import web3.Busd.IBusd;
import web3.Matic;
import web3.RetVal;
import web3.Rusd;
import web3.Rusd.IRusd;

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
	private String baseUrl; // used by Monitor program and RefAPI
	private String hookNameSuffix;
	private int chainId;

	// Fireblocks
	private String admin1Addr;
//	private String admin1Key;
	private String ownerKey;  // for Fireblocks, this is "Owner"
	private String refWalletAddr;
	private String refWalletKey;

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
		this.hookNameSuffix = m_tab.getRequiredString("hookNameSuffix");
		this.baseUrl = m_tab.get("baseUrl");
		this.refWalletAddr = m_tab.getRequiredString("refWalletAddr");
		this.refWalletKey = m_tab.getRequiredString("refWalletKey"); // this is used only for deployment and doesn't need to be in the config file
		this.ownerKey = m_tab.getRequiredString("ownerKey"); // this is used only for deployment and testing and doesn't need to be in the config file
		this.chainId = m_tab.getRequiredInt( "chainId");
		
		Alerts.setEmail( this.alertEmail);
		
		// Fireblocks
		this.platformBase = m_tab.getRequiredString("platformBase");
		
		this.web3Type = Util.getEnum( m_tab.getRequiredString( "web3type"), Web3Type.values() );
		
		IRusd rusdCore;
		IBusd busdCore;
		
		if (web3Type == Web3Type.Fireblocks) {
			rusdCore = new FbRusd(
					m_tab.getRequiredString("rusdAddr").toLowerCase(),
					m_tab.getRequiredInt("rusdDecimals") );

			busdCore = new FbBusd(
					m_tab.getRequiredString("busdAddr").toLowerCase(),
					m_tab.getRequiredInt("busdDecimals"),
					m_tab.getRequiredString("busdName") );
			
			this.fireblocksApiKey = m_tab.getRequiredString("fireblocksApiKey"); 
			this.fireblocksPrivateKey = m_tab.getRequiredString("fireblocksPrivateKey");
			this.fbServerPort = m_tab.getRequiredInt("fbServerPort");
			this.fbPollIingInterval = m_tab.getRequiredInt("fbPollIingInterval");
			this.fbAdmins = m_tab.getRequiredString("fbAdmins");
			this.fbStablecoin = m_tab.get("fbStablecoin");
			
			// the fireblocks keys could contain the actual keys, or they could
			// contain the paths to the google secrets containing the keys
			if (fireblocksApiKey.startsWith("projects/") ) {
				fireblocksApiKey = Secret.readValue( fireblocksApiKey);
				fireblocksPrivateKey = Secret.readValue( fireblocksPrivateKey);
			}

			// update Fireblocks static keys and admins
			Fireblocks.setKeys( fireblocksApiKey, fireblocksPrivateKey, platformBase);
			Accounts.instance.setAdmins( fbAdmins);
		}
		else {
			admin1Addr = m_tab.getRequiredString("admin1Addr");
			
			rusdCore = new RbRusd(
					m_tab.getRequiredString("rusdAddr").toLowerCase(),
					m_tab.getRequiredInt("rusdDecimals") );

			busdCore = new RbBusd(
					m_tab.getRequiredString("busdAddr").toLowerCase(),
					m_tab.getRequiredInt("busdDecimals"),
					m_tab.getRequiredString("busdName") );
			
			Refblocks.setChainId( 
					chainId,
					m_tab.getRequiredString( "rpcUrl") );
		}

		m_rusd = new web3.Rusd(
				m_tab.getRequiredString("rusdAddr").toLowerCase(),
				m_tab.getRequiredInt("rusdDecimals"),
				m_tab.getRequiredString( "admin1Key"),  // need to pass all FB related stuff here OR set it on the Fireblocks object
				rusdCore);

		m_busd = new Busd( 
				m_tab.getRequiredString("busdAddr").toLowerCase(),
				m_tab.getRequiredInt("busdDecimals"),
				m_tab.getRequiredString ("busdName"),
				m_tab.getRequiredString( "admin1Key"),
				busdCore);

		// update Moralis chain
		this.moralisPlatform = m_tab.getRequiredString("moralisPlatform").toLowerCase();
		MoralisServer.setChain( moralisPlatform);

		if (isProduction() ) {
			this.blockchainExplorer = m_tab.getRequiredString("blockchainExpl");
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
		m_tab.put( "busdAddr", address);
	}

	static class RefApiConfig extends Config {
		
		public void readFromSpreadsheet(String tabName) throws Exception {
			super.readFromSpreadsheet(tabName);
		}
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
		try ( MySqlConnection conn = createConnection() ) {
			return conn.queryToJson(sql, params);
		}
	}

	/** @deprecated use one of the others */
	public JsonArray sqlQuery(SqlQuery query) throws Exception {
		try ( MySqlConnection conn = createConnection() ) {
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
	public void sendEmail(String to, String subject, String text) {
		Util.wrap( () -> {
			String emailFmt = """ 
				<div style="margin: 0px; padding: 1px; background-color: #D3CAEE; font-family: Arial, sans-serif; border-radius: 6px;">
				<div style="margin: 10px auto; padding: 20px; background-color: #ffff; border-radius: 6px; max-width: 600px;">
				%s
				</div></div> """;
			String html = String.format( emailFmt, text);
			Util.sendEmail(m_emailUsername, m_emailPassword, "Reflection", to, subject, html, true);
		});
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
	
	public static Config ask(String prefix) throws Exception {
		return readFrom( prefix + "-config");
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
	
	public String blockchainTx(String hash) {
		return String.format( "%s/tx/%s", blockchainExplorer, hash);
	}

	public String blockchainAddress(String address) {
		return String.format( "%s/address/%s", blockchainExplorer, address);
	}

	public String baseUrl() {
		return baseUrl;
	}
	
	public String getHookNameSuffix() {
		return hookNameSuffix;
	}
	
	public String admin1Addr() {
		return admin1Addr;
	}

	public String refWalletAddr() {
		return refWalletAddr;
	}
	
	public String refWalletKey() {
		return refWalletKey;
	}
	
	public String ownerKey() {  // private key or "Owner"
		return ownerKey;
	}

	public RetVal mintBusd(String wallet, double amt) throws Exception {
		return busd().mint( wallet, amt);
	}
	
	public Matic matic() {
		return web3Type == Web3Type.Fireblocks ? new FbMatic() : new RbMatic();
	}
	
	public int chainId() {
		return chainId;
	}
}
