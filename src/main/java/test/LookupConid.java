package test;

import java.io.IOException;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.controller.ApiController;
import com.ib.controller.ConnectionAdapter;

import reflection.Config;
import reflection.Util;
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

	public void onConnected() {
		try {
			Tab tab = NewSheet.getTab( NewSheet.Reflection, "Master-symbols");
			ListEntry[] rows = tab.fetchRows();
			for (ListEntry row : rows) {
				S.sleep(50);
				
				String symbol = row.getString("Token Symbol");
				boolean tfh = row.getBool("24-Hour");
				int conid = row.getInt("Conid");
				
				Contract c = new Contract();
				c.symbol(symbol);
				c.currency("USD");
				c.exchange("SMART");
				c.secType("STK");
				
				m_controller.reqContractDetails(c, list -> {
					try {
						Util.require(list.size() > 0, "Nothing found for " + symbol);
						ContractDetails item = list.get(0);
						if (conid == 0) {
							row.setValue("Conid", "" + item.conid() );
							row.update();
						}
						else {
							Util.require(conid == item.conid(), "Conid doesn't match for " + symbol);
						}
					}
					catch( Exception e) {
						S.out( e.getMessage() );
					}
				});
				
				if (tfh) {
					c.exchange("IBEOS");

					m_controller.reqContractDetails(c, list -> {
						try {
							Util.require(list.size() > 0, "No IBEOS listing found for " + symbol);
						}
						catch( Exception e) {
							S.out( e.getMessage() );
						}
					});
					
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
