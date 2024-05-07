package web3;

import common.Util;
import fireblocks.RetVal;

public class Busd extends Stablecoin {
	private IBusd m_core;
	
	public Busd(String address, int decimals, String name, IBusd core) throws Exception {
		super( address, decimals, name);
		Util.require( core != null, "null core");
		m_core = core;
	}

	public RetVal mint( String callerKey, String address, double amount) throws Exception {
		return m_core.mint( callerKey, address, amount);
	}
//
//	// you need to pass the approver id or name for FB;
//	// what do you need for Web3 impl?
	public RetVal approve(String caller, String spenderAddr, double amt) throws Exception {
		return m_core.approve( caller, spenderAddr, amt);
	}

//	double getAllowance(String wallet, String rusdAddr) throws Exception {
//	}
	
	public interface IBusd {
		RetVal approve( String callerKey, String spenderAddr, double amt) throws Exception;
		RetVal mint( String callerKey, String address, double amount) throws Exception;
	}
}
