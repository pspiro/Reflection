package test;

import reflection.Config;

/** Just test that you can connect to the database. */
public class TestPostgres {
	static int i = 0;
	public static void main(String[] args) throws Exception {
		Config c = Config.ask();
		c.useExternalDbUrl();
		c.sqlQuery( """
select * from transactions 
left join users on transactions.wallet_public_key = users.wallet_public_key
where status = 'COMPLETED';
""").print();
	}
}
