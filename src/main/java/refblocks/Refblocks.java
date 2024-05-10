package refblocks;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.json.simple.JsonObject;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.StaticEIP1559GasProvider;
import org.web3j.tx.response.EmptyTransactionReceipt;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import common.Util;
import http.MyClient;
import tw.util.S;
import web3.Erc20;

/** Support code for Web3j library */
public class Refblocks {
	static final BigInteger defaultBaseFee = BigInteger.valueOf(1_000_000_000L);  // used only if we can't fetch it
	static final BigInteger defaultPriorityFee = BigInteger.valueOf(35_000_000_000L);  // used only if we can't fetch it
	static final long deployGas = 2000000;
	static Web3j web3j;
	static long chainId;  // set from Config
	static String gasUrl = "https://api.polygonscan.com/api?module=gastracker&action=gasoracle"; // api to get gas
	private static String polygonRpcUrl = "https://polygon-rpc.com/";

	/** Called when Config is read */
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
			S.sleep( 200);  // don't break pacing of max 5 req per second; how do they know it's me???
			JsonObject json = MyClient.getJson( gasUrl)
					.getObject( "result");
			fees.baseFee = json.getBlockchain( "suggestBaseFee", 9); // convert base fee from gwei to wei
			fees.priorityFee = json.getBlockchain( "FastGasPrice", 9);  // it's very unclear if this is returning the priority fee or the total fee, but it doesn't matter because the base fee is so low 
		} catch (Exception e) {
			if (e.getMessage().contains("Max rate limit reached") ) {
				S.out( "Error: Max rate limit exceeded for getFees(); using default values");
			}
			else {
				e.printStackTrace();
			}
			fees.baseFee = defaultBaseFee;
			fees.priorityFee = defaultPriorityFee;
		}
		
		return fees;
	}
	
	public static String toString(TransactionReceipt receipt) {
		BigInteger gasUsed = decodeQuantity( receipt.getGasUsedRaw() );
		BigInteger gasPrice = decodeQuantity( receipt.getEffectiveGasPrice() );
		
		return String.format(
				"statusOK=%s  hash=%s  gasUsed=%s  gasPrice=%s  totalCost=%s matic  reason=%s",
				receipt.isStatusOK(),
				receipt.getTransactionHash(),
				gasUsed,
				gasPrice,
				Erc20.fromBlockchain( gasUsed.multiply( gasPrice).toString(), 18),
				receipt.getRevertReason()
				);
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
	
	/** This transaction processor returns immediately; user should then call
	 *  reallyWait() to wait for the transaction receipt. That could be done
	 *  by RbRetVal. */
	static class DelayedTrp extends PollingTransactionReceiptProcessor {
	    private String m_transactionHash;
	    
	    DelayedTrp( Web3j web3j) {
	    	super( web3j, 5000, 40);
	    }

	    /** return immediately with an empty receipt */
		@Override public TransactionReceipt waitForTransactionReceipt(String transactionHash)
	            throws IOException, TransactionException {
			
			m_transactionHash = transactionHash;
			return new EmptyTransactionReceipt( transactionHash);
	    }

		/** wait for the receipt */
	    public TransactionReceipt reallyWait()
	            throws IOException, TransactionException {

	        return super.waitForTransactionReceipt(m_transactionHash);
	    }
	}
	
	/** Used with the exec() function below */
	interface Func {
		RemoteFunctionCall<TransactionReceipt> call( TransactionManager tm) throws Exception;
	}
	
	/** If we use DelayedTrp, the call returns immediately; you then call
	 *  RetVal.wait(). Otherwise, the call blocks until we have the trans receipt */
	static RbRetVal exec( String callerKey, Func function) throws Exception {
		try {
			DelayedTrp trp = new DelayedTrp( web3j);

			RawTransactionManager tm = new RawTransactionManager(
					web3j,
					Credentials.create( callerKey ), // how to do this elegantly
					chainId,
					trp);
			
			TransactionReceipt receipt = function.call( tm).send();  // returns empty receipt
			Util.require( receipt instanceof EmptyTransactionReceipt, "should be EmptyReceipt; use DelayedTrp");

			return new RbRetVal( trp);
		}
		catch( Exception e) {
			S.out( "Error for caller %s: %s", callerKey, e.getMessage() );
			throw e;
		}
	}
	
	/** This transaction manager returns ASAP; caller then calls 
	 *  DelayedTrp.reallyWait() to wait for the transaction receipt;
	 *  used by exec(). */
	public static RawTransactionManager getInstantTm(String privateKey) throws Exception {
		Util.require( chainId != 0, "set chainId");
		
		return new RawTransactionManager(
				web3j,
				Credentials.create( privateKey ),
				chainId,
				new DelayedTrp( web3j) );
	}

	/** This transaction manager waits for the transaction receipt. If there is an
	 *  error, it queries for the real error text from the contract.
	 *  Used by deploy(). */
	public static RawTransactionManager getWaitingTm(String privateKey) throws Exception {
		Util.require( chainId != 0, "set chainId");
		
		return new RawTransactionManager(
				web3j,
				Credentials.create( privateKey ),
				chainId );
	}
	
	/** Get the EIP1559 gas provider which knows the base fee, priority fee, and total fee */
	public static StaticEIP1559GasProvider getGp( long units) {
		Fees fees = getFees();
		
		return new StaticEIP1559GasProvider( // fails with this
				chainId,
				fees.totalFee(),
				fees.priorityFee(),
				BigInteger.valueOf(units)
				);
	}

	/** getAddress() might be better */
	public static String getPublicKey(String privateKey) {
		return Credentials.create( privateKey ).getAddress();
	}
	
	/** Transfer native token */
	public static String transfer(String privateKey, String to, double amt) throws Exception {
		Credentials cred = Credentials.create( privateKey);

		S.out( "Transferring %s matic from %s to %s",
				amt, cred.getAddress(), to); 

		TransactionReceipt receipt = Transfer.sendFundsEIP1559(
		        web3j, 
		        cred,
		        to,
		        BigDecimal.valueOf( amt),
		        Convert.Unit.ETHER,
		        BigInteger.valueOf( 40000),
		        getFees().priorityFee(),
		        getFees().priorityFee()
				).send();

		S.out( "transferred hash " + receipt.getTransactionHash() );
		return receipt.getTransactionHash();
	}
}

// error.getData() is null so toString() fails and you don't get to see the real error
// need to fix or override
// you get get address from private key using Credentials
// TransactionReceiptProcessor is involved
// uses PollingTransactionReceiptProcessor, use a custom ctor
// QueuingTransactionReceiptProcessor this one returns asap and then queries in the background 
// it polls only every 15 sec; that's too slow  JsonRpc2_0Web3j.DEFAULT_BLOCK_TIME = 15 * 1000;
// consider FastRawTransactionManager to have multiple trans per block, I assume per caller
