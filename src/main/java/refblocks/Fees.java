package refblocks;

import java.math.BigInteger;
import java.util.ArrayList;

import org.json.simple.JsonObject;

import common.Util;
import positions.MoralisServer;
import tw.util.S;

class Fees {
	static String gasUrl = "https://api.polygonscan.com/api?module=gastracker&action=gasoracle"; // api to get gas

	private BigInteger baseFee;
	private BigInteger priorityFee;

	Fees( double base, double priority) {
		baseFee = BigInteger.valueOf( (long)base);
		priorityFee = BigInteger.valueOf( (long)priority);
	}

	public BigInteger totalFee() {
		return baseFee.add( priorityFee);
	}

	public BigInteger priorityFee() {
		return priorityFee;
	}

	@Override public String toString() {
		return String.format( "base=%s  priority=%s  total=%s",
				baseFee, priorityFee, baseFee.add( priorityFee) );
	}

	public static Fees fetch() throws Exception {
		JsonObject json = MoralisServer.getFeeHistory(5).getObject( "result");

		// get base fee of last/pending block
		long baseFee = Util.getLong( json.<String>getArrayOf( "baseFeePerGas").get( 0) );

		// take average of median priority fee over last 5 blocks 
		long sum = 0;
		ArrayList<ArrayList> reward = json.<ArrayList>getArrayOf( "reward");
		for ( ArrayList ar : reward) {
			sum += Util.getLong( ar.get(0).toString() );
		}

		return new Fees( baseFee * 1.2, sum / 5.);
	}

	// this uses a third party API but you would need to pay for it
	//		static Fees getFees() {
	//			Fees fees = new Fees();
	//			
	//			try {
	//				S.sleep( 200);  // don't break pacing of max 5 req per second; how do they know it's me???
	//				JsonObject json = MyClient.getJson( gasUrl)
	//						.getObject( "result");
	//				fees.baseFee = json.getBlockchain( "suggestBaseFee", 9); // convert base fee from gwei to wei
	//				fees.priorityFee = json.getBlockchain( "FastGasPrice", 9);  // it's very unclear if this is returning the priority fee or the total fee, but it doesn't matter because the base fee is so low 
	//			} catch (Exception e) {
	//				if (e.getMessage().contains("Max rate limit reached") ) {
	//					S.out( "Error: Max rate limit exceeded for getFees(); using default values");
	//				}
	//				else {
	//					e.printStackTrace();
	//				}
	//				fees.baseFee = defaultBaseFee;
	//				fees.priorityFee = defaultPriorityFee;
	//			}
	//			
	//			return fees;
	//		}
}