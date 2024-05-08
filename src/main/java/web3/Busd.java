package web3;

import common.Util;
import fireblocks.RetVal;

public class Busd extends Stablecoin {
	private IBusd m_core;
	private String m_adminKey; // private key or "Admin1" for FB

	public Busd(String address, int decimals, String name, String adminKey, IBusd core) throws Exception {
		super( address, decimals, name);

		Util.require( core != null, "null core");
		
		m_adminKey = adminKey;
		m_core = core;
	}

	/** For testing only; anyone can call this but they must have some gas */
	public RetVal mint( String address, double amount) throws Exception {
		return m_core.mint( m_adminKey, address, amount);
	}

	public RetVal approve(String caller, String spenderAddr, double amt) throws Exception {
		return m_core.approve( caller, spenderAddr, amt);
	}

	public interface IBusd {
		RetVal approve( String callerKey, String spenderAddr, double amt) throws Exception;
		RetVal mint( String callerKey, String address, double amount) throws Exception;
	}
}
