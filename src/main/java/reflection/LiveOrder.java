package reflection;

import java.util.Random;

import org.json.simple.JsonObject;

import fireblocks.Transactions;
import tw.util.S;

// probably this should be a JSONObject
class LiveOrder {
	enum LiveOrderStatus { Working, Filled, Failed };

	private long m_finished;  // don't keep them forever. pas
	private String m_description;  // either order description or error description
	private String m_action;
	private LiveOrderStatus m_status;
	private int m_progress;
	private RefCode m_errorCode;
	private String m_uid = Util.id(5);  // this is an id that the client can use to match up the initial order with the blockchain order status 
	private String m_fireblocksId;
	

	LiveOrder(String action, String description) {
		m_action = action;
		m_description = description;
		m_status = LiveOrderStatus.Working;
		m_progress = 10;
	}
	
	enum FireblocksStatus {
		SUBMITTED(30), // The transaction was submitted to the Fireblocks system and is being processed
		QUEUED(50), // Transaction is queued. Pending for another transaction to be processed
		PENDING_AUTHORIZATION(60), // The transaction is pending authorization by other users (as defined in the Transaction Authorization Policy)
		PENDING_SIGNATURE(70), // The transaction is pending the initiator to sign the transaction
		BROADCASTING(80), // The transaction is pending broadcast to the blockchain network
		CONFIRMING(90), // Pending confirmation on the blockchain
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
		
		boolean isFinal() {
			return m_pct == 100;
		}
		
		boolean failed() {
			return isFinal() && this != FireblocksStatus.COMPLETED;
		}
		
		LiveOrderStatus getLiveOrderStatus() {
			if (this == FireblocksStatus.COMPLETED) {
				return LiveOrderStatus.Filled;
			}

			// any other completion besides COMPLETED is a failure
			return failed() ? LiveOrderStatus.Failed : LiveOrderStatus.Working;
		}
	}
	
	void updateFrom(FireblocksStatus stat) {
		m_progress = stat.pct();
		m_status = stat.getLiveOrderStatus();
	}
		
	void updateStatus() {
		try {
			if (S.isNotNull(m_fireblocksId) ) {
				FireblocksStatus stat = Util.getEnum(
						LiveOrderMgr.getStatus(m_fireblocksId), 
						FireblocksStatus.values() );
				updateFrom(stat);
			}
			// if no FB id yet, leave it at 10%
		}
		catch( Exception e) {
			e.printStackTrace();
			S.out( "Error: unknown Fireblocks status " + LiveOrderMgr.getStatus(m_fireblocksId) );
		}
	}

	void failed(Exception e) {
		m_status = LiveOrderStatus.Failed;

		m_description = e.getMessage();
		if (e instanceof RefException) {
			m_errorCode = ((RefException)e).code();
		}
		m_finished = System.currentTimeMillis();
	}

	void filled() {
		m_status = LiveOrderStatus.Filled;
		m_description = m_description
				.replace("Buy", "Bought")
				.replace("Sell", "Sold");
		m_finished = System.currentTimeMillis();
	}

	public LiveOrderStatus status() {
		return m_status;
	}

	public String id() {
		return m_uid;
	}

	public JsonObject getWorkingOrder() {
		JsonObject order = new JsonObject();
		order.put( "id", m_uid);
		order.put( "action", m_action);
		order.put( "description", m_description);
		order.put( "progress", m_progress);
		return order;
	}

	public JsonObject getCompletedOrder() {
		JsonObject order = new JsonObject();
		order.put( "id", m_uid);
		order.put( "type", m_status == LiveOrderStatus.Failed ? "error" : "message");   
		order.put( "text", m_description);
		order.put( "status", m_status.toString() );
		if (m_errorCode != null) {
			order.put( "errorCode", m_errorCode.toString() );
		}
		return order;
	}

	public void fireblocksId(String id) {
		m_fireblocksId = id;
	}
}
