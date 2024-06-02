package web3;

import java.io.IOException;

import org.json.simple.JsonObject;

import tw.util.OStream;

/** This program writes the abi and bytecode from the compiled json file to
 *  files that can be read by the web3j generate solidity commands below */
public class WriteAbi {
	public static void main(String[] args) throws IOException, Exception {
		String filename = "c:/work/smart-contracts/build/contracts/busd.json";
		write( filename);
		
		filename = "c:/work/smart-contracts/build/contracts/rusd.json";
		write( filename);
		
		filename = "c:/work/smart-contracts/build/contracts/stocktoken.json";
		write( filename);
	}
	
	static void write( String filename) throws Exception {
		String[] toks = filename.split( "/");
		String base = toks[toks.length-1].split( "\\.")[0];
		
		JsonObject json = JsonObject.readFromFile(filename);
		json.getArray( "abi").writeToFile( "c:/work/" + base + ".abi");
		
		try (OStream os = new OStream( "c:/work/" + base + ".bin") ) {
			os.write( json.getString( "bytecode") );
		}
	}
}

/*
web3j generate solidity -b busd.bin -a busd.abi -o Reflection/src/main/main -p web3core
web3j generate solidity -b rusd.bin -a rusd.abi -o Reflection/src/main/main -p web3core
web3j generate solidity -b stocktoken.bin -a stocktoken.abi -o Reflection/src/main/main -p web3core
*/