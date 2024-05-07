package positions;

import static reflection.Main.require;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import fireblocks.MyServer;
import http.BaseTransaction;
import http.MyClient;
import reflection.Config;
import reflection.RefCode;
import reflection.Stocks;
import test.MyTimer;
import tw.util.S;


/** HookServer tracks the balances for all contract, including both stock
 *  tokens and stablecoins. First we query for the current balance, then
 *  we listen for updates from Moralis via a WebHook server. 

 *  There are three types of updates from Moralis: token transfers, native 
 *  token transfers, and approvals
 *  
 *  You can use grok to create a tcp/ip tunnel to receive the webhook messages 
 */
public class HookServer {
	static double ten18 = Math.pow(10, 18);
	final static double small = .0001;    // positions less than this will not be reported
	final Config m_config = new Config();
	final Stocks stocks = new Stocks();
	String[] m_allContracts;  // list of contract for which we want to request and monitor position; all stocks plus BUSD and RUSD
	String m_transferStreamId;
	static final long m_started = System.currentTimeMillis(); // timestamp that app was started

	/** Map wallet, lower case to HookWallet */ 
	final Map<String,HookWallet> m_hookMap = new ConcurrentHashMap<>();
	String chain() { return Util.toHex( m_config.chainId() ); }
	
	public static void main(String[] args) {
		try {
			Thread.currentThread().setName("Hook");
			S.out( "Starting HookServer");
			
			if (args.length == 0) {
				throw new Exception( "You must specify a config tab name");
			}

			new HookServer().run(args[0]);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(2);  // we need this because listening on the port will keep the app alive
		}
	}
	
	void run(String tabName) throws Exception {
		MyClient.filename = "hookserver.http.log";
		m_config.readFromSpreadsheet(tabName);
		stocks.readFromSheet( m_config);
		BaseTransaction.setDebug( true);  // just temporary
		
		// build list of all contracts that we want to listen for ERC20 transfers
		ArrayList<String> list = new ArrayList<>();  // keep a list as array for speed
		list.addAll( Arrays.asList( stocks.getAllContractsAddresses() ) );
		list.add( m_config.busd().address() );
		list.add( m_config.rusd().address() );
		m_allContracts = list.toArray( new String[list.size()]);

		MyServer.listen( m_config.hookServerPort(), 10, server -> {
			server.createContext("/hook/webhook", exch -> new Trans(exch, false).handleWebhook() );
			server.createContext("/hook/get-wallet", exch -> new Trans(exch, false).handleGetWallet() );
			server.createContext("/hook/get-wallet-map", exch -> new Trans(exch, false).handleGetWalletMap() );
			server.createContext("/hook/get-all-wallets", exch -> new Trans(exch, false).handleGetAllWallets() );
			server.createContext("/hook/mywallet", exch -> new Trans(exch, false).handleMyWallet() );
			server.createContext("/api/mywallet", exch -> new Trans(exch, false).handleMyWallet() );
			server.createContext("/hook/reset", exch -> new Trans(exch, false).handleReset() );
			server.createContext("/hook/reset-all", exch -> new Trans(exch, false).handleResetAll() );

			server.createContext("/hook/ok", exch -> new BaseTransaction(exch, false).respondOk() ); 
			server.createContext("/hook/status", exch -> new Trans(exch, false).handleStatus() ); 
			server.createContext("/hook/debug-on", exch -> new BaseTransaction(exch, true).handleDebug(true) ); 
			server.createContext("/hook/debug-off", exch -> new BaseTransaction(exch, true).handleDebug(false) );
			
			server.createContext("/", exch -> new Trans(exch, false).respondOk() ); // respond 200 here, I don't want to spook the Moralis client
		});

		// listen for ERC20 transfers and native transfers 
		m_transferStreamId = Streams.createStream(
						Streams.erc20Transfers, 
						"transfer-" + m_config.getHookNameSuffix(), 
						m_config.hookServerUrl(),
						chain() ); 

		// listen for "approve" transactions
		// you could pass BUSD, RUSD, or the user addresses
		// it would be ideal if there were a way to combine these two streams into one,
		// then it could just work off the user address, same as the transfer stream
		Streams.createStream(
						Streams.approval, 
						"approval-" + m_config.getHookNameSuffix(), 
						m_config.hookServerUrl(), 
						chain(),
						m_config.rusd().address() );
		
		S.out( "**ready**");
	}

	class Trans extends BaseTransaction {
		public Trans(HttpExchange exchange, boolean debug) {
			super(exchange, debug);
		}

