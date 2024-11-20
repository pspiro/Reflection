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

import chain.Chain;
import chain.Chains;
import chain.Stocks;
import common.Util;
import http.BaseTransaction;
import http.MyClient;
import http.MyServer;
import positions.HookConfig.HookType;
import reflection.Config;
import reflection.RefCode;
import test.MyTimer;
import tw.util.S;
import web3.Erc20;
import web3.NodeInstance;


/** HookServer tracks the balances for all contract, including both stock
 *  tokens and stablecoins. First we query for the current balance, then
 *  we listen for updates from Moralis via a WebHook server. 

 *  There are three types of updates from Moralis: token transfers, native 
 *  token transfers, and approvals
 *  
 *  You can use grok to create a tcp/ip tunnel to receive the webhook messages 
 */
public class HookServer {
	// constants
	static final double ten18 = Math.pow(10, 18);
	static final long m_started = System.currentTimeMillis(); // timestamp that app was started
	static final double small = .0001;    // positions less than this will not be reported
	
	private final HookConfig2 m_config = new HookConfig2();
	final Chain m_chain;
	final Stocks m_stocks = new Stocks();
	final StreamMgr sm;
	String[] m_allContracts;  // list of contract for which we want to request and monitor position; all stocks plus BUSD and RUSD
	String m_transferStreamId;
	NodeInstance m_node;

	/** Map wallet, lower case to HookWallet */ 
	final Map<String,HookWallet> m_hookMap = new ConcurrentHashMap<>();
	
	static {
	}
	
	String chain() { 
		return Util.toHex( m_chain.chainId() ); 
	}
	
	public static void main(String[] args) {
		try {
			Thread.currentThread().setName("Hook");
			S.out( "Starting HookServer");
			
			new HookServer(args).run();
		}
		catch (Exception e) {
			e.printStackTrace();
			S.out( "The application is terminating - " + e.getMessage() );
			System.exit(2);  // we need this because listening on the port will keep the app alive
		}
	}
	// chain, hookType (moralis or alchemy), 
	HookServer(String[] args) throws Exception {
		Util.require( args.length > 0, "pass in chain name (column header)");
		//m_config.readFromSpreadsheet( Config.getTabName( args) );
		m_chain = new Chains().readOne( args[0], true);
		m_config.setChain( m_chain);
		m_node = m_chain.node();

		m_stocks.readFromSheet();
		
		Util.require( m_config.hookType() != HookType.None, "Invalid hookType");
		sm = m_config.hookType() == HookType.Moralis
				? new MoralisStreamMgr()
				: new AlchemyStreamMgr( m_config.alchemyChain(), m_chain.params().platformBase(), m_chain.busd().address() );
	}
	
	void run() throws Exception {
		MyClient.filename = "hook.http.log";
		
		// build list of all contracts that we want to listen for ERC20 transfers
		ArrayList<String> list = new ArrayList<>();  // keep a list as array for speed
		list.addAll( Arrays.asList( m_chain.getAllContractsAddresses() ) );
		list.add( m_chain.busd().address() );
		list.add( m_chain.rusd().address() );
		m_allContracts = list.toArray( new String[list.size()]);

		String suffix = m_chain.params().getWebhookUrlSuffix();
		
		MyServer.listen( m_chain.params().hookServerPort(), 10, server -> {
			server.createContext(suffix, exch -> new Trans(exch, false).handleWebhook() );
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

		if (streaming() ) {
			// for Alchemy, the hooks don't have names, so we can only run one set of 
			// hooks per chain (or we could remember the set of ID's and delete by id)
			if (m_config.hookType() == HookType.Alchemy) {  // improve this to delete the chains better
				AlchemyStreamMgr.deleteAllForChain( m_config.alchemyChain() ); 
			}

			String webhookUrl = m_chain.params().getWebhookUrl();
			S.out( "Webhook URL is %s", webhookUrl);
			
			// listen for ERC20 transfers and native transfers
			try {
				m_transferStreamId = sm.createTransfersStream( webhookUrl);
				S.out( "  transfer stream created");
			}
			catch( Exception e) {
				e.printStackTrace();
				S.out( "WARNING: TRANSFER STREAM IS NOT ACTIVE");
			}
			
			// listen for "approve" transactions
			// you could pass BUSD, RUSD, or the user addresses
			// it would be ideal if there were a way to combine these two streams into one,
			// then it could just work off the user address, same as the transfer stream
			// listen for RUSD because there are so many BUSD approvals
			try {
				sm.createApprovalStream( webhookUrl, m_chain.rusd().address() );
				S.out( "  approval stream created");
			}
			catch( Exception e) {
				e.printStackTrace();
				S.out( "WARNING: APPROVAL STREAM IS NOT ACTIVE");
			}
			S.out( "**ready**");
		}
		else { 
			S.out( "***NOT USING STREAMS");
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread( this::shutdown) );
	}
	
	void shutdown() {
		S.out( "------");
		S.out( "RECEIVED SHUTDOWN HOOK");
		S.out( "------");
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
				respond( getOrCreateHookWallet( getWalletFromUri() )
						.getAllJson( m_config.minTokenPosition() ) );
			});
		}

