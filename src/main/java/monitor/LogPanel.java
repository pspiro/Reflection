package monitor;

import monitor.Monitor.RefPanel;

public class LogPanel extends QueryPanel implements RefPanel {
	LogPanel() {
		super( 	"created_at,wallet_public_key,uid,type,data", 
				"select * from log order by created_at DESC limit 40");
	}



}
