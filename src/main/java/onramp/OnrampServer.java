package onramp;

import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import chain.Chain;
import common.Alerts;
import common.LogType;
import common.Util;
import http.SimpleTransaction;
import reflection.Config.MultiChainConfig;
import tw.google.NewSheet;
import tw.util.S;
import web3.NodeInstance.Transfer;

/* use this transaction id for a transfer of 12.04: 1060395
insert into onramp (wallet_public_key, trans_id, amount) values ('0xabc', '1060395', 12.0399); 
update onramp set state = '';
*/

/** runs as a separate program. It polls the onramp API and the database every 10 sec.
 *  looking for unfilled transactions
 *  
 *  Has no communication with any of our processes. It responds to the /onramp/ok request */
public class OnrampServer {
	enum State { Funding, Completed, Error }

	static int poll = 10000;
	static final long confRequired = 24;  // require 24 confirmations on Polygon, 50% more than the 16 required 
	static final double maxAuto = 300;
	static final double tolerance = .01; // max percentage slippage
	private static final String transactionHash = "transactionHash";

	static final JsonObject errCodes = Util.toJson(
			"-4", "The sent amount does not match the required amount",
			"-3", "The names on the bank account and KYC do not match",
			"-2", "The user has abandoned the transaction",
			"-1", "The transaction has exceeded the allowable time limit");  
	
	private final MultiChainConfig m_config = new MultiChainConfig();;
	private final HashMap<String, JsonObject> m_onrampMap = new HashMap<>(); // map onramp transId to onramp record
	private boolean m_testMode;
	
	void setTestMode() {
		m_testMode = true;
	}
	
	public static void main(String[] args) throws Exception {
		Thread.currentThread().setName("OnRamp");
		
		// don't allow running twice
		SimpleTransaction.listen("0.0.0.0", 5003, SimpleTransaction.nullHandler);

		new OnrampServer();
	}
	
	public OnrampServer() throws Exception {
		S.out( "Starting onramp server with %s ms polling interval", poll);

		Onramp.debugOff();

		// read config
		var book = NewSheet.getBook( NewSheet.Reflection);
		m_config.readFromSpreadsheet( book, "Prod-config");
		
		// poll onramp and database every ten sec
		Util.executeEvery(0, poll, this::check);
	}
	
	void check() {
		Util.wrap( () -> {
			checkOnrampTransactions();

			// look for unfulfilled database transactions
			JsonArray rows = m_config.sqlQuery( "select * from onramp where (state is null or state = '')");
			for (var dbTrans : rows) {
				try {
					processDbTrans( dbTrans);
				}
				catch( Exception e) {
					S.out( "Error - " + e.getMessage() + " " + dbTrans);
					
					// update state so we won't try again; admin must intervene 
					Util.wrap( () -> updateState( State.Error, "", dbTrans.getString( "trans_id") ) );  

					m_config.log( Util.toJson(
							"type", LogType.ONRAMP,
							"wallet_public_key", dbTrans.getString( "wallet_public_key"),
							"chainId", dbTrans.getString( "chainId"),
							"data", Util.toJson( 
									"type", "error", 
									"text", e.getMessage() ) ) );
					
					Alerts.alert( "OnRamp Server", "Funding user wallet failed", "Admin intervention required");

					e.printStackTrace();
				}
			}
		});
	}
	
	/** process one transaction with no hash code and no state 
	 * @throws Exception */
	void processDbTrans( JsonObject dbTrans) throws Exception {
		String id = dbTrans.getString( "trans_id");
		Util.require( S.isNotNull( id), "Error: onramp id should not be null");
		
		String wallet = dbTrans.getString( "wallet_public_key");
		Util.require( S.isNotNull( wallet), "Error: wallet should not be null");
		
		int chainId = dbTrans.getInt( "chainId");
		Util.require( chainId > 0, "Error: chainId is zero");
		
		Chain userChain = m_config.chains().get( chainId);  // the chain that the user wants the crypto on
		Util.require( userChain != null, "Error: chainId is invalid");
		
		Chain polygon = m_config.chains().polygon();  // currently, we can only receive from onramp on polygon, but that could change
		
		// get onramp transaction
		var onrampTrans = m_onrampMap.get( id);
		Util.require( onrampTrans != null, "Error: could not find onramp transaction  id=%s  wallet=%s", id, wallet);
		
		S.out( "found onramp transaction: " + onrampTrans);

		// negative status implies an error
		int status = onrampTrans.getInt( "status");
		Util.require( status >= 0, S.notNull( 
				errCodes.getString( "" + status),  // preferred msg
				"An error occurred with status " + status) );  // default msg

		// get transaction hash of transfer from onramp to RefWallet 
		String onrampToReflHash = onrampTrans.getString( transactionHash); // <<< ON POLYGON!!!
		if (S.isNotNull( onrampToReflHash) ) {
			S.out( "got onramp-to-reflection hash %s on chain %s", onrampToReflHash, polygon.params().name() );
			
			Util.require( Util.isValidHash( onrampToReflHash), "the hash for trans_id %s is invalid", id); // the hash is always invalid in the OnRamp sandbox
			
			// check for required number of confirmations
			long ago = polygon.node().getTransactionAge(onrampToReflHash);  // <<< ON POLYGON!!!
			if (ago >= confRequired) {

				// get receipt and info about the transfer
				Transfer received = polygon.node().getTransferReceipt( onrampToReflHash, polygon.busd().decimals() ); // <<< ON POLYGON!!!

				// confirm correct contract address
				Util.require( received.contract().equalsIgnoreCase( polygon.busd().address() ), 
						"Error: the blockchain transaction is for the wrong contract  id=%s  wallet=%s  expected=%s  got=%s",
						id, wallet, polygon.busd().address(), received.contract() );

				// confirm correct receipient address
				Util.require( received.to().equalsIgnoreCase( polygon.params().refWalletAddr() ), 
						"Error: the crypto was sent to the wrong address  id=%s  wallet=%s  expected=%s  got=%s",
						id, wallet, polygon.params().refWalletAddr(), received.to() );
				
				// confirm correct amount
				double expected = dbTrans.getDouble( "crypto_amount");
				double lostPct = (expected - received.amount() ) / expected;
				Util.require( lostPct <= tolerance, 
						"Error: the received amount is insufficient  id=%s  wallet=%s  expected=%s  got=%s  lossPct=%s",
						id, wallet, expected, received.amount(), lostPct);
				
				// update database so we don't double-send
				updateState( State.Funding, "", id);
				
				if (!m_testMode) {
					// send funds to the user, same as we received
					S.out( "funding %s with %s", dbTrans, received.amount() );
					String reflToUserHash = userChain.rusd().mintRusd(   // pulsechain, e.g.
							wallet, 
							received.amount(), 
							userChain.getAnyStockToken()
							).waitForReceipt();
					
					// update database again that it was successful
					updateState( State.Completed, reflToUserHash, id);
					
					// notify user by email
					sendEmail( 
							wallet, 
							received.amount(), 
							reflToUserHash, 
							userChain.params().name(), 
							userChain.browseTx(reflToUserHash) );
				
					// notify us  (we also get a copy of the email above)
					Alerts.alert( "OnRamp Server", "Funding user wallet succeeded", reflToUserHash);
				
					// we should send them an update on the platform here as well. pas
	
					// create a log entry
					var log = Util.toJson( 
							"type", LogType.ONRAMP,
							"wallet_public_key", wallet,
							"chainId", userChain.chainId(),
							"data", Util.toJson(
									"type", "funded",
									"amount", received.amount() ) );
					m_config.log( log);
				}
				else {
					S.out( "test mode: would be funding %s with %s", dbTrans, received.amount() );
				}
			}
			else {
				S.out( "not enough blocks yet for transaction " + onrampTrans);
			}						
		}
		else {  // check for final status. pas
			//S.out( "no hash yet for transaction " + onrampTrans);
		}
	}

	private void updateState(State state, String hash, String id) throws Exception {
		m_config.sqlCommand( sql -> sql.execWithParams( 
				"update onramp set state='%s', hash='%s' where trans_id = '%s'", 
				state, hash, id) );
	}

	static String emailTempl = """
			Dear #username#,<br>
			<br>
			Your fiat-to-crypto transaction is complete!<br>
			<br>
			<strong>Amount:</strong> #amount#<br>
			<strong>Wallet:</strong> #wallet#<br>
			<strong>Blockchain:</strong> #blockchain#<br>
			<br>
			You can <a href="#link#">view the transaction in the blockchain explorer.</a><br>
			<br>
			If you have any questions or concerns, please don't hesitate to contact us.<br>
			<br>
			Sincerely,<br>
			<br>
			The Reflection team<br>
			""";
	
	/** Send email to user telling them the transaction is complete */
	private void sendEmail( String wallet, double amount, String hash, String blockchain, String link) throws Exception {
		var user = getUser( wallet);
				
		String username = String.format( "%s %s", user.getString( "first_name"), user.getString( "last_name") ).trim();

		String html = emailTempl
				.replace( "#username#", username)
				.replace( "#wallet#", wallet)
				.replace( "#blockchain#", blockchain)
				.replace( "#amount#", S.fmt2c( amount) )
				.replace( "#link#", link);
		
		m_config.sendEmail( 
				user.getString( "email"), 
				"Reflection on-ramp transaction completed", 
				html);
	}

	private JsonObject getUser(String wallet) throws Exception {
		var users = m_config.sqlQuery( "select * from users where wallet_public_key = '%s'", wallet.toLowerCase() );
		Util.require( users.size() == 1, "Error: no user found for wallet '%s'", wallet);
		return users.get( 0); 
	}

	/** build the map and note transitions */
	void checkOnrampTransactions() {
		Util.wrap( () -> { 
			for (var trans : Onramp.prodRamp.getAllTransactions() ) {
				String id = trans.getString( "transactionId");
				int status = trans.getInt( "status");
				
				var old = m_onrampMap.get( id);
				
				if (old == null) {
					S.out( "New onramp transaction detected - " + trans);
					m_onrampMap.put( id, trans);
				}
				else {
					if (old.getInt( "status") != status) {
						S.out( "Status change detected from %s to %s - %s",
								old.getInt( "status"), status, trans);
					}
					if (!old.has( transactionHash) && trans.has( transactionHash) ) {
						S.out( "Hash was set to %s - %s", trans.getString( "hash"), trans);
					}
				}
				m_onrampMap.put( id, trans);
			}
			
		});
	}
}