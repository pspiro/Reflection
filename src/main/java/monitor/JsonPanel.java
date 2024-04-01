package monitor;

import java.awt.LayoutManager;

import javax.swing.JPopupMenu;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import common.JsonModel;
import tw.util.S;

/** Panel with a table that contains rows of Json objects; each column header is a key 
 *  in the Json table */
public abstract class JsonPanel extends MonPanel {
	final protected JsonModel m_model;
	
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

	protected void buildMenu(JPopupMenu menu, JsonObject record, String tag, Object val) {
	}

	/** Pass events to the panel so need only subclass the panel */
	class JsonPanelModel extends JsonModel {
		public JsonPanelModel(String allNames) {
			super(allNames);
		}

		@Override protected Object format(String tag, Object value) {
			return JsonPanel.this.format(tag, value);
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

		@Override public final void buildMenu(JPopupMenu menu, JsonObject record, String tag, Object val) {
			JsonPanel.this.buildMenu(menu, record, tag, val);
		}
	}
}
