package fireblocks;

import json.MyJsonArray;
import json.MyJsonObject;

/** Works for prod and test. */
public class Accounts {
	MyJsonArray accounts;   // create a map. pas
	
	private Accounts() {
	}
	
	public void read() throws Exception {
		accounts = Fireblocks.getObject("/v1/vault/accounts_paged")
				.getAr("accounts");
	}
	
	MyJsonObject getAccount(String name) throws Exception {
		for (MyJsonObject account : accounts) {
			if (account.getString("name").equals( name) ) {
				return account;
			}
		}
		throw new Exception( "No such account: " + name);
	}

	int getId( String name) throws Exception {
		return Integer.valueOf( getAccount(name).getString("id") );
	}
	
	/** Note that there could be different addresses for different platforms */
	String getAddress(String accountName) throws Exception {
		int accountId = getId( accountName);  // don't query every time. pas
		
		String url = String.format( "/v1/vault/accounts/%s/%s/addresses", accountId, Fireblocks.platformBase);
		return Fireblocks.getArray(url).getJsonObj(0).getString("address");
	}

	public static Accounts instance = new Accounts();
	
	public static int nextAdminId() throws Exception {
		return instance.getId("Admin1");
	}
}
