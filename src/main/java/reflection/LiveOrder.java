package reflection;

import org.json.simple.JsonObject;

// probably this should be a JSONObject; you could move all of this into OrderTransaction or Order
class LiveOrder {
	enum LiveOrderStatus { Working, Filled, Failed };

	private OrderTransaction m_trans;
	private String m_description;  // either order description or error description
	private LiveOrderStatus m_status;
	private int m_progress;
	private RefCode m_errorCode;
	private long m_finished;  // don't keep them forever. pas

	LiveOrder(OrderTransaction trans, String description) {
		m_trans = trans;
		m_description = description;
		m_status = LiveOrderStatus.Working;
		m_progress = 10;
	}
	
	private boolean isBuy() {
		return m_trans.isBuy();
	}

	public LiveOrderStatus status() {
		return m_status;
	}

	public String uid() {
		return m_trans.uid();
	}
	
	enum FireblocksStatus {
		STOCK_ORDER_FILLED(15), // Not a FB status
		SUBMITTED(30), // The transaction was submitted to the Fireblocks system and is being processed
		QUEUED(45), // Transaction is queued. Pending for another transaction to be processed
		PENDING_AUTHORIZATION(60), // The transaction is pending authorization by other users (as defined in the Transaction Authorization Policy)
		PENDING_SIGNATURE(75), // The transaction is pending the initiator to sign the transaction
		BROADCASTING(90), // The transaction is pending broadcast to the blockchain network
		CONFIRMING(100), // Pending confirmation on the blockchain; it seems this is as good as complete
		COMPLETED(100), // Successfully completed
		CANCELLED(100), // The transaction was cancelled or rejected by the user on the Fireblocks platform or by the 3rd party service from which the funds are withdrawn
		REJECTED(100), // The transaction was rejected by the Fireblocks system or by the 3rd party service
		BLOCKED(100), // The transaction was blocked due to a policy rule
		FAILED(100); // The transaction has failed
		
//		other possible statuses, ignoring for now		
//		PENDING_3RD_PARTY_MANUAL_APPROVAL - The transaction is pending manual approval as required by the 3rd party, usually an email approval
//		PENDING_3RD_PARTY - The transaction is pending approval by the 3rd party service (e.g. exchange)
//		PARTIALLY_COMPLETED - (Only for Aggregated transactions) One or more of the transaction records have completed successfully
//		PENDING_AML_SCREENING - In case the AML screening feature is enabled, transaction is pending AML screening result
		
		private int m_pct; // 0 to 100

		FireblocksStatus(int pct) {
			m_pct = pct;
		}
		
		int pct() { 
			return m_pct; 
		}
	}
	
	/** Called when the stock order is filled or we receive an update from the Fireblocks server.
	 *  The status is already logged before we come here  */
	synchronized void updateFrom(FireblocksStatus stat) {
		if (stat == FireblocksStatus.CONFIRMING || stat == FireblocksStatus.COMPLETED) {
			filled();
		}
		else if (stat.pct() == 100) {
			fail( new Exception( "The blockchain transaction failed with status " + stat) );
			
			// informational only; don't throw an exception
			try {
				m_trans.onBlockchainOrderFailed();
			}
			catch( Exception e) {
				e.printStackTrace();
			}
		}
		else {
			m_progress = stat.pct();
		}
	}

	/** Called during testing if we bypass the FB processing */
	synchronized void filled() {
		if (m_status == LiveOrderStatus.Working) {
			m_status = LiveOrderStatus.Filled;
			m_progress = 100;
			m_description = m_description
					.replace("Buy", "Bought")
					.replace("Sell", "Sold");
			m_finished = System.currentTimeMillis();
		}
	}

	/** Called when an error occurs after the order is submitted to IB */
	synchronized void fail(Exception e) {
		if (m_status == LiveOrderStatus.Working) {
			m_status = LiveOrderStatus.Failed;
	
			m_description = e.getMessage();
			if (e instanceof RefException) {
				m_errorCode = ((RefException)e).code();
			}
			m_finished = System.currentTimeMillis();
		}
	}

	/** Called when the user queries status of live orders */
	public synchronized JsonObject getWorkingOrder() {
		JsonObject order = new JsonObject();
		order.put( "id", uid() );
		order.put( "action", isBuy() ? "Buy" : "Sell");
		order.put( "description", m_description);
		order.put( "progress", m_progress);
		return order;
	}

	/** Called when the user queries status of live orders */
	public synchronized JsonObject getCompletedOrder() {
		JsonObject order = new JsonObject();
		order.put( "id", uid() );
		order.put( "type", m_status == LiveOrderStatus.Failed ? "error" : "message");   
		order.put( "text", m_description);
		order.put( "status", m_status.toString() );
		if (m_errorCode != null) {
			order.put( "errorCode", m_errorCode.toString() );
		}
		return order;
	}
}

// change CONFIRMING to 100%