		public void handleStatus() {
			wrap( () -> {
				respond( Util.toJson( code, RefCode.OK, "started", m_started, "suffix", m_config.getHookNameSuffix() ) );
			});
		}

		/** Returns wallet lower case */
		public String getWalletFromUri() throws Exception {
			String wallet = Util.getLastToken(m_uri, "/");
			require( Util.isValidAddress(wallet), RefCode.INVALID_REQUEST, "Wallet address is invalid");
			return wallet.toLowerCase();
		}
		
		/** Return all data about the requested wallet; if we don't have the data already,
		 *  we will request it; called by RefAPI to get positions for My Reflection panel on Dashboard 
		 *  tags are: native, approved, positions, wallet
		 *  positions is an array with fields address, position */ 
		public void handleGetWallet() {
			wrap( () -> {
				respond( getOrCreateHookWallet( getWalletFromUri() ).getAllJson( m_config.minTokenPosition() ) );
			});
		}

		/** tags are: native, approved, positions, wallet 
		 *  positions is an object key address, position */ 
		public void handleGetWalletMap() {
			wrap( () -> {
				respond( getOrCreateHookWallet( getWalletFromUri() ).getAllJsonMap( m_config.minTokenPosition() ) );
			});
		}

		/** Return all wallets that we currently have in the map */
		public void handleGetAllWallets() {
			wrap( () -> {
				JsonArray ar = new JsonArray();
				Util.forEach( m_hookMap.values(), hookWallet -> ar.add( hookWallet.getAllJson(m_config.minTokenPosition() ) ) );
				respond( ar);
			});
		}
		
		public void handleMyWallet() {
			wrap( () -> {
				String walletAddr = getWalletFromUri();
				
				HookWallet hookWallet = getOrCreateHookWallet( walletAddr);
				
				// RUSD
				JsonObject rusd = new JsonObject();
				rusd.put( "name", "RUSD");
				rusd.put( "balance", hookWallet.getBalance( m_config.rusdAddr() ) );
				rusd.put( "tooltip", m_config.getTooltip(Config.Tooltip.rusdBalance) );
				rusd.put( "buttonTooltip", m_config.getTooltip(Config.Tooltip.redeemButton) );
				
				double approved = hookWallet.getAllowance(m_config.busd(), walletAddr, m_config.rusdAddr() );
				approved = Math.min(1000000, approved);  // fix a display issue where some users approved a huge size by mistake

				// BUSD
				JsonObject busd = new JsonObject();
				busd.put( "name", m_config.busd().name() );
				busd.put( "balance", hookWallet.getBalance( m_config.busd().address() ) );
				busd.put( "tooltip", m_config.getTooltip(Config.Tooltip.busdBalance) );
				busd.put( "buttonTooltip", m_config.getTooltip(Config.Tooltip.approveButton) );
				busd.put( "approvedBalance", approved);
				busd.put( "stablecoin", true);

				// native token (e.g. MATIC)
				JsonObject base = new JsonObject();
				base.put( "name", m_config.nativeTokName() );
				base.put( "balance", hookWallet.getNativeBalance() );
				base.put( "tooltip", m_config.getTooltip(Config.Tooltip.baseBalance) );
				
				JsonArray ar = new JsonArray();
				ar.add(rusd);
				ar.add(busd);
				ar.add(base);
				
				JsonObject obj = new JsonObject();
				obj.put( "refresh", m_config.myWalletRefresh() );
				obj.put( "tokens", ar);
				respond(obj);
			});
		}
		
		void handleWebhook() {
			wrap( () -> {
				if (m_exchange.getRequestBody().available() == 0) {
					S.out( "  (no data)");
				}
				else {
					handleHookWithData();
				}
				respondOk();
			});
		}
		
