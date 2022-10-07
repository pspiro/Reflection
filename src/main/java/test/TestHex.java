package test;

import java.math.BigDecimal;
import java.math.BigInteger;

import tw.util.S;

public class TestHex {
	public static void main(String[] args) {
		S.out( hexToDec( "112", 1) );
	}
	
	/** Convert hex to decimal and then apply # of decimal digits.
	 *  Could be used to read the transaction size from the logs. */
	public static double hexToDec(String str, int decDigits) {
		BigInteger tot = new BigInteger("0", 16);

		for (int i = str.length() - 1; i >= 0; i--) {
        	BigInteger mult = new BigInteger( "16").pow( str.length() - i - 1);
        	String digit = String.valueOf( Character.digit(str.charAt(i), 16) ); // convert hex char to integer 0-15
        	BigInteger dec = new BigInteger(digit).multiply( mult);
        	tot = tot.add( dec);
        }
		
        S.out( tot);
        BigInteger div = new BigInteger("10").pow( decDigits);
        BigDecimal ans = new BigDecimal( tot).divide( new BigDecimal( div) );
        return ans.doubleValue();
    }
}
