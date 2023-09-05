package tw.google;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

import com.google.api.services.sheets.v4.Sheets.Spreadsheets;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;

import common.Util;
import tw.google.NewSheet.Book.Tab.ListEntry;
import tw.util.MyException;
import tw.util.OStream;
import tw.util.S;

/** Note: you cannot have null values in the list for insert or update operations.
 *  You might want to check and throw an exception. */
public class NewSheet {
	public static class Rows extends ArrayList<ListEntry> {
		@Override public void removeRange(int fromIndex, int toIndex) {
			super.removeRange(fromIndex, toIndex);
		}
	}

	public static final String Qt = "1Oh__k1j0zpbb58hjBG_tShwp-eFMyC2ODwIZ_XmvlQw";
	public static final String Bf = "17SeDo2BMsKl7nKfY9Y_4fxeLRx4JANv0h2hcPDHxpjA";
	public static final String Skippers = "1f3DS26ARD_BQnpcNaeUOTvEj4ZsT2XaUVAj2_oGe6gw";
	public static final String Personal = "1d-FLgVu7D5qZWh2AVcRfZ-IGVVcowOItWJaxPBsTqa4";
	public static final String Tw = "1NIK0RFtpsquqTqw3CPDuKne7icyl0ny1O9J3u6bfpMU";
	public static final String Rjjw = "15GeuF0_20-2v-vg8po6RuOB4HbztaaxKVEuvPfsCD5M";
	public static final String Reflection = "1yxE8i8Qfm0ppLXI_GF0AB4n-p3yL_jqNF8ESIz-OZCA";
	public static String Remittance = "16jO882MA5_Lvehh1sEbgjkfVjjtcb7tH4h-GmnRSuYM";
	//public static String Remittance = "1Rc4hUFlqjaHE-4DSZs9Q1KLQSa9zWre5P-Dj5x06ZyE"; // test
	
	public static void main(String[] args) throws Exception {
		
		Book.Tab tab = getTab( Reflection, "Symbols");
		
		ListEntry[] rows = tab.fetchRows();
		for (ListEntry row : rows) {
			S.out( row.getString("Symbol") );
		}
	}
	
	public static Book.Tab getTab( String sheetId, String tabName) throws Exception {
		return getBook( sheetId).getTab( tabName);
	}
	
	public static Book getBook( String sheetId) throws Exception {
		if (sheetId.length() != Qt.length() ) {
			throw new MyException( "Error: invalid sheet id %s", sheetId);
		}
		Spreadsheet spreadsheet = sheets().get(sheetId).execute();
		return new Book( spreadsheet);
	}
	
	static Spreadsheets sheets() throws IOException, Exception {
		return Auth.auth().getSheetsService().spreadsheets();
	}
	
	public static class Book {
		private static final String USER_ENTERED = "USER_ENTERED";
		private String m_name;
		private Spreadsheet m_spreadsheet;
		
		public String id() { return m_spreadsheet.getSpreadsheetId(); }
		public String name() { return m_name; }
		
		public Book( Spreadsheet spreadsheet) {
			m_spreadsheet = spreadsheet;
			m_name = spreadsheet.getProperties().getTitle();
		}
		
		/** This method works but the tab is not accessible. You would have to force it to re-read
		 *  the list of sheets somehow. */
		public void createTab(String name) throws Exception {
			List<Request> requests = new ArrayList<>();
			requests.add(new Request().setAddSheet(new AddSheetRequest().setProperties(new SheetProperties().setTitle(name))));
			
			BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(requests);
			sheets().batchUpdate(id(), body).execute();
		}

		public void save(String folder) throws Exception {
			new File(folder).mkdir();
			for (Sheet sheet : m_spreadsheet.getSheets() ) {
				new Tab(sheet).save(folder);
			}
		}

		public Tab getTab( String name) throws Exception {
			for (Sheet tab : m_spreadsheet.getSheets() ) {
				if (tab.getProperties().getTitle().compareToIgnoreCase( name) == 0) {
					return new Tab( tab);
				}
			}
			throw new MyException( "Error: no tab %s in book %s", name, m_name);
		}
 		
		/** Does not return empty rows at the bottom. */
		public List<List<Object>> getRows( String tabName, int rows, int cols, String renderOption) throws Exception {
			String range = String.format( "'%s'!A1:%s%s", tabName, intToLetters(cols), rows);
			List<List<Object>> vals = sheets().values().get( id(), range)
					.setValueRenderOption(renderOption).execute().getValues();
			return vals;
		}

		public void insert(String tabName, List<List<Object>> rows) throws Exception {
			if (rows.size() > 10000) { // google can go up to 40,000 but 10k is a red flag for us
				throw new MyException( "Error: max insert rows exceeded in %s", tabName);
			}
			
			// set all not null
//			for (List<Object> row : rows) {
//				for (int i = 0; i < row.size(); i++) {
//					if (row.get(i) == null) {
//						row.set(i, "");
//					}
//				}
//			}
			
			ValueRange content = new ValueRange();
			content.setValues( rows);
			insert( tabName, content);
		}
		
		public void insert(String tabName, ValueRange content) throws Exception {
			String range = String.format( "'%s'!A1:A1", tabName);
			sheets().values().append(id(), range, content)
				.setValueInputOption( USER_ENTERED).execute();
		}
		
		/** This will break if any rows have been deleted or re-ordered. */
		public void update(String tabName, ListEntry listEntry) throws Exception {
			ValueRange content = new ValueRange().setValues( listEntry.asList() );
			
			sheets().values().update(id(), listEntry.getRange(), content)
					.setValueInputOption( USER_ENTERED).execute();
		}
		
		public void update(String tabName, Rows entries) throws Exception {
			ArrayList<ValueRange> data = new ArrayList<ValueRange>();
			
			for (ListEntry entry : entries) {
				ValueRange content = new ValueRange()
						.setValues( entry.asList() )
						.setRange( entry.getRange() );
				data.add( content);
			}
			
			BatchUpdateValuesRequest content = new BatchUpdateValuesRequest()
					.setData(data)
					.setValueInputOption( USER_ENTERED);
			sheets().values().batchUpdate(id(), content).execute();
		}

		/** Row and column are 1-based. */
		public void updateCell(String tabName, int row, int col, String val) throws Exception {
			String range = String.format( "'%s'!%s%s", tabName, intToLetters(col), row);
			
			List<Object> arow = new ArrayList<Object>();
			arow.add( val);
			
			List<List<Object>> rows = new ArrayList<List<Object>>();
			rows.add( arow);
			
			ValueRange content = new ValueRange().setValues( rows);
			
			sheets().values().update(id(), range, content)
					.setValueInputOption( USER_ENTERED).execute();
		}

		public class Tab {
			private Sheet m_sheet;
			private String m_name;
			private String[] m_headerRow; // could be shorter than the longest row
			private HashMap<String,Integer> m_map = new HashMap<String,Integer>(); // map column header to zero-based index
			private Rows m_insEntries; // for bulk inserts
			private Rows m_updEntries; // for bulk inserts

			String name() { return  m_name; }
			
			Tab( Sheet sheet) throws Exception {
				m_sheet = sheet;
				m_name = sheet.getProperties().getTitle();
				
				// build map of column header to zero-based index
				// remove spaces in the header names
				int cc = 0;
				for (String title : getHeaderRow() ) {
					m_map.put( title.replaceAll( " ", ""), Integer.valueOf( cc++) ); 
				}
			}
			
			public void save(String folder) throws IOException, Exception {
				List<List<Object>> rows = getRows(false);
				if (rows == null) return;  // some sheets may have no rows
				
				JsonArray ar = new JsonArray();
				
				for (int i = 1; i < rows.size(); i++) {  // skip header row
					List<Object> row = rows.get(i);
					JsonObject obj = new JsonObject();
					for (int c = 0; c < row.size() && c < m_headerRow.length; c++) {
						String name = m_headerRow[c];
						if (S.isNotNull(name) ) {
							obj.put(name, row.get(c) );
						}
					}
					ar.add(obj);
				}
				if (ar.size() > 0) {
					String filename = String.format("%s/%s.json", folder, m_name);
					try (OStream os = new OStream(filename) ) {
						os.write( ar.toString() );
					}
				}
			}
			
			/** Find and return the row when the value in the "col" column is equal to value. */
			public ListEntry findRow( String col, String value) throws Exception {
				for ( ListEntry row : fetchRows() ) {
					if (value.equals( row.getString( col) ) ) {
						return row;
					}
				}
				return null;
			}
			
			public void startTransaction() throws Exception {
				if (m_insEntries != null || m_updEntries != null) {
					throw new MyException( "Transaction already started in %s", m_name);
				}
				m_insEntries = new Rows();
				m_updEntries = new Rows();
			}
			
			public void commit() throws Exception {
				if (m_insEntries == null && m_updEntries == null) {
					throw new MyException( "Error: no transaction started in %s", m_name);
				}
				
				if (m_insEntries.size() > 0) {
					S.out( "Inserting into %s", m_name);
					insert( m_insEntries);
				}
				m_insEntries = null;
				
				if (m_updEntries.size() > 0) {
					S.out( "Updating %s", m_name);
					update( m_updEntries);
				}
				m_updEntries = null;
			}
			
			/** Use this version if the sheet has an auto-calculating column as the last column;
			 *  could be changed to look only at the first column to determine if it's empty or not.
			 *  This version updates existing rows with the new values and inserts the rows
			 *  for which there is no room. */
			public void commitInsert(ListEntry[] rows) throws Exception {
				if (m_insEntries == null) {
					throw new MyException( "Error: no transaction started in %s", m_name);
				}
				
				S.out( "Cleverly inserting into %s", m_name);
				int lastUsed = getLastUsedRowNum(rows);
				int blankRows = rows() - lastUsed;
				int needed = m_insEntries.size() - blankRows;
				
				int rowNum = lastUsed + 1;
				for (ListEntry row : m_insEntries) {
					row.rowNum( rowNum++);
				}

				Rows ins = new Rows();
				if (needed > 0) {
					int start = m_insEntries.size() - needed;
					for (int i = start; i < m_insEntries.size(); i++) {
						ins.add( m_insEntries.get(i) );
					}
					m_insEntries.removeRange( start, m_insEntries.size() );
				}

				if (m_insEntries.size() > 0) {
					update( m_insEntries);
				}
				
				if (ins.size() > 0) {
					insert( ins);
				}

				m_insEntries = null;
			}
			
			/** Return rownum of last used row. */
			private int getLastUsedRowNum(ListEntry[] rows) {
				for (int i = rows.length - 1; i >= 0; i--) {
					if (!rows[i].isMostlyEmpty() ) {
						return i + 2;
					}
				}
				return 1;
			}

			public String[] getHeaderRow() throws Exception {
				if (m_headerRow == null) {
					List<List<Object>> rows = Book.this.getRows( m_name,  1, cols(), "FORMULA");
					if (rows == null || rows.size() == 0) {
						S.out( "Warning: no header row on tab %s", m_name);
						return new String[0];
					}
					
					int i = 0;
					List<Object> row = rows.get(0);
					String[] ar = new String[row.size()];
					for (Object obj : row) {
						ar[i++] = obj != null ? obj.toString() : "";
					}
					m_headerRow = ar;
				}
				
				return m_headerRow;
			}
			
			public ListEntry newListEntry() {
				return new ListEntry();
			}
			
			/** Row and column are 1-based. */
			public void setValue(int row, int col, String val) throws IOException, Exception {
				Book.this.updateCell( m_name, row, col, val);
			}

			public void insert(List<List<Object>> vals) throws IOException, Exception {
				Book.this.insert( m_name, vals);
			}

			/** Adds rows; ignores transaction. */
			public void insert(Rows entries) throws IOException, Exception {
				List<List<Object>> rows = new ArrayList<List<Object>>();
				for (ListEntry entry : entries) {
					rows.add( entry.row() );
				}
				Book.this.insert( m_name, rows);
			}
			
			/** Insert row or add to list if we are within a transaction. */
			public void insert(ListEntry entry) throws IOException, Exception {
				if (m_insEntries != null) {
					m_insEntries.add( entry);
				}
				else {
					Book.this.insert( m_name, entry.asList() );
				}
			}
			
			public void update(ListEntry listEntry) throws IOException, Exception {
				if (m_updEntries != null) {
					m_updEntries.add( listEntry);
				}
				else {
					Book.this.update( m_name, listEntry);
				}
			}

			public void update(Rows entries) throws IOException, Exception {
				Book.this.update( m_name, entries); 
			}

			/** You probably want fetchRows() instead. Does not return empty rows at the bottom. */
			public List<List<Object>> getRows(boolean formatted) throws IOException, Exception {
				String renderOption = formatted ? "FORMATTED_VALUE" : "UNFORMATTED_VALUE"; 
				return Book.this.getRows( m_name, rows(), cols(), renderOption);
			}
			
			/** Returns the real number of cols, blank or not. */
			public int cols() {
				return m_sheet.getProperties().getGridProperties().getColumnCount();
			}
			
			/** Returns the real number of rows, blank or not. */
			public int rows() {
				return m_sheet.getProperties().getGridProperties().getRowCount();
			}

			/** Does not return the blank rows at the bottom, unless there is a formula that extends down,
			 *  i.e. Register tabs. Add two to index to get the rowNum. */
			public ListEntry[] fetchRows() throws IOException, Exception {
				return fetchRows( true);
			}
			
			/** Does not return empty rows at the bottom. */
			public ListEntry[] fetchRows(boolean formatted) throws IOException, Exception {
				List<List<Object>> rows = getRows(formatted);

				ListEntry[] entries = new ListEntry[rows.size()-1]; // contains all rows except header
				
				int rc = 0, ec = 0;
				for (List<Object> row : rows) {
					// skip header row
					if (rc++ == 0) {
						continue;
					}
					else {
						entries[ec++] = new ListEntry( row, rc);  // rc has already been incremented, but we want 1-based, so it works
					}
				}
				 
				return entries;
			}

			public class ListEntry {
				private List<Object> m_row;
				private int m_rowNum; // 1-based, starting at first data row
				
				public List<Object> row() { return m_row; }

				public void rowNum(int v) { m_rowNum = v; } 

				/** Return true if all columns are empty except the last one. */
				public boolean isMostlyEmpty() {
					for (int i = 0; i < m_row.size() - 1; i++) {
						if (S.isNotNull( m_row.get(i).toString() ) ) {
							return false;
						}
					}
					return true;
				}

				public int rowNum() { return m_rowNum; }  // 1-based, starting at first data row

				public ListEntry() {
					m_row = new ArrayList<Object>();
				}
	
				public ListEntry(List<Object> row, int rowNum) {
					m_row = row;
					m_rowNum = rowNum;
				}
				
				public List<List<Object>> asList() {
					List<List<Object>> rows = new ArrayList<List<Object>>();
					rows.add( m_row);
					return rows;
				}

				public String getRange() {
					return String.format( "'%s'!A%s", Tab.this.m_name, rowNum() );
				}
				
				@Override public java.lang.String toString() {
					return m_row.toString(); 
				}
	
				int getIndex( String tag) throws MyException {
					Integer i = m_map.get( tag.replaceAll( " ", ""));
					if (i == null) {
						throw new MyException( "Error: no column %s in tab %s", tag, Tab.this.name() );
					}
					return i;
				}
				
				public void insert() throws Exception {
					Tab.this.insert( this);
				}
	
				public String getString( String tag) throws MyException {
					int i = getIndex( tag);
					return i > m_row.size() - 1 ? "" : m_row.get( i).toString();
				}
				
				public void setExactValue(String tag, String val) throws MyException {
					setValue( tag, "'" + val);
				}
				
				public ListEntry addValue(String val) {
					if (val != null) {
						m_row.add( val);
					}
					else {
						m_row.add( "");  // if you don't add a string, the columns can get out of sync
					}
					return this;
				}
				
				public ListEntry setValue(String tag, String val) throws MyException {
					int i = getIndex( tag);

					// grow the array if necessary
					while( m_row.size() <= i) {
						m_row.add( "");
					}
					
					// you cannot set a null value or all subsequent columns are moved left
					m_row.set( getIndex( tag), val == null ? "" : val);
					return this;
				}
				
				public void update() throws IOException, Exception {
					Tab.this.update( this); 
				}

				public String[] getTags() throws Exception {
					return getHeaderRow();
				}

				/** Return date in yyyy-mm-dd format. */ 
				public String getDate(String tag) throws Exception {
					return S.formatDate( getString( tag) );
				}

				public boolean hasTag(String tag) {
					return m_map.get( tag) != null;
				}

				/** Returns zero for null value. 
				 * @throws MyException */
				public int getInt(String tag) throws Exception {
					String val = getString( tag);
					return S.isNotNull( val) ? Integer.valueOf( val) : 0; 
				}

				public boolean getBool(String tag) throws MyException {
					String val = getString(tag);
					return "true".equalsIgnoreCase(val) || "Y".equalsIgnoreCase(val); 
				}

				public double getDouble(String tag) throws Exception {
					String val = getString(tag);
					return S.isNotNull(val) ? Double.valueOf(val) : 0.0;
				}
			}

		}


	}
	
	
	static private String intToLetters( int col) {
		if (col > 26) {
			char first = (char)('A' - 1 + col / 26);
			char second = (char)('A' - 1 + col % 26);
			return String.format( "%s%s", first, second);
		}
		return String.valueOf( (char)('A' - 1 + col) );
	}

}
