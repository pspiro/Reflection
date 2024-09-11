package common;

import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import reflection.MySqlConnection;
import test.MyTimer;
import tw.google.NewSheet;
import tw.util.S;
import web3.Rusd;

/** chared by monitor and RefApi */
public class SignupReport {
	/** create a report combining sign-ups, jotform, connections, and transactions
	 *  used to generate the "/api/sag" report
	 * @param progressBar could be null */
	public static JsonArray create( int days, MySqlConnection sql, Rusd rusd, Runnable progressBar) throws Exception {
		//super.refresh();
		int DaysLookback = days;

		MyTimer t = new MyTimer();
		
		// build trans-count table
		HashMap<String,Integer> transMap = new HashMap<>();
		for (var rec : sql.queryToJson( "select wallet_public_key, count(*) from transactions group by wallet_public_key") ) {
			transMap.put( rec.getString( "wallet_public_key"), rec.getInt( "count") );
		}

		// build jotmap
		t.next( "building jotmap");
		HashMap<String,JsonObject> jotMap = new HashMap<>();
		for (var jot : NewSheet.getTab( NewSheet.Prefinery, "jotform").queryToJson() ) {
			var email = jot.getString( "Email").toLowerCase();
			
			jotMap.put( email, jot);
		}
		
		// read users table; build maps of email to user and wallet to user
		t.next( "building user maps");
		HashMap<String,JsonObject> userByEmail = new HashMap<>();
		HashMap<String,JsonObject> userByWallet = new HashMap<>();
		for (var user : sql.queryToJson( "select * from users") ) {
			Util.iff( user.getString( "email"), email -> userByEmail.put( email, user) );
			userByWallet.put( user.getString( "wallet_public_key"), user);
		}
		
		// query sign-ups
		//"created_at,email,first,last,country,referer,ip,utm_source",
		t.next( "querying signups");
		var signups = sql.queryToJson( "select * from signup where created_at >= '%s' order by created_at", 
				Util.yToS.format( System.currentTimeMillis() - DaysLookback * Util.DAY) );
		S.out( "  read %s signups", signups.size() );

		t.next( "adjusting email");
		signups.update("email", email -> email.toString().toLowerCase() );
		
		for (var signup : signups) {
			var email = signup.getString( "email");
			S.out( "processing signup %s", email);

			// look for user in jotform
			var jot = jotMap.get( email);
			if (jot != null) {
				signup.put( "jotform", "Yes");
			}
			
			// look for user in users table, by email
			var user = userByEmail.get( email);
			if (user == null && jot != null) {
				// then by wallet address
				user = userByWallet.get( jot.getString( "Wallet address").toLowerCase() );
				if (user != null) {
					S.out( "found user by wallet, not by email");
				}
			}

			if (user != null) {
				signup.put( "connected", "Yes");
				
				var wallet = user.getString( "wallet_public_key");
				
				Integer transCount = transMap.get( wallet);
				if (transCount != null && transCount > 0) {
					signup.put( "transactions", transCount);
				}

				t.next( "querying RUSD balance");  // could you send a single query for all?
				double rusdBal = rusd.getPosition( wallet);
				signup.put( "rusd", rusdBal);
				t.done();
			}
			
			if (progressBar != null) {
				progressBar.run();
			}
		}
		
		return signups;
	}
}
