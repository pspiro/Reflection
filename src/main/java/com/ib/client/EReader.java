/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.client;

import java.io.EOFException;
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;

import tw.util.S;



/**
 * This class reads commands from TWS and passes them to the user defined
 * EWrapper.
 *
 * This class is initialized with a DataInputStream that is connected to the
 * TWS. Messages begin with an ID and any relevant data are passed afterwards.
 */
public class EReader extends Thread {
    private EClientSocket 	m_clientSocket;
    private EJavaSignal m_signal;
    private EDecoder m_processMsgsDecoder;
    private static final int IN_BUF_SIZE_DEFAULT = 8192;
    private byte[] m_iBuf = new byte[IN_BUF_SIZE_DEFAULT];
    private int m_iBufLen = 0;
    private final Deque<EMessage> m_msgQueue = new LinkedList<>();
	private boolean m_startupReader;  // just reads the first message, could help for debugging
    
    protected boolean isUseV100Plus() {  // always returns true
		return m_clientSocket.isUseV100Plus();
	}

	protected EClient parent()    { return m_clientSocket; }
    private EWrapper eWrapper()         { return parent().wrapper(); }

    /**
     * Construct the EReader.
     * @param parent An EClientSocket connected to TWS.
     * @param signal A callback that informs that there are messages in msg queue.
     */
    public EReader(EClientSocket parent, EJavaSignal signal, boolean startupReader) {
    	m_clientSocket = parent;
        m_signal = signal;
        m_startupReader = startupReader;
        m_processMsgsDecoder = new EDecoder(parent.serverVersion(), parent.wrapper(), parent);
    }
    
    /**
     * Read and put messages to the msg queue until interrupted or TWS closes connection.
     */
    @Override
    public void run() {
        try {
            // loop until thread is terminated
            while (!isInterrupted()) {
            	if (!putMessageToQueue())
            		break;
            }
        }
        catch ( Exception ex ) {
        	// sometimes it goes into an infinite loop here with error CONNECT_FAIL, it retries every one second but never succeeds even though tws is running. pas
        	//if (parent().isConnected()) {
        		if( ex instanceof EOFException ) {
            		eWrapper().error(EClientErrors.NO_VALID_ID, EClientErrors.BAD_LENGTH.code(),
            				EClientErrors.BAD_LENGTH.msg() + " " + ex.getMessage(), null);
        		}
        		else {
        			eWrapper().error( ex);
        		}
        		
        		parent().eDisconnect();
        	//}
        } 
        
        m_signal.issueSignal();
    }

	public boolean putMessageToQueue() throws IOException {
		EMessage msg = readSingleMessage();
		
		if (msg == null) {
			S.out( "Error: read null msg");  // this is bad and we never recover; I saw this happen
			return false;					// when TWS was sitting at "This is not a brokerage account" message
		}

		synchronized(m_msgQueue) {
			m_msgQueue.addFirst(msg);
		}
		
		m_signal.issueSignal();
		
		return true;
	}   

	protected EMessage getMsg() {
    	synchronized (m_msgQueue) {
    		return m_msgQueue.isEmpty() ? null : m_msgQueue.removeLast();
		}
    }
	
    static final int MAX_MSG_LENGTH = 0xffffff;

	private static class InvalidMessageLengthException extends IOException {
		private static final long serialVersionUID = 1L;

		InvalidMessageLengthException(String message) {
			super(message);
		}
    }
    
    public void processMsgs() throws IOException {
    	EMessage msg = getMsg();
    	
    	while (msg != null && m_processMsgsDecoder.processMsg(msg) > 0) {
    		msg = getMsg();
    	}
    }

	private EMessage readSingleMessage() throws IOException {
		// read message size
		int msgSize = 0;
		try {
			msgSize = m_clientSocket.readInt();
		} 
		catch (Exception ex) {
			ex.printStackTrace();
			if (ex instanceof IOException) {  // probably we should always disconnect, for any exception type
				parent().connectionError();
				parent().eDisconnect();
				S.out("Disconnecting");
			}
			S.out( "Returning null from exception but not disconnecting?"); 
			return null;
		}
		
		if (msgSize > MAX_MSG_LENGTH) {
			throw new InvalidMessageLengthException("message is too long: "
					+ msgSize);
		}
		
		byte[] buf = new byte[msgSize];
		
		int offset = 0;
		
		while (offset < msgSize) {
			offset += m_clientSocket.read(buf, offset, msgSize - offset);
		}
					
		return new EMessage(buf, buf.length);
	}

	protected int appendIBuf() throws IOException {
		return m_clientSocket.read(m_iBuf, m_iBufLen, m_iBuf.length - m_iBufLen);
	}   
}