		/** tags are: native, approved, positions, wallet 
		 *  positions is an object key address, position */ 
		public void handleGetWalletMap() {
			wrap( () -> {
				respond( getOrCreateHookWallet( getWalletFromUri() )
						.getAllJsonMap( m_config.minTokenPosition() ) );
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
				rusd.put( "balance", hookWallet.getBalance( m_chain.rusd().address() ) );
				rusd.put( "tooltip", m_config.getTooltip(Config.Tooltip.rusdBalance) );
				rusd.put( "buttonTooltip", m_config.getTooltip(Config.Tooltip.redeemButton) );
				
				double approved = hookWallet.getAllowance(m_chain.busd(), walletAddr, m_chain.rusd().address() );
				approved = Math.min(1000000, approved);  // fix a display issue where some users approved a huge size by mistake

				// BUSD
				JsonObject busd = new JsonObject();
				busd.put( "name", m_chain.busd().name() );
				busd.put( "balance", hookWallet.getBalance( m_chain.busd().address() ) );
				busd.put( "tooltip", m_config.getTooltip(Config.Tooltip.busdBalance) );
				busd.put( "buttonTooltip", m_config.getTooltip(Config.Tooltip.approveButton) );
				busd.put( "approvedBalance", approved);
				busd.put( "stablecoin", true);

				// native token (e.g. MATIC)
				JsonObject base = new JsonObject();
				base.put( "name", m_chain.params().platformBase() );
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
					out( "  (no data)");
				}
				else {
					JsonObject obj = parseToObject();
					S.out( "Received webhook: " + obj);
					sm.handleHookWithData( obj, HookServer.this);
				}
				respondOk();
			});
		}
		
		/** Reset one wallet; this will cause HookServer to re-query for all 
		 *  wallet values next time a request comes in for that wallet;
		 *  called by Monitor; should be called if a wrong value is being reported */
		public void handleReset() {
			wrap( () -> {
				String walletAddr = getWalletFromUri();
				m_hookMap.remove( walletAddr.toLowerCase() );
				out( "Removed %s from map");
				respondOk();
			});
		}

		/** Reset all wallets; this will cause HookServer to re-query for all 
		 *  wallet values next time a request comes in for any wallet;
		 *  called by Monitor; should be called if a wrong values are being reported */
		public void handleResetAll() {
			wrap( () -> {
				m_hookMap.clear();
				out( "Cleared map");
				respondOk();
			});
		}
	}

	void adjustTokenBalance(String wallet, String contract, double amt, boolean confirmed) throws Exception {
		Util.lookup( m_hookMap.get(wallet), hookWallet -> hookWallet.adjustERC20( contract, amt, confirmed) );

		// if no hookWallet found, it means we are not yet tracking the positions
		// for this wallet, and we would query all positions if a request comes in
	}
	
	void adjustNativeBalance( String wallet, double amt, boolean confirmed) throws Exception {
		Util.lookup( m_hookMap.get(wallet), hookWallet -> hookWallet.adjustNative( amt, confirmed) );
		
		// if no hookWallet found, it means we are not yet tracking the positions
		// for this wallet, and we would query all positions if a request comes in
	}
	
	/** Note that the entire call is synced on m_hookMap, which means it will
	 *  block other calls while it creates the wallet
	 *  @param walletAddr is lower case */
	private HookWallet getOrCreateHookWallet(String walletAddr) throws Exception {
		return Util.getOrCreateEx(m_hookMap, walletAddr, () -> {
			MyTimer t = new MyTimer();
			t.next( "Creating HookWallet for %s", walletAddr);
			
			// query ERC20 position map
			HashMap<String, Double> positions = m_node.reqPositionsMap( walletAddr, m_allContracts, 0);
			
			// query allowance
			double approved = m_chain.busd().getAllowance(walletAddr, m_chain.rusd().address() );
			
			// query native balance
			double nativeBal = m_node.getNativeBalance( walletAddr);
			
			if (streaming() ) {
				Util.require( S.isNotNull( m_transferStreamId), "Cannot handle requests until transferStreamId is set");  // this can happen if we receive events from the old stream before the new stream is created
				sm.addAddressToStream( m_transferStreamId, walletAddr);  // watch all transfers for this wallet so we can see the MATIC transfers 
			}
			
			t.done();
			
			return new HookWallet( m_node, walletAddr, positions, approved, nativeBal);
		});
	}

	private boolean streaming() {
		return !m_config.noStreams();
	}
	
	static abstract class StreamMgr {
		protected abstract String createTransfersStream(String urlBase) throws Exception;
		protected abstract String createApprovalStream(String urlBase, String address) throws Exception;
		protected abstract void addAddressToStream(String id, String address) throws Exception;
		protected abstract void handleHookWithData(JsonObject obj, HookServer hookServer) throws Exception;
	}
	
	class MoralisStreamMgr extends StreamMgr {
		@Override public String createTransfersStream( String urlBase) throws Exception {
			return MoralisStreams.createStream(
					MoralisStreams.erc20Transfers, 
					"transfer-" + m_config.getHookNameSuffix(), 
					urlBase + "/hook/webhook",
					chain() ); 
		}
		
		@Override public String createApprovalStream( String urlBase, String address) throws Exception {
			return MoralisStreams.createStream(
					MoralisStreams.approval, 
					"approval-" + m_config.getHookNameSuffix(), 
					urlBase + "/hook/webhook", 
					chain(),
					address);
		}

		@Override public void addAddressToStream(String id, String address) throws Exception {
			MoralisStreams.addAddressToStream(id, address);
		}
		
		@Override protected void handleHookWithData(JsonObject obj, HookServer hookServer) throws Exception {
			String tag = obj.getString("tag");
			boolean confirmed = obj.getBool("confirmed");

//			S.out( "Received webhook [%s - %s] %s", tag, confirmed, BaseTransaction.debug() ? obj : "");
			
			// process native transactions
			for (JsonObject trans : obj.getArray("txs" ) ) {
				//double amt = trans.getDouble("value") / ten18;
				double amt = Erc20.fromBlockchain( trans.getString("value"), 18);

				if (amt != 0) {
					String from = trans.getString("fromAddress").toLowerCase(); 
					String to = trans.getString("toAddress").toLowerCase();

					S.out( "MORALIS NATIVE TRANSFER  %s %s was transferred from %s to %s", 
							amt, m_chain.params().platformBase(), from, to);

					hookServer.adjustNativeBalance( from, -amt, confirmed);
					hookServer.adjustNativeBalance( to, amt, confirmed);
				}
			}
			
			// process approvals;
			// this code is the same as for Moralis and could be shared, but
			// they are separate in case one changes and not the other
			for (JsonObject trans : obj.getArray("erc20Approvals") ) {
				// tags are: contract, logIndex, owner, spender, tokenDecimals, tokenName, tokenSymbol, transactionHash, value, valueWithDecimals, 

				String contract = trans.getString("contract");
				
				if (contract.equalsIgnoreCase(m_chain.busd().address() ) ) {
					String owner = trans.getString("owner");
					String spender = trans.getString("spender");
					double amt = trans.getDouble("valueWithDecimals");
					int dec = trans.getInt( "tokenDecimals");
					String value = trans.getString( "value");
					
					// sometimes decimal amount is not provided, we have to look it up
					if (amt == 0 && dec == 0) {
						dec = m_node.getDecimals( contract);  // could send a query
						amt = Erc20.fromBlockchain( value, dec);
						S.out( "  looked up dec val  dec=%s  amt=%s  raw=%s", dec, amt, value);
					}
					S.out( "MORALIS APPROVAL  %s can spend %s %s on behalf of %s",
							spender, amt, contract, owner);  // use java formatting for amt which can be huge

					double finalAmt = amt;
					Util.lookup( hookServer.m_hookMap.get(owner), hookWallet -> {
						hookWallet.approved( finalAmt);	
					});
				}
			}
			
			// process ERC20 transfers
			for (JsonObject trans : obj.getArray("erc20Transfers") ) {
				String contract = trans.getString("contract").toLowerCase();
				double amt = Erc20.fromBlockchain( trans.getString("value"), m_node.getDecimals(contract) ); // note this can send a query to get decimals

				if (amt != 0) {
					String from = trans.getString("from").toLowerCase(); 
					String to = trans.getString("to").toLowerCase();

					S.out( "MORALIS ERC20 TRANSFER  %s %s was transferred from %s to %s",
							amt, contract, from, to);

					// "confirmed" is our signal that something changed recently,
					// it is completely done and we should re-query;
					// one loophole is if two transactions are entered before the
					// first one is confirmed
					hookServer.adjustTokenBalance( from, contract, -amt, confirmed);
					hookServer.adjustTokenBalance( to, contract, amt, confirmed);
				}
			};
		}

	}

}

/** This could be improved so that you always get the current balance back with the web hook */
//void createNativeStream() throws Exception {
//	S.out( "***Creating native stream***");
//	nativeStreamId = Streams.createStreamWithAddresses(
//			String.format( Streams.nativeTrans, chain(), m_config.hookServerUrl(), chain() ) );
//	S.out( "  created native stream with id %s" + nativeStreamId);
//}
