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

	public RetVal mint(String address, double amount) throws Exception {
		return m_core.mint( address, amount);
	}

	public void deploy(String filename) throws Exception {
		throw new Exception( "not implemented");
	}
	
	// you need to pass the approver id or name for FB;
	// what do you need for Web3 impl?
	public RetVal approve(String spenderAddr, double amt) throws Exception {
		return m_core.approve( spenderAddr, amt);
	}

//	double getAllowance(String wallet, String rusdAddr) throws Exception {
//	}
	
	public interface IBusd {
		RetVal mint(String address, double amount) throws Exception;
		RetVal approve(String spenderAddr, double amt) throws Exception;
	}
}
