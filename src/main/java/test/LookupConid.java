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
				S.sleep(100);
				
				String symbol = row.getString("Token Symbol");
				String tfh = row.getString("24-Hour");
				int conid = row.getInt("Conid");
				String primary = row.getString("Primary Exchange");
				String description = row.getString("Description");
				
				Contract c = new Contract();
				c.symbol(symbol.replace("-", " "));  // handle BRK-B
				c.currency("USD");
				c.exchange("SMART");
				c.secType("STK");
				
				m_controller.reqContractDetails(c, list -> {
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
							S.out( "Conid doesn't match for %s (%s vs %s)", symbol, conid, item.conid() );
						}
						
						// check and set primary exchange
						if (S.isNull(primary) ) {
							row.setValue("Primary Exchange", item.contract().primaryExch() );
							set = true;
						}
						else if (!primary.equals( item.contract().primaryExch() ) ) {
							S.out( "Primary exchange doesn't match for %s (%s vs %s)", symbol, primary, item.contract().primaryExch() );
						}
						
						// check and set 24H
						boolean isOvernight = item.validExchanges().indexOf("OVERNIGHT") != -1;						
						if (S.isNull(tfh) ) {
							row.setValue( "24-Hour", isOvernight ? "TRUE" : "FALSE");
							set = true;
						}
						else if ( tfh.equals("TRUE") != isOvernight) {
							S.out( "Overnight hours does not match for %s (%s vs %s)", symbol, tfh, isOvernight);
						}
						
						// update description if blank
						if (S.isNull(description) ) {
							row.setValue("Description", item.longName() );
							set = true;
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
			}
			m_allSent = true;
			check();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
