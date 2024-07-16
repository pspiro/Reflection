package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import common.Util.ExRunnable;
import common.Util.ExSupplier;
import fireblocks.Accounts;
import http.MyClient;
import http.MyHttpClient;
import junit.framework.TestCase;
import reflection.Config;
import reflection.RefCode;
import reflection.Stocks;
import tw.util.S;

public class MyTestCase extends TestCase {
	public static String dead = "0x000000000000000000000000000000000000dead";

	static protected Config m_config;
	static protected Accounts accounts = Accounts.instance;
	static protected Stocks stocks = new Stocks();  // you must read the stocks before using this

	protected MyHttpClient cli;  // could probably just change this to static and remove client()	
	
	static {
		try {
			m_config = Config.read();
			assertTrue( !m_config.isProduction() );  // don't even think about it!
			stocks.readFromSheet(m_config);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		S.out( "lkj");
	}
	
	MyHttpClient cli() throws Exception {
		cli = new MyHttpClient("localhost", 8383);
		cli.addHeader("X-Country-Code", "IN");
		return cli;
	}

	public static void startsWith( String expected, String got) {
		assertEquals( expected, Util.left( got, expected.length() ) );
	}
	
	MyHttpClient postOrder( JsonObject obj) throws Exception {
		return cli().post( "/api/order", obj.toString() ); 
	}

	JsonObject postOrderToObj( JsonObject obj) throws Exception {
		return postOrder(obj).readJsonObject();
	}

	String postOrderToId( JsonObject obj) throws Exception {
		return postOrder(obj).readJsonObject().getString("id");
	}

	JsonObject getWorkingLiveOrder(String id) throws Exception {
		JsonArray msgs = getAllLiveOrders(Cookie.wallet).getArray("orders");
		return msgs != null ? msgs.find( "id", id) : null;
	}

	public JsonObject getAllLiveOrders(String address) throws Exception {
		return cli().get("/api/working-orders/" + address)
				.readJsonObject();
	}
	
	JsonObject getLiveMessage(String id) throws Exception {
		JsonArray msgs = getLiveMessages();
		return msgs != null ? msgs.find( "id", id) : null;
	}
	
	JsonArray getLiveMessages() throws Exception {
		return getAllLiveOrders(Cookie.wallet).getArray("messages");
	}

	protected void assert200() throws Exception {
		if (cli.getResponseCode() != 200) {
			S.out( "%s - %s", cli.getRefCode(), cli.getMessage() );
			assertEquals( RefCode.OK, cli.getRefCode() );
			assertEquals( 200, cli.getResponseCode() );
		}
	}
	
	protected void assert400() throws Exception {
		if (cli.getResponseCode() != 400) {
			S.out( "%s - %s", cli.getRefCode(), cli.getMessage() );
		}
		assertEquals( 400, cli.getResponseCode() );
	}
	
	protected void assertNotEquals(String notExpected, String actual) {
		assertTrue( 
				String.format( "Got %s which was not expected", notExpected),
				!notExpected.equals(actual) );
	}
	
	/** Modify a config setting, refresh RefAPI, run some code, restore the setting */
	void modifySetting(String key, String val, ExRunnable run) throws Exception {
		String saved = m_config.getSetting(key);
		try {
			m_config.setSetting(key, val);
			cli().get("/api/?msg=refreshConfig").readString();
			assert200();
			S.out( "Modified config setting '%s' to '%s'", key, val);
			
			run.run();
		}
		finally {
			m_config.setSetting(key, saved);
			cli().get("/api/?msg=refreshConfig").readString();
			assert200();
			S.out( "Restored config setting '%s' to '%s'", key, saved);
		}
	}
	
	public static void assertStartsWith(String expected, Object actual) {
		assertEquals( expected, actual.toString().substring( 0, expected.length() ) );
	}

	/** Wait for HookServer to catch up Exception */
	protected static void waitForRusdBalance(String walletAddr, double bal, boolean lt) throws Exception {
		waitForBalance( walletAddr, m_config.rusdAddr(), bal, lt);
	}

	/** Wait for HookServer to catch up Exception */
	protected static void waitForBalance(String walletAddr, String tokenAddr, double bal, boolean lt) throws Exception {
		waitFor( 120, () -> {
			
			double balance = MyClient.getJson( "http://localhost:8484/hook/get-wallet-map/" + walletAddr)
					.getObjectNN( "positions")
					.getDouble( tokenAddr.toLowerCase() );
			S.out( "waiting for balance (%s) to be %s %s", balance, lt ? "<" : ">", bal);
			return (lt && balance < bal + .01 || !lt && balance > bal - .01);
		});
	}

	/** wait n seconds for supplier to return true, then fail */
	public static void waitFor( int sec, ExSupplier<Boolean> sup) throws Exception {
		int i = Util.waitFor( sec, sup);
		
		if (i >= 1) {
			S.out( "succeeded in %s sec", i);
		}
		else {
			assertTrue( false);
		}
	}

	String str;
	
	/** For for order with uid 'uid' to return message with status filled */
	public String waitForFilled(String uid) throws Exception {
		waitFor( 120, () -> {
			JsonObject ret = getLiveMessage( uid);
			if (ret != null && ret.getString("type").equals( "message") && 
					ret.getString( "status").equals( "Filled") ) {
				str = ret.getString("text");
				return true;
			}
			S.out( ret != null ? ret : "no message for order " + uid);
			return false;
		});
		return str;
	}
	
	protected void shouldFail( ExRunnable run) {
		try {
			run.run();
			assertTrue( false);
		}
		catch( Exception e) {
			S.out( e.getMessage() );
		}
	}

	public static void mintBusd(String wallet, double amt) throws Exception {
		m_config.busd().mint( wallet, amt)
				.waitForHash();
		waitForBalance(wallet, m_config.busd().address(), amt, false); // make sure the new balance will register with the RefAPI
	}
	
	public static void mintRusd(String wallet, double amt) throws Exception {
		if (m_config.rusd().getPosition(wallet) < amt) {
			S.out( "Minting %s RUSD into %s", amt, wallet);
	
			m_config.rusd()
					.sellStockForRusd( wallet, amt, stocks.getAnyStockToken(), 0)
					.waitForHash();
			
			waitForRusdBalance(wallet, amt - .1, false); // make sure the new balance will register with the RefAPI
		}
	}
	
	void failWith(RefCode refCode) throws Exception {
		assertEquals( refCode, cli.getRefCode() );
	}
	
	void failWith(RefCode refCode, String message) throws Exception {
		if (refCode != cli.getRefCode() || !message.equals( cli.getMessage() ) ) {
			S.out( "RefCode got%s   message got=%s", cli.getRefCode(), cli.getMessage() );
		}
		startsWith( message, cli.getMessage() );
		assertEquals( refCode, cli.getRefCode() );
	}
	
}
