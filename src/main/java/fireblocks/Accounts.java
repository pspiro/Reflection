package fireblocks;

import java.util.HashMap;

import json.MyJsonArray;
import json.MyJsonObject;
import reflection.Util;
import tw.util.S;

/** Works for prod and test. */
public class Accounts {
	private MyJsonArray m_accounts;   // create a map. pas
	private boolean m_read;
	private String[] m_admins;
	private int m_nextAdminIndex = -1;
	private final HashMap<String,String> m_mapUserToAdmin = new HashMap<>();
	
	public static Accounts instance = new Accounts();

	private Accounts() {
	}

	public void display() throws Exception {
		read();
		m_accounts.display();
	}
	
	public synchronized void read() throws Exception {
		if (!m_read) {
			S.out( "Querying Fireblocks accounts");
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
	
	private HashMap<Integer,String> m_addressMap = new HashMap<>(); // map account name to wallet address  
	
	/** Returns the wallet address of the platform base token. Note that
	 *  other tokens (like Bitcoin) would have a different addresses. */
	public String getAddress(String accountName) throws Exception {
		return getAddress( getId(accountName) );
	}
	
	public synchronized String getAddress(int accountId) throws Exception {
		String address = m_addressMap.get(accountId);
		if (address == null) {
			S.out("Querying wallet address for %s", accountId);
			String url = String.format("/v1/vault/accounts/%s/%s/addresses", accountId, Fireblocks.platformBase);
			address = Fireblocks.getArray(url).getJsonObj(0).getString("address");
			m_addressMap.put( accountId, address);
		}
		return address;
	}
	
	/** Set the list of admins
	 *  @param admins is comma-delimited */
	public void setAdmins( String admins) {
		m_admins = admins.split(",");
		S.out( "Setting %s admins", m_admins.length);
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
