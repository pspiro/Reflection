package refblocks;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.ManagedTransaction;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.StaticEIP1559GasProvider;
import org.web3j.tx.response.EmptyTransactionReceipt;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import common.Util;
import tw.util.S;
import web3.Erc20;

/** Support code for Web3j library */
public class Refblocks {
	static BigInteger billion = BigInteger.valueOf( 1000000000);
	static final BigInteger defaultBaseFee = BigInteger.valueOf(1_000_000_000L);  // used only if we can't fetch it
	static final BigInteger defaultPriorityFee = BigInteger.valueOf(35_000_000_000L);  // used only if we can't fetch it
	static final long deployGas = 2000000;
	public static final long PollingInterval = 5000;  // polling interval for transaction receipt
	static Web3j web3j;
	static long chainId;  // set from Config
	private static String polygonRpcUrl = "https://polygon-rpc.com/";
	static HashMap<String,FasterTm> mgrMap = new HashMap<>();

	/** Called when Config is read */
	public static void setChainId( long id, String rpcUrl) {
		chainId = id;
		web3j = Web3j.build( new HttpService( rpcUrl) );
	}

	/** returns same fees that are displayed here: https://polygonscan.com/gastracker */
	
	public static String toString(TransactionReceipt receipt) throws Exception {
		Util.require( receipt != null, "null receipt");

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
	 *  by RbRetVal.
	 *  
	 *  NOTE you could inherit from QueuingTransactionReceiptProcessor to query
	 *  from a single thread sequentially */
	static class DelayedTrp extends PollingTransactionReceiptProcessor {
	    DelayedTrp( Web3j web3j) {
	    	super( web3j, PollingInterval, 24); // two minutes
	    }

	    /** return immediately with an empty receipt */
		@Override public TransactionReceipt waitForTransactionReceipt(String transactionHash)
	            throws IOException, TransactionException {

			return new EmptyTransactionReceipt( transactionHash);
	    }

		/** wait for the transaction receipt */
		public TransactionReceipt reallyWait(TransactionReceipt receipt) throws Exception {
	        return super.waitForTransactionReceipt( receipt.getTransactionHash() );
	    }
	}
	
	/** Used with the exec() function below */
	interface Func {
		RemoteFunctionCall<TransactionReceipt> getCall( TransactionManager tm) throws Exception;
	}
		
	/** Uses MyTransMgr and DelayedTrp. The call returns immediately; you then call
	 *  RetVal.wait(). Otherwise, the call blocks until we have the trans receipt
	 *  
	 *  NOTE that this is no longer needed because the tm can be retrieved with a static function
	 *  because we have one tm per address, not per call */
	static RbRetVal exec( String callerKey, Func function) throws Exception {
		FasterTm tm = null;
		
		try {
			tm = getFasterTm( callerKey);
			
			TransactionReceipt receipt = function.getCall( tm).send();  // EmptyTransactionReceipt
			Util.require( receipt instanceof EmptyTransactionReceipt, "should be EmptyReceipt; use DelayedTrp");

			return new RbRetVal( receipt);
		}
		catch( Exception e) {
			S.out( "Error for caller %s: %s", 
					tm != null ? tm.getFromAddress() : "???", 
					e.getMessage() );
			throw e;
		}
	}
	
	/** If a transaction is submitted with too low, gas, it can get stuck and remain
	 *  in pending state indefinitely. To cancel it, you submit a new transaction
	 *  with same nonce and sufficient gas. You must consider if you want the subsequent
	 *  transactions to go through or not and either cancel them first or not
	 * @param address
	 * @throws Exception 
	 */
	static void cancelStuckTransaction(String address) throws Exception {
		// start by showing all nonces and figuring out which one or ones
		// need to be canceled
		showAllNonces(address);
		
		// get the nonce to cancel
		// create a RawTransactionManager that will create a transaction what that nonce
	}

	/** for debugging 
	 * @param pending */
	static void showAllNonces(String address) throws Exception {
		Refblocks.showNonce( address, DefaultBlockParameterName.ACCEPTED);
		Refblocks.showNonce( address, DefaultBlockParameterName.EARLIEST);
		Refblocks.showNonce( address, DefaultBlockParameterName.FINALIZED);
		Refblocks.showNonce( address, DefaultBlockParameterName.LATEST);
		Refblocks.showNonce( address, DefaultBlockParameterName.PENDING);
		Refblocks.showNonce( address, DefaultBlockParameterName.SAFE);
	}
	
	static void showNonce(String address, DefaultBlockParameterName type) throws Exception {
		EthGetTransactionCount count = web3j.ethGetTransactionCount( address, type)
				.send();
        S.out( "%s nonce is %s", type, count.getTransactionCount() );
	}
	
	/** note that we could use a singleton DelayedTrp if desired; there is no state */
	protected static FasterTm getFasterTm(String callerKey) {
		return Util.getOrCreate( mgrMap, callerKey, () -> new FasterTm( 
				web3j, 
				Credentials.create( callerKey), 
				new DelayedTrp( web3j) ) );
	}

	/** This is only used for deployment and minting stock tokens.
	 *  Should not be used in production because nonce will get mixed up with FasterTm
	 * 
	 *  This transaction manager queries for the nonce and waits for the transaction 
	 *  receipt. If there is an error, it queries for the real error text from the contract.
	 *  Used by deploy(). Rapid transactions with this hang because even if you wait for
	 *  the receipt, the next nonce that gets returned is not incremented */
	public static RawTransactionManager getWaitingTm(String callerKey) throws Exception {
		Util.require( chainId != 0, "set chainId");
		
		return new RawTransactionManager(
				web3j,
				Credentials.create( callerKey ),
				chainId );
	}
	
	/** Get the EIP1559 gas provider which knows the base fee, priority fee, and total fee 
	 * @throws Exception */
	public static StaticEIP1559GasProvider getGp( long units) throws Exception {
		Fees fees = Fees.fetch();

		S.out( "total gas price: %s gwei", 
				fees.totalFee().divide( billion) );

		return new StaticEIP1559GasProvider( // fails with this
				chainId,
				fees.totalFee(),
				fees.priorityFee(),
				BigInteger.valueOf(units)
				);
	}

	/** @param privateKey must be a wallet private key;
	 *  if it could be a Fireblocks key, use Matic.getAddress() */
	protected static String getAddressPk(String privateKey) {
		return Credentials.create( privateKey ).getAddress();
	}
	
	/** Create our own transaction class so we can use our own TransactionManager.
	 *  We would like to use the org.web3j.tx.Transfer class but the send() methods are private.
	 *  Alternatively, you could copy the code from the library and then modify it directly */
	static class MyTransfer extends ManagedTransaction {
		MyTransfer(TransactionManager tm) {
			super( Refblocks.web3j, tm);
		}
		
		/** @param amt is decimal amount of ether */
	    TransactionReceipt send( String toAddress, BigDecimal amt)
	            throws Exception {

	        BigDecimal weiValue = Convert.toWei(amt, Convert.Unit.ETHER);
            Util.require( Numeric.isIntegerValue(weiValue), "Non decimal Wei value provided");

    		Fees fees = Fees.fetch();

    		S.out( "baseGas=%s  priority=%s  maxCost=%s",  
    				fees.baseFee().divide( billion),
    				fees.priorityFee().divide( billion),
    				fees.totalFee().doubleValue() / Math.pow( 10, 18) );

    		return sendEIP1559(
	                chainId,
	                ensResolver.resolve(toAddress),
	                "",
	                weiValue.toBigIntegerExact(),
	                BigInteger.valueOf( 40000),  // higher than needed, you can reduce it if desired
	                fees.priorityFee() ,
	                fees.totalFee() );
	    }
	}
	
	/** taken from Transfer.sendFundsEIP1559() 
	 * @throws Exception */
	static RbRetVal transfer(String senderKey, String toAddr, double amt) throws Exception {
		S.out( "transferring %s matic from %s to %s",
				amt,
				Refblocks.getAddressPk(senderKey),
				toAddr);
		
		TransactionReceipt receipt = new MyTransfer( getFasterTm(senderKey) )
				.send( toAddr, BigDecimal.valueOf( amt) );
		
		return new RbRetVal( receipt);
	}
	
	
	/** This transaction manager increments the nonce for each call but
	 *  queries for the nonce after one minute to reset it in case we get
	 *  out of sync */
	static class FasterTm extends FastRawTransactionManager {
		static int Interval = 60000; // re-query for nonce after one minute to get reset in case we get out of sync  

		private String m_key;  // for debugonly
		private long m_lastTime;

		public FasterTm(Web3j web3j, Credentials credentials, TransactionReceiptProcessor trp) {
			super(web3j, credentials, chainId, trp);
			m_key = credentials.getAddress().substring( 0, 6);
		}

		@Override protected synchronized BigInteger getNonce() throws IOException {
			// reset nonce after one min in case we get out of sync; we should also do this
			// if there is an exception relating to the nonce
			if (getCurrentNonce().signum() > 0 && System.currentTimeMillis() - m_lastTime > Interval) {
				S.out( "resetting nonce for %s", m_key);
				setNonce( BigInteger.valueOf( -1) );
			}
			
			BigInteger nonce = super.getNonce();
			S.out( "return nonce %s for %s", nonce, m_key);

			m_lastTime = System.currentTimeMillis();

			return nonce;
		}
	}

	/** wait for the transaction receipt with polling */
	public static TransactionReceipt waitForReceipt(TransactionReceipt receipt) throws Exception {
		return new DelayedTrp( web3j).reallyWait( receipt);
	}
}

// MUST we wait for the transaction receipt from first call before sending second call???

// error.getData() is null so toString() fails and you don't get to see the real error
// need to fix or override
// you get get address from private key using Credentials
// TransactionReceiptProcessor is involved
// uses PollingTransactionReceiptProcessor, use a custom ctor
// QueuingTransactionReceiptProcessor this one returns asap and then queries in the background 
// it polls only every 15 sec; that's too slow  JsonRpc2_0Web3j.DEFAULT_BLOCK_TIME = 15 * 1000;
// consider FastRawTransactionManager to have multiple trans per block, I assume per caller
// use RevertReasonExtractor.extractRevertReason() to get error text
// use FunctionEncoder.encode(function) to get data