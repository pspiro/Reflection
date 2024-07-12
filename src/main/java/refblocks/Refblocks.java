package refblocks;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
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
import web3.Fees;
import web3.NodeServer;

/** Support code for Web3j library */
public class Refblocks {
	static final BigInteger defaultBaseFee = BigInteger.valueOf(1_000_000_000L);  // used only if we can't fetch it
	static final BigInteger defaultPriorityFee = BigInteger.valueOf(35_000_000_000L);  // used only if we can't fetch it
	static final long deployGas = 2000000;
	public static final long PollingInterval = 5000;  // polling interval for transaction receipt
	static Web3j web3j;
	static NodeServer nodeServer;
	static long chainId;  // set from Config
	//private static String polygonRpcUrl = "https://polygon-rpc.com/";
	static HashMap<String,FasterTm> mgrMap = new HashMap<>();

	/** Called when Config is read */
	public static void setChainId( long id, String rpcUrl) {
		chainId = id;
		web3j = Web3j.build( new HttpService( rpcUrl) );
		nodeServer = new NodeServer( rpcUrl);
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
	public static BigInteger decodeQuantity(String hex) {
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
	    DelayedTrp() {
	    	super( web3j, PollingInterval, 24); // two minutes
	    }

	    /** return immediately with an empty receipt */
		@Override public TransactionReceipt waitForTransactionReceipt(String transactionHash)
	            throws IOException, TransactionException {

			return new EmptyTransactionReceipt( transactionHash);
	    }

		/** wait for the transaction receipt; not that this is a difference instance than
		 *  the one where the initial wait is called; it doesn't matter since there is 
		 *  no state */
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
		TransactionManager tm = null;
		
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
	public static void showAllNonces(String address) throws Exception {
		S.out( "%s nonce  finalized=%s  latest=%s  pending=%s",
        		address,
        		getNonce( address, DefaultBlockParameterName.FINALIZED),
        		getNonce( address, DefaultBlockParameterName.LATEST),
        		getNonce( address, DefaultBlockParameterName.PENDING)
        		);
	}
	
	static BigInteger getNonce(String address, DefaultBlockParameterName type) throws Exception {
		return decodeQuantity( web3j.ethGetTransactionCount( address, type)
				.send()
				.getResult() );
	}
	
	/** note that we could use a singleton DelayedTrp if desired; there is no state */
	protected static TransactionManager getFasterTm(String callerKey) {
//		return new SlowerTm(web3j, Credentials.create( callerKey), new DelayedTrp() );
		return Util.getOrCreate( mgrMap, callerKey, () -> new FasterTm( 
				web3j, 
				Credentials.create( callerKey), 
				new DelayedTrp() ) );
	}

	/** This is only used for deployment and minting stock tokens.
	 *  Should not be used in production because nonce will get mixed up with FasterTm
	 *  
	 *  It polls for receipt every 15 sec.
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
	public static StaticEIP1559GasProvider getGp( long unitsIn) throws Exception {
		BigInteger units = BigInteger.valueOf( unitsIn);
		
		Fees fees = nodeServer.queryFees();
		fees.showFees(units);

		return new StaticEIP1559GasProvider( // fails with this
				chainId,
				fees.totalFee(),
				fees.priorityFee(),
				units
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

            // this could be reduced if needed
    		BigInteger gasUnits = BigInteger.valueOf( 40000);
    		
    		Fees fees = nodeServer.queryFees();
    		fees.showFees( gasUnits);
    		
    		return sendEIP1559(
	                chainId,
	                ensResolver.resolve(toAddress),
	                "",
	                weiValue.toBigIntegerExact(),
	                gasUnits,  // higher than needed, you can reduce it if desired
	                fees.priorityFee(),
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
	
	static class SlowerTm extends RawTransactionManager {
		static int Interval = 60000; // re-query for nonce after one minute to get reset in case we get out of sync  

		private String m_key;  // for debugonly

		public SlowerTm(Web3j web3j, Credentials credentials, TransactionReceiptProcessor trp) {
			super(web3j, credentials, chainId, trp);
			m_key = credentials.getAddress().substring( 0, 6);
		}
		
		@Override protected synchronized BigInteger getNonce() throws IOException {
			BigInteger nonce = super.getNonce();
			S.out( "  using nonce %s for %s", nonce, m_key);
			return nonce;
		}
	}

	
	/** This transaction manager increments the nonce for each call but
	 *  queries for the nonce after one minute to reset it in case we get
	 *  out of sync */
	static class FasterTm extends FastRawTransactionManager {
		/** re-query for nonce after one minute to reset it in case we get out of sync;
		 *  this would happen if a call is made for the same wallet from another
		 *  application, i.e. Monitor and RefApi.
		 *  
		 *  After a transfer or smart contract call is made, it can take up to 20 sec 
		 *  (the max I observed) for the call to retrieve the next nonce to return
		 *  a higher nonce, so that is the absolute minimum for this setting.
		 *  
		 *  Two alternatives would be:
		 *  a) create a nonce server that all applications use to retrieve the next nonce, or
		 *  b) don't allow two applications to access the same account, e.g. let
		 *  RefAPI use admin1 and Monitor use admin2 
		 */
		static int Interval = 60000;
		

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
				S.out( "  resetting nonce");
				setNonce( BigInteger.valueOf( -1) );
			}
			
			BigInteger nonce = super.getNonce();
			S.out( "  using nonce %s for %s", nonce, m_key);

			m_lastTime = System.currentTimeMillis();

			return nonce;
		}
	}

