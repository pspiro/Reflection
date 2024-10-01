package onramp;

import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Alerts;
import common.Util;
import reflection.Config;
import reflection.Stocks;
import tw.util.S;
import util.LogType;
import web3.NodeInstance.TransferReceipt;

/* use this transaction id for a transfer of 12.04: 1060395
insert into onramp (wallet_public_key, trans_id, amount) values ('0xabc', '1060395', 12.0399); 
update onramp set state = '';
*/


public class OnrampServer {
	enum State { Funding, Completed, Error }

	static int poll = 10000;
	static final long confRequired = 12;  // require 12 confirmations on PulseChain
	static final double maxAuto = 300;
	static final double tolerance = .01; // received amount vs expected amount
	private static final String transactionHash = "transactionHash";

	static final JsonObject errCodes = Util.toJson(
			"-4", "The sent amount does not match the required amount",
			"-3", "The names on the bank account and KYC do not match",
			"-2", "The user has abandoned the transaction",
			"-1", "The transaction has exceeded the allowable time limit");  
	
	private Config m_polygon;
	private Config m_pulsechain;
	private HashMap<String, JsonObject> map = new HashMap<>(); // map onramp transId to onramp record
	private Stocks m_stocks;

	public static void main(String[] args) throws Exception {
		Thread.currentThread().setName("OnRamp");
		S.out( "Starting onramp server with %s ms polling interval", poll);
		//Onramp.useProd();
		Onramp.debugOff();
		new OnrampServer( args);
	}

	public OnrampServer(String[] args) throws Exception {
		m_polygon = Config.readFrom( "Prod-config");  // must come first! because Config.read() sets the NodeServer instance
		m_pulsechain = Config.readFrom( "Dev3-config");  // must come second!
		m_stocks = m_pulsechain.readStocks();
		Util.executeEvery(0, poll, this::check);
	}
	
	void check() {
		Util.wrap( () -> {
			checkOnrampTransactions();

			JsonArray rows = m_pulsechain.sqlQuery( "select * from onramp where (state is null or state = '')");
			
			for (var dbTrans : rows) {
				try {
					processDbTrans( dbTrans);
				}
				catch( Exception e) {
					S.out( "Error - " + e.getMessage() + " " + dbTrans);
					
					// update state so we won't try again; admin must intervene 
					Util.wrap( () -> updateState( State.Error, "", dbTrans.getString( "trans_id") ) );  

					m_pulsechain.log( Util.toJson(
							"type", LogType.ONRAMP,
							"wallet_public_key", dbTrans.getString( "wallet_public_key"),
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

		// get onramp transaction
		var onrampTrans = map.get( id);
		Util.require( onrampTrans != null, "Error: could not find onramp transaction  id=%s  wallet=%s", id, wallet);
		
		S.out( "found onramp transaction: " + onrampTrans);

		// negative status implies an error
		int status = onrampTrans.getInt( "status");
		Util.require( status >= 0, S.notNull( errCodes.getString( "" + status), "An error occurred with status " + status) );

		// get transaction hash of transfer from onramp to RefWallet 
		String onrampToReflHash = onrampTrans.getString( transactionHash); // <<< ON POLYGON!!!
		if (S.isNotNull( onrampToReflHash) ) {
			S.out( "got onramp-to-reflection hash %s", onrampToReflHash);
			
			Util.require( Util.isValidHash( onrampToReflHash), "the hash for trans_id %s is invalid", id); // the hash is always invalid in the OnRamp sandbox
			
			// check for required number of confirmations
			long ago = m_polygon.node().getTransactionAge(onrampToReflHash);  // <<< ON POLYGON!!!
			if (ago >= confRequired) {

				// get receipt and info about the transfer
				TransferReceipt received = m_polygon.node().getTransferReceipt( onrampToReflHash, m_polygon.busd().decimals() ); // <<< ON POLYGON!!!

				// confirm correct contract address
				Util.require( received.contract().equalsIgnoreCase( m_polygon.busdAddr() ), "Error: the blockchain transaction is for the wrong contract  id=%s  wallet=%s  expected=%s  got=%s",
						id, wallet, m_polygon.busdAddr(), received.contract() );

				// confirm correct receipient address
				Util.require( received.to().equalsIgnoreCase( m_pulsechain.refWalletAddr() ), "Error: the crypto was sent to the wrong address  id=%s  wallet=%s  expected=%s  got=%s",
						id, wallet, m_pulsechain.refWalletAddr(), received.to() );
				
				// confirm correct amount
				Util.require( Util.isEq( received.amount(), dbTrans.getDouble( "amount"), tolerance), "Error: the received amount is incorrect  id=%s  wallet=%s  expected=%s  got=%s",
						id, wallet, dbTrans.getDouble( "amount"), received.amount() );
				
				// update database so we don't double-send
				updateState( State.Funding, "", id);

				// send funds to the user, same as we received
				S.out( "funding %s with %s", dbTrans, received.amount() );
				String reflToUserHash = m_pulsechain.rusd().mintRusd(   // pulsechain
						wallet, 
						received.amount(), 
						m_stocks.getAnyStockToken()
						).waitForHash();
				
				// update database again that it was successful
				updateState( State.Completed, reflToUserHash, id);
				
				// notify user by email
				sendEmail( wallet, received.amount(), reflToUserHash);

				// notify us
				Alerts.alert( "OnRamp Server", "Funding user wallet succeeded", reflToUserHash);
				
				// we should send them an update on the platform here as well. pas

				// create a log entry
				var log = Util.toJson( 
						"type", LogType.ONRAMP,
						"wallet_public_key", wallet,
						"data", Util.toJson(
								"type", "funded",
								"amount", received.amount() ) );
				m_pulsechain.log( log);
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
		m_pulsechain.sqlCommand( sql -> sql.execWithParams( 
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
	private void sendEmail( String wallet, double amount, String hash) throws Exception {
		var user = getUser( wallet);
				
		String username = String.format( "%s %s", user.getString( "first_name"), user.getString( "last_name") ).trim();

		String html = emailTempl
				.replace( "#username#", username)
				.replace( "#wallet#", wallet)
				.replace( "#amount#", S.fmt2( amount) )
				.replace( "#link#", m_pulsechain.blockchainTx(hash) );
		
		m_pulsechain.sendEmail( 
				user.getString( "email"), 
				"Reflection on-ramp transaction completed", 
				html);
	}

	private JsonObject getUser(String wallet) throws Exception {
		var users = m_pulsechain.sqlQuery( "select * from users where wallet_public_key = '%s'", wallet.toLowerCase() );
		Util.require( users.size() == 1, "Error: no user found for wallet '%s'", wallet);
		return users.get( 0); 
	}

	/** build the map and note and transitions */
	void checkOnrampTransactions() {
		Util.wrap( () -> { 
			for (var trans : Onramp.getAllTransactions() ) {
				String id = trans.getString( "transactionId");
				int status = trans.getInt( "status");
				
				var old = map.get( id);
				
				if (old == null) {
					S.out( "New onramp transaction detected - " + trans);
					map.put( id, trans);
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
				map.put( id, trans);
			}
			
		});
	}
}