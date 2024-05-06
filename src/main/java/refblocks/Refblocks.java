package refblocks;

import java.math.BigInteger;

import org.json.simple.JsonObject;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.StaticEIP1559GasProvider;
import org.web3j.utils.Numeric;

import common.Util;
import http.MyClient;

//
//import java.math.BigInteger;
//
//import org.json.simple.JsonObject;
//import org.web3j.crypto.Credentials;
//import org.web3j.protocol.Web3j;
//import org.web3j.protocol.core.DefaultBlockParameterName;
//import org.web3j.protocol.core.methods.response.EthBlock.Block;
//import org.web3j.protocol.core.methods.response.EthEstimateGas;
//import org.web3j.protocol.core.methods.response.Transaction;
//import org.web3j.protocol.core.methods.response.TransactionReceipt;
//import org.web3j.tx.RawTransactionManager;
//import org.web3j.tx.gas.StaticEIP1559GasProvider;
//import org.web3j.utils.Numeric;
//
//import http.MyClient;
//import web3.Rusd;
//
public class Refblocks {
//	static String admin1 = "0xb8C30268E0c4A1AaE6aB3c22A9d4043148216614";
//	static String refWallet = "0xF6e9Bff937ac8DF6220688DEa63A1c92b6339510";
//	static String rusdAddr = "0x455759a3f9124bf2576da81fb9ae8e76b27ff2d6";
//	static String homeWallet = "0x2ab0e9e4ee70fff1fb9d67031e44f6410170d00e";
//	static String publicKey = "0x6c3644344E5AdDFb39144b1B76Cae79da6719228";
//	static String privateKey = "9d136e05de9a38ab85ac6c471578bd4e101402df3373c006b7e07e69ab2073cb";
	static BigInteger defaultBaseFee = BigInteger.valueOf(1_000_000_000L);  // used only if we can't fetch it
	static BigInteger defaultPriorityFee = BigInteger.valueOf(35_000_000_000L);  // used only if we can't fetch it
	static Web3j web3j = Web3j.build(new HttpService("https://polygon-rpc.com/"));
	static long chainId;  // set from Config
	static long deployGas = 2000000;
	
	public static void setChainId( long id) {
		chainId = id;
	}
	
//	static Web3j web3j = Web3j.build(new HttpService("https://evocative-fabled-fire.matic.quiknode.pro/32cf5be29cb98c393bd95643b7ac25e016d6e8b1/"));
//
//	static RawTransactionManager txManager = new RawTransactionManager(
//			web3j,
//			Credentials.create(privateKey),
//			chainId);
//
//	public static void main( String[] args) throws Exception {
//		try {
//			//transfer();
//			//deploy();
//		}
//		catch( Exception e) {
//			e.printStackTrace();
//		}
//	}
//
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
			JsonObject json = MyClient.getJson( "https://api.polygonscan.com/api?module=gastracker&action=gasoracle");
			fees.baseFee = json.getBigInt( "suggestBaseFee");
			fees.priorityFee = json.getBigInt( "FastGasPrice");  // it's very unclear if this is returning the priority fee or the total fee, but it doesn't matter because the base fee is so low 
		} catch (Exception e) {
			e.printStackTrace();
			fees.baseFee = defaultBaseFee;
			fees.priorityFee = defaultPriorityFee;
		}
		
		return fees;
	}
