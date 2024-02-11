package positions;

import static reflection.Main.require;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import fireblocks.MyServer;
import http.BaseTransaction;
import reflection.Config;
import reflection.RefCode;
import reflection.Stocks;
import tw.util.S;

// bug: when we received 2, we updated to 2 instead of add
// issue: how to tell if the updates are old or new; you don't
// want to play back old updates

public class HookServer {
	static double ten18 = Math.pow(10, 18);
	final static double small = .0001;    // positions less than this will not be reported
	final HookServerConfig m_config = new HookServerConfig();
	final Stocks stocks = new Stocks();
	String[] m_allContracts;  // query positions and list to ERC20 transfers to all of these
	String nativeStreamId;

	/** Map wallet, lower case to HookWal */ 
	final HashMap<String,HookWallet> m_hookMap = new HashMap<>();
	
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
		m_config.readFromSpreadsheet(tabName);
		stocks.readFromSheet( m_config);
		
		Streams.deleteAll();  // not sure I need this
		
		// build list of all contracts that we want to listen for ERC20 transfers
		ArrayList<String> list = new ArrayList<>();  // keep a list as array for speed
		list.addAll( Arrays.asList( stocks.getAllContractsAddresses() ) );
		list.add( m_config.busd().address() );
		list.add( m_config.rusd().address() );
		m_allContracts = list.toArray( new String[list.size()]);

		MyServer.listen( m_config.hookServerPort(), 10, server -> {
			server.createContext("/hook/webhook", exch -> new Trans(exch, true).handleWebhook() );
			server.createContext("/hook/get-wallet", exch -> new Trans(exch, false).handleGetWallet() );
			server.createContext("/hook/get-all-wallets", exch -> new Trans(exch, false).handleGetAllWallets() );
			server.createContext("/hook/mywallet", exch -> new Trans(exch, false).handleMyWallet() );
			server.createContext("/api/mywallet", exch -> new Trans(exch, false).handleMyWallet() );

			server.createContext("/hook/dump", exch -> new Trans(exch, false).handleDump() );
			server.createContext("/hook/ok", exch -> new BaseTransaction(exch, false).respondOk() ); 
			server.createContext("/hook/debug-on", exch -> new BaseTransaction(exch, true).handleDebug(true) ); 
			server.createContext("/hook/debug-off", exch -> new BaseTransaction(exch, true).handleDebug(false) );
			
			server.createContext("/", exch -> new Trans(exch, false).respondOk() );
		});

