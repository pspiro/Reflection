package test;

import java.io.IOException;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.controller.ApiController;
import com.ib.controller.ConnectionAdapter;

import common.Util;
import reflection.SingleChainConfig;
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
		SingleChainConfig config = SingleChainConfig.readFrom("Dev3-config");
		m_controller.connect(config.twsOrderHost(), config.twsOrderPort(), config.twsOrderClientId() , null);
	}

	public void onRecNextValidId(int id) {
		Util.execute( () -> query() );
	}
	
	boolean m_allSent;
	int m_outstanding;
	
	void dec() {
		m_outstanding--;
		check();
	}
	
	private synchronized void check() {
		if (m_allSent && m_outstanding == 0) {
			S.out( "done");
			m_controller.disconnect();
		}
	}
	
	@Override public void onDisconnected() {
		System.exit(0);
	}
	
	public void query() {
		try {
			Tab tab = NewSheet.getTab( NewSheet.Reflection, "Lookup");
			ListEntry[] rows = tab.fetchRows();
			for (ListEntry row : rows) {
				S.sleep(100);
				
				String symbol = row.getString("Symbol");
				int conid = row.getInt("Conid");
				String queryExch = row.getString("Query Exch");
				String secType = row.getString("SecType");
				String description = row.getString("Description");
				String primary = row.getString("Primary Exch");
				String tradingHours = row.getString("Trading hours");
				
				Contract c = new Contract();
				c.symbol(symbol.replace("-", " "));  // handle BRK-B
				c.currency("USD");
				c.exchange(queryExch);
				c.secType( Util.valOr( secType, "STK") );
				
				// or do this to get overnight hours--only works with conid!
//				c.conid( conid);
//				c.exchange( "OVERNIGHT");
				
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
						else {
							Util.require( 
									conid == item.conid(), 
									"Conid doesn't match for %s (%s vs %s)", 
									symbol, conid, item.conid() );
						}
						
						// check and set primary exchange
						if (S.isNull(primary) ) {
							row.setValue("Primary Exch", item.contract().primaryExch() );
							set = true;
						}
						else if (!queryExch.equals( item.contract().primaryExch() ) ) {
							S.out( "Primary exchange doesn't match for %s (%s vs %s)", symbol, queryExch, item.contract().primaryExch() );
						}
						
						// check and set 24H
						boolean isOvernight = item.validExchanges().indexOf("OVERNIGHT") != -1;						
						row.setValue( "24-Hour", isOvernight ? "TRUE" : "FALSE");
						set = true;
						
						// update description if blank
						if (S.isNull(description) ) {
							row.setValue("Description", item.longName() );
							set = true;
						}
						
						if (S.isNull(tradingHours) ) {
							row.setValue("Trading hours", item.tradingHours() );
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
				m_outstanding++;
			}
			m_allSent = true;
			check();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
