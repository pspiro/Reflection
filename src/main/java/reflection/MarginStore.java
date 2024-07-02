package reflection;

import java.io.IOException;
import java.util.HashMap;

import org.json.simple.JsonArray;
import org.json.simple.TsonArray;

import com.ib.client.Contract;
import com.ib.client.Execution;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.LiveOrder;

import tw.util.S;

class MarginStore extends TsonArray<MarginOrder> {
	private String m_filename ;
	
	public MarginStore(String filename) {
		m_filename = filename;
	}
	
	void save() {
		try {
			writeToFile(m_filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	// we should filter the orders and only pass the relevant live orders for each MarginOrder. pas
	public void onReconnected(ApiController conn) throws Exception {
		HashMap<Integer, LiveOrder> permIdMap = conn.reqLiveOrderMap();  // map permId to LiveOrder
		HashMap<String, LiveOrder> orderRefMap = getOrderRefMap( permIdMap);  // map orderRef to LiveOrder
		
		forEach( order -> {
			try {
				order.onReconnected( permIdMap, orderRefMap);
			}
			catch( Exception e) {
				e.printStackTrace();  // should we cancel? pas
			}
		});
	}

	public static HashMap<String, LiveOrder> getOrderRefMap(HashMap<Integer, LiveOrder> permIdMap) {
		HashMap<String, LiveOrder> orderRefMap = new HashMap<>();
		for (var liveOrder : permIdMap.values() ) {
			if (S.isNotNull( liveOrder.orderRef() ) ) {
				orderRefMap.put( liveOrder.orderRef(), liveOrder);
			}
		}
		return orderRefMap;
	}

	public void tradeReport(String tradeKey, Contract contract, Execution exec) {
//		forEach()
		
	}
	
}