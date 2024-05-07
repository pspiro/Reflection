package refblocks;

import java.math.BigInteger;

import org.json.simple.JsonObject;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.JsonRpcError;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.StaticEIP1559GasProvider;
import org.web3j.utils.Numeric;

import common.Util;
import http.MyClient;
import web3.Erc20;

/** Support code for Web3j library */
public class Refblocks {
	static final BigInteger defaultBaseFee = BigInteger.valueOf(1_000_000_000L);  // used only if we can't fetch it
	static final BigInteger defaultPriorityFee = BigInteger.valueOf(35_000_000_000L);  // used only if we can't fetch it
	static final long deployGas = 2000000;
	static Web3j web3j;
	static long chainId;  // set from Config
	
	public static void setChainId( long id, String rpcUrl) {
		chainId = id;
		web3j = Web3j.build( new HttpService( rpcUrl) );
	}

	static class Fees {
		private BigInteger baseFee;
		private BigInteger priorityFee;  // use the highest base fee; not sure if this includes the base fee already or not
		
		public BigInteger totalFee() {
			return baseFee.add( priorityFee);
		}

		public BigInteger priorityFee() {
			return priorityFee;
		}
	}

	
	/** returns same fees that are displayed here: https://polygonscan.com/gastracker */
	static Fees getFees() {
		Fees fees = new Fees();
		
		try {
			JsonObject json = MyClient.getJson( "https://api.polygonscan.com/api?module=gastracker&action=gasoracle")
					.getObject( "result");
			fees.baseFee = json.getBlockchain( "suggestBaseFee", 9); // convert base fee from gwei to wei
			fees.priorityFee = json.getBlockchain( "FastGasPrice", 9);  // it's very unclear if this is returning the priority fee or the total fee, but it doesn't matter because the base fee is so low 
		} catch (Exception e) {
			e.printStackTrace();
			fees.baseFee = defaultBaseFee;
			fees.priorityFee = defaultPriorityFee;
		}
		
		return fees;
	}

	public static void showReceipt(TransactionReceipt trans) {
		BigInteger gasPrice = decodeQuantity( trans.getEffectiveGasPrice() );
		String str = String.format(
				"hash=%s  gasUsed=%s  gasPrice=%s  totalCost=%s matic",
				trans.getTransactionHash(),
				trans.getGasUsed(),
				gasPrice,
				Erc20.fromBlockchain( trans.getGasUsed().multiply( gasPrice).toString(), 18)
				);
		System.out.println( str);
		System.out.println();
	}
	
	/** Can take hex or decimal */
	private static BigInteger decodeQuantity(String hex) {
		try {
			return Numeric.decodeQuantity( hex);
		}
		catch( Exception e) {
			return BigInteger.ZERO;
		}
	}

	/** Get the transaction manager which knows the chainId and the private key of the caller */
	public static RawTransactionManager getTm(String privateKey) throws Exception {
		Util.require( chainId != 0, "set chainId");
		
		return new RawTransactionManager(
				web3j,
				Credentials.create( privateKey ),
				chainId);
	}

	/** Get the gas provider which knows the base fee, priority fee, and total fee */
	public static StaticEIP1559GasProvider getGp( long units) {
		Fees fees = getFees();
		
		return new StaticEIP1559GasProvider( // fails with this
				chainId,
				fees.totalFee(),
				fees.priorityFee(),
				BigInteger.valueOf(units)
				);
	}
}
// there is a bug here in Contract.executeTransaction()
//} catch (JsonRpcError error) {
//    throw new TransactionException(error.getData().toString());
//}
// error.getData() is null so toString() fails and you don't get to see the real error
// need to fix or override
// you get get address from private key using Credentials