		private void handleHookWithData() throws Exception {
			JsonObject obj = parseToObject();

			String tag = obj.getString("tag");
			boolean confirmed = obj.getBool("confirmed");

			if (BaseTransaction.debug() ) {
				S.out( "Received hook [%s - %s] %s", tag, confirmed, obj);  // change this to debug mode only
			}
			
			// process native transactions
			for (JsonObject trans : obj.getArray("txs" ) ) {
				double amt = trans.getDouble("value") / ten18;

				if (amt != 0) {
					String from = trans.getString("fromAddress").toLowerCase(); 
					String to = trans.getString("toAddress").toLowerCase();

					S.out( "  %s %s was transferred from %s to %s", 
							amt, m_config.nativeTokName(), from, to);

					adjustNativeBalance( from, -amt, confirmed);
					adjustNativeBalance( to, amt, confirmed);
				}
			}
			
			// process approvals
			// NOTE: we get ALL approvals for USDT--and there are many, like every second
			for (JsonObject trans : obj.getArray("erc20Approvals") ) {
				String contract = trans.getString("contract");
				
				if (contract.equalsIgnoreCase(m_config.busd().address() ) ) {
					String owner = trans.getString("owner");
					String spender = trans.getString("spender");
					double amt = trans.getDouble("valueWithDecimals");

					Util.lookup( m_hookMap.get(owner), hookWallet -> {
						S.out( "  %s can spend %s %s on behalf of %s",
								spender, "" + amt, contract, owner);  // use java formatting for amt which can be huge
						hookWallet.approved( amt);	
					});
				}
			}
			
			// process ERC20 transfers
			for (JsonObject trans : obj.getArray("erc20Transfers") ) {
				double amt = trans.getDouble("valueWithDecimals");

				if (amt != 0) {
					String contract = trans.getString("contract").toLowerCase();
					String from = trans.getString("from").toLowerCase(); 
					String to = trans.getString("to").toLowerCase();

					S.out( "  %s %s was transferred from %s to %s",
							amt, contract, from, to);

					// "confirmed" is our signal that something changed recently,
					// it is completely done and we should re-query;
					// one loophole is if two transactions are entered before the
					// first one is confirmed
					adjustTokenBalance( from, contract, -amt, confirmed);
					adjustTokenBalance( to, contract, amt, confirmed);
				}
			};
		}

		/** Reset one wallet; this will cause HookServer to re-query for all 
		 *  wallet values next time a request comes in for that wallet;
		 *  called by Monitor; should be called if a wrong value is being reported */
		public void handleReset() {
			wrap( () -> {
				String walletAddr = getWalletFromUri();
				m_hookMap.remove( walletAddr.toLowerCase() );
				S.out( "Removed %s from map");
				respondOk();
			});
		}

		/** Reset all wallets; this will cause HookServer to re-query for all 
		 *  wallet values next time a request comes in for any wallet;
		 *  called by Monitor; should be called if a wrong values are being reported */
		public void handleResetAll() {
			wrap( () -> {
				m_hookMap.clear();
				S.out( "Cleared map");
				respondOk();
			});
		}

		private void adjustTokenBalance(String wallet, String contract, double amt, boolean confirmed) throws Exception {
			Util.lookup( m_hookMap.get(wallet), hookWallet -> hookWallet.adjustERC20( contract, amt, confirmed) );

			// if no hookWallet found, it means we are not yet tracking the positions
			// for this wallet, and we would query all positions if a request comes in
		}
		
		private void adjustNativeBalance( String wallet, double amt, boolean confirmed) throws Exception {
			Util.lookup( m_hookMap.get(wallet), hookWallet -> hookWallet.adjustNative( amt, confirmed) );
			
			// if no hookWallet found, it means we are not yet tracking the positions
			// for this wallet, and we would query all positions if a request comes in
		}
	}
	
	/** Note that the entire call is synced on m_hookMap, which means it will
	 *  block other calls while it creates the wallet */
	private HookWallet getOrCreateHookWallet(String walletAddr) throws Exception {
		return Util.getOrCreateEx(m_hookMap, walletAddr, () -> {
			MyTimer t = new MyTimer();
			t.next( "Creating HookWallet for %s", walletAddr);
			
			// query ERC20 position map
			HashMap<String, Double> positions = new Wallet( walletAddr)
					.reqPositionsMap(m_allContracts);
			
			// query allowance
			double approved = m_config.busd().getAllowance(walletAddr, m_config.rusdAddr() );
			
			// query native balance
			double nativeBal = MoralisServer.getNativeBalance( walletAddr);
			Util.require( S.isNotNull( m_transferStreamId), "Cannot handle requests until transferStreamId is set");  // this can happen if we receive events from the old stream before the new stream is created
			Streams.addAddressToStream( m_transferStreamId, walletAddr);  // watch all transfers for this wallet so we can see the MATIC transfers 
			
			t.done();
			
			return new HookWallet( walletAddr, positions, approved, nativeBal);
		});
	}

}

/** This could be improved so that you always get the current balance back with the web hook */
//void createNativeStream() throws Exception {
//	S.out( "***Creating native stream***");
//	nativeStreamId = Streams.createStreamWithAddresses(
//			String.format( Streams.nativeTrans, chain(), m_config.hookServerUrl(), chain() ) );
//	S.out( "  created native stream with id %s" + nativeStreamId);
//}
