package web3;

import java.math.BigInteger;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;

import tw.util.S;

/** Support code for Web3j library */
public class Refblocks {
	private Web3j web3j;
	private NodeInstance node;

	/** Called when Config is read */
	public Refblocks( long chainIdIn, Web3j web3jIn, NodeInstance nodeIn) {
		web3j = web3jIn;
		node = nodeIn;
	}

	/** empty string returns zero */
	public static BigInteger decodeQuantity(String hex) {
		try {
			return Erc20.decodeQuantity( hex);
		}
		catch( Exception e) {
			return BigInteger.ZERO;
		}
	}

	/** for debugging, show three types of nonces for one account (wallet address)
	 * @param pending */
	public void showAllNonces(String walletAddr) throws Exception {
		S.out( "%s... nonces  latest=%s  pending=%s",
        		walletAddr.substring( 0, 7),
        		getNonce( walletAddr, DefaultBlockParameterName.LATEST),
        		getNonce( walletAddr, DefaultBlockParameterName.PENDING)
        		);
	}
	
	/** same as NodeInstance.getNonce() */
	BigInteger getNonce(String address, DefaultBlockParameterName type) throws Exception {
		return decodeQuantity( web3j.ethGetTransactionCount( address, type)
				.send()
				.getResult() );
	}

	/** transfer native token; taken from Transfer.sendFundsEIP1559() */ 
	public RetVal transfer(String senderKey, String toAddr, double amt) throws Exception {
		return node.transfer( senderKey, toAddr, amt);
	}
}
