package testcase;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import chain.Chain;
import common.Util;
import common.Util.ExRunnable;
import common.Util.ExSupplier;
import http.MyHttpClient;
import junit.framework.TestCase;
import reflection.SingleChainConfig;
import tw.util.S;
import web3.NodeInstance;

public class MyTestCase extends TestCase {
	public static String dead = "0x000000000000000000000000000000000000dead";
	public static String prodWallet = "0x2703161D6DD37301CEd98ff717795E14427a462B".toLowerCase();
	
	static protected SingleChainConfig m_config;
	//static protected Stocks stocks;
	static protected int port = 8383;
	protected static Chain chain;

	protected MyHttpClient cli;  // could probably just change this to static and remove client()	
	
	static {
		try {
			m_config = SingleChainConfig.read();  // pull from config.txt
			assertTrue( !m_config.isProduction() ); // don't even think about it!
			chain = m_config.chain();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected static int chainId() {
		return chain.chainId();
	}
	
	MyHttpClient cli() throws Exception {
		cli = new MyHttpClient("localhost", port);
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

	/** for use with messages that return 200 but no RefCode.OK, e.g. get-profile */
	protected void assert200() throws Exception {
		if (cli.getResponseCode() != 200) {
			S.out( "%s - %s", cli.getResponseCode(), cli.readString() );
		}
		assertEquals( 200, cli.getResponseCode() );
	}
	
	protected void assert400() throws Exception {
		S.out( "%s - %s", cli.getResponseCode(), cli.readString() );
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
		assertEquals( expected, Util.left( actual.toString(), expected.length() ) );
	}

	/** Wait for HookServer to catch up Exception */
	protected static void waitForRusdBalance(String walletAddr, double bal, boolean lt) throws Exception {
		waitForBalance( walletAddr, m_config.rusdAddr(), bal, lt);
	}

	/** Wait for HookServer to catch up Exception */
	protected static void waitForBalance(String walletAddr, String tokenAddr, double bal, boolean lt) throws Exception {
		waitFor( 120, () -> {
			double balance = node().getBalance( tokenAddr, walletAddr, 0);
			S.out( "waiting for balance (%s) to be %s %s", balance, lt ? "<=" : ">=", bal);
			return (lt && balance < bal + .01 || !lt && balance > bal - .01);
		});
	}

	/** wait n seconds for supplier to return true, then fail */
	public static void waitFor( int sec, ExSupplier<Boolean> sup) throws Exception {
		for (int i = 0; i < sec; i++) {
			S.out( i);
			if (sup.get() ) {
				S.out( "succeeded in %s seconds", i);
				return;
			}
			S.sleep(1000);
		}
		assertTrue( false);
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
		m_config.mintBusd( wallet, amt)
				.waitForReceipt();
		waitForBalance(wallet, m_config.busd().address(), amt, false); // make sure the new balance will register with the RefAPI
	}
	
	public static void mintRusd(String wallet, double amt) throws Exception {
		if (m_config.rusd().getPosition(wallet) < amt) {
			S.out( "Minting %s RUSD into %s", amt, wallet);
	
			m_config.rusd()
					.sellStockForRusd( wallet, amt, chain.getAnyStockToken(), 0)
					.waitForReceipt();
			
			waitForRusdBalance(wallet, amt - .1, false); // make sure the new balance will register with the RefAPI
		}
	}
	
	static protected Chain chain() {
		return m_config.chain();
	}

	static protected NodeInstance node() {
		return m_config.chain().node();
	}
	
}
