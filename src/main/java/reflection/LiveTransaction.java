package reflection;

interface LiveTransaction {
	String uid();
	String walletAddr();
	void onUpdateFbStatus(FireblocksStatus status, String hash);

	/** Return database table name */
	String tableName();
}
