/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.client;

import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.net.Socket;

import tw.util.S;

public class EClientSocket extends EClient implements EClientMsgSink  {

	protected int m_redirectCount = 0;
	protected int m_defaultPort;
    private boolean m_allowRedirect;
    protected DataInputStream m_dis;
	private boolean m_connected = false;
	private Socket m_socket;
		
	public EClientSocket(EWrapper eWrapper) {
		super(eWrapper);
	}

	@Override
	protected Builder prepareBuffer() {
        Builder buf = new Builder( 1024 );
        if( m_useV100Plus ) {
            buf.allocateLengthHeader();
        }
        return buf;
    }

	@Override
	protected void closeAndSend(Builder buf) throws IOException {
    	if( m_useV100Plus ) {
    		buf.updateLength( 0 ); // New buffer means length header position is always zero
    	}
    	
    	EMessage msg = new EMessage(buf);
    	
    	sendMsg(msg);
    }

	protected synchronized void eConnect(Socket socket) throws IOException {
	    // create io streams
	    m_socketTransport = new ESocket(socket);
	    m_dis = new DataInputStream(socket.getInputStream());
	    m_defaultPort = socket.getPort();
	    m_socket = socket;
	
	    sendConnectRequest();
	    S.out( "  sentconnectrequest");
	
	    // start reader thread
	    EReader reader = new EReader(this, m_signal, true);
	    S.out( "  created reader");
	    
    	if (!reader.putMessageToQueue()) {
    		S.out( "  putMessageToQueue failed");
    		return;
    	}
    	
    	S.out( "  messages were put to queue");    	

    	// process the server num message, which is first message, I guess
    	// not the same as the nextValidId message
    	while (m_serverVersion == 0) {
    		S.out( "  waiting for server version");
    		//S.sleep(3000);
    		m_signal.waitForSignal();
    		S.out( "    signaled");
    		// System.out.println( "processing reader msgs");  //pas
    		reader.processMsgs();
    		S.out( "  processed msgs");
    	}       
	}

	public synchronized boolean eConnect( String host, int port, int clientId, boolean extraAuth) {
		if( isConnected() ) {
			S.out( "EClientSocket.connect() already connected");
			return true;
		}
		
		if (clientId == 0) {
			clientId = common.Util.rnd.nextInt( 100, 100000000);
		}

		S.out( "Connecting to TWS on %s:%s with client id %s", host, port, clientId);

	    m_host = host;
	    m_clientId = clientId;
	    m_extraAuth = extraAuth;
	    m_redirectCount = 0;
	
	    try{
	        Socket socket = new Socket( m_host, port);
	        S.out( "  created socket");
	        eConnect(socket);
	        S.out( "  econnected");
	        return true;
	    }
	    catch( Exception e) {
	    	e.printStackTrace();
	    	eDisconnect();
	        connectionError();
	        return false;
	    }
	}

	public boolean allowRedirect() {
		return m_allowRedirect;
	}

	public void allowRedirect(boolean val) {
		m_allowRedirect = val;
	}

	@Override
	public synchronized void redirect(String newAddress) {
	    if( m_useV100Plus ) {
	    	if (!m_allowRedirect) {
	    		m_eWrapper.error(EClientErrors.NO_VALID_ID, EClientErrors.CONNECT_FAIL.code(), EClientErrors.CONNECT_FAIL.msg(), null);
	    		return;
	    	}
	    	
	        ++m_redirectCount;
	        
	        if ( m_redirectCount > REDIRECT_COUNT_MAX ) {
	            eDisconnect();
	            m_eWrapper.error( "Redirect count exceeded" );
	            return;
	        }
	                    
	        eDisconnect( false );
	        
	      	try {
				performRedirect( newAddress, m_defaultPort );
			} catch (IOException e) {
				m_eWrapper.error(e);
			}
	    }
	}

	@Override
	public synchronized void serverVersion(int version, String time) {
		//System.out.println( "received server version");  // pas
		m_serverVersion = version;
		m_TwsTime = time;	
		
		if( m_useV100Plus && (m_serverVersion < MIN_VERSION || m_serverVersion > MAX_VERSION) ) {
			eDisconnect();
			m_eWrapper.error(EClientErrors.NO_VALID_ID, EClientErrors.UNSUPPORTED_VERSION.code(), EClientErrors.UNSUPPORTED_VERSION.msg(), null);
			return;
		}
		
	    if( m_serverVersion < MIN_SERVER_VER_SUPPORTED) {
	    	eDisconnect();
	        m_eWrapper.error( EClientErrors.NO_VALID_ID, EClientErrors.UPDATE_TWS.code(), EClientErrors.UPDATE_TWS.msg(), null);
	        return;
	    }

	    if ( m_serverVersion < MIN_SERVER_VER_LINKING) {
			try {
				send( m_clientId);
			} catch (IOException e) {
				m_eWrapper.error(e);
			}
		}
	    
	    
	    // set connected flag
	    S.out( "Received server version %s", version);
	    m_connected = true;       

    	sendStartApiMsg();
    	
    	m_eWrapper.onConnected();
	}

	protected synchronized void performRedirect( String address, int defaultPort ) throws IOException {
	    System.out.println("Server Redirect: " + address);
	    
	    // Get host:port from address string and reconnect (note: port is optional)
	    String[] array = address.split(":");
	    m_host = array[0]; // reset connected host
	    int newPort;
	    try {
	        newPort = ( array.length > 1 ) ? Integer.parseInt(array[1]) : defaultPort;
	    }
	    catch ( NumberFormatException e ) {
	        System.out.println( "Warning: redirect port is invalid, using default port");
	        newPort = defaultPort;
	    }
	    eConnect( new Socket( m_host, newPort ) );
	}

	@Override
    public synchronized void eDisconnect() {
	    eDisconnect( true );
	}

	/** This is called if we can't connect and probably if there is a disconnect */
	private synchronized void eDisconnect( boolean resetState ) {
	    // not connected?
	    if( m_dis == null && m_socketTransport == null) {
	        return;
	    }
	
	    if ( resetState ) {
	    	m_connected = false;
	        m_extraAuth = false;
	        m_clientId = -1;
	        m_serverVersion = 0;
	        m_TwsTime = "";
	        m_redirectCount = 0;
	    }
	
	    FilterInputStream dis = m_dis;
	    m_dis = null;
	    if (m_socketTransport != null) {
			try {
				m_socketTransport.close();
			} catch (IOException ignored) {
			} finally {
				m_socketTransport = null;
			}
		}
	
	    try {
	        if (dis != null)
	        	dis.close();
	    } catch (Exception ignored) {
	    }
	    // Notify client: connection closed
	    m_eWrapper.connectionClosed();
	}

	public int read(byte[] buf, int off, int len) throws IOException {
		return m_dis.read(buf, off, len);
	}

	public int readInt() throws IOException {
		return m_dis.readInt();
	}

	/** Return true if we are connected and have received server version. */
	@Override public synchronized boolean isConnected() {
		return m_socket != null && m_socket.isConnected() && m_connected;
	}
}
