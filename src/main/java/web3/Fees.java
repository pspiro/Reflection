package web3;

import java.math.BigInteger;

import tw.util.S;

/** Here's how it works: first, the base fee, which is calculated by the system,
 *  is paid out of the total. Then, the remainder goes towards the tip, but not
 *  more than the priority fee. */
public class Fees {
	public static double billion = Math.pow( 10, 9);
	public static double ten18 = Math.pow( 10, 18);

	static String gasUrl = "https://api.polygonscan.com/api?module=gastracker&action=gasoracle"; // api to get gas

	private BigInteger baseFee;
	private BigInteger priorityFee;

	Fees( double base, double priority) {
//		baseFee = BigInteger.ONE;
//		priorityFee = BigInteger.ONE;
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
		display( 21000);
	}
	
	public void display(long gasUnits) {
		display( BigInteger.valueOf( gasUnits), BigInteger.ZERO);
	}
	
	/** @param xferAmt is weiValue of transfer */
	public void display(BigInteger gasUnits, BigInteger xferAmt) {
		S.out( "  baseGas=%s wei  priority=%s wei  effectiveGas=%s wei  xferAmt=%s wei  maxCost=%s native tokens",  
				baseFee.doubleValue(),
				priorityFee.doubleValue(),
				baseFee.add( priorityFee).doubleValue(),
				xferAmt,
				maxCost( gasUnits, xferAmt)
				);
	}

	public double maxCost(BigInteger gasUnits, BigInteger xferAmt) {
		return totalFee().multiply( gasUnits).add( xferAmt).doubleValue() / ten18;
	}
}
