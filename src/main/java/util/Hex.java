package util;

import java.math.BigInteger;

public class Hex {
	public static void main(String[] args) {
		System.out.println( new BigInteger(args[0], 16) );
	}
}
