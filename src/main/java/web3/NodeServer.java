package web3;

import java.util.HashMap;

/** This is deprecated; use NodeInstance or Config.node() instead; 
 *  kept here for backwards compatibility */
public class NodeServer {
	private static NodeInstance inst; 	// same instance as in Config

	/** same instance as in Config; use Config.node() */
	public static void setInstance( NodeInstance node) {
		inst = node;
	}
	
	public static double getNativeBalance(String walletAddr) throws Exception {
		return inst.getNativeBalance(walletAddr);
	}

	public static Fees queryFees() throws Exception {
		return inst.queryFees();
	}

	public static double getTotalSupply(String contractAddr, int decimals) throws Exception {
		return inst.getTotalSupply(contractAddr, decimals);
	}
	
	public static double getAllowance(String contractAddr, String approverAddr, String spenderAddr, int decimals) throws Exception {
		return inst.getAllowance(contractAddr, approverAddr, spenderAddr, decimals);
	}
	
	public synchronized static int getDecimals(String contractAddr) throws Exception {
		return inst.getDecimals(contractAddr);
	}

	public static void setDecimals(int decimals, String[] addresses) {
		if (inst != null) {  // lame hack to fix mdserver
			inst.setDecimals(decimals, addresses);
		}
	}

	/** @param decimals pass zero for lookup */
	public static HashMap<String, Double> reqPositionsMap(String walletAddr, String[] contracts, int decimals) throws Exception {
		return inst.reqPositionsMap(walletAddr, contracts, decimals);
	}
	
	public static double getBalance(String contractAddr, String walletAddr, int decimals) throws Exception {
		return inst.getBalance(contractAddr, walletAddr, decimals);
	}
	
	public static boolean isKnownTransaction(String transHash) throws Exception {
		return inst.isKnownTransaction(transHash);
	}
}
