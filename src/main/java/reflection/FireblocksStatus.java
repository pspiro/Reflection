package reflection;

public enum FireblocksStatus {
		// these are NOT Fireblocks status codes
		LIVE(5), // Live order, waiting for IB order to fill 
		STOCK_ORDER_FILLED(15), // IB order filled
		DENIED(100), // Order failed before IB order was placed
		
		SUBMITTED(30), // The transaction was submitted to the Fireblocks system and is being processed
		QUEUED(45), // Transaction is queued. Pending for another transaction to be processed
		PENDING_AUTHORIZATION(60), // The transaction is pending authorization by other users (as defined in the Transaction Authorization Policy)
		PENDING_SIGNATURE(70), // The transaction is pending the initiator to sign the transaction
		BROADCASTING(80), // The transaction is pending broadcast to the blockchain network
		CONFIRMING(90), // Pending confirmation on the blockchain; it seems this is as good as complete
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

		private FireblocksStatus(int pct) {
			m_pct = pct;
		}
		
		public int pct() { 
			return m_pct; 
		}
	}