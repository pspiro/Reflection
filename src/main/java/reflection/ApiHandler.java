package reflection;

import java.util.List;

import com.ib.controller.ApiConnection.ILogger;
import com.ib.controller.ApiController.IConnectionHandler;

import tw.util.S;

public class ApiHandler implements IConnectionHandler { // move IConnectionHandler to ConnectionMgr
	private Main m_main;

	ApiHandler(Main main) {
		m_main = main;
	}
	
	@Override public void connected() {
		m_main.m_connMgr.onConnected();
	}

	@Override public void disconnected() {
		m_main.m_connMgr.onDisconnected();
	}

	@Override public void error(Exception e) {
		e.printStackTrace();
	}

	@Override public void show(String string) {
		S.out( "Show: " + string);
	}

	@Override public void accountList(List<String> list) {
	}

	@Override public void message(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
		switch (errorCode) {
			case 1100: 
				m_main.ibConnection( false); 
				break;
			case 1102: 
				m_main.ibConnection( true); 
				break;
		}
		
		S.out( "RECEIVED %s %s %s", id, errorCode, errorMsg);
	}
}