		createTransfersStream( m_allContracts);
		createNativeStream();
		createApprovalStream();
	}

	class Trans extends BaseTransaction {
		public Trans(HttpExchange exchange, boolean debug) {
			super(exchange, debug);
		}

		/** Returns wallet lower case */
		public String getWalletFromUri() throws Exception {
			String wallet = Util.getLastToken(m_uri, "/");
			require( Util.isValidAddress(wallet), RefCode.INVALID_REQUEST, "Wallet address is invalid");
			return wallet.toLowerCase();
		}
		
		/** Return all data about the requested wallet; if we don't have the data already,
		 *  we will request it; for debugging only */
		public void handleGetWallet() {
			wrap( () -> {
				respond( getOrCreateHookWallet( getWalletFromUri() ).getAllJson() );
			});
		}

		/** Return all wallets that we currently have in the map */
		public void handleGetAllWallets() {
			wrap( () -> {
				JsonArray ar = new JsonArray();
				Util.forEach( m_hookMap.values(), hookWallet -> ar.add( hookWallet.getAllJson() ) );
				respond( ar);
			});
		}
		
		public void handleMyWallet() {
			wrap( () -> {
				String walletAddr = getWalletFromUri();
				
				HookWallet hookWallet = getOrCreateHookWallet( walletAddr);
				
				JsonObject rusd = new JsonObject();
				rusd.put( "name", "RUSD");
				rusd.put( "balance", hookWallet.getBalance( m_config.rusdAddr() ) );
				rusd.put( "tooltip", m_config.getTooltip(Config.Tooltip.rusdBalance) );
				rusd.put( "buttonTooltip", m_config.getTooltip(Config.Tooltip.redeemButton) );
				
				double approved = hookWallet.getAllowance(m_config.busd(), walletAddr, m_config.rusdAddr() );

				// fix a display issue where some users approved a huge size by mistake
				approved = Math.min(1000000, approved);
				
				JsonObject busd = new JsonObject();
				busd.put( "name", m_config.busd().name() );
				busd.put( "balance", hookWallet.getBalance( m_config.busd().address() ) );
				busd.put( "tooltip", m_config.getTooltip(Config.Tooltip.busdBalance) );
				busd.put( "buttonTooltip", m_config.getTooltip(Config.Tooltip.approveButton) );
				busd.put( "approvedBalance", approved);
				busd.put( "stablecoin", true);
				
				JsonObject base = new JsonObject();
				base.put( "name", m_config.nativeTok() );
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
		
		// don't forget about syncing

		private HookWallet getOrCreateHookWallet(String walletAddr) throws Exception {
			return Util.getOrCreateEx(m_hookMap, walletAddr, () -> {  // call is synced on m_hookMap
				S.out( "Creating HookWallet for %s", walletAddr);
				
				S.out( "  querying all positions");
				HashMap<String, Double> positions = new Wallet( walletAddr)
						.reqPositionsMap(m_allContracts);
				
				S.out( "  querying approval", walletAddr);
				double approved = m_config.busd().getAllowance(walletAddr, m_config.rusdAddr() );
				S.out( "    contr:%s  wal:%s  spender:%s  amt:%s", 
						m_config.busd().address(),
						walletAddr,
						m_config.rusdAddr(),
						approved);
				
				S.out( "  querying native balance", walletAddr);
				double nativeBal = MoralisServer.getNativeBalance( walletAddr);
				
				Streams.addAddressToStream( nativeStreamId, walletAddr);
				
				return new HookWallet( walletAddr, positions, approved, nativeBal);
			});
		}

		public void handlesStatus() {
			wrap( () -> {
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
			S.out( "Received " + obj);
			
			String tag = obj.getString("tag");
			boolean confirmed = obj.getBool("confirmed");
			
			// process native transactions
			for (JsonObject trans : obj.getArray("txs" ) ) {
				double amt = trans.getDouble("value") / ten18;

				if (amt != 0) {
					String from = trans.getString("fromAddress").toLowerCase(); 
					String to = trans.getString("toAddress").toLowerCase();

					S.out( "Received hook [%s]: transferred %s native balance from %s to %s (%s)",
							tag,
							amt,
							from,
							to,
							obj.getBool("confirmed"));

					adjustNativeBalance( from, -amt, confirmed);
					adjustNativeBalance( to, amt, confirmed);
				}
			}
			
			// process approvals
			for (JsonObject trans : obj.getArray("approvals") ) {
				String contract = trans.getString("contract");
				
				if (contract.equalsIgnoreCase(m_config.busd().address() ) ) {
					String owner = trans.getString("owner");
					String spender = trans.getString("spender");
					double amt = trans.getDouble("valueWithDecimals");

					S.out( "Received hook [%s]: on %s, %s can spend %s on behalf of %s",
							contract, spender, amt, owner);
							
					Util.lookup( m_hookMap, owner, hookWallet -> hookWallet.approved( amt) );
				}
			}
			
			// process ERC20 transfers
			for (JsonObject trans : obj.getArray("erc20Transfers") ) {
				double amt = trans.getDouble("valueWithDecimals");

				if (amt != 0) {
					String contract = trans.getString("contract").toLowerCase();
					String from = trans.getString("from").toLowerCase(); 
					String to = trans.getString("to").toLowerCase();

					S.out( "Received hook [%s]: %s transferred %s from %s to %s (%s)",
							tag,
							contract, 
							amt,
							from,
							to,
							obj.getBool("confirmed"));

					// "confirmed" is our signal that something changed recently,
					// it is completely done and we should re-query;
					// one loophole is if two transactions are entered before the
					// first one is confirmed
					adjustTokenBalance( from, contract, -amt, confirmed);
					adjustTokenBalance( to, contract, amt, confirmed);
				}
			};
		}

		private void adjustTokenBalance(String wallet, String contract, double amt, boolean confirmed) throws Exception {
			Util.lookup( m_hookMap, wallet, hookWallet -> hookWallet.adjust( contract, amt, confirmed) );

			// if no hookWallet found, it means we are not yet tracking the positions
			// for this wallet, and we would query all positions if a request comes in
		}
		
		private void adjustNativeBalance( String wallet, double amt, boolean confirmed) throws Exception {
			Util.lookup( m_hookMap, wallet, hookWallet -> hookWallet.adjustNative( amt, confirmed) );
			
			// if no hookWallet found, it means we are not yet tracking the positions
			// for this wallet, and we would query all positions if a request comes in
		}

		void handleDump() {
			S.out( "Dumping");
			S.out( m_hookMap);
		}
	}
	
	void createTransfersStream(String... contracts) throws Exception {
		S.out( "Creating transfer stream");
		Streams.createStreamWithAddresses(
				String.format( Streams.erc20Transfers, m_config.hookServerUrl(), "0x5"),
				contracts);
	}

	/** This could be improved so that you always get the current balance back with the web hook */
	void createNativeStream() throws Exception {
		S.out( "Creating native stream");
		nativeStreamId = Streams.createStreamWithAddresses(
				String.format( Streams.nativeTrans, m_config.hookServerUrl(), "0x5") );
	}

	/** Listen for approvals on the contract, e.g. USDT */
	void createApprovalStream() throws Exception {
		S.out( "Creating approval stream");
		Streams.createStreamWithAddresses(
				String.format( Streams.approval, m_config.hookServerUrl(), "0x5"),
				m_config.busd().address() );
	}
}