//	
//	static void transfer() throws Exception {
//		Rusd c = web3.tokens.Rusd.load(
//				rusdAddr,
//				web3j, 
//				txManager,
//				getGp( 40000) );
//		
//		TransactionReceipt rec = c.transfer(publicKey, BigInteger.valueOf( 2) ).send();
//		showReceipt( rec);
//
//		Transaction myTrans = web3j.ethGetTransactionByHash( rec.getTransactionHash() )
//				.send()
//				.getTransaction()
//				.get();
//		show( myTrans);		
//	}
//
//	/** this is to estimate the amount of gas a transaction will take;
//	 *  it doesn't work for deploy(), probably safer to do a transaction and look at the history */
//	static void estimateGas() throws Exception {
//		BigInteger baseFee = fetchCurrentBaseFee();
//		BigInteger priorityFee = BigInteger.valueOf(33_000_000_000L);  // need to figure out how to get the correct value
//		BigInteger totalFee = baseFee.add( priorityFee);
//		BigInteger unitsOfGas = BigInteger.valueOf(2000000);
//
//		StaticEIP1559GasProvider gp = new StaticEIP1559GasProvider(
//				chainId,
//				totalFee,
//				priorityFee,
//				unitsOfGas
//				);
//
//		org.web3j.protocol.core.methods.request.Transaction t = new org.web3j.protocol.core.methods.request.Transaction(
//				publicKey,
//				null,
//				null,
//				BigInteger.valueOf( 0), // gasLimit,
//				publicKey,
//				BigInteger.valueOf( 0),
//				Rusd.getDeploymentBinary(),
//				chainId,
//				BigInteger.valueOf( 35000000000L), // maxPriorityFeePerGas,
//				BigInteger.valueOf( 36000000000L) // maxFeePerGas
//				);
//
//		System.out.println( "estimating gas");
//		EthEstimateGas est = web3j.ethEstimateGas( t).send();
//		System.out.println( "estimated gas: " + est.getAmountUsed() );
//	}
//	
////	static void deploy() throws Exception {
////		BigInteger baseFee = fetchCurrentBaseFee();
////		BigInteger priorityFee = BigInteger.valueOf(33_000_000_000L);  // need to figure out how to get the correct value
////		BigInteger totalFee = baseFee.add( priorityFee);
////		BigInteger unitsOfGas = BigInteger.valueOf(2000000);
////
////		StaticEIP1559GasProvider gp = new StaticEIP1559GasProvider( // fails with this
////				chainId,
////				totalFee,
////				priorityFee,
////				unitsOfGas
////				);
////
////		Rusd rusd = Rusd.deploy(
////				web3j, 
////				txManager,
////				gp,
////				refWallet,
////				admin1).send();
////		System.out.println( "deployed to " + rusd.getContractAddress() );
////	}
//
	public static void showReceipt(TransactionReceipt trans) {
		String str = String.format(
				"hash=%s \ngasUsed=%s \ngasPrice=%s",
				trans.getTransactionHash(),
				trans.getGasUsed(),
				decodeQuantity(trans.getEffectiveGasPrice())
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
//
//	/** it would be nice if we could show gas unit used as well */
//	private static void show(Transaction trans) {
//		String str = String.format( 
//				" gasUnitsLimit=%s \n actualFeePerGas=%s \n maxFeePerGas=%s \n maxPriorityFeePerGas=%s", 
//				trans.getGas(),
//				trans.getGasPrice(),
//				trans.getMaxFeePerGas(),
//				trans.getMaxPriorityFeePerGas()
//				);
//		System.out.println( str);
//	}
//
//	/** not used, for debugging only */
//	private static void showFeesForBlock(DefaultBlockParameterName type) throws Exception {
//		Block block = web3j.ethGetBlockByNumber(type, false)
//				.send()
//				.getBlock();
//		System.out.println( "block type: " + type);
//		System.out.println( "block number: " + block.getNumber() );
//		System.out.println( "base fee: " + block.getBaseFeePerGas() );
//		System.out.println( "");
//	}
//
//	/** this is not used because we can get base and priority in a single call, but is here for example */
//	private static BigInteger fetchCurrentBaseFee() throws Exception {
//		Block block = web3j.ethGetBlockByNumber(org.web3j.protocol.core.DefaultBlockParameterName.PENDING, false)
//				.send()
//				.getBlock();
//		return block.getBaseFeePerGas();
//	}

	public static RawTransactionManager getTm(String privateKey) throws Exception {
		Util.require( chainId != 0, "set chainId");
		
		return new RawTransactionManager(
				web3j,
				Credentials.create( privateKey ),
				chainId);
	}
	
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
//
//
//
///*
//gas                =         300 000 units
//projected base fee =          27 641
//my maxFeePerGas    = 100 000 027 641      // max I will pay 
//gasPrice           =  35 000 027 432
//maxPriorityFee     =  35 000 000 000
//
//the base fee does not have to be exact; it will pull from the priority fee if necessary
//
//  Questions:
//  how do I calculate the correct base fee; how much should I add on top of the calculated amount?
//  why does a transfer cost 300000 gas and not 21000 gas?
//  why must I add 100gw on top of the base fee, and how to calculate the correct amount?
//  how
//
//  gas amounts
//  token transfer: 26,978   // = 5978 more than what is reported by estimate()  
//  deploy RUSD: 1,648,634
//
//  could cost .15 matic to deploy a contract, actual is .058
//
// you have to learn how to estimate correct base and priority fees
// you have to determine the total amount of gas that could be used by each transaction
// 
// open items
// use the algo from upwork to calculate more accurate base fee, if you want to
// work on estimating gas units
//
// */
//// estimated 135468 for deployment, seems wrong  was 1,648,634  always, so just double it or something
//// forget about the estimation, you only call a few methods
//
//
//
//
//
//