	/** wait for the transaction receipt with polling */
	public static TransactionReceipt waitForReceipt(TransactionReceipt receipt) throws Exception {
		return new DelayedTrp().reallyWait( receipt);
	}

	private static int getTokenDecimals(String contractAddr) throws Exception {
		Function function = new Function(
				"decimals",
				Arrays.asList(),
				Arrays.asList(new TypeReference<Uint256>() {})
				);

		EthCall response = web3j.ethCall(
				Transaction.createEthCallTransaction(null, contractAddr, FunctionEncoder.encode(function) ),
				org.web3j.protocol.core.DefaultBlockParameterName.LATEST
				).sendAsync().get();

		return Numeric.decodeQuantity( response.getValue() )
				.intValue();
		
		// same as this:
//		String json = """
//				{
//				"jsonrpc": "2.0",
//				"method": "eth_call",
//				"params": [
//					{
//					"to": "<contract_Address>",
//					"data":"0x313ce567"
//					},
//					"latest"
//				],
//				"id":1
//				}";
//				""";

    }
	
	
	/** map contract address (lower case) to number of Decimals, so we only query it once;
	 *  this assumes all calls are on the same blockchain */ 
	static HashMap<String,Integer> decMap = new HashMap<>();
	
	/** Return number of decimals for the contract; first time sends a query,
	 *  subsequent times are map lookup */
	private synchronized static int getDecimals(String contractAddr) throws Exception {
		return Util.getOrCreateEx( decMap, contractAddr.toLowerCase(), () ->
			getTokenDecimals( contractAddr.toLowerCase() ) );
	}
	
	/** Pass zero for decimals to look it up; first time sends a query; if you know it, pass it */
	static public double getERC20Balance(String walletAddr, String contractAddr, int decimals) throws Exception {
		Util.reqValidAddress(walletAddr);
		Util.reqValidAddress(contractAddr);

		Function function = new Function(
				"balanceOf",
				Arrays.asList(new Address(walletAddr)),
				Arrays.asList(new TypeReference<Uint256>() {})
				);

		EthCall response = web3j.ethCall(
				Transaction.createEthCallTransaction(walletAddr, contractAddr, FunctionEncoder.encode(function) ),
				org.web3j.protocol.core.DefaultBlockParameterName.LATEST
				).sendAsync().get();

		return Erc20.fromBlockchain( 
				response.getValue(), 
				decimals == 0 ? getDecimals( contractAddr) : decimals);
	}

	/** Makes a separate call for each one */
	static public HashMap<String, Double> reqPositionsMap(String walletAddr, String[] contracts, int decimals) throws Exception {
		HashMap<String, Double> positionsMap = new HashMap<>();

		for (String contractAddr : contracts) {
			positionsMap.put( contractAddr, getERC20Balance(walletAddr, contractAddr, decimals) );
		}

		return positionsMap;
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