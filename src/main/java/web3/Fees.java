package web3;

import java.math.BigInteger;

import tw.util.S;

public class Fees {
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

	public void display() {
		display( BigInteger.valueOf( 21000) );
	}
	
	public void display(BigInteger gasUnits) {
		S.out( "  baseGas=%s wei  priority=%s wei  effectiveGas=%s wei  maxCost=%s native tokens",  
				baseFee.doubleValue(),
				priorityFee.doubleValue(),
				baseFee.add( priorityFee).doubleValue(),
				S.fmt4( totalFee().multiply( gasUnits).doubleValue() / ten18 ) );
	}
//	public void showFees(BigInteger gasUnits) {
//		S.out( "  baseGas=%s gw  priority=%s gw  effectiveGas=%s  maxCost=$%s",  
//				baseFee.doubleValue() / billion,
//				priorityFee.doubleValue() / billion,
//				baseFee.add( priorityFee).doubleValue() / billion,
//				S.fmt4( totalFee().multiply( gasUnits).doubleValue() / ten18 ) );
//	}
}
