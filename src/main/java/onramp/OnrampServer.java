package onramp;

import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import reflection.Config;
import reflection.RefException;
import reflection.Stocks;
import tw.util.S;
import util.LogType;
import web3.NodeServer;
import web3.NodeServer.Received;

public class OnrampServer {
	enum State { Funding, Completed, Error }

	static int poll = 5000;
	static final long confRequired = 12;  // require 12 confirmations on PulseChain
	static final double maxAuto = 300;
	static final double tolerance = .01; // received amount vs expected amount
	private static final String transactionHash = "transactionHash";
	
	private Config m_config;
	private HashMap<String, JsonObject> map = new HashMap<>(); // map onramp transId to onramp record
	private Stocks m_stocks;

	public static void main(String[] args) throws Exception {
		S.out( "Starting onramp server with %s ms polling interval", poll);
		Onramp.useProd();
		Onramp.debugOff();
		new OnrampServer( Config.ask() );
	}

	public OnrampServer(Config ask) throws Exception {
		m_config = ask;
		m_stocks = m_config.readStocks();
		Util.executeEvery(0, poll, this::check);
	}
	
	void check() {
		checkOnrampTransactions();

		Util.wrap( () -> {
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
							"data", Util.toJson( 
									"type", "error", 
									"text", e.getMessage() ) ) );

					e.printStackTrace();
				}
			}
		});
	}
	
	/* use this transaction id for a transfer of 12.04: 1060395
insert into onramp (wallet_public_key, trans_id, amount) values ('0xabc', '1060395', 12.0399); 
update onramp set state = '';
	*/
	
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

		// get transaction hash of transfer from onramp to RefWallet 
		String onrampToReflHash = onrampTrans.getString( transactionHash); // <<< THIS IS ON POLYGON!!!
		if (S.isNotNull( onrampToReflHash) ) {
			S.out( "got onramp-to-reflection hash %s", onrampToReflHash);
			
			// check for required number of confirmations
			long ago = NodeServer.getTransactionAge(onrampToReflHash);
			if (ago >= confRequired) {

				// get receipt
				Received received = m_config.busd().checkReceipt( onrampToReflHash);

				// confirm correct contract address
				Util.require( received.contract().equalsIgnoreCase( m_config.busdAddr() ), "Error: the blockchain transaction is for the wrong contract  id=%s  wallet=%s  expected=%s  got=%s",
						id, wallet, m_config.busdAddr(), received.contract() );

				// confirm correct receipient address
				Util.require( received.to().equalsIgnoreCase( m_config.refWalletAddr() ), "Error: the crypto was sent to the wrong address  id=%s  wallet=%s  expected=%s  got=%s",
						id, wallet, m_config.refWalletAddr(), received.to() );
				
				// confirm correct amount
				Util.require( Util.isEq( received.amount(), dbTrans.getDouble( "amount"), tolerance), "Error: the received amount is incorrect  id=%s  wallet=%s  expected=%s  got=%s",
						id, wallet, dbTrans.getDouble( "amount"), received.amount() );
				
				// update database so we don't double-send
				updateState( State.Funding, "", id);

				// send funds to the user, same as we received
				S.out( "funding %s with %s", dbTrans, received.amount() );
				String reflToUserHash = m_config.rusd().mintRusd( 
						wallet, 
						received.amount(), 
						m_stocks.getAnyStockToken()
						).waitForHash();
				
				// update database again that it was successful
				updateState( State.Completed, reflToUserHash, id);
				
				// notify user by email
				sendEmail( wallet, received.amount(), reflToUserHash);
				
				// we should send them an update on the platform here as well. pas

				// create a log entry
				var log = Util.toJson( 
						"type", LogType.ONRAMP,
						"wallet_public_key", wallet,
						"data", Util.toJson(
								"type", "funded",
								"amount", received.amount() ) );
				m_config.log( log);
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
			Your fiat-to-crypto transaction is complete.<br>
			<br>
			WALLET: #wallet#<br>
			AMOUNT: #amount#<br>
			HASH: #hash#<br>
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
				.replace( "#hash#", hash);
		
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