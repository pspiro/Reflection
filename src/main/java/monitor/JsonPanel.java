package monitor;

import java.awt.LayoutManager;

import javax.swing.JPanel;

import common.Util;
import tw.util.NewTabbedPanel.INewTab;

public class JsonPanel extends JPanel implements INewTab {
	public JsonPanel(LayoutManager layout) {
		super(layout);
	}

	public JsonPanel() {
	}

	@Override public void activated() {
		Util.wrap( () -> refresh() );
	}
	
	@Override public void closed() {
	}
	
	public void refresh() throws Exception {
	}
}
