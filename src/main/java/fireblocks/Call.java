package fireblocks;

import tw.util.IStream;
import tw.util.S;

public class Call {
	public static void main(String[] args) throws Exception {
		Fireblocks.setVals();
		String getfirstKeccak = "65ad0967";
		//call( "0x14d7e955b156d9f294daea57b7b119db54d71124", getfirstKeccak);
		String addr = "0xab52e8f017fbd6c7708c7c90c0204966690e7fc8";
//		Fireblocks.call( addr, getfirstKeccak, null, null, "RUSD");
	}

	// add calling w/ parameters
	// add picking up the return value*** we don't know how to do this
	// for deploy, add getting the contract address
	// you are here
}

