package fireblocks;

import json.MyJsonArray;
import json.MyJsonObject;

public class Transactions {
	static void displayLastTransactions(int n) throws Exception {
		MyJsonArray ar = getTransactions();
		
		for (int i = 0; i < n; i++) { 
			ar.getJsonObj(i).display();
		}
	}

	public static MyJsonArray getTransactions() throws Exception {
		return Fireblocks.getArray( "/v1/transactions");
	}

	public static MyJsonObject getTransaction(String id) throws Exception {
		return Fireblocks.getObject( "/v1/transactions/" + id);
	}

	public static void main(String[] args) throws Exception {
		displayLastTransactions(3);
	}
}
