package test;

import reflection.Config;
import tw.util.OStream;

public class SqlReport {
	public static void main(String[] args) throws Exception {
		Config c = Config.ask( "prod");
//		var transs = c.sqlQuery( "select * from transactions where status = 'COMPLETED' order by created_at desc limit 200");
		var transs = c.sqlQuery( "select wallet_public_key from transactions where status = 'COMPLETED' and created_at > '2024-10-01'");
		// you could also just select distinct
		
		var wallets = transs.getValues( "wallet_public_key");
		
		var list = String.join( "','", wallets);

		var users = c.sqlQuery( "select first_name,last_name,email,phone,wallet_public_key,kyc_status,country,geo_code from users where wallet_public_key in ('%s')", list);
		users.print();

		try( OStream os = new OStream( "c:/temp/users.t") ) {
			os.write( users.toHtml(true) );
		}
		
	}
}
