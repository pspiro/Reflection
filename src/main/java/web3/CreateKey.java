package web3;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Scanner;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.json.simple.JsonObject;
import org.web3j.crypto.Credentials;

import common.Util;
import reflection.Config;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab;
import tw.util.S;

public class CreateKey {
	static SecureRandom rnd = new SecureRandom();
	
	public static void main(String[] args) throws Exception {
		Config.ask();  // we need this 
		createUserWallet();
//		String privateKey = createPrivateKey();
//		String address = Credentials.create( privateKey ).getAddress();
//		S.out( privateKey);
//		S.out( address);
	}

	/** Input wallet params from command line and create wallet on Prod-wallets tab */
	public static void createUserWallet() throws Exception {
		try( Scanner scanner = new Scanner( System.in) ) {
			String pw1 = input( scanner, "Enter password: ");
			String pw2 = input( scanner, "Re-enter password: ");
			Util.require( pw1.equals( pw2), "Mismatch");

			String name = input( scanner, "Enter wallet name: ");
			String description = input( scanner, "Enter description: ");
			String hint = input( scanner, "Enter password hint: ");
			
			createProdWallet( pw1, name, description, hint);
		}
	}
	
	/** Create the wallet and add it to the "Prod-wallets" tab */
	public static void createProdWallet(String pw, String name, String description, String hint) throws Exception {
		JsonObject json = getEncryptedPrivateKey( pw);

		// write it to spreadsheet
		Tab tab = NewSheet.getTab( NewSheet.Reflection, "Prod-wallets");
		tab.insert( Util.toJson( 
				"Name", name,
				"Description", description,
				"Address", json.getString( "address"),
				"Key", json.toString(),
				"Hint", hint) );
				
		// restore it and confirm
		JsonObject json2 = tab.findRow("Name", name).getJsonObject( "Key");
		String restoredKey = decryptFromJson( pw, json2); 
		
		Util.require( 
				Credentials.create( restoredKey).getAddress().equals( json.getString( "address") ), 
				"Failed, encrypted and decrypted values do not match");
		
		S.out( "Created prod wallet %s - %s", name, description);
	}
	
	/** fields in json are address, salt, data, ivstr */ 
	public static String decryptFromJson( String pw, JsonObject json) throws Exception {
		return Encrypt.decrypt( 
				json.getString( "data"),
				Encrypt.getKeyFromPassword( pw, json.getString( "salt") ),
				json.getString( "ivstr") );
	}
		
	
	public static JsonObject getEncryptedPrivateKey(String pw) throws Exception {
		String privateKey = createPrivateKey();
		String address = Credentials.create( privateKey ).getAddress();
		String salt = Util.uid( 8);
		SecretKey secretKey = Encrypt.getKeyFromPassword(pw, salt);
		IvParameterSpec iv = Encrypt.generateIv();
		String ivstr = Encrypt.encode( iv.getIV() );  // this is data added to the input to make it unique
		String encrypted = Encrypt.encrypt( privateKey, secretKey, iv);
		
		return Util.toJson( 
				"data", encrypted,
				"address", address,
				"salt", salt,
				"ivstr", ivstr);
	}

	public static String input(Scanner scanner, String string) {
		S.out( string);
		return scanner.nextLine();
	}

	public static String createPrivateKey() {
		// Generate a 32-bytes private key
		byte[] bytes = new byte[32];
		rnd.nextBytes(bytes);

		// Convert the byte array to a hexadecimal string
		String key = new BigInteger(1, bytes).toString(16);

		// Ensure the hexadecimal string is 64 characters long (pad with leading zeros if necessary)
		while (key.length() < 64) {
			key = "0" + key;
		}
		
		return key;
	}
}
