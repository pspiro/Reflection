package refblocks;

import java.math.BigInteger;
import java.util.ArrayList;

import org.json.simple.JsonObject;

import common.Util;
import positions.MoralisServer;
import reflection.Config;
import tw.util.S;

class Fees {
	public static double billion = Math.pow( 10, 9);
	public static double ten18 = Math.pow( 10, 18);

	static String gasUrl = "https://api.polygonscan.com/api?module=gastracker&action=gasoracle"; // api to get gas

	private BigInteger baseFee;
	private BigInteger priorityFee;

	Fees( double base, double priority) {
		baseFee = BigInteger.valueOf( (long)base);
		priorityFee = BigInteger.valueOf( (long)priority);
	}

	public BigInteger baseFee() {
		return baseFee;
	}

	public BigInteger priorityFee() {
		return priorityFee;
	}

	public BigInteger totalFee() {
		return baseFee.add( priorityFee);
	}

	@Override public String toString() {
		return String.format( "base=%s  priority=%s  total=%s",
				baseFee, priorityFee, baseFee.add( priorityFee) );
	}

	public static Fees fetch() throws Exception {
		JsonObject json = MoralisServer.getFeeHistory(5, 50).getObject( "result");

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

	public static void main(String[] args) throws Exception {
		Config.ask( "Dt2");
		for (int i = 0; i < 5; i++) {
			S.out( fetch() );
			S.sleep( 1000);
		}
	}

	public void showFees(BigInteger gasUnits) {
		S.out( "  baseGas=%s gwi  priority=%s gwi  maxCost=$%s",  
				baseFee.doubleValue() / billion,
				priorityFee.doubleValue() / billion,
				S.fmt4( totalFee().multiply( gasUnits).doubleValue() / ten18 ) );
	}
}