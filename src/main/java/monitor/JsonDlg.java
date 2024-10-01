package monitor;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.json.simple.JsonObject;

import tw.util.VerticalPanel;

/** Display a dialog box field fields for each tag in the json */
public class JsonDlg extends JDialog {
    private JsonObject m_json;
    private HashMap<String, JTextField> m_fieldMap;
    private boolean m_ok;

    /** You probably want to pass OrderedJson */
    public JsonDlg(Frame parent, JsonObject json) {
        super(parent, "Edit JSON", true);  // true to make it modal
        this.m_json = json;
        this.m_fieldMap = new HashMap<>();
        this.m_ok = false;
        
        setLayout(new BorderLayout());
        
        // Create panel for form inputs
        JPanel formPanel = new VerticalPanel();
        formPanel.setBorder( new EmptyBorder( 40, 40, 40, 40));
        
        // Iterate through the JSON object fields
        
        for (var entry : m_json.entrySet() ) {
        	var field = new JTextField( entry.getValue().toString(), 30);
            formPanel.add( entry.getKey(), field);
            m_fieldMap.put( entry.getKey(), field);
        }

        // Create panel for OK and Cancel buttons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Action listeners
        okButton.addActionListener(e -> onOK());
        cancelButton.addActionListener(e -> onCancel());

        pack();
        setLocationRelativeTo(parent);
    }

    private void onOK() {
        // Update the JSON object with the values from text fields
        for (String key : m_fieldMap.keySet()) {
            String newValue = m_fieldMap.get(key).getText();
            m_json.put(key, newValue);  // Update the JSON object
        }
        m_ok = true;
        setVisible(false);  // Close the dialog
    }

    private void onCancel() {
        m_ok = false;
        setVisible(false);  // Close the dialog
    }

    public boolean ok() {
        return m_ok;
    }

	public static boolean edit(JFrame parent, JsonObject json) {
		var dlg = new JsonDlg( parent, json);
		dlg.setVisible( true);
		return dlg.ok();
	}
}
