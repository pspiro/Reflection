package reflection;

import static reflection.Main.m_config;
import static reflection.Main.require;

import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import common.Util.ExRunnable;
import onramp.Onramp;
import onramp.Onramp.KycStatus;
import tw.util.S;
import util.LogType;

public class OnrampTransaction extends MyTransaction {
	static String KycMessage = "You will now be redirected to our on-ramp partner to verify your identiy. When completed, please come back to this screen and resubmit your request.";

	OnrampTransaction(Main main, HttpExchange exchange) {
		super(main, exchange, true);
	}

	public void handleGetQuote() {
		wrapOnramp( () -> {
			parseMsg();
			m_walletAddr = m_map.getWalletAddress("wallet_public_key");  // used in jlog

			String currency = m_map.getRequiredString("currency");
			require( Onramp.isValidCurrency( currency), RefCode.INVALID_REQUEST, "The selected currency is invalid");
			
			double buyAmt = m_map.getRequiredDouble( "buyAmt");
			require( buyAmt > 0, RefCode.INVALID_REQUEST, "The buy amount is invalid");
			
			double quote = Onramp.getQuote( currency, buyAmt);
			respond( Util.toJson( "recAmt", quote) );
			
			jlog( LogType.ONRAMP, Util.toJson( 
					"type", "quote",
					"currency", currency,
					"buyAmt", buyAmt,
					"recAmt", quote) );
		});
	}
	
	public final void wrapOnramp( ExRunnable runnable) {
		try {
			runnable.run();
		}
		catch( Exception e) {
			RefCode refCode = e instanceof RefException ex ? ex.code() : RefCode.UNKNOWN;
			
			if ( ! (e instanceof RefException) ) { 
				e.printStackTrace();
			}

			elog( LogType.ONRAMP, e);  // could change to onramp error
			respondFull( RefException.eToJson(e, refCode), 400, null);
			postWrap(null);
		}
	}
	

	/** user sends: currency, amount, receive-amount 
	 *  can return: code, message, url */
	public void handleConvert() {
		wrapOnramp( () -> {
			parseMsg();
			m_walletAddr = m_map.getWalletAddress("wallet_public_key");
			validateCookie("onramp-convert");
			
			var user = getorCreateUser();
			String onrampId = user.getString( "onramp_id");  // cust id and phone

			// first time?
			if (S.isNull( onrampId) ) {
				String phone = user.getString( "phone");

				require( isValidPhone( phone), RefCode.INVALID_USER_PROFILE, 
						"Please update your user profile to include a valid phone number.\n\n"
						+ "The required format is: '##-########'\n\nwhere the first set of digits is the country code");

				var json = Onramp.getKycUrlFirst( m_walletAddr, format( phone), m_config.baseUrl() );
				String custId = json.getString( "customerId");
				Util.require( S.isNotNull( custId), "No on-ramp ID was assigned");
				
				m_config.sqlCommand( sql -> sql.insertOrUpdate(
						"users",
						Util.toJson( 
							"wallet_public_key", m_walletAddr.toLowerCase(),
							"onramp_id", custId),
						"where wallet_public_key='%s'",
						m_walletAddr.toLowerCase() ) );
				
				respond( json
						.append( code, RefCode.OK)
						.append( Message, KycMessage) );
				
				jlog( LogType.ONRAMP, Util.toJson( "type", "order/KYC part 1") );

				alert( "User started onramp KYC", 
						String.format( "%s %s", user.getString( "first_name"), user.getString( "last_name") ) );
			}
			
			// we have ID; KYC already completed?
			else if (isCompleted( Onramp.getKycStatus( onrampId) ) ) {
				String currency = m_map.getRequiredString("currency");
				require( Onramp.isValidCurrency( currency), RefCode.INVALID_REQUEST, "The selected currency is invalid");
				
				double buyAmt = m_map.getRequiredDouble( "buyAmt");
				require( buyAmt > 0, RefCode.INVALID_REQUEST, "The buy amount is invalid");
				
				double receiveAmt = m_map.getRequiredDouble( "recAmt");
				require( receiveAmt > 0, RefCode.INVALID_REQUEST, "The receive amount is invalid");
	
				// submit order
				var resp = Onramp.transact( 
						onrampId,
						buyAmt,
						currency,
						m_config.refWalletAddr(),
						receiveAmt);
				
				require( resp.getInt( "code") == 200, RefCode.ONRAMP_FAILED,
					"An on-ramp error occurred - " + resp.getString( "error") );
				
				var data = resp.getObjectNN( "data");

				double amount = data.getDouble( "fiatAmount");
				Util.require( amount > 0, "Error - the on-ramp amount returned is invalid");
				
				var instr = data.getObject( "fiatPaymentInstructions");
				// onramp sandbox does not return payment instructions; add it
				if (instr == null && !m_config.isProduction() && m_map.getBool( "test") ) {
					instr = JsonObject.parse( testInstr);
				}
				Util.require( instr != null, "Error: no fiatPaymentInstructions returned");
				
				var bank = instr.getObject( "bank");
				Util.require( bank != null, "Error: no bank instructions returned");

				// create return json
				// tags are: fiatAmount, createdAt, bank, iban, name, type, Message, code
				JsonObject send = new JsonObject();
				send.copyFrom( instr, "bank");  // note that bank has order maintained; we will display all fields
				send.put( "amount", data.getDouble( "fiatAmount") );
				send.put( "createdAt", data.getString( "createdAt") );
				send.put( Message, "The transaction has been accepted");
				
				S.out( "sending to Frontend:");
				send.display();
				
				// respond to Frontend
				respond( send);
				
				// send email to customer
				StringBuilder bankInstr = new StringBuilder();
				bank.forEach( (key,val) -> {
					bankInstr.append( String.format( "<strong>%s: </strong>%s<br>", key.toUpperCase(), val) );
				});
				
				Profile profile = new Profile( user);
				String html = emailTemplate
						.replace( "#username#", String.format( "%s %s", profile.first(), profile.last() ) )
						.replace( "#instructions#", bankInstr)
						.replace( "#amount#", S.fmt2( send.getDouble( "amount") ) )
						.replace( "#date#", send.getString( "createdAt") );
						
				Main.m_config.sendEmail( profile.email(), "Reflection Fiat Onramp Transaction", html); 
				
				jlog( LogType.ONRAMP, Util.toJson(
						"type", "order/place",
						"currency", currency,
						"buyAmt", buyAmt,
						"recAmt", receiveAmt,
						"data", data,
						"returned", send) );
				
				alert( "USER SUBMITTED ONRAMP ORDER", 
						String.format( "%s %s", user.getString( "first_name"), user.getString( "last_name") ) );
			}
			
			// continue with subsequent KYC
			else {
				var json = Onramp.getKycUrlNext( onrampId, m_config.baseUrl() );
				Util.require( json.getString( "customerId").equals( onrampId), "The on-ramp ID has changed" );  //onramp id should not change

				respond( json
						.append( code, RefCode.OK)
						.append( Message, "Please continue your KYC with our on-ramp partner"));

				jlog( LogType.ONRAMP, Util.toJson( "type", "order/KYC part 2") );
			}
		});
	}
	
