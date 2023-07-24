package test;

import java.io.IOException;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.controller.ApiController;
import com.ib.controller.ConnectionAdapter;

import common.Util;
import reflection.Config;
import tw.google.NewSheet;
import tw.google.NewSheet.Book.Tab;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.S;

public class LookupConid extends ConnectionAdapter {
	private final ApiController m_controller = new ApiController( this, null, null);

	public static void main(String[] args) throws IOException, Exception {
		new LookupConid();
	}

	LookupConid() throws Exception {
		Config config = Config.readFrom("Dt-config");
		m_controller.connect(config.twsOrderHost(), config.twsOrderPort(), 9383, null);
	}

	public void onRecNextValidId(int id) {
		Util.execute( () -> query() );
	}
	
	boolean m_allSent;
	int m_sent;
	
	void dec() {
		m_sent--;
		check();
	}
	
	private synchronized void check() {
		if (m_allSent && m_sent == 0) {
			S.out( "done");
			m_controller.disconnect();
		}
	}
	
	@Override public void onDisconnected() {
		System.exit(0);
	}
	
	public void query() {
		try {
			Tab tab = NewSheet.getTab( NewSheet.Reflection, "Master-symbols");
			ListEntry[] rows = tab.fetchRows();
			for (ListEntry row : rows) {
				S.sleep(40);
				
				String symbol = row.getString("Token Symbol");
				boolean tfh = row.getBool("24-Hour");
				int conid = row.getInt("Conid");
				String primary = row.getString("Primary Exchange");
				
				Contract c = new Contract();
				c.symbol(symbol);
				c.currency("USD");
				c.exchange("SMART");
				c.secType("STK");
				
				S.out( "Send " + symbol);
				m_controller.reqContractDetails(c, list -> {
					S.out( "Rec " + symbol);
					try {
						Util.require(list.size() == 1, symbol + " returned " + list.size() );
						ContractDetails item = list.get(0);
						boolean set = false;
						
						// check and set conid
						if (conid == 0) {
							row.setValue("Conid", "" + item.conid() );
							set = true;
						}
						else if (conid != item.conid() ) {
							S.out( "Conid doesn't match for " + symbol);
						}
						
						// check and set primary exchange
						if (S.isNull(primary) ) {
							row.setValue("Primary Exchange", item.contract().primaryExch() );
							set = true;
						}
						else if (!primary.equals( item.contract().primaryExch() ) ) {
							S.out( "Primary exchange doesn't match for " + symbol);
						}

						// update row
						if (set) {
							row.update();  // maybe this takes a minute
						}
					}
					catch( Exception e) {
						S.out( e.getMessage() );
					}
					dec();
				});
				m_sent++;
				
				if (tfh) {
					c.exchange("IBEOS");

					S.out( "Send " + symbol + " IBEOS");
					m_controller.reqContractDetails(c, list -> {
						S.out( "Rec " + symbol + " IBEOS");
						try {
							Util.require(list.size() > 0, "No IBEOS listing found for " + symbol);
						}
						catch( Exception e) {
							S.out( e.getMessage() );
						}
						dec();
					});
					m_sent++;
				}
				
			}
			m_allSent = true;
			check();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
