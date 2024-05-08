package refblocks;

import java.io.IOException;
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
import org.web3j.tx.gas.StaticEIP1559GasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import common.Util;
import fireblocks.RetVal;
import http.MyClient;
import reflection.Config;
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

	public static void main( String[] args) throws Exception {
		//setChainId( 137, polygonRpcUrl);
		
		S.out( RbBusd.deploy( Config.ask( "Dt").ownerKey() ) );

//		Busd busd = Busd.deploy( 
//				web3j,
//				getTm("cd11a9b7eb7140da458eba6dad1bcc206d41a1c3c677068e2593370165446f3d"),
//				getGp(2000000)
//				).send();
				
	}
	
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
			JsonObject json = MyClient.getJson( gasUrl)
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
	
	public static class RbRetVal extends RetVal {
		private TransactionReceipt m_receipt;
		private DelayedTrp m_trp;

		/** If we already have the receipt */
		RbRetVal( TransactionReceipt receipt) {
			super( null);
			m_receipt = receipt;
		}
		
		/** If we need to wait for the receipt */
		public RbRetVal(DelayedTrp trp) {
			super( null);
			m_trp = trp;
		}

		/** Wait for receipt or return it hash if we already have it */
		public String waitForHash() throws Exception {
			if (m_trp != null) {
				m_receipt = m_trp.reallyWait();
				S.out( this);
			}
			return m_receipt.getTransactionHash();
		}
		
		/** This blocks for up to 2 min */
		public RetVal waitForCompleted() throws Exception {
			return this;
		}

		/** This blocks for up to 2 min */
		public RetVal waitForStatus(String status) throws Exception {
			throw new Exception();
		}
		
		@Override public String toString() {
			return m_receipt != null ? Refblocks.toString( m_receipt) : "";
		}
	}

	public static String toString(TransactionReceipt receipt) {
		BigInteger gasPrice = decodeQuantity( receipt.getEffectiveGasPrice() );
		return String.format(
				"hash=%s  gasUsed=%s  gasPrice=%s  totalCost=%s matic  reason=%s",
				receipt.getTransactionHash(),
				receipt.getGasUsed(),
				gasPrice,
				Erc20.fromBlockchain( receipt.getGasUsed().multiply( gasPrice).toString(), 18),
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
	
	static class DelayedTrp extends PollingTransactionReceiptProcessor {
	    private String m_transactionHash;
	    
	    DelayedTrp( Web3j web3j) {
	    	super( web3j, 5000, 40);
	    }

	    /** return immediately with an empty receipt */
		@Override public TransactionReceipt waitForTransactionReceipt(String transactionHash)
	            throws IOException, TransactionException {
			
			m_transactionHash = transactionHash;
			return new EmptyReceipt( transactionHash);
	    }

		/** wait for the receipt */
	    public TransactionReceipt reallyWait()
	            throws IOException, TransactionException {

	        return super.waitForTransactionReceipt(m_transactionHash);
	    }
	}
	
	static class EmptyReceipt extends TransactionReceipt {
		EmptyReceipt( String hash) {
			setTransactionHash( hash);
		}
	}

	interface Func {
		RemoteFunctionCall<TransactionReceipt> call( TransactionManager tm) throws Exception;
	}
	
	static RbRetVal exec( String callerKey, Func function) throws Exception {
		try {
			DelayedTrp trp = new DelayedTrp( web3j);

			RawTransactionManager tm = new RawTransactionManager(
					web3j,
					Credentials.create( callerKey ), // how to do this elegantly
					chainId,
					trp);
			
			TransactionReceipt receipt = function.call( tm).send();  // returns empty receipt
			Util.require( receipt instanceof EmptyReceipt, "should be EmptyReceipt");

			return new RbRetVal( trp);
		}
		catch( Exception e) {
			S.out( "Error for caller %s: %s", callerKey, e.getMessage() );
			throw e;
		}
	}
	
	static RetVal oldexec(String callerKey, RemoteFunctionCall<TransactionReceipt> func) throws Exception {
		try {
			TransactionReceipt rec = func.send();

			Refblocks.showReceipt( rec);
			
			return new RbRetVal( rec);
		}
		catch( Exception e) {
			S.out( "Error for caller %s: %s", callerKey, e.getMessage() );
			throw e;
		}
	}

	/** Get the transaction manager which knows the chainId and the private key of the caller */
	public static RawTransactionManager getTm(String privateKey) throws Exception {
		Util.require( chainId != 0, "set chainId");
		
//		return new RawTransactionManager(
//				web3j,
//				Credentials.create( privateKey ),
//				chainId );
		
		return new RawTransactionManager(
				web3j,
				Credentials.create( privateKey ),
				chainId,
				new DelayedTrp( web3j) );
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

	public static void showReceipt(TransactionReceipt rec) {
		S.out( toString( rec) );
	}
}
// use this one for instant callback QueuingTransactionReceiptProcessor where you pass in a callback
// start with TransactionManager.execute()
// there is a bug here in Contract.executeTransaction()
//} catch (JsonRpcError error) {
//    throw new TransactionException(error.getData().toString());
//}
// error.getData() is null so toString() fails and you don't get to see the real error
// need to fix or override
// you get get address from private key using Credentials
// TransactionReceiptProcessor is involved
// uses PollingTransactionReceiptProcessor, use a custom ctor
// QueuingTransactionReceiptProcessor this one returns asap and then queries in the background 
// it polls only every 15 sec; that's too slow  JsonRpc2_0Web3j.DEFAULT_BLOCK_TIME = 15 * 1000;
// consider FastRawTransactionManager to have multiple trans per block, I assume per caller
