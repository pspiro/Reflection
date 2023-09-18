package reflection;

import java.util.Iterator;
import java.util.List;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import reflection.OrderTransaction.LiveOrderStatus;
import tw.util.S;
import util.LogType;

/** The client will query the live order status every couple of seconds. This should be changed
 *  to use WebSockets and push the change to the client w/ no query */
public class LiveOrderTransaction extends MyTransaction {

	LiveOrderTransaction(Main main, HttpExchange exchange) {
		super(main, exchange);
	}

	/** Called by the Monitor program */
	public void handleAllLiveOrders() {
		wrap( () -> {
			JsonArray retLiveOrders = new JsonArray();
			
			liveOrders.forEach( (walletAddr, list) -> {
				for (OrderTransaction order : list) {
					retLiveOrders.add( order.getLiveOrder() );
				}
			});
			
			respond( retLiveOrders);
		});
	}
	
	/** Return live orders to Frontend for a single wallet */
	public void handleLiveOrders() {
		wrap( () -> {
			String walletAddr = getWalletFromUri();
			
			JsonArray orders = new JsonArray();
			JsonArray messages = new JsonArray();

			List<OrderTransaction> walletOrders = liveOrders.get(walletAddr.toLowerCase());
			if (walletOrders != null) {
				Iterator<OrderTransaction> iter = walletOrders.iterator();  // get list for this wallet
				while (iter.hasNext() ) {
					OrderTransaction liveOrder = iter.next();
					
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
			ret.put( "orders", orders);      // rename to "working", rename message to "live"
			ret.put( "messages", messages);  // rename to "completed"
			respond( ret);
		});
	}

	/** This handles a message from the FbActiveServer which is monitoring for blockchain transactions */
	public void handleFireblocks() {
		wrap( () -> {
			parseMsg();  // (all keys -> lower case)
			
			String id = m_map.getRequiredParam("id");
			FireblocksStatus status = m_map.getEnumParam("status", FireblocksStatus.values() );
			String hash = S.notNull( m_map.getParam("txhash") );
			
			updateCryptoTable( id, status, hash);
			
			OrderTransaction liveOrder = allLiveOrders.get(id);
			
			if (liveOrder != null) {
				liveOrder.onUpdateStatus(status);  // note that we don't update live order w/ COMPLETED status; that will happen after the method returns
			}
			else {
				// this will happen anytime this is a FB transactions that is not an order; we can remove it
				out( "Error: no live order with id %s; could not update status to %s", id, status);
			}
			
			respondOk();
		});
	}

	private void updateCryptoTable(String uid, FireblocksStatus status, String hash) {
		// update the crypto-transactions table IF there is a hash code which means the transaction has succeeded
		try {
			log( LogType.FB_UPDATE, "uid=%s  status=%s  hash=%s", uid, status, hash); // this give us the history of the timing

			JsonObject obj = new JsonObject();
			obj.put("status", status.toString() );
			obj.put("blockchain_hash", hash);
			
			m_main.sqlConnection( conn -> conn.updateJson("crypto_transactions",  obj, "fireblocks_id = '%s'", uid) );				
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}

}
