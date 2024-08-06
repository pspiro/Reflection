package monitor;

import java.awt.BorderLayout;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import common.SignupReport;
import common.Util;

class SignupPanel extends JsonPanel {
	JProgressBar bar = new JProgressBar();
	
	SignupPanel() {
		super( new BorderLayout(), "created_at,email,first,last,country,referer,ip,utm_source,jotform,connected,transactions,rusd");
		//add( "Signups", BorderLayout.NORTH);
		add( bar, BorderLayout.NORTH);
		add( m_model.createTable() );
	}
	
	@Override protected void refresh() throws Exception {
		bar.setValue( 0);
		bar.setMaximum(130);  // estimate
		
		Util.execute( () -> {
			try {
				Monitor.m_config.sqlCommand( sql -> {
					var ar = SignupReport.create( 3, sql, Monitor.m_config.rusd(), () -> {
						SwingUtilities.invokeLater( () -> {
							bar.setValue( bar.getValue() + 1);
							bar.repaint();
						});
					});
					setRows( ar);
					SwingUtilities.invokeLater( () -> m_model.fireTableDataChanged() );
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	
	@Override protected Object format(String key, Object value) {
		return key.equals("referer") ? Util.unescHtml(value.toString()) : value;
	}
}