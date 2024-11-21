package monitor;

import org.json.simple.JsonObject;

class TransPanel extends QueryPanel {
		static String names = "created_at,wallet_public_key,name,uid,chainid,status,ref_code,action,quantity,amount,price,currency,symbol,conid,tds,rounded_quantity,commission,country,ip_address";
		static String sql = """
select 
	transactions.created_at,
	transactions.wallet_public_key,
	first_name || ' ' || last_name as name,
	uid,
	chainid,
	status,
	ref_code,
	action,
	quantity,
	conid,
	symbol,
	blockchain_hash,
	price,
	transactions.country as country,
	ip_address,
	tds,
	rounded_quantity,
	commission,
	currency 
from transactions
left join users using (wallet_public_key)
$where 
order by created_at desc 
$limit""";  // you must order by desc to get the latest entries
		
		TransPanel() {
			super( "transactions", names, sql);
		}
		
		@Override public void adjust(JsonObject obj) {
			double v = obj.getDouble( "quantity") * obj.getDouble("price");
			if (v != 0) {
				obj.put( "amount", v);    // note this doesn't include the commission or tds so does not match blockchain trans amt
			}
		}
		
		@Override protected void onDouble(String tag, Object val) {
			switch(tag) {
			case "wallet_public_key":
				Monitor.m_tabs.select( "Wallet");
				Monitor.m_walletPanel.setWallet( val.toString() );
				break;
			case "uid":
				Monitor.m_tabs.select( "Log");
				Monitor.m_logPanel.filterByUid(val.toString());
				break;
			case "blockchain_hash":
				// show in explorer
				break;
				default:
					super.onDouble(tag, val);
			}
		}

		public void setWallet(String walletAddr) {
			where.setText( String.format("where wallet_public_key = '%s'", walletAddr.toLowerCase() ) );
		}
}
