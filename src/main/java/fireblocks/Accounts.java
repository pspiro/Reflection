package fireblocks;

import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.Util;
import positions.Wallet;
import reflection.Config;
import tw.util.S;

/** Works for prod and test. */
public class Accounts {
	private JsonArray m_accounts;   // create a map. pas
	private boolean m_read;
	private String[] m_admins;
	private int m_nextAdminIndex = -1;
	private final HashMap<String,String> m_mapUserToAdmin = new HashMap<>();
	private HashMap<Integer,String> m_addressMap = new HashMap<>(); // map account id to wallet address (mixed case)  
	
	public static Accounts instance = new Accounts();

	private Accounts() {
	}
	
	public static void main(String[] args) throws Exception {
		Config.readFrom("Dt-config");
		S.out( instance.getAddress(4) );
	}

	public void display() throws Exception {
		read();
		S.out( m_accounts);
	}
	
	public synchronized void read() throws Exception {
		if (!m_read) {
			S.out( "Querying Fireblocks accounts");
			m_accounts = Fireblocks.fetchObject("/v1/vault/accounts_paged")
					.getArray("accounts");
			m_read = true;
		}
	}

	public int getId( String name) throws Exception {
		return Integer.valueOf( getAccount(name).getString("id") );
	}
	
	JsonObject getAccount(String name) throws Exception {
		read();
		
		for (JsonObject account : m_accounts) {
			if (account.getString("name").equals( name) ) {
				return account;
			}
		}
		throw new Exception( "No such account: " + name);
	}
	
	public Wallet getWallet(String accountName) throws Exception {
		return new Wallet(getAddress(accountName));
	}
	
	/** Returns the wallet address of the platform native token (mixed case).
	 *  Note that other tokens (like Bitcoin) would have a different addresses. */
	public String getAddress(String accountName) throws Exception {
		return getAddress( getId(accountName) );
	}
	
	/** Returns the wallet address of the platform native token (mixed case).
	 *  Note that other tokens (like Bitcoin) would have a different addresses. */
	public synchronized String getAddress(int accountId) throws Exception {
		String address = m_addressMap.get(accountId);
		if (address == null) {
			S.out("Querying wallet address for %s", accountId);
			String url = String.format("/v1/vault/accounts/%s/%s/addresses", accountId, Fireblocks.platformBase);
			JsonArray ar = Fireblocks.fetchArray(url);
			Util.require(ar.size() > 0, "The wallet does not have an address for the native token; add some native token to the wallet");
			address = ar.getJsonObj(0).getString("address");
			m_addressMap.put( accountId, address);
		}
		return address;
	}
	
	/** Set the list of admins
	 *  @param admins is comma-delimited */
	public void setAdmins( String admins) {
		m_admins = admins.split(",");
		S.out( "Fireblocks admin accounts are: %s", admins);
	}
		
	public int getAdminAccountId(String userAddr) throws Exception {
		return getId( getAdmin(userAddr) );
	}
	
	private synchronized String getAdmin(String userAddr) throws Exception {
		Util.require(m_admins != null && m_admins.length > 0, "No admins set");
		
		String admin = m_mapUserToAdmin.get(userAddr);
		if (admin == null) {
			m_nextAdminIndex = (m_nextAdminIndex + 1) % m_admins.length;
			admin = m_admins[m_nextAdminIndex];
			m_mapUserToAdmin.put(userAddr, admin);
		}
		return admin;
	}
	
}
