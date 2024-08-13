package refblocks;

import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;

import common.Util;
import tw.util.S;
import web3.Erc20;
import web3.RetVal;

public class RbTok extends Erc20 {

	protected RbTok(String address, int decimals, String name) {
		super(address, decimals, name);
	}

	public Busd loadErc20(TransactionManager tm) throws Exception {
		return Busd.load(  // could use any ERC-20 contract here 
				address(), 
				Refblocks.web3j, 
				tm, 
				Refblocks.getGp( 500000)  // this is good for everything except deployment
				);
	}

	public RetVal approve(String approverKey, String spenderAddr, double amt) throws Exception {
		Util.reqValidKey(approverKey);
		Util.reqValidAddress(spenderAddr);
		
		S.out( "%s approving %s to spend %s %s", 
				Refblocks.getAddressPk(approverKey), spenderAddr, amt, m_name);
		
		return Refblocks.exec( approverKey, tm -> loadErc20( tm)
				.approve( spenderAddr, toBlockchain( amt) ) );
	}
	
//	public static getBalance( String contractAddr, int decimals, String walletAddr) {
//		RbTok tok = new RbTok( contractAddr, )
//	}
}
