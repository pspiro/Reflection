package reflection;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.ib.client.Types.TimeInForce;

import chain.Allow;
import chain.Chain;
import chain.Chains;
import common.Alerts;
import common.SmtpSender;
import common.Util;
import reflection.MySqlConnection.SqlCommand;
import reflection.MySqlConnection.SqlQuery;
import siwe.SiweTransaction;
import tw.google.Auth;
import tw.google.GTable;
import tw.google.NewSheet;
import tw.google.NewSheet.Book;
import tw.google.NewSheet.Book.Tab;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.IStream;
import tw.util.S;
import web3.Busd;
import web3.MoralisServer;
import web3.Rusd;

public abstract class Config {
	protected GTable m_tab;
	
	public abstract boolean isProduction();
		
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
	private int twsOrderClientId;  // connect to TWS w/ this client ID; must be the same each time so we get the right orders
	private int refApiPort;  // port for RefAPI to listen on
	private long orderTimeout = 7000;  // order timeout in ms
	private long timeout = 7000;  // all other messages timeout 
	private long reconnectInterval = 5000;  // when we lost connection with TWS
	private long recentPrice;
	private String postgresUrl;
	private String postgresExtUrl;  // external URL, used by Monitor
	private String postgresUser;
	private String postgresPassword;
	private String backendConfigTab;
	private double commission;
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
	private String m_emailPassword;  // leave this in case we bring back sending email not using google
	private int threads;
	private double fbLookback;
	private String mdsConnection;
	private double minPartialFillPct;  // min pct for partial fills
	private String alertEmail;
	private double maxAutoRedeem;
	private String baseUrl; // used by Monitor program and RefAPI
	private double autoReward; // automatically send users rewards
	private boolean sendTelegram;
	private int maxSummaryEmails;
	private int fbServerPort;
	private int fbPollIingInterval;

	public long recentPrice() { return recentPrice; }
	public Allow allowTrading() { return allowTrading; }
	public int fbPollIingInterval() { return fbPollIingInterval; }
	
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
	public int twsOrderClientId() { return twsOrderClientId; }

	//public String refApiHost() { return refApiHost; }
	public int refApiPort() { return refApiPort; }
	public double commission() { return commission; }
	
	public double minTokenPosition() { return minTokenPosition; }
	public String errorCodesTab() { return errorCodesTab; }  // yes, no, random
	public TimeInForce tif() { return tif; }
	public String fbAdmins() { return fbAdmins; }
	
	/** try first from args and then from config.txt file in resources folder */
	public static SingleChainConfig read( String[] args) throws Exception {
		return readFrom( getTabName( args) );
	}

	/** takes the prefix */
	public static SingleChainConfig ask(String prefix) throws Exception {
		return readFrom( prefix + "-config");
	}

	/** asks for prefix */
	public static SingleChainConfig ask() throws Exception {
		return readFrom( Util.ask("Enter config tab name prefix") + "-config");
	}

	/** takes tab name from config.txt file */
	public static SingleChainConfig read() throws Exception {
		return readFrom( getTabName( new String[0]) );
	}

	/** takes full tab name */
	public static SingleChainConfig readFrom(String tab) throws Exception {
		SingleChainConfig config = new SingleChainConfig();
		config.readFromSpreadsheet(tab);
		return config;
	}

	/** get tab name from args or config.txt file */
	public static String getTabName(String[] args) throws Exception {
		return args.length > 0 
				? args[0] 
				: IStream.readLine( "config.txt");
	}

	public final void readFromSpreadsheet(Book book, String tabName) throws Exception {
		readFromSpreadsheet( book.getTab(tabName) );
	}

	public final void readFromSpreadsheet(String tabName) throws Exception {
		S.out( "Using config tab %s", tabName);
		readFromSpreadsheet( NewSheet.getTab(NewSheet.Reflection, tabName) );
	}
	
	/** Override this version */
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
		this.twsOrderClientId = m_tab.getInt( "twsOrderClientId");  // if not found, use a random one; okay for Monitor, testing
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
		this.backendConfigTab = m_tab.get( "backendConfigTab");
		this.errorCodesTab = m_tab.get("errorCodesTab");
		this.tif = Util.getEnum(m_tab.getOrDefault("tif", "IOC"), TimeInForce.values() );
		this.allowTrading = Util.getEnum(m_tab.getRequiredString("allowTrading"), Allow.values() );
		this.allowRedemptions = m_tab.getBoolean("allowRedemptions");
		this.nonKycMaxOrderSize = m_tab.getRequiredDouble("non_kyc_max_order_size");
		this.m_emailUsername = m_tab.getRequiredString("emailUsername");
		this.m_emailPassword = m_tab.getRequiredString("emailPassword");
		this.recentPrice = m_tab.getRequiredInt("recentPrice");
		this.threads = m_tab.getRequiredInt("threads");
		this.mdsConnection = m_tab.getRequiredString("mdsConnection");
		this.minPartialFillPct = m_tab.getRequiredDouble("minPartialFillPct");
		this.alertEmail = m_tab.getRequiredString("alertEmail");
		this.maxAutoRedeem = m_tab.getRequiredDouble("maxAutoRedeem");
		this.baseUrl = m_tab.get("baseUrl");
		this.autoReward = m_tab.getDouble("autoReward");
		this.sendTelegram = m_tab.getBoolean( "sendTelegram");
		this.maxSummaryEmails = m_tab.getInt( "maxSummaryEmails");
				
		// siwe config items
		this.siweTimeout = m_tab.getRequiredInt("siweTimeout");
		this.sessionTimeout = m_tab.getRequiredInt("sessionTimeout");
		SiweTransaction.setTimeouts( siweTimeout, sessionTimeout);
		
		Alerts.setEmail( this.alertEmail);
		
		require( buySpread > 0 && buySpread < .05, "buySpread");
		require( sellSpread > 0 && sellSpread <= .021, "sellSpread");  // stated max sell spread of 2% in the White Paper 
		require( minBuySpread > 0 && minBuySpread < .05 && minBuySpread < buySpread, "minBuySpread");
		require( minSellSpread > 0 && minSellSpread < .05 && minSellSpread < sellSpread, "minSellSpread");
		require( minOrderSize > 0 && minOrderSize <= 100, "minOrderSize");
		require( maxOrderSize > 0 && maxOrderSize <= 100000, "maxOrderSize");
		require( reconnectInterval >= 1000 && reconnectInterval <= 60000, "reconnectInterval");
		require( orderTimeout >= 1000 && orderTimeout <= 60000, "orderTimeout");
		require( timeout >= 1000 && timeout <= 20000, "timeout");
		require( S.isNotNull( backendConfigTab), "backendConfigTab" );
		require( tif == TimeInForce.DAY || tif == TimeInForce.IOC, "TIF");
		//require( !isPulseChain() || faucetAmt > 0, "faucetAmt");
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

	/** email template; replace %text with html or plain text */
	public static final String template = """
		<div style="margin: 0px; padding: 6px; background-color: #d3caee; font-family: Arial, sans-serif; border-radius: 6px;">
		<div style="margin: 0px auto; padding: 10px; background-color: #ffff; border-radius: 6px; max-width: 600px; min-height: 200px">
		<div style="text-align: center;"><img src="https://www.jotform.com/uploads/peter_peter662/form_files/Logo%201.6644b6589be269.57034100.png" alt="" width="253" height="60" /></div>
		<div style="margin:10px; font-size:16px">		
		%text
		</div>
		</div>
		</div>
		"""; 

// footer with horz line and address; good for marketing email but not regular comm.
//	<hr />
//	<p style="text-align: center; font-size: xx-small">Reflection.Trading Inc<br />6th Floor, Water&rsquo;s Edge Building 1<br />Wickham&rsquo;s Cay II, Road Town<br />Tortola, British Virgin Islands</p>

	/** don't throw an exception; it's usually not critical */
	public void sendEmail(String to, String subject, String html) {
		Util.wrap( () -> {
			Auth.auth().getMail().send(
					"Reflection", 
					m_emailUsername,  // must be a valid "from" address in gmail; display name is supported 
					to, 
					subject, 
					template.replace( "%text", html), 
					true);
		});
	}
	
	public void sendEmailSes(String to, String subject, String html, SmtpSender.Type type) {
		Util.wrap( () -> {
			SmtpSender.Ses.send(
					"Reflection",
					"josh@reflection.trading", 
					to, 
					subject, 
					template.replace( "%text", html), 
					type);
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

	public String getTabName() {
		return m_tab.tabName();
	}

	/** RefAPI uses internal url; Monitor and java programs use external url 
	 * @throws Exception */ 
	public void useExternalDbUrl() throws Exception {
		require( S.isNotNull(postgresExtUrl), "No external URL set");
		postgresUrl = postgresExtUrl;
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
	
	public double maxAutoRedeem() {
		return maxAutoRedeem;
	}
	
	public String baseUrl() {
		return baseUrl;
	}
		
	public double autoReward() {
		return autoReward;
	}
	
	public boolean sendTelegram() {
		return sendTelegram;
	}
	
	public void log(JsonObject obj) throws Exception {
		sqlCommand( conn -> conn.insertJson( "log", obj) );
	}
	
	public int maxSummaryEmails() {
		return maxSummaryEmails;
	}
	
	/** return completed transactions from the database for a single wallet 
	 * @throws Exception */
	public JsonArray getCompletedTransactions(String wallet) throws Exception {
		return sqlQuery( """ 
				select * from transactions
				where wallet_public_key = '%s' and status = 'COMPLETED'
				order by created_at""", wallet.toLowerCase() );
	}
	
	
	/** Used by RefAPI and OnrampServer */
	public static class MultiChainConfig extends Config {
		protected final Chains chains = new Chains();
		private Chain defaultChain; // temporary, for upgrade only; remove after upgrade

		/** read blockchain table from Reflection/Blockchain tab */
		protected void readFromSpreadsheet(Tab tab) throws Exception {
			super.readFromSpreadsheet(tab);
			
			String[] names = m_tab.getRequiredString( "chains").split( ",");
			chains.read( names, true);
			
			defaultChain = chains.get( m_tab.getRequiredInt( "defaultChainId") ); // temporary, for upgrade only; remove after upgrade
			require( defaultChain != null, "defaultChainId");
		}
		
		/** this chain is returned if the frontend does not pass chainId with the message */
		public Chain defaultChain() {
			return defaultChain;
		}
		
		public Chains chains() {
			return chains;
		}
		
		public Chain getChain( int id) {
			Chain chain = chains.get( id);
			if (chain == null) {
				throw new RuntimeException( "Error: invalid chainId " + id);
			}
			return chain;
		}
		
		public Rusd rusd( int chainId) throws Exception {
			return getChain( chainId).rusd();
		}

		public Busd busd(int chainId) {
			return getChain( chainId).busd();
		}
		
		public boolean isProduction() {  // probably need a separate setting for this
			return true;  
		}

		/* check for stale mkt data in production but not test */ 
		public boolean checkStaleData() {
			return chains().size() > 1;
		}
	}
	

}
