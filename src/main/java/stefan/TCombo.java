package stefan;

import javax.swing.JComboBox;

/** A types combo box. */
public class TCombo<T> extends JComboBox {
	public TCombo( T... strs) {
		super( strs);
	}

	public String getText() {
		return getSelectedItem() == null ? null : getSelectedItem().toString();
	}
	
	@Override public T getSelectedItem() {
		return (T)super.getSelectedItem();
	}
}
