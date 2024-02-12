package monitor;

import java.awt.LayoutManager;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.JsonModel;
import common.Util;
import monitor.Monitor.MonPanel;
import tw.util.S;

/** Panel with a table that contains rows of Json objects; each column header is a key 
 *  in the Json table */
public abstract class JsonPanel extends MonPanel {
	final JsonModel m_model;
	
	protected abstract void refresh() throws Exception;

	public JsonPanel(LayoutManager layout, String allNames) {
		super(layout);
		m_model = createModel(allNames);
	}
	
	JsonModel createModel(String allNames) {
		return new JsonPanelModel(allNames);
	}
	
	protected void setRows( JsonArray rows) {
		m_model.setRows( rows);
	}
	
	protected JsonArray rows() {
		return m_model.ar();
	}
	
	protected String getTooltip(JsonObject row, String tag) {
		return null;
	}

	/** Format doubles with comma and two decimals */
	protected Object format(String key, Object value) {
		return value instanceof Double ? S.fmt2((double)value) : value; 
	}

	protected void delete(int row, int col) {
	}
	
	protected void onCtrlClick(JsonObject row, String tag) {
	}	

	protected void onDouble(String tag, Object val) {
	}

	protected void onRightClick(MouseEvent e, JsonObject record, String tag, Object val) {
		JPopupMenu m = new JPopupMenu();
		m.add( JsonModel.menuItem("Copy", ev -> Util.copyToClipboard(val) ) );
		m.show( e.getComponent(), e.getX(), e.getY() );
	}

	/** Pass events to the panel so need only subclass the panel */
	class JsonPanelModel extends JsonModel {
		public JsonPanelModel(String allNames) {
			super(allNames);
		}

		@Override protected Object format(String tag, Object value) {
			return JsonPanel.this.format(tag, value);
		}
		
		@Override protected void delete(int row, int col) {
			JsonPanel.this.delete(row, col);
		}
		
		@Override protected void onCtrlClick(JsonObject row, String tag) {
			JsonPanel.this.onCtrlClick(row, tag);
		}
		
		@Override protected void onDoubleClick(String tag, Object val) {
			JsonPanel.this.onDouble(tag, val);
		}

		@Override protected final String getTooltip(JsonObject row, String tag) {
			return JsonPanel.this.getTooltip(row, tag);
		}

		@Override public final void onRightClick(MouseEvent e, JsonObject record, String tag, Object val) {
			JsonPanel.this.onRightClick(e, record, tag, val);
		}
	}
}