	private boolean isCompleted(String status) {
		return Util.equals( status.toUpperCase(), 
				"BASIC_KYC_COMPLETED", 
				"INTERMEDIATE_KYC_COMPLETED", 
				"ADVANCE_KYC_COMPLETED");
	}

	public static void main(String[] args) throws Exception {
		S.out( isValidPhone( "-9393939393") );
		S.out( isValidPhone( "2-22224") );
		S.out( isValidPhone( "2 -22224") );
		S.out( isValidPhone( "2- 22224") );
		S.out( isValidPhone( "34-8282") );
		S.out( isValidPhone( "222-9393939") );
		S.out( isValidPhone( "+939-939393") );
		S.out( isValidPhone( "+9349-939393") );
		
		S.out( format( "2-838383"));
		S.out( format( "+2 -838383"));
	}
	
	/** add the required '+' sign if necessary, strip spaces */
	static String format( String phone) {
		phone = strip( phone);
		return phone.startsWith( "+") ? phone : "+" + phone;
	}
	
	static boolean isValidPhone(String phone) {
		String[] toks = strip(phone).split( "-");
		return toks.length == 2 && toks[0].length() >= 1 && toks[0].length() <= 4; 
	}
	
	private static String strip(String phone) {
		return phone.replaceAll( " ", "");
	}

	interface Func {
		void process( String str1, String str2);
	}

	// useless
	void split( String str, String sep, Func func) {
		String[] split = str.split( sep);
		func.process( split[0], split[1]);
	}

	static String testInstr = """
		{ "bank" : { "bank" : "somebank", "iban" : "TR700005901010130101011089", "name" : "somebank name" },  "bankNotes" : [ { "msg" : "You can only send TRY from an individual bank account registered in your name.", "type" : 1 }, { "msg" : "You can withdraw and send money whenever you want during bank working hours.", "type" : 1 } ], "otp" : "4970", "type" : "TRY_BANK_TRANSFER" }""";

	static String emailTemplate = """
			Dear #username#,<br>
			<br>
			Your fiat-to-crypto transaction has been accepted.<br>
			<br>
			Please send funds as follows:<br>
			<br>
			#instructions#
			<br>
			<strong>AMOUNT:</strong> $#amount#<br>
			<br>
			You will be notified by email when funds have been received and the token transfer is complete.<br>
			<br>
			Sincerely,<br>
			<br>
			The Reflection team<br>
			<br>
			CREATED AT: #date#<br>
			""";
}
