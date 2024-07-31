package reflection;

import static reflection.Main.require;

import java.util.Iterator;
import java.util.List;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.sun.net.httpserver.HttpExchange;

import common.Util;
import reflection.OrderTransaction.LiveOrderStatus;
import tw.util.S;
import util.LogType;

/** The client will query the live order status every couple of seconds. This should be changed
 *  to use WebSockets and push the change to the client w/ no query */
public class LiveOrderTransaction extends MyTransaction {

	LiveOrderTransaction(Main main, HttpExchange exchange, boolean debug) {
		super(main, exchange, debug);
	}
	
	/** Could be used to clear live orders off the screen without restarting RefAPI */
	public void clearLiveOrders() {
		wrap( () -> {
			//require( !Main.m_config.isProduction(), RefCode.INVALID_REQUEST, "Dev only");
			// we should auto-clear live orders after a while, but for now allow manual clearing from Monitor
			liveOrders.clear();
			allLiveTransactions.clear();
			respondOk();
		});		
	}

	/** Called by the Monitor program */
	public void handleAllLiveOrders() {
		wrap( () -> {
			JsonArray retLiveOrders = new JsonArray();
			
			liveOrders.forEach( (walletAddr, list) -> {  // can't use allLiveOrders because it is not the complete list
				for (OrderTransaction order : list) {
					retLiveOrders.add( order.getLiveOrder() );  // change this to use allLiveOrders to you pick up the redemption requests, but that will miss this 
				}
			});
			
			respond( retLiveOrders);
		});
	}
	
	/** Return live orders to Frontend for a single wallet; 
	 *  the list is displayed in the Working Orders panel */
	public void handleGetLiveOrders() {
		wrap( () -> {
			// read wallet address into m_walletAddr (last token in URI)
			getWalletFromUri();
			
			JsonArray orders = new JsonArray();
			JsonArray messages = new JsonArray();

			List<OrderTransaction> walletOrders = liveOrders.get(m_walletAddr.toLowerCase());
			if (walletOrders != null) {
				Iterator<OrderTransaction> iter = walletOrders.iterator();  // get list for this wallet
				while (iter.hasNext() ) {
					OrderTransaction liveOrder = iter.next();
					
					if (liveOrder.status() == LiveOrderStatus.Working) {
						orders.add( liveOrder.getWorkingOrder() );
						
						Util.iff( liveOrder.getMessage(), msg -> messages.add( msg) );
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

	/** This handles a message from the FbServer which is monitoring for blockchain transactions */
	public void handleFireblocks() {
		wrap( () -> {
			parseMsg();  // (all keys -> lower case)
			
			String fbId = m_map.getRequiredParam("id");
			FireblocksStatus status = m_map.getEnumParam("status", FireblocksStatus.values() );
			String hash = S.notNull( m_map.getParam("txhash") );

			LiveTransaction liveTrans = allLiveTransactions.get(fbId);  // could be Order or Redemption
			
			if (liveTrans != null) {
				// update hash only in transactions or redemptions table
				// fails silently if there is no transaction to update;
				// it's possible that the database entry was not made yet
				m_main.queueSql( sql -> sql.execWithParams( 
						"update %s set blockchain_hash = '%s' where fireblocks_id = '%s'", liveTrans.tableName(), hash, fbId) );

				// for log entries, use the uid and wallet from the order 
				m_uid = liveTrans.uid();
				m_walletAddr = liveTrans.walletAddr();
				olog( LogType.FB_UPDATE, "id", fbId, "status", status, "hash", hash); // this gives us the history of the timing
				
				liveTrans.onUpdateFbStatus(status, hash);  // we might not update live order w/ COMPLETED status; that could happen after the method returns

				// remove the transaction from allLiveTransactions if it is completed
				if (status.pct() == 100) {
					allLiveTransactions.remove(fbId);
				}
			}
			else {
				out( "Ignoring transaction %s %s", fbId, status);
			}
			
			respondOk();
		});
	}

}
