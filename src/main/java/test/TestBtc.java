package test;

import java.io.IOException;

import com.ib.client.Contract;
import com.ib.client.TickAttrib;
import com.ib.client.TickType;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.TopMktDataAdapter;
import com.ib.controller.ConnectionAdapter;

import tw.util.S;

public class TestBtc extends ConnectionAdapter {
	final ApiController c = new ApiController(this);
	
	public static void main(String[] args) throws IOException, Exception {
		new TestBtc().run();
	}

	private void run() {
		c.connect("localhost", 9395, 22, null);
	}
	
	@Override
	public void onRecNextValidId(int id) {
		final Contract contract = new Contract();
		contract.conid( 479624278);
		contract.exchange( "PAXOS");
		
		c.reqTopMktData(contract, "", false, false, new TopMktDataAdapter() {
			@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
				S.out( "ticking %s %s %s", tickType, price, attribs);
			}
		});
		
	}
}
