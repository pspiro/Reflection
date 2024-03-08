package monitor;

import fireblocks.Transactions;

class TransPanel extends QueryPanel {
		static String names = "created_at,wallet_public_key,name,uid,status,ref_code,action,quantity,conid,symbol,price,tds,rounded_quantity,commission,currency";
		static String sql = """
select 
	transactions.created_at,
	transactions.wallet_public_key,
	first_name || ' ' || last_name as name,
	uid,
	status,
	ref_code,
	action,
	quantity,
	conid,
	symbol,
	price,
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
			case "fireblocks_id":
				wrap( () -> Transactions.getTransaction(val.toString()).display() );
				break;
			case "blockchain_hash":
				// show in explorer
				break;
				default:
					super.onDouble(tag, val);
			}
		}
	}