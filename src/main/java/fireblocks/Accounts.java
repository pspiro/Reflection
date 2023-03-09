package fireblocks;

import json.MyJsonArray;
import json.MyJsonObject;
import reflection.Config;
import tw.util.S;

/** Works for prod and test. */
public class Accounts {
	private MyJsonArray m_accounts;   // create a map. pas
	private boolean m_read;
	
	public static Accounts instance = new Accounts();

	private Accounts() {
	}
	
	public static void main(String[] args) throws Exception {
		Config config = new Config();
		config.readFromSpreadsheet("Test-config");

		instance.display();
	}
	public void display() throws Exception {
		read();
		m_accounts.display();
	}
	
	public synchronized void read() throws Exception {
		if (!m_read) {
			S.out( "Reading Fireblocks accounts");
			m_accounts = Fireblocks.getObject("/v1/vault/accounts_paged")
					.getAr("accounts");
			m_read = true;
		}
	}

	int getId( String name) throws Exception {
		return Integer.valueOf( getAccount(name).getString("id") );
	}
	
	MyJsonObject getAccount(String name) throws Exception {
		read();
		
		for (MyJsonObject account : m_accounts) {
			if (account.getString("name").equals( name) ) {
				return account;
			}
		}
		throw new Exception( "No such account: " + name);
	}
	
	/** Returns the wallet address of the platform base token. Note that
	 *  other tokens (like Bitcoin) would have a different addresses. */
	String getAddress(String accountName) throws Exception {
		int accountId = getId( accountName);
		
		String url = String.format( "/v1/vault/accounts/%s/%s/addresses", accountId, Fireblocks.platformBase);
		return Fireblocks.getArray(url).getJsonObj(0).getString("address");
	}

	public static int nextAdminId() throws Exception {
		return instance.getId("Admin1");
	}
}
