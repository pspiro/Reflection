package monitor;

import java.awt.LayoutManager;
import java.awt.Window;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import common.Util;
import common.Util.ExRunnable;
import tw.util.UI;
import tw.util.NewTabbedPanel.INewTab;

public abstract class MonPanel extends JPanel implements INewTab {
	public MonPanel(LayoutManager layout) {
		super(layout);
	}

	@Override public void activated() {
		refreshTop();
	}

	/** Display hourglass and refresh, catch and display exceptions */
	protected final void refreshTop() {
		wrap( () -> UI.watch( Monitor.m_frame, () -> refresh() ) );
	}
	
	/** Display the message in a popup */
	public void wrap(ExRunnable runner) {
		try {
			UI.watch( Monitor.m_frame, runner); // display hourglass and catch exceptions
		}
		catch (Throwable e) {
			e.printStackTrace();
			Util.inform( this, e.getMessage() );
		}
	}

	protected abstract void refresh() throws Exception;
	
	@Override public void switchTo() {
	}

	@Override public void closed() {
	}
	
	protected Window getWindow() {
		return SwingUtilities.getWindowAncestor(this);
	}
}