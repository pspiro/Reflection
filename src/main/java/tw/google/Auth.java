package tw.google;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow.Builder;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;

import tw.util.S;

public class Auth {
	private static final String applicationName ="Reflection"; // was Brisco
	private static FileDataStoreFactory dataStoreFactory;
	private static final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
	private static HttpTransport httpTransport;
	private static final String clientId = "1061634809648-vrnvk8f7ltagdn36442oa12a9hnubpr7.apps.googleusercontent.com";
	private static final String secret = "3eTlljq0d9uSyAySFl2tELH6";
	
	private Credential m_credentials;
	private Sheets m_sheet;    // new
	private TwMail m_mail;
	
	public Credential c() { return m_credentials; }
	
	static {
		try {
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			
			Logger.getLogger(FileDataStoreFactory.class.getName()).setLevel(Level.SEVERE);
			File dataStoreDir = new File(".", ".googfiles"); // Directory to store user credentials for this application.
			dataStoreFactory = new FileDataStoreFactory(dataStoreDir);
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void main(String[] args) throws Exception {
		Sheets sheetsService = auth().getSheetsService();
		Spreadsheets spreadsheets = sheetsService.spreadsheets();
		Spreadsheet spreadsheet = spreadsheets.get("16jO882MA5_Lvehh1sEbgjkfVjjtcb7tH4h-GmnRSuYM").execute();
		List<Sheet> tabs = spreadsheet.getSheets();
		for (Sheet tab : tabs) {
			S.out( tab.getProperties().getTitle() ); 
		}
	}
	
	private static Auth s_auth;
	
	public static Auth auth() throws Exception {
		if (s_auth == null) {
			try {
				s_auth = new Auth();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return s_auth;
	}
	
	private Auth() throws Exception {
		// Global instance of the scopes required by this quickstart.
		// Look here for full list: https://developers.google.com/gmail/api/auth/scopes
		// !!!NOTE: you must delete the .credentials folder after modifying this list!!!
		ArrayList<String> scopes = new ArrayList<String>();
//		scopes.add( "https://www.googleapis.com/auth/gmail.modify");
//		scopes.add( "https://www.googleapis.com/auth/gmail.readonly");
		scopes.add( "https://www.googleapis.com/auth/gmail.compose");
		//scopes.add( "https://docs.google.com/feeds/");
		scopes.add( "https://spreadsheets.google.com/feeds");
		//scopes.add( "https://www.googleapis.com/auth/calendar");
		//scopes.add( "https://www.googleapis.com/auth/tasks");

		Builder builder = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, clientId, secret, scopes);
		builder.setDataStoreFactory(dataStoreFactory);
		builder.setAccessType("offline"); //???
		m_credentials = new AuthorizationCodeInstalledApp(builder.build(), new LocalServerReceiver()).authorize("user");
	}
		
    /**
     * Build and return an authorized Sheets API client service.
     * @return 
     * @return an authorized Sheets API client service
     * @throws IOException
     */
    public Sheets getSheetsService() throws IOException {
    	if (m_sheet == null) {
        	m_sheet = new Sheets.Builder(httpTransport, jsonFactory, m_credentials)
                .setApplicationName(applicationName)
                .build();
    	}
    	return m_sheet;
    }

	public TwMail getMail() {
		if (m_mail == null) {
			Gmail m_gmail = new Gmail.Builder(httpTransport, jsonFactory, m_credentials)
                .setApplicationName(applicationName)
                .build();
			m_mail = new TwMail( m_gmail);
		}
		return m_mail;
	}
}