package monitor;

import java.awt.BorderLayout;

public class HoldersPanel extends JsonPanel {
	HoldersPanel() {
		super( new BorderLayout(), "wallet,balance");
		add( m_model.createTable() );
	}

	public void refresh(String contractAddr) {
		
	}

}
