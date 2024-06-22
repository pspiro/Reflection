package reflection;

import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JsonArray;
import org.json.simple.TJsonArray;

import tw.util.S;

class MarginStore extends TJsonArray<MarginOrder> {
	static String filename = "margin.store";
	
	static MarginStore store;
	
	static void restore() {
		try {
			S.out( "Reading margin store");
			
			store = (MarginStore)JsonArray.parse(
					new FileReader( filename),
					() -> new MarginOrder(),
					() -> new MarginStore() );
			
			S.out( "  read %s records", store.size() );
		}
		catch( Exception e) {
			e.printStackTrace();
			store = new MarginStore();
		}
	}

	void save() {
		try {
			writeToFile(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void placeOrder( MarginOrder order) throws Exception {
		add( order);
		save();
		
		order.placeBuyOrder();
	}

	/** Return orders for the specified wallet address */
	public JsonArray getOrders(String walletAddr) {
		JsonArray ar = new JsonArray();
		
		for (MarginOrder order : this) {
			if (order.wallet().equalsIgnoreCase( walletAddr) ) {
				ar.add( order);
			}
		}
		
		return ar;
	}

	public MarginOrder getById(String orderId) {
		for (MarginOrder order : this) {
			if (order.orderId().equals( orderId) ) {
				return order;
			}
		}
		return null;
	}
	
}