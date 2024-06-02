package common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;

import com.sun.net.httpserver.HttpExchange;

import http.BaseTransaction;
import http.MyClient;
import http.MyServer;
import tw.util.S;

/** This serve issues Linux commands received from the client and returns the results;
 *  not currently being used */
public class LinuxServer {
	public static void main(String[] args) throws Exception {
		Util.wrap( () -> MyClient.getResponse( "http://localhost?54545/linux/quit") );
		S.sleep( 100);
		new LinuxServer().run();
	}
	
	void run() throws IOException {
		MyServer.listen( 54545, 10, server -> {
			server.createContext("/linux/execute", exch -> new Trans(exch, true).handleCommand() );
			server.createContext("/linux/quit", exch -> new Trans(exch, true).handleCommand() );
		});

	}
	
	class Trans extends BaseTransaction {
		public Trans(HttpExchange exchange, boolean debug) {
			super(exchange, debug);
		}

		public void handleCommand() {
			wrap( () -> {
				String command = "GET".equals(m_exchange.getRequestMethod() )
						? m_uri.split("\\?")[1]
						: parseToObject().getString("command");
				
				command = URLDecoder.decode( command);

				// Execute the command
				S.out( "Executing " + command);
			    Process process = Runtime.getRuntime().exec(command);
			    
			    StringBuilder sb = new StringBuilder();

			    // Read the output of the command
			    S.out( "Reading output");
			    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			    String line;
			    while ((line = reader.readLine()) != null) {
			        sb.append( line);
			        sb.append( "\n");
			    }
			    
			    S.out( "Waiting for completion");
			    int exitVal = process.waitFor();
			    sb.append( "Exit val: " + exitVal);
			    
			    respondWithPlainText(sb.toString() );
			}); 
		}
	}
}
