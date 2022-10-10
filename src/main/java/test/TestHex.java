package test;

import reflection.Util;
import tw.util.S;

public class TestHex {
	public static void main(String[] args) {
		S.out( Util.hexToDec( "0001a", 0) );
		S.out( Util.hexToDec( "0x0001a", 0) );
		S.out( Util.hexToDec( "0X0001a", 0) );
	}
	
}
