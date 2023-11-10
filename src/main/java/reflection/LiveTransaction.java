package reflection;

interface LiveTransaction {
	String uid();
	String walletAddr();
	void onUpdateFbStatus(FireblocksStatus status);

	/** Return database table name */
	String tableName();
}
