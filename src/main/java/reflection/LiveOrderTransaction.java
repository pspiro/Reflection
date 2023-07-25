package reflection;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import reflection.LiveOrder.FireblocksStatus;
import reflection.LiveOrder.LiveOrderStatus;
import tw.util.S;
import util.LogType;

public class LiveOrderTransaction extends MyTransaction {
	static HashMap<String,Vector<LiveOrder>> liveOrders = new HashMap<>();  // key is wallet address; used Vector because it is synchronized and we will be adding/removing to the list from different threads; write access to the map should be synchronized 
	static HashMap<String,LiveOrder> allLiveOrders = new HashMap<>();  // key is fireblocks id 

	LiveOrderTransaction(Main main, HttpExchange exchange) {
		super(main, exchange);
	}

	public void handleWorkingOrders() {
		wrap( () -> {
			String walletAddr = getWalletFromUri();
			
			JsonArray orders = new JsonArray();
			JsonArray messages = new JsonArray();

			List<LiveOrder> walletOrders = liveOrders.get(walletAddr.toLowerCase());
			if (walletOrders != null) {
				Iterator<LiveOrder> iter = walletOrders.iterator();
				while (iter.hasNext() ) {
					LiveOrder liveOrder = iter.next();
					
					if (liveOrder.status() == LiveOrderStatus.Working) {
						orders.add( liveOrder.getWorkingOrder() );
					}
					else {
						
						// if the order is completed, display a message and remove it from the list
						messages.add( liveOrder.getCompletedOrder() );
						iter.remove();
						
						// if we just removed the last one, let the empty list stay in the liveOrders map,
						// it's not hurting anyone
						
						// by the way, if the user is running two browsers, only one of them will get the message
					}
				}
			}
			
			JsonObject ret = new JsonObject();
			ret.put( "orders", orders);
			ret.put( "messages", messages);
			respond( ret);
		});
	}

	public void handleFireblocks() {
		wrap( () -> {
			parseMsg();
			
			String id = m_map.getRequiredParam("id");
			FireblocksStatus status = m_map.getEnumParam("status", FireblocksStatus.values() ); 
			
			LiveOrder liveOrder = allLiveOrders.get(id);
			
			if (liveOrder != null) {
				log( LogType.FB_UPDATE, "uid=%s  status=%s", liveOrder.uid(), status);
				liveOrder.updateFrom(status);  // note that we don't update live order w/ COMPLETED status; that will happen after the method returns
			}
			else {
				// this will happen anytime this is a FB transactions that is not an order; we can remove it
				S.out( "Error: no live order with id %s; could not update status to %s", id, status);
			}
			
			respondOk();
		});
	}

}
