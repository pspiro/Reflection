package common;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Scanner;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.json.simple.JsonObject;
import org.web3j.crypto.Credentials;

import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;
import web3.Encrypt;

public class CreateKey {
	static SecureRandom rnd = new SecureRandom();
	
	public static void main(String[] args) throws Exception {
		try( Scanner scanner = new Scanner( System.in) ) {
			String name = input( scanner, "Enter wallet name: ");
			String pw = input( scanner, "Enter password: ");
			String hint = input( scanner, "Enter password hint: ");
			String description = input( scanner, "Enter description: ");
			
			String privateKey = createPrivateKey();
			String address = Credentials.create( privateKey ).getAddress();
			String salt = Util.uid( 8);
			SecretKey secretKey = Encrypt.getKeyFromPassword(pw, salt);
			IvParameterSpec iv = Encrypt.generateIv();
			String ivstr = Encrypt.encode( iv.getIV() );  // this is data added to the input to make it unique
			String encrypted = Encrypt.encrypt( privateKey, secretKey, iv);
			
			JsonObject json = Util.toJson( 
					"data", encrypted,
					"salt", salt,
					"ivstr", ivstr);

			// write it to spreadsheet
			Tab tab = NewSheet.getTab( NewSheet.Reflection, "Prod-wallets");
			tab.insert( Util.toJson( 
					"Name", name,
					"Description", description,
					"Address", address,
					"Key", json.toString(),
					"Hint", hint) );
					
			// restore it and confirm
			String pw2 = input( scanner, "Re-enter password: ");
			JsonObject json2 = tab.findRow("Name", name).getJsonObject( "Key");
			SecretKey secret2 = Encrypt.getKeyFromPassword( pw2, json2.getString( "salt") );  
			String decrypted = Encrypt.decrypt( 
					json.getString( "data"),
					secret2,
					json2.getString( "ivstr") );
			
			S.out( decrypted.equals( privateKey) ? "Succeeded" : "Failed, encrypted and decrypted values do not match");
		}
	}
	
	private static String input(Scanner scanner, String string) {
		S.out( string);
		return scanner.nextLine();
	}

	public static String createPrivateKey() {
		// Generate a 256-bit (32 bytes) private key
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
