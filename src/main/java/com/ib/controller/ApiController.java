/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.function.Consumer;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDescription;
import com.ib.client.ContractDetails;
import com.ib.client.Decimal;
import com.ib.client.DeltaNeutralContract;
import com.ib.client.DepthMktDataDescription;
import com.ib.client.EClientErrors;
import com.ib.client.EJavaSignal;
import com.ib.client.EReader;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.ExecutionFilter;
import com.ib.client.FamilyCode;
import com.ib.client.HistogramEntry;
import com.ib.client.HistoricalSession;
import com.ib.client.HistoricalTick;
import com.ib.client.HistoricalTickBidAsk;
import com.ib.client.HistoricalTickLast;
import com.ib.client.MarketDataType;
import com.ib.client.NewsProvider;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.PriceIncrement;
import com.ib.client.ScannerSubscription;
import com.ib.client.SoftDollarTier;
import com.ib.client.TagValue;
import com.ib.client.TickAttrib;
import com.ib.client.TickAttribBidAsk;
import com.ib.client.TickAttribLast;
import com.ib.client.TickType;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DeepSide;
import com.ib.client.Types.DeepType;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.ExerciseType;
import com.ib.client.Types.FADataType;
import com.ib.client.Types.FundamentalType;
import com.ib.client.Types.NewsType;
import com.ib.client.Types.WhatToShow;
import com.ib.controller.ApiConnection.ILogger;

import common.Util;
import tw.util.S;

public class ApiController implements EWrapper {
	public static record ApiParams( String host, int port, int clientId) {}

	private ApiConnection m_client;
	private final ILogger m_outLogger;
	private final ILogger m_inLogger;
	private int m_reqId = -99999;	// used for all requests except orders; designed not to conflict with m_orderId
	private int m_orderId;

	private final IConnectionHandler m_connectionHandler;
	private ITradeReportHandler m_tradeReportHandler;
    private ICompletedOrdersHandler m_completedOrdersHandler;
	private IAdvisorHandler m_advisorHandler;
	private IScannerHandler m_scannerHandler;
	private ITimeHandler m_timeHandler;
	private IUserInfoHandler m_userInfoHandler;
	private final Map<Integer,IInternalHandler> m_contractDetailsMap = new HashMap<>();
	private final Map<Integer,IOptHandler> m_optionCompMap = new HashMap<>();
	private final Map<Integer,IEfpHandler> m_efpMap = new HashMap<>();
	private final Map<Integer,ITopMktDataHandler> m_topMktDataMap = new HashMap<>();
	private final Map<Integer,IDeepMktDataHandler> m_deepMktDataMap = new HashMap<>();
	private final Map<Integer, IScannerHandler> m_scannerMap = new HashMap<>();
	private final Map<Integer, IRealTimeBarHandler> m_realTimeBarMap = new HashMap<>();
	private final Map<Integer, IHistoricalDataHandler> m_historicalDataMap = new HashMap<>();
	private final Map<Integer, IHeadTimestampHandler> m_headTimestampMap = new HashMap<>();
	private final Map<Integer, IHistogramDataHandler> m_histogramDataMap = new HashMap<>();
	private final Map<Integer, IFundamentalsHandler> m_fundMap = new HashMap<>();
	private final Map<Integer, IOrderHandler> m_orderHandlers = new HashMap<>();
    private final Map<Integer, IOrderCancelHandler> m_orderCancelHandlers = new HashMap<>();
	private final Map<Integer,IAccountSummaryHandler> m_acctSummaryHandlers = new HashMap<>();
	private final Map<Integer,IMarketValueSummaryHandler> m_mktValSummaryHandlers = new HashMap<>();
	private final Set<IPositionHandler> m_positionHandlers = new ConcurrentHashSet<>();
	private final Set<IAccountHandler> m_accountHandlers = new ConcurrentHashSet<>();
	private final Set<ILiveOrderHandler> m_liveOrderHandlers = new ConcurrentHashSet<>();
	private final Map<Integer, IPositionMultiHandler> m_positionMultiMap = new HashMap<>();
	private final Map<Integer, IAccountUpdateMultiHandler> m_accountUpdateMultiMap = new HashMap<>();
	private final Map<Integer, ISecDefOptParamsReqHandler> m_secDefOptParamsReqMap = new HashMap<>();
	private final Map<Integer, ISoftDollarTiersReqHandler> m_softDollarTiersReqMap = new HashMap<>();
	private final Set<IFamilyCodesHandler> m_familyCodesHandlers = new ConcurrentHashSet<>();
	private final Map<Integer, ISymbolSamplesHandler> m_symbolSamplesHandlerMap = new HashMap<>();
	private final Set<IMktDepthExchangesHandler> m_mktDepthExchangesHandlers = new ConcurrentHashSet<>();
	private final Map<Integer, ITickNewsHandler> m_tickNewsHandlerMap = new HashMap<>();
	private final Map<Integer, ISmartComponentsHandler> m_smartComponentsHandler = new HashMap<>();
	private final Set<INewsProvidersHandler> m_newsProvidersHandlers = new ConcurrentHashSet<>();
	private final Map<Integer, INewsArticleHandler> m_newsArticleHandlerMap = new HashMap<>();
	private final Map<Integer, IHistoricalNewsHandler> m_historicalNewsHandlerMap = new HashMap<>();
	private final Set<IMarketRuleHandler> m_marketRuleHandlers = new ConcurrentHashSet<>();
    private final Map<Integer, IPnLHandler> m_pnlMap = new HashMap<>();
    private final Map<Integer, IPnLSingleHandler> m_pnlSingleMap = new HashMap<>();
    private final Map<Integer, IHistoricalTickHandler> m_historicalTicksMap = new HashMap<>();
    private final Map<Integer, ITickByTickDataHandler> m_tickByTickDataMap = new HashMap<>();
    private final Map<Integer, IWshMetaDataHandler> m_wshMetaDataMap = new HashMap<>();
    private final Map<Integer, IWshEventDataHandler> m_wshEventDataMap = new HashMap<>();
    private final Map<Integer, IHistoricalScheduleHandler> m_historicalScheduleMap = new HashMap<>();

	public ApiConnection client() { return m_client; }

	// ---------------------------------------- Constructor and Connection handling ----------------------------------------
	public interface IConnectionHandler {
		void onRecNextValidId(int id);
		void onConnected();
		void onDisconnected();
		void accountList(List<String> list);
		void error(Exception e);
		void message(int id, int errorCode, String errorMsg, String advancedOrderRejectJson);
		void show(String string);
	}

	public ApiController( IConnectionHandler handler) {
		this(handler, null, null);
	}

	public ApiController( IConnectionHandler handler, ILogger inLogger, ILogger outLogger) {
		m_connectionHandler = handler;
		m_client = new ApiConnection( this, inLogger, outLogger);
		m_inLogger = inLogger;
		m_outLogger = outLogger;
	}
	
	private void startMsgProcessingThread() {
		final EJavaSignal signal = new EJavaSignal();		
		final EReader reader = new EReader(client(), signal, false);
		
		reader.start();
		
		common.Util.execute( "API-P", () -> {
            while (client().isConnected()) {
                signal.waitForSignal();
                try {
                    reader.processMsgs();
                } catch (IOException e) {
                    error(e);
                }
            }
        });
	}

	/** @deprecated */
	public void connect(String host, int port, int clientId, String connectionOpts) {
		connect( new ApiParams( host, port, clientId), connectionOpts);
	}

	public boolean connect( ApiParams params, String connectionOpts ) {
		if (m_client.eConnect(params.host(), params.port(), params.clientId(), false) ) {
			startMsgProcessingThread();  // seems wrong, we start thread even if already connected
	        sendEOM();
	        return true;
		}
		return false;
    }

	/** Called when we receive the server version. */
	@Override public void onConnected() {
		m_connectionHandler.onConnected();
	}

	public void disconnect() {
		if (!checkConnection())
			return;

		m_client.eDisconnect();
		//m_connectionHandler.onDisconnected();
		sendEOM();
	}

	@Override public void managedAccounts(String accounts) {
		List<String> list = new ArrayList<>();
		for( StringTokenizer st = new StringTokenizer( accounts, ","); st.hasMoreTokens(); ) {
			list.add( st.nextToken() );
		}
		m_connectionHandler.accountList( list);
		recEOM();
	}

	/** Remove this. It doesn't always get called, namely if TWS is not running when we first connect. */
	@Override public void nextValidId(int orderId) {
		m_orderId = orderId;
		m_reqId = -999990; // let order id's not collide with other request id's
		if (m_connectionHandler != null) {
			m_connectionHandler.onRecNextValidId(orderId);
		}
		recEOM();
	}

	@Override public void error(Exception e) {
		m_connectionHandler.error( e);
	}

	@Override public void error(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
		// Order Event Warning:Attribute 'Outside Regular Trading Hours' is ignored based on the order type and destination. PlaceOrder is now being processed.
		if (errorCode == 2109) {
			// useless msg; to avoid it, don't set the flag during liquid hours. pas
			return;
		}
		
		
		IOrderHandler handler = m_orderHandlers.get( id);
		if (handler != null) {
			handler.onRecOrderError( errorCode, errorMsg);  // once from here @*&   (fixed)
		}

        IOrderCancelHandler orderCancelHandler = m_orderCancelHandlers.get( id);
        if (orderCancelHandler != null) {
            orderCancelHandler.handle( errorCode, errorMsg);
        }

		for (ILiveOrderHandler liveHandler : m_liveOrderHandlers) {
			liveHandler.handle( id, errorCode, errorMsg);
		}

		// "no sec def found" response?
		if (errorCode == 200) {
			IInternalHandler hand = m_contractDetailsMap.remove( id);
			if (hand != null) {
				hand.contractDetailsEnd(); // once from here @*&  (fixed)
			}
		}

		// you could make an argument that you should not call this if any of the other handlers
		// have been called
		m_connectionHandler.message( id, errorCode, errorMsg, advancedOrderRejectJson);
		
		recEOM();
	}

	@Override public void connectionClosed() {
		m_connectionHandler.onDisconnected();
	}


	// ---------------------------------------- Account and portfolio updates ----------------------------------------
	public interface IAccountHandler {
		void accountValue(String account, String key, String value, String currency);
		void accountTime(String timeStamp);
		void accountDownloadEnd(String account);
		void updatePortfolio(Position position);
	}

    public void reqAccountUpdates(boolean subscribe, String acctCode, IAccountHandler handler) {
		if (!checkConnection())
			return;

		m_accountHandlers.add( handler);
    	m_client.reqAccountUpdates(subscribe, acctCode);
		sendEOM();
    }

	@Override public void updateAccountValue(String tag, String value, String currency, String account) {
		if ("Currency".equals(tag)) { // ignore this, it is useless
			return;
		}

		for( IAccountHandler handler : m_accountHandlers) {
			handler.accountValue( account, tag, value, currency);
		}
		recEOM();
	}

	@Override public void updateAccountTime(String timeStamp) {
		for( IAccountHandler handler : m_accountHandlers) {
			handler.accountTime( timeStamp);
		}
		recEOM();
	}

	@Override public void accountDownloadEnd(String account) {
		for( IAccountHandler handler : m_accountHandlers) {
			handler.accountDownloadEnd( account);
		}
		recEOM();
	}

	@Override public void updatePortfolio(Contract contract, Decimal positionIn, double marketPrice, double marketValue, double averageCost, double unrealizedPNL, double realizedPNL, String account) {
		contract.exchange( contract.primaryExch());

		Position position = new Position( contract, account, positionIn, marketPrice, marketValue, averageCost, unrealizedPNL, realizedPNL);
		for( IAccountHandler handler : m_accountHandlers) {
			handler.updatePortfolio( position);
		}
		recEOM();
	}

	// ---------------------------------------- Account Summary handling ----------------------------------------
	public interface IAccountSummaryHandler {
		void accountSummary( String account, AccountSummaryTag tag, String value, String currency);
		void accountSummaryEnd();
	}

	public interface IMarketValueSummaryHandler {
		void marketValueSummary( String account, MarketValueTag tag, String value, String currency);
		void marketValueSummaryEnd();
	}

	/** @param group pass "All" to get data for all accounts */
	public void reqAccountSummary(String group, AccountSummaryTag[] tags, IAccountSummaryHandler handler) {
		if (!checkConnection())
			return;
		
		StringBuilder sb = new StringBuilder();
		for (AccountSummaryTag tag : tags) {
			if (sb.length() > 0) {
				sb.append( ',');
			}
			sb.append( tag);
		}

		int reqId = m_reqId++;
		m_acctSummaryHandlers.put( reqId, handler);
		m_client.reqAccountSummary( reqId, group, sb.toString() );
		sendEOM();
	}

	/** Return true if we have received server version */
	public boolean isConnected() {
		return m_client.isConnected();
	}

	public void cancelAccountSummary(IAccountSummaryHandler handler) {
		if (!checkConnection())
			return;
		
		Integer reqId = getAndRemoveKey( m_acctSummaryHandlers, handler);
		if (reqId != null) {
			m_client.cancelAccountSummary( reqId);
			sendEOM();
		}
	}

	public void reqMarketValueSummary(String group, IMarketValueSummaryHandler handler) {
		if (!checkConnection())
			return;

		int reqId = m_reqId++;
		m_mktValSummaryHandlers.put( reqId, handler);
		m_client.reqAccountSummary( reqId, group, "$LEDGER");
		sendEOM();
	}

	public void cancelMarketValueSummary(IMarketValueSummaryHandler handler) {
		if (!checkConnection())
			return;

		Integer reqId = getAndRemoveKey( m_mktValSummaryHandlers, handler);
		if (reqId != null) {
			m_client.cancelAccountSummary( reqId);
			sendEOM();
		}
	}

	@Override public void accountSummary( int reqId, String account, String tag, String value, String currency) {
		if ("Currency".equals(tag)) { // ignore this, it is useless
			return;
		}

		IAccountSummaryHandler handler = m_acctSummaryHandlers.get( reqId);
		if (handler != null) {
			handler.accountSummary(account, AccountSummaryTag.valueOf( tag), value, currency);
		}

		IMarketValueSummaryHandler handler2 = m_mktValSummaryHandlers.get( reqId);
		if (handler2 != null) {
			handler2.marketValueSummary(account, MarketValueTag.valueOf( tag), value, currency);
		}

		recEOM();
	}

	@Override public void accountSummaryEnd( int reqId) {
		IAccountSummaryHandler handler = m_acctSummaryHandlers.get( reqId);
		if (handler != null) {
			handler.accountSummaryEnd();
		}

		IMarketValueSummaryHandler handler2 = m_mktValSummaryHandlers.get( reqId);
		if (handler2 != null) {
			handler2.marketValueSummaryEnd();
		}

		recEOM();
	}

	// ---------------------------------------- Position handling ----------------------------------------
	public interface IPositionHandler {
		void position( String account, Contract contract, Decimal pos, double avgCost);
		void positionEnd();
	}

	public void reqPositions( IPositionHandler handler) {
		if (!checkConnection())
			return;

		m_positionHandlers.add( handler);
		m_client.reqPositions();
		sendEOM();
	}

	public void cancelPositions( IPositionHandler handler) {
		if (!checkConnection())
			return;

		m_positionHandlers.remove( handler);
		m_client.cancelPositions();
		sendEOM();
	}

	@Override public void position(String account, Contract contract, Decimal pos, double avgCost) {
		for (IPositionHandler handler : m_positionHandlers) {
			handler.position( account, contract, pos, avgCost);
		}
		recEOM();
	}

	@Override public void positionEnd() {
		for (IPositionHandler handler : m_positionHandlers) {
			handler.positionEnd();
		}
		recEOM();
	}

	// ---------------------------------------- Contract Details ----------------------------------------
	public interface IContractDetailsHandler {
		void contractDetails(List<ContractDetails> list);
	}

	/** Warning: the callback is called TWICE if the contract is not found  (search for "@*&")
	 *  It's a bug
	 *   
	 *  Warning: I THINK what I am seeing is that if you query on a directed exchange by symbol,
	 *  it still returns SMART; you must query by conid+exchange to get the directed exchange
	 *  
	 *  */
	public void reqContractDetails( Contract contract, final IContractDetailsHandler processor) {
		if (!checkConnection())
			return;

		final List<ContractDetails> list = new ArrayList<>();
		internalReqContractDetails( contract, new IInternalHandler() {
			@Override public void contractDetails(ContractDetails data) {
				list.add( data);
			}
			@Override public void contractDetailsEnd() {
				processor.contractDetails( list);
			}
		});
		sendEOM();
	}

	private interface IInternalHandler {
		void contractDetails(ContractDetails data);
		void contractDetailsEnd();
	}

	private void internalReqContractDetails( Contract contract, final IInternalHandler processor) {
		int reqId = m_reqId++;
		
		//S.out( "Requesting contract details  %s  %s", reqId, contract.conid() );
		
		// this is bad, we're mapping the same reqId to two separate handlers
		// and they will BOTH get called if the contract is not found
		
		m_contractDetailsMap.put( reqId, processor);
		
//		m_orderHandlers.put(reqId, new IOrderHandler() { 
//			public void handle(int errorCode, String errorMsg) {
//				processor.contractDetailsEnd();
//			}
//
//			@Override
//			public void orderState(OrderState orderState) {
//				// TODO Auto-generated method stub
//				
//			}
//	
//			@Override
//			public void orderStatus(OrderStatus status, Decimal filled,
//					Decimal remaining, double avgFillPrice, int permId,
//					int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
//				// TODO Auto-generated method stub
//				
//			}
//		});
		
		m_client.reqContractDetails(reqId, contract);
		sendEOM();
	}

	@Override public void contractDetails(int reqId, ContractDetails contractDetails) {
		IInternalHandler handler = m_contractDetailsMap.get( reqId);
		if (handler != null) {
			handler.contractDetails(contractDetails);
		}
		else {
			show( "Error: no contract details handler for reqId " + reqId);
		}
		recEOM();
	}

	@Override public void bondContractDetails(int reqId, ContractDetails contractDetails) {
		IInternalHandler handler = m_contractDetailsMap.get( reqId);
		if (handler != null) {
			handler.contractDetails(contractDetails);
		}
		else {
			show( "Error: no bond contract details handler for reqId " + reqId);
		}
		recEOM();
	}

	@Override public void contractDetailsEnd(int reqId) {
		IInternalHandler handler = m_contractDetailsMap.remove( reqId);
		if (handler != null) {
			handler.contractDetailsEnd();
		}
		else {
			show( "Error: no contract details handler for reqId " + reqId);
		}
		recEOM();
	}

	// ---------------------------------------- Top Market Data handling ----------------------------------------
	public interface ITopMktDataHandler {
		void tickPrice(TickType tickType, double price, TickAttrib attribs);
		void tickSize(TickType tickType, Decimal size);
		void tickString(TickType tickType, String value);
		void tickSnapshotEnd();
		void marketDataType(int marketDataType);
		void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions);
	}

	public interface IEfpHandler extends ITopMktDataHandler {
		void tickEFP(int tickType, double basisPoints, String formattedBasisPoints, double impliedFuture, int holdDays, String futureLastTradeDate, double dividendImpact, double dividendsToLastTradeDate);
	}

	public interface IOptHandler extends ITopMktDataHandler {
		void tickOptionComputation( TickType tickType, int tickAttrib, double impliedVol, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice);
	}

	public static class TopMktDataAdapter implements ITopMktDataHandler {
		@Override public void tickPrice(TickType tickType, double price, TickAttrib attribs) {
		}
		@Override public void tickSize(TickType tickType, Decimal size) {
		}
		@Override public void tickString(TickType tickType, String value) {
		}
		@Override public void tickSnapshotEnd() {
		}
		@Override public void marketDataType(int marketDataType) {
		}
		@Override public void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions) {
		}
	}

	public void dump() {
		S.out( "-----Dump: Api Controller Top Market Data-----");
		for (Integer key : m_topMktDataMap.keySet() ) {
			S.out( "reqid=%s", key); 
		}
	}
	
    public int reqTopMktData(Contract contract, String genericTickList, boolean snapshot, boolean regulatorySnapshot, ITopMktDataHandler handler) {
		if (!checkConnection())
			return 0;

    	int reqId = m_reqId++;
		
    	S.out( "Requesting mkt data  conid=%s  symbol=%s  exch=%s  reqId=%s", 
				contract.conid(), contract.symbol(), contract.exchange(), reqId);
		
    	m_topMktDataMap.put( reqId, handler);
    	m_client.reqMktData( reqId, contract, genericTickList, snapshot, regulatorySnapshot, Collections.emptyList() );
		sendEOM();
		return reqId;
    }

    public void reqOptionMktData(Contract contract, String genericTickList, boolean snapshot, boolean regulatorySnapshot, IOptHandler handler) {
		if (!checkConnection())
			return;

    	int reqId = m_reqId++;
    	m_topMktDataMap.put( reqId, handler);
    	m_optionCompMap.put( reqId, handler);
    	m_client.reqMktData( reqId, contract, genericTickList, snapshot, regulatorySnapshot, Collections.emptyList() );
		sendEOM();
    }

    public void reqEfpMktData(Contract contract, String genericTickList, boolean snapshot, boolean regulatorySnapshot, IEfpHandler handler) {
		if (!checkConnection())
			return;

    	int reqId = m_reqId++;
    	m_topMktDataMap.put( reqId, handler);
    	m_efpMap.put( reqId, handler);
    	m_client.reqMktData( reqId, contract, genericTickList, snapshot, regulatorySnapshot, Collections.emptyList() );
		sendEOM();
    }

    /** added by me. pas */
    public void cancelAllTopMktData() {
    	for (Integer reqId : m_topMktDataMap.keySet() ) {
    		m_client.cancelMktData( reqId);
    	}
    	m_topMktDataMap.clear();
    }

    public void cancelTopMktData( ITopMktDataHandler handler) {
		if (!checkConnection())
			return;

		Integer reqId = getAndRemoveKey( m_topMktDataMap, handler);
    	if (reqId != null) {
    		m_client.cancelMktData( reqId);
    	}
    	else {
    		show( "Error: could not cancel top market data");
    	}
		sendEOM();
    }

    public void cancelOptionMktData( IOptHandler handler) {
    	cancelTopMktData( handler);
    	getAndRemoveKey( m_optionCompMap, handler);
    }

    public void cancelEfpMktData( IEfpHandler handler) {
    	cancelTopMktData( handler);
    	getAndRemoveKey( m_efpMap, handler);
    }

    /** see here: https://interactivebrokers.github.io/tws-api/market_data_type.html
     *  and here: MarketDataType.java
     * 
     * @param mktDataType 3=delayed    */
	public void reqMktDataType( int mktDataType) {
		if (!checkConnection())
			return;

		m_client.reqMarketDataType( mktDataType);
		sendEOM();
		switch(mktDataType){
			case MarketDataType.REALTIME:
				show( "Frozen, Delayed and Delayed-Frozen market data types are disabled");
				break;
			case MarketDataType.FROZEN:
				show( "Frozen market data type is enabled");
				break;
			case MarketDataType.DELAYED:
				show( "Delayed market data type is enabled, Delayed-Frozen market data type is disabled");
				break;
			case MarketDataType.DELAYED_FROZEN:
				show( "Delayed and Delayed-Frozen market data types are enabled");
				break;
			default:
				show( "Unknown market data type");
				break;
		}
	}

	@Override public void tickPrice(int reqId, int tickType, double price, TickAttrib attribs) {
		ITopMktDataHandler handler = m_topMktDataMap.get( reqId);
		if (handler != null) {
			handler.tickPrice( TickType.get( tickType), price, attribs);
		}
		recEOM();
	}

	@Override public void tickGeneric(int reqId, int tickType, double value) {
		ITopMktDataHandler handler = m_topMktDataMap.get( reqId);
		if (handler != null) {
			handler.tickPrice( TickType.get( tickType), value, new TickAttrib());
		}
		recEOM();
	}

//	@Override public void tickSize(int reqId, int tickType, Decimal size) {
//		ITopMktDataHandler handler = m_topMktDataMap.get( reqId);
//		if (handler != null) {
//			handler.tickSize( TickType.get( tickType), size);
//		}
//		recEOM();
//	}

	@Override public void tickString(int reqId, int tickType, String value) {
		ITopMktDataHandler handler = m_topMktDataMap.get( reqId);
		if (handler != null) {
			handler.tickString( TickType.get( tickType), value);
		}
		recEOM();
	}

	@Override public void tickEFP(int reqId, int tickType, double basisPoints, String formattedBasisPoints, double impliedFuture, int holdDays, String futureLastTradeDate, double dividendImpact, double dividendsToLastTradeDate) {
		IEfpHandler handler = m_efpMap.get( reqId);
		if (handler != null) {
			handler.tickEFP( tickType, basisPoints, formattedBasisPoints, impliedFuture, holdDays, futureLastTradeDate, dividendImpact, dividendsToLastTradeDate);
		}
		recEOM();
	}

	@Override public void tickSnapshotEnd(int reqId) {
		ITopMktDataHandler handler = m_topMktDataMap.get( reqId);
		if (handler != null) {
			handler.tickSnapshotEnd();
		}
		recEOM();
	}

	@Override public void marketDataType(int reqId, int marketDataType) {
		ITopMktDataHandler handler = m_topMktDataMap.get( reqId);
		if (handler != null) {
			handler.marketDataType( marketDataType );
		}
		recEOM();
	}


	// ---------------------------------------- Deep Market Data handling ----------------------------------------
	public interface IDeepMktDataHandler {
		void updateMktDepth(int position, String marketMaker, DeepType operation, DeepSide side, double price, Decimal size);
	}

    public void reqDeepMktData( Contract contract, int numRows, boolean isSmartDepth, IDeepMktDataHandler handler) {
		if (!checkConnection())
			return;

    	int reqId = m_reqId++;
    	m_deepMktDataMap.put( reqId, handler);
    	List<TagValue> mktDepthOptions = new ArrayList<>();
    	m_client.reqMktDepth( reqId, contract, numRows, isSmartDepth, mktDepthOptions);
		sendEOM();
    }

    public void cancelDeepMktData( boolean isSmartDepth, IDeepMktDataHandler handler) {
		if (!checkConnection())
			return;

    	Integer reqId = getAndRemoveKey( m_deepMktDataMap, handler);
    	if (reqId != null) {
    		m_client.cancelMktDepth( reqId, isSmartDepth);
    		sendEOM();
    	}
    }

	@Override public void updateMktDepth(int reqId, int position, int operation, int side, double price, Decimal size) {
		IDeepMktDataHandler handler = m_deepMktDataMap.get( reqId);
		if (handler != null) {
			handler.updateMktDepth( position, null, DeepType.get( operation), DeepSide.get( side), price, size);
		}
		recEOM();
	}

	@Override public void updateMktDepthL2(int reqId, int position, String marketMaker, int operation, int side, double price, Decimal size, boolean isSmartDepth) {
		IDeepMktDataHandler handler = m_deepMktDataMap.get( reqId);
		if (handler != null) {
			handler.updateMktDepth( position, marketMaker, DeepType.get( operation), DeepSide.get( side), price, size);
		}
		recEOM();
	}

	// ---------------------------------------- Option computations ----------------------------------------
	public void reqOptionVolatility(Contract c, double optPrice, double underPrice, IOptHandler handler) {
		if (!checkConnection())
			return;

		int reqId = m_reqId++;
		m_optionCompMap.put( reqId, handler);
		m_client.calculateImpliedVolatility( reqId, c, optPrice, underPrice, null);
		sendEOM();
	}

	public void reqOptionComputation( Contract c, double vol, double underPrice, IOptHandler handler) {
		if (!checkConnection())
			return;

		int reqId = m_reqId++;
		m_optionCompMap.put( reqId, handler);
		m_client.calculateOptionPrice(reqId, c, vol, underPrice, null);
		sendEOM();
	}

	void cancelOptionComp( IOptHandler handler) {
		if (!checkConnection())
			return;

		Integer reqId = getAndRemoveKey( m_optionCompMap, handler);
		if (reqId != null) {
			m_client.cancelCalculateOptionPrice( reqId);
			sendEOM();
		}
	}

	@Override public void tickOptionComputation(int reqId, int tickType, int tickAttrib, double impliedVol, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice) {
		IOptHandler handler = m_optionCompMap.get( reqId);
		if (handler != null) {
			handler.tickOptionComputation( TickType.get( tickType), tickAttrib, impliedVol, delta, optPrice, pvDividend, gamma, vega, theta, undPrice);
		}
		else {
			System.out.println( String.format( "not handled %s %s %s %s %s %s %s %s %s %s", tickType, tickAttrib, impliedVol, delta, optPrice, pvDividend, gamma, vega, theta, undPrice) );
		}
		recEOM();
	}


	// ---------------------------------------- Trade reports ----------------------------------------
	public interface ITradeReportHandler {
		void tradeReport(String tradeKey, Contract contract, Execution execution);
		void tradeReportEnd();
		void commissionReport(String tradeKey, CommissionReport commissionReport);
	}

    public void reqExecutions( ExecutionFilter filter, ITradeReportHandler handler) {
		if (!checkConnection())
			return;

    	m_tradeReportHandler = handler;
    	m_client.reqExecutions( m_reqId++, filter);
		sendEOM();
    }

    public void handleExecutions(ITradeReportHandler handler) {
    	m_tradeReportHandler = handler;
    }

	@Override public void execDetails(int reqId, Contract contract, Execution execution) {
		if (m_tradeReportHandler != null) {
			int i = execution.execId().lastIndexOf( '.');
			String tradeKey = execution.execId().substring( 0, i);
			m_tradeReportHandler.tradeReport( tradeKey, contract, execution);
		}
		recEOM();
	}

	@Override public void execDetailsEnd(int reqId) {
		if (m_tradeReportHandler != null) {
			m_tradeReportHandler.tradeReportEnd();
		}
		recEOM();
	}

	@Override public void commissionReport(CommissionReport commissionReport) {
		if (m_tradeReportHandler != null) {
			int i = commissionReport.execId().lastIndexOf( '.');
			String tradeKey = commissionReport.execId().substring( 0, i);
			m_tradeReportHandler.commissionReport( tradeKey, commissionReport);
		}
		recEOM();
	}

	// ---------------------------------------- Advisor info ----------------------------------------
	public interface IAdvisorHandler {
		void groups(List<Group> groups);
		void profiles(List<Profile> profiles);
		void aliases(List<Alias> aliases);
		void updateGroupsEnd(String text);
		void updateProfilesEnd(String text);
	}
	
	private static final int REPLACE_FA_GROUPS_REQ_ID = 0;
	private static final int REPLACE_FA_PROFILES_REQ_ID = 1;

	public void reqAdvisorData( FADataType type, IAdvisorHandler handler) {
		if (!checkConnection())
			return;

		m_advisorHandler = handler;
		m_client.requestFA( type.ordinal() );
		sendEOM();
	}

	public void updateGroups( List<Group> groups) {
		if (!checkConnection())
			return;

		m_client.replaceFA( REPLACE_FA_GROUPS_REQ_ID, FADataType.GROUPS.ordinal(), AdvisorUtil.getGroupsXml( groups) );
		sendEOM();
	}

	public void updateProfiles(List<Profile> profiles) {
		if (!checkConnection())
			return;

		m_client.replaceFA( REPLACE_FA_PROFILES_REQ_ID, FADataType.PROFILES.ordinal(), AdvisorUtil.getProfilesXml( profiles) );
		sendEOM();
	}

	@Override public final void receiveFA(int faDataType, String xml) {
		if (m_advisorHandler == null) {
			return;
		}

		FADataType type = FADataType.get( faDataType);

		switch( type) {
			case GROUPS:
				List<Group> groups = AdvisorUtil.getGroups( xml);
				m_advisorHandler.groups(groups);
				break;

			case PROFILES:
				List<Profile> profiles = AdvisorUtil.getProfiles( xml);
				m_advisorHandler.profiles(profiles);
				break;

			case ALIASES:
				List<Alias> aliases = AdvisorUtil.getAliases( xml);
				m_advisorHandler.aliases(aliases);
				break;

			default:
				break;
		}
		recEOM();
	}
	
	@Override public final void replaceFAEnd(int reqId, String text) {
		switch(reqId) {
		case REPLACE_FA_GROUPS_REQ_ID:
			m_advisorHandler.updateGroupsEnd(text);	
			break;
		case REPLACE_FA_PROFILES_REQ_ID:
			m_advisorHandler.updateProfilesEnd(text);	
			break;
		default:
			break;
		}
		recEOM();
	}

	// ---------------------------------------- Trading and Option Exercise ----------------------------------------
	/** This interface is for receiving events for a specific order placed from the API.
	 *  Compare to ILiveOrderHandler.
	 *  Note that both order and orderState contains the updated trailing stop price */
	public interface IOrderHandler {
		void orderState(OrderState orderState);
		void onRecOrderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice);
		void onRecOrderError(int errorCode, String errorMsg);
	}

    public interface IOrderCancelHandler {
        void orderStatus(String orderStatus);
        void handle(int errorCode, String errorMsg);
    }

	/** This method must be synchronized because we cannot allow orders to get placed out of order
	 * or you receive dup order id error. This also assumes there is only one ApiController.
	 * It also means that the order id is not available until after the order is placed */ 
	public synchronized void placeOrder(Contract contract, final Order order, final IOrderHandler handler) throws Exception {
		if (!checkConnection())
			return;

		order.orderId( m_orderId++);  // must be in the same sync block as placeOrder()
		order.permId(0);
		order.status(OrderStatus.Unknown);
		
		Util.require( order.roundedQty() > 0, "Set rounded quantity");
		
		if (handler != null) {
			m_orderHandlers.put( order.orderId(), handler);
		}

		m_client.placeOrder( contract, order);    // must be in the same sync block as setting order id
		sendEOM();
	}

	public synchronized void modifyOrder(Contract contract, final Order order) throws Exception {
		if (!checkConnection())
			return;
		m_client.placeOrder( contract, order);    // must be in the same sync block as setting order id
		sendEOM();
	}

	public void cancelOrder(int orderId, String manualOrderCancelTime, final IOrderCancelHandler orderCancelHandler) {
		if (!checkConnection())
			return;

        if (orderCancelHandler != null) {
            m_orderCancelHandlers.put( orderId, orderCancelHandler);
        }

        m_client.cancelOrder( orderId, manualOrderCancelTime);
		sendEOM();
	}

	public void cancelAllOrders() {
		if (!checkConnection())
			return;
		
		m_client.reqGlobalCancel();
		sendEOM();
	}

	public void exerciseOption( String account, Contract contract, ExerciseType type, int quantity, boolean override) {
		if (!checkConnection())
			return;

		m_client.exerciseOptions( m_reqId++, contract, type.ordinal(), quantity, account, override ? 1 : 0);
		sendEOM();
	}

	public void removeOrderHandler( IOrderHandler handler) {
		getAndRemoveKey(m_orderHandlers, handler);
	}

    public void removeOrderCancelHandler( IOrderCancelHandler orderCancelHandler) {
        getAndRemoveKey(m_orderCancelHandlers, orderCancelHandler);
    }

	// ---------------------------------------- Live order handling ----------------------------------------
	/** This interface is for downloading and receiving events for all live orders.
	 *  Compare to IOrderHandler. */
	public interface ILiveOrderHandler {
		void openOrder(Contract contract, Order order, OrderState orderState);
		void openOrderEnd();
		void orderStatus(int orderId, OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice);
		void handle(int orderId, int errorCode, String errorMsg);  // add permId?
	}

	public void reqLiveOrders( ILiveOrderHandler handler) {
		if (!checkConnection())
			return;

		m_liveOrderHandlers.add( handler);
		m_client.reqAllOpenOrders();
		sendEOM();
	}

	public void takeTwsOrders( ILiveOrderHandler handler) {
		if (!checkConnection())
			return;

		m_liveOrderHandlers.add( handler);
		m_client.reqOpenOrders();
		sendEOM();
	}

	public void takeFutureTwsOrders( ILiveOrderHandler handler) {
		if (!checkConnection())
			return;

		m_liveOrderHandlers.add( handler);
		m_client.reqAutoOpenOrders( true);
		sendEOM();
	}

	public void removeLiveOrderHandler(ILiveOrderHandler handler) {
		m_liveOrderHandlers.remove( handler);
	}

	/** note that both order and orderState contains the updated trailing stop price */
	@Override public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
		IOrderHandler handler = m_orderHandlers.get( orderId);
		if (handler != null) {
			handler.orderState(orderState);
		}

		if (!order.whatIf() ) {
			for (ILiveOrderHandler liveHandler : m_liveOrderHandlers) {
				liveHandler.openOrder( contract, order, orderState );
			}
		}
		recEOM();
	}

	@Override public void openOrderEnd() {
		for (ILiveOrderHandler handler : m_liveOrderHandlers) {
			handler.openOrderEnd();
		}
		recEOM();
	}

	@Override public void orderStatus(int orderId, String status, Decimal filled, Decimal remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
		IOrderHandler handler = m_orderHandlers.get( orderId);
		if (handler != null) {
			handler.onRecOrderStatus( OrderStatus.valueOf( status), filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld, mktCapPrice);
		}

        IOrderCancelHandler orderCancelHandler = m_orderCancelHandlers.get( orderId);
        if (orderCancelHandler != null) {
            orderCancelHandler.orderStatus(status);
        }

		for (ILiveOrderHandler liveOrderHandler : m_liveOrderHandlers) {
			liveOrderHandler.orderStatus(orderId, OrderStatus.valueOf( status), filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld, mktCapPrice);
		}
		recEOM();
	}


	// ---------------------------------------- Market Scanners ----------------------------------------
	public interface IScannerHandler {
		void scannerParameters(String xml);
		void scannerData( int rank, ContractDetails contractDetails, String legsStr);
		void scannerDataEnd();
	}

	public void reqScannerParameters( IScannerHandler handler) {
		if (!checkConnection())
			return;

		m_scannerHandler = handler;
		m_client.reqScannerParameters();
		sendEOM();
	}

	public void reqScannerSubscription(ScannerSubscription sub, List<TagValue> filterOptions, IScannerHandler handler) {
		if (!checkConnection())
			return;

		int reqId = m_reqId++;
		
		m_scannerMap.put(reqId, handler);
		m_client.reqScannerSubscription(reqId, sub, null, filterOptions);
		
		sendEOM();
	}

	public void cancelScannerSubscription( IScannerHandler handler) {
		if (!checkConnection())
			return;

		Integer reqId = getAndRemoveKey( m_scannerMap, handler);
		if (reqId != null) {
			m_client.cancelScannerSubscription( reqId);
			sendEOM();
		}
	}

	@Override public void scannerParameters(String xml) {
		m_scannerHandler.scannerParameters( xml);
		recEOM();
	}

	@Override public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark, String projection, String legsStr) {
		IScannerHandler handler = m_scannerMap.get( reqId);
		if (handler != null) {
			handler.scannerData( rank, contractDetails, legsStr);
		}
		recEOM();
	}

	@Override public void scannerDataEnd(int reqId) {
		IScannerHandler handler = m_scannerMap.get( reqId);
		if (handler != null) {
			handler.scannerDataEnd();
		}
		recEOM();
	}


	// ----------------------------------------- Historical data handling ----------------------------------------
	public interface IHistoricalDataHandler {
		void historicalData(Bar bar);
		void historicalDataEnd();
	}
	
	
    public void reqHistoricalData(Contract contract, Date endDate, int duration, DurationUnit durationUnit, BarSize barSize, WhatToShow whatToShow, boolean rthOnly, boolean keepUpToDate, Consumer<ArrayList<Bar>> handler) {
    	ArrayList<Bar> list = new ArrayList<>();
    	
    	reqHistoricalData( contract, endDate, duration, durationUnit, barSize, whatToShow, rthOnly, keepUpToDate, new IHistoricalDataHandler() {
			@Override public void historicalDataEnd() {
				S.out( "received end of bars");
				handler.accept( list);
			}
			
			@Override public void historicalData(Bar bar) {
				list.add( bar);
			}
		}); 
    }


	/** @param endDateTime format is YYYYMMDD HH:MM:SS [tm/zone]
	 *         date and time zone ARE OPTIONAL
	 *         e.g. 20250130 13:30:00 US/Eastern
	 *  @param duration is number of durationUnits */
    public void reqHistoricalData(Contract contract, Date endDate, int duration, DurationUnit durationUnit, BarSize barSize, WhatToShow whatToShow, boolean rthOnly, boolean keepUpToDate, IHistoricalDataHandler handler) {
		if (!checkConnection())
			return;
		
		String zoneName = "America/New_York";
		TimeZone zone = TimeZone.getTimeZone( zoneName);  // note: us/eastern is deprecated
		
		SimpleDateFormat fmt = new SimpleDateFormat( "yyyyMMdd HH:mm:ss");  // 24 hr clock
		fmt.setTimeZone( zone);

		String endDateTime = fmt.format( endDate) + " " + zoneName;
		
		S.out( "Requesting historical data conid=%s  symbol=%s  end=%s  duration=%s  unit=%s  barSize=%s  what=%s  rthOnly=%s  keepUpToDate=%s",
				contract.conid(), contract.symbol(), endDateTime, duration, durationUnit, barSize, whatToShow, rthOnly, keepUpToDate);

    	int reqId = m_reqId++;
    	m_historicalDataMap.put( reqId, handler);
    	String durationStr = duration + " " + durationUnit.toString().charAt( 0);
    	m_client.reqHistoricalData(reqId, contract, endDateTime, durationStr, barSize.apiString(), whatToShow.toString(), rthOnly ? 1 : 0, 2, keepUpToDate, Collections.emptyList());
		sendEOM();
    }

    public void cancelHistoricalData( IHistoricalDataHandler handler) {
		if (!checkConnection())
			return;

		Integer reqId = getAndRemoveKey( m_historicalDataMap, handler);
    	if (reqId != null) {
    		m_client.cancelHistoricalData( reqId);
    		sendEOM();
    	}
    }

	@Override public void historicalData(int reqId, com.ib.client.Bar bar) {
		IHistoricalDataHandler handler = m_historicalDataMap.get( reqId);
		if (handler != null) {
			if (bar.time().startsWith( "finished")) {
				handler.historicalDataEnd();
			}
			else {
				long longDate;
				if (bar.time().length() == 8) {
					int year = Integer.parseInt( bar.time().substring( 0, 4) );
					int month = Integer.parseInt( bar.time().substring( 4, 6) );
					int day = Integer.parseInt( bar.time().substring( 6) );
					longDate = new GregorianCalendar( year, month - 1, day).getTimeInMillis() / 1000;
				}
				else {
					longDate = Long.parseLong( bar.time());
				}
				Bar bar2 = new Bar( longDate, bar.high(), bar.low(), bar.open(), bar.close(), bar.wap(), bar.volume(), bar.count());
				handler.historicalData(bar2);
			}
		}
		recEOM();
	}


	//----------------------------------------- Real-time bars --------------------------------------
	public interface IRealTimeBarHandler {
		void realtimeBar(Bar bar); // time is in seconds since epoch
	}

    public void reqRealTimeBars(Contract contract, WhatToShow whatToShow, boolean rthOnly, IRealTimeBarHandler handler) {
		if (!checkConnection())
			return;

    	int reqId = m_reqId++;
    	m_realTimeBarMap.put( reqId, handler);
    	List<TagValue> realTimeBarsOptions = new ArrayList<>();
    	m_client.reqRealTimeBars(reqId, contract, 0, whatToShow.toString(), rthOnly, realTimeBarsOptions);
		sendEOM();
    }

    public void cancelRealtimeBars( IRealTimeBarHandler handler) {
		if (!checkConnection())
			return;

    	Integer reqId = getAndRemoveKey( m_realTimeBarMap, handler);
    	if (reqId != null) {
    		m_client.cancelRealTimeBars( reqId);
    		sendEOM();
    	}
    }

    @Override public void realtimeBar(int reqId, long time, double open, double high, double low, double close, Decimal volume, Decimal wap, int count) {
    	IRealTimeBarHandler handler = m_realTimeBarMap.get( reqId);
		if (handler != null) {
			Bar bar = new Bar( time, high, low, open, close, wap, volume, count);
			handler.realtimeBar( bar);
		}
		recEOM();
	}

    // ----------------------------------------- Fundamentals handling ----------------------------------------
	public interface IFundamentalsHandler {
		void fundamentals( String str);
	}

    public void reqFundamentals( Contract contract, FundamentalType reportType, IFundamentalsHandler handler) {
		if (!checkConnection())
			return;

    	int reqId = m_reqId++;
    	m_fundMap.put( reqId, handler);
    	m_client.reqFundamentalData( reqId, contract, reportType.getApiString(), null);
		sendEOM();
    }

    @Override public void fundamentalData(int reqId, String data) {
		IFundamentalsHandler handler = m_fundMap.get( reqId);
		if (handler != null) {
			handler.fundamentals( data);
		}
		recEOM();
	}

	// ---------------------------------------- Time handling ----------------------------------------
	public interface ITimeHandler {
		void currentTime(long time); // time in in seconds */
	}

	/** time comes in seconds */
	public void reqCurrentTime( ITimeHandler handler) {
		if (!checkConnection())
			return;

		m_timeHandler = handler;
		m_client.reqCurrentTime();
		sendEOM();
	}

	/** this should really throw an exception. pas */
	protected boolean checkConnection() {
		if (!isConnected()) {
			error(EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED.code(), EClientErrors.NOT_CONNECTED.msg(), null);
			return false;
		}
		
		return true;
	}

	@Override public void currentTime(long time) {
		m_timeHandler.currentTime(time);
		recEOM();
	}

	// ---------------------------------------- Bulletins handling ----------------------------------------
	public interface IBulletinHandler {
		void bulletin(int msgId, NewsType newsType, String message, String exchange);
	}

	public void reqBulletins( boolean allMessages, IBulletinHandler handler) {
	}

	public void cancelBulletins() {
		if (!checkConnection())
			return;

		m_client.cancelNewsBulletins();
	}

	@Override public void updateNewsBulletin(int msgId, int msgType, String message, String origExchange) {
	}

	// ---------------------------------------- Position Multi handling ----------------------------------------
	public interface IPositionMultiHandler {
		void positionMulti( String account, String modelCode, Contract contract, Decimal pos, double avgCost);
		void positionMultiEnd();
	}

	public void reqPositionsMulti( String account, String modelCode, IPositionMultiHandler handler) {
		if (!checkConnection())
			return;

		int reqId = m_reqId++;
		m_positionMultiMap.put( reqId, handler);
		m_client.reqPositionsMulti( reqId, account, modelCode);
		sendEOM();
	}

	public void cancelPositionsMulti( IPositionMultiHandler handler) {
		if (!checkConnection())
			return;

		Integer reqId = getAndRemoveKey( m_positionMultiMap, handler);
		if (reqId != null) {
			m_client.cancelPositionsMulti( reqId);
			sendEOM();
		}
	}

	@Override public void positionMulti( int reqId, String account, String modelCode, Contract contract, Decimal pos, double avgCost) {
		IPositionMultiHandler handler = m_positionMultiMap.get( reqId);
		if (handler != null) {
			handler.positionMulti( account, modelCode, contract, pos, avgCost);
		}
		recEOM();
	}

	@Override public void positionMultiEnd( int reqId) {
		IPositionMultiHandler handler = m_positionMultiMap.get( reqId);
		if (handler != null) {
			handler.positionMultiEnd();
		}
		recEOM();
	}
	
	// ---------------------------------------- Account Update Multi handling ----------------------------------------
	public interface IAccountUpdateMultiHandler {
		void accountUpdateMulti( String account, String modelCode, String key, String value, String currency);
		void accountUpdateMultiEnd();
	}

	public void reqAccountUpdatesMulti( String account, String modelCode, boolean ledgerAndNLV, IAccountUpdateMultiHandler handler) {
		if (!checkConnection())
			return;

		int reqId = m_reqId++;
		m_accountUpdateMultiMap.put( reqId, handler);
		m_client.reqAccountUpdatesMulti( reqId, account, modelCode, ledgerAndNLV);
		sendEOM();
	}

	public void cancelAccountUpdatesMulti( IAccountUpdateMultiHandler handler) {
		if (!checkConnection())
			return;

		Integer reqId = getAndRemoveKey( m_accountUpdateMultiMap, handler);
		if (reqId != null) {
			m_client.cancelAccountUpdatesMulti( reqId);
			sendEOM();
		}
	}

	@Override public void accountUpdateMulti( int reqId, String account, String modelCode, String key, String value, String currency) {
		IAccountUpdateMultiHandler handler = m_accountUpdateMultiMap.get( reqId);
		if (handler != null) {
			handler.accountUpdateMulti( account, modelCode, key, value, currency);
		}
		recEOM();
	}

	@Override public void accountUpdateMultiEnd( int reqId) {
		IAccountUpdateMultiHandler handler = m_accountUpdateMultiMap.get( reqId);
		if (handler != null) {
			handler.accountUpdateMultiEnd();
		}
		recEOM();
	}

	@Override public void verifyMessageAPI( String apiData) {}
	@Override public void verifyCompleted( boolean isSuccessful, String errorText) {}
	@Override public void verifyAndAuthMessageAPI( String apiData, String xyzChallenge) {}
	@Override public void verifyAndAuthCompleted( boolean isSuccessful, String errorText) {}
	@Override public void displayGroupList(int reqId, String groups) {}
	@Override public void displayGroupUpdated(int reqId, String contractInfo) {}

	// ---------------------------------------- other methods ----------------------------------------
	/** Not supported in ApiController. */
	@Override public void deltaNeutralValidation(int reqId, DeltaNeutralContract deltaNeutralContract) {
		show( "RECEIVED DN VALIDATION");
		recEOM();
	}

	protected void sendEOM() {
		if (m_outLogger != null) {
			m_outLogger.log( "\n");
		}
	}

	private void recEOM() {
		if (m_inLogger != null) {
			m_inLogger.log( "\n");
		}
	}

	public void show(String string) {
		m_connectionHandler.show( string);
	}

    private static <K,V> K getAndRemoveKey( Map<K,V> map, V value) {
    	for (Entry<K,V> entry : map.entrySet() ) {
    		if (entry.getValue() == value) {
    			map.remove( entry.getKey() );
    			return entry.getKey();
    		}
    	}
		return null;
    }

	/** Obsolete, never called. */
	@Override public void error(String str) {
		throw new RuntimeException();
	}
	
	@Override public void connectAck() {
	}

	public void reqSecDefOptParams( String underlyingSymbol, String futFopExchange, /*String currency,*/ String underlyingSecType, int underlyingConId, ISecDefOptParamsReqHandler handler) {
		if (!checkConnection())
			return;

		int reqId = m_reqId++;
		m_secDefOptParamsReqMap.put( reqId, handler);
		m_client.reqSecDefOptParams(reqId, underlyingSymbol, futFopExchange, /*currency,*/ underlyingSecType, underlyingConId);
		sendEOM();
	} 
	
	public interface ISecDefOptParamsReqHandler {
		void securityDefinitionOptionalParameter(String exchange, int underlyingConId, String tradingClass,
				String multiplier, Set<String> expirations, Set<Double> strikes);
		void securityDefinitionOptionalParameterEnd(int reqId);
	}
	
	@Override
	public void securityDefinitionOptionalParameter(int reqId, String exchange, int underlyingConId, String tradingClass,
			String multiplier, Set<String> expirations, Set<Double> strikes) {
		ISecDefOptParamsReqHandler handler = m_secDefOptParamsReqMap.get( reqId);
		
		if (handler != null) {
			handler.securityDefinitionOptionalParameter(exchange, underlyingConId, tradingClass, multiplier, expirations, strikes);
		}
	}

	@Override
	public void securityDefinitionOptionalParameterEnd(int reqId) {
		ISecDefOptParamsReqHandler handler = m_secDefOptParamsReqMap.get( reqId);
		if (handler != null) {
			handler.securityDefinitionOptionalParameterEnd(reqId);
		}		
	}
	

	public interface ISoftDollarTiersReqHandler {
		void softDollarTiers(SoftDollarTier[] tiers);
	}
	
	public void reqSoftDollarTiers(ISoftDollarTiersReqHandler handler) {
		if (!checkConnection())
			return;

    	int reqId = m_reqId++;
    	
		m_softDollarTiersReqMap.put(reqId, handler);		
		m_client.reqSoftDollarTiers(reqId);
		sendEOM();
	}
	
	@Override
	public void softDollarTiers(int reqId, SoftDollarTier[] tiers) {
		ISoftDollarTiersReqHandler handler = m_softDollarTiersReqMap.get(reqId);
		
		if (handler != null) {
			handler.softDollarTiers(tiers);
		}
	}

    public interface IFamilyCodesHandler {
        void familyCodes(FamilyCode[] familyCodes);
    }

    public void reqFamilyCodes(IFamilyCodesHandler handler) {
        if (!checkConnection())
            return;

        m_familyCodesHandlers.add(handler);
        m_client.reqFamilyCodes();
        sendEOM();
    }

    @Override
    public void familyCodes(FamilyCode[] familyCodes) {
        for( IFamilyCodesHandler handler : m_familyCodesHandlers) {
            handler.familyCodes(familyCodes);
        }
        recEOM();
    }
    
    public interface ISymbolSamplesHandler {
        void symbolSamples(ContractDescription[] contractDescriptions);
    }

    public void reqMatchingSymbols(String pattern, ISymbolSamplesHandler handler) {
        if (!checkConnection())
            return;
        
        int reqId = m_reqId++;

        m_symbolSamplesHandlerMap.put(reqId, handler);
        m_client.reqMatchingSymbols(reqId, pattern);
        sendEOM();
    }

    @Override
    public void symbolSamples(int reqId, ContractDescription[] contractDescriptions) {
        ISymbolSamplesHandler handler = m_symbolSamplesHandlerMap.get(reqId);

        if (handler != null) {
            handler.symbolSamples(contractDescriptions);
        }
        recEOM();
    }

	@Override
	public void historicalDataEnd(int reqId, String startDateStr, String endDateStr) {
		IHistoricalDataHandler handler = m_historicalDataMap.get(reqId);
		
		if (handler != null) {
			handler.historicalDataEnd();
		}
	}

	public interface IMktDepthExchangesHandler {
		void mktDepthExchanges(DepthMktDataDescription[] depthMktDataDescriptions);
	}

	public void reqMktDepthExchanges(IMktDepthExchangesHandler handler) {
		if (!checkConnection())
			return;

		m_mktDepthExchangesHandlers.add(handler);
		m_client.reqMktDepthExchanges();
		sendEOM();
	}

	@Override
	public void mktDepthExchanges(DepthMktDataDescription[] depthMktDataDescriptions) {
		for( IMktDepthExchangesHandler handler : m_mktDepthExchangesHandlers) {
			handler.mktDepthExchanges(depthMktDataDescriptions);
		}
		recEOM();
	}
	
	public interface ITickNewsHandler {
		void tickNews(long timeStamp, String providerCode, String articleId, String headline, String extraData);
	}

	public void reqNewsTicks(Contract contract, ITickNewsHandler handler) {
		if (!checkConnection())
			return;

		int tickerId = m_reqId++;

		m_tickNewsHandlerMap.put(tickerId, handler);
		m_client.reqMktData(tickerId, contract, "mdoff,292", false, false, Collections.emptyList());
		sendEOM();
	}

	@Override
	public void tickNews(int tickerId, long timeStamp, String providerCode, String articleId, String headline, String extraData) {
		ITickNewsHandler handler = m_tickNewsHandlerMap.get(tickerId);

		if (handler != null) {
			handler.tickNews(timeStamp, providerCode, articleId, headline, extraData);
		}
		recEOM();
	}

	public interface ISmartComponentsHandler {
		
		void smartComponents(int reqId, Map<Integer, Entry<String, Character>> theMap);
		
	}

	@Override
	public void smartComponents(int reqId, Map<Integer, Map.Entry<String, Character>> theMap) {
		ISmartComponentsHandler handler = m_smartComponentsHandler.get(reqId);
		
		if (handler != null) {
			handler.smartComponents(reqId, theMap);
		}
	}
	
	public void reqSmartComponents(String bboExchange, ISmartComponentsHandler handler) {
		if (!checkConnection())
			return;
		
		int reqId = m_reqId++;
		
		m_smartComponentsHandler.put(reqId, handler);
		m_client.reqSmartComponents(reqId, bboExchange);
		sendEOM();
	}

	@Override
	public void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions) {
		ITopMktDataHandler handler = m_topMktDataMap.get(tickerId);
		
		if (handler != null) {
			handler.tickReqParams(tickerId, minTick, bboExchange, snapshotPermissions);
		}
		
		recEOM();
	}

	public interface INewsProvidersHandler {
		void newsProviders(NewsProvider[] newsProviders);
	}

	public void reqNewsProviders(INewsProvidersHandler handler) {
		if (!checkConnection())
			return;

		m_newsProvidersHandlers.add(handler);
		m_client.reqNewsProviders();
		sendEOM();
	}

	@Override
	public void newsProviders(NewsProvider[] newsProviders) {
		for( INewsProvidersHandler handler : m_newsProvidersHandlers) {
			handler.newsProviders(newsProviders);
		}
		recEOM();
	}
	
	public interface INewsArticleHandler {
		void newsArticle(int articleType, String articleText);
	}

	public void reqNewsArticle(String providerCode, String articleId, INewsArticleHandler handler) {
		if (!checkConnection())
			return;

		int requestId = m_reqId++;

		m_newsArticleHandlerMap.put(requestId, handler);
		m_client.reqNewsArticle(requestId, providerCode, articleId, Collections.emptyList());
		sendEOM();
	}

	@Override
	public void newsArticle(int requestId, int articleType, String articleText) {
		INewsArticleHandler handler = m_newsArticleHandlerMap.get(requestId);

		if (handler != null) {
			handler.newsArticle(articleType, articleText);
		}
		recEOM();
	}

	public interface IHistoricalNewsHandler {
		void historicalNews( String time, String providerCodes, String articleId, String headline);
		void historicalNewsEnd( boolean hasMore);
	}

	public void reqHistoricalNews( int conId, String providerCodes, String startDateTime, String endDateTime, int totalResults, IHistoricalNewsHandler handler) {
		if (!checkConnection())
			return;

		int requestId = m_reqId++;
		m_historicalNewsHandlerMap.put( requestId, handler);
		m_client.reqHistoricalNews( requestId, conId, providerCodes, startDateTime, endDateTime, totalResults, Collections.emptyList());
		sendEOM();
	}

	@Override public void historicalNews( int requestId, String time, String providerCode, String articleId, String headline) {
		IHistoricalNewsHandler handler = m_historicalNewsHandlerMap.get( requestId);
		if (handler != null) {
			handler.historicalNews( time, providerCode, articleId, headline);
		}
		recEOM();
	}

	@Override public void historicalNewsEnd( int requestId, boolean hasMore) {
		IHistoricalNewsHandler handler = m_historicalNewsHandlerMap.get( requestId);
		if (handler != null) {
			handler.historicalNewsEnd( hasMore);
		}
		recEOM();
	}

	public interface IHeadTimestampHandler {

		void headTimestamp(int reqId, long headTimestamp);
		
	}
	
	public void reqHeadTimestamp(Contract contract, WhatToShow whatToShow, boolean rthOnly, IHeadTimestampHandler handler) {
		if (!checkConnection())
			return;

    	int reqId = m_reqId++;
		
    	m_headTimestampMap.put(reqId, handler);
    	m_client.reqHeadTimestamp(reqId, contract, whatToShow.toString(), rthOnly ? 1 : 0, 2);
	}

	@Override
	public void headTimestamp(int reqId, String headTimestamp) {
		IHeadTimestampHandler handler = m_headTimestampMap.get(reqId);
		
		if (handler != null) {
			handler.headTimestamp(reqId, Long.parseLong(headTimestamp));
		}
		
		recEOM();
	}
	
	public interface IHistogramDataHandler {

		void histogramData(int reqId, List<HistogramEntry> items);
		
	}
	
	public void reqHistogramData(Contract contract, int duration, DurationUnit durationUnit, boolean rthOnly, IHistogramDataHandler handler) {
		if (!checkConnection())
			return;

    	int reqId = m_reqId++;
    	String durationStr = duration + " " + durationUnit.toString().toLowerCase() + "s";
    	
    	m_histogramDataMap.put(reqId, handler);
    	m_client.reqHistogramData(reqId, contract, rthOnly, durationStr);
	}
	
    public void cancelHistogramData(IHistogramDataHandler handler) {
		if (!checkConnection())
			return;

		Integer reqId = getAndRemoveKey(m_histogramDataMap, handler);
		
    	if (reqId != null) {
    		m_client.cancelHistogramData(reqId);
    		sendEOM();
    	}
    }

	@Override
	public void histogramData(int reqId, List<HistogramEntry> items) {
		IHistogramDataHandler handler = m_histogramDataMap.get(reqId);
		
		if (handler != null) {
			handler.histogramData(reqId, items);
		}
		
		recEOM();
	}

    @Override
    public void historicalDataUpdate(int reqId, com.ib.client.Bar bar) {
        historicalData(reqId, bar);
    }

	@Override public void rerouteMktDataReq(int reqId, int conId, String exchange) {
		show( "Re-route market data request. ReqId: " + reqId + ", ConId: " + conId + ", Exchange: " + exchange);
	}

	@Override public void rerouteMktDepthReq(int reqId, int conId, String exchange) {
		show( "Re-route market depth request. ReqId: " + reqId + ", ConId: " + conId + ", Exchange: " + exchange);
	}

    public interface IMarketRuleHandler {
        void marketRule(int marketRuleId, PriceIncrement[] priceIncrements);
    }

    public void reqMarketRule(int marketRuleId, IMarketRuleHandler handler) {
        if (!checkConnection())
            return;

        m_marketRuleHandlers.add(handler);
        m_client.reqMarketRule(marketRuleId);
        sendEOM();
    }

    @Override
    public void marketRule(int marketRuleId, PriceIncrement[] priceIncrements) {
        for( IMarketRuleHandler handler : m_marketRuleHandlers) {
            handler.marketRule(marketRuleId, priceIncrements);
        }
        recEOM();
    }

	
	public interface IPnLHandler {

        void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL);
	    
	}

	public void reqPnL(String account, String modelCode, IPnLHandler handler) {
	    if (!checkConnection())
	        return;

	    int reqId = m_reqId++;

	    m_pnlMap.put(reqId, handler);

	    m_client.reqPnL(reqId, account, modelCode);
	}

	public void cancelPnL(IPnLHandler handler) {
	    if (!checkConnection())
	        return;

	    Integer reqId = getAndRemoveKey(m_pnlMap, handler);

	    if (reqId != null) {
	        m_client.cancelPnL(reqId);
	        sendEOM();
	    }
	}	

    @Override
    public void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL) {
        IPnLHandler handler = m_pnlMap.get(reqId);
        
        if (handler != null) {
            handler.pnl(reqId, dailyPnL, unrealizedPnL, realizedPnL);
        }
        
        recEOM();
    }
    
    public interface IPnLSingleHandler {

        void pnlSingle(int reqId, Decimal pos, double dailyPnL, double unrealizedPnL, double realizedPnL, double value);
        
    }

    public void reqPnLSingle(String account, String modelCode, int conId, IPnLSingleHandler handler) {
        if (!checkConnection())
            return;

        int reqId = m_reqId++;

        m_pnlSingleMap.put(reqId, handler);

        m_client.reqPnLSingle(reqId, account, modelCode, conId);
    }

    public void cancelPnLSingle(IPnLSingleHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(m_pnlSingleMap, handler);

        if (reqId != null) {
            m_client.cancelPnLSingle(reqId);
            sendEOM();
        }
    }    

    @Override
    public void pnlSingle(int reqId, Decimal pos, double dailyPnL, double unrealizedPnL, double realizedPnL, double value) {
        IPnLSingleHandler handler = m_pnlSingleMap.get(reqId);
        
        if (handler != null) {
            handler.pnlSingle(reqId, pos, dailyPnL, unrealizedPnL, realizedPnL, value);
        }
        
        recEOM();
    }
    
    public interface IHistoricalTickHandler {

        void historicalTick(int reqId, List<HistoricalTick> ticks);        
        void historicalTickBidAsk(int reqId, List<HistoricalTickBidAsk> ticks);        
        void historicalTickLast(int reqId, List<HistoricalTickLast> ticks);
        
    }

    public void reqHistoricalTicks(Contract contract, String startDateTime,
            String endDateTime, int numberOfTicks, String whatToShow, int useRth, boolean ignoreSize, IHistoricalTickHandler handler) {
        if (!checkConnection())
            return;

        int reqId = m_reqId++;

        m_historicalTicksMap.put(reqId, handler);

        m_client.reqHistoricalTicks(reqId, contract, startDateTime, endDateTime, numberOfTicks, whatToShow, useRth, ignoreSize, Collections.emptyList());
    }   

    @Override
    public void historicalTicks(int reqId, List<HistoricalTick> ticks, boolean last) {
        IHistoricalTickHandler handler = m_historicalTicksMap.get(reqId);
        
        if (handler != null) {
            handler.historicalTick(reqId, ticks);
        }

        ITickByTickDataHandler handlerTickByTick = m_tickByTickDataMap.get(reqId);
        
        if (handlerTickByTick != null) {
           handlerTickByTick.tickByTickHistoricalTick(reqId, ticks);
        }
        
        
        recEOM();
    }
    
    @Override
    public void historicalTicksBidAsk(int reqId, List<HistoricalTickBidAsk> ticks, boolean done) {
        IHistoricalTickHandler handler = m_historicalTicksMap.get(reqId);
        
        if (handler != null) {
            handler.historicalTickBidAsk(reqId, ticks);
        }
        
        ITickByTickDataHandler handlerTickByTick = m_tickByTickDataMap.get(reqId);
        
        if (handlerTickByTick != null) {
           handlerTickByTick.tickByTickHistoricalTickBidAsk(reqId, ticks);
        }
        
        recEOM();
    }

    @Override
    public void historicalTicksLast(int reqId, List<HistoricalTickLast> ticks, boolean done) {
        IHistoricalTickHandler handler = m_historicalTicksMap.get(reqId);
        
        if (handler != null) {
            handler.historicalTickLast(reqId, ticks);
        }
        
        ITickByTickDataHandler handlerTickByTick = m_tickByTickDataMap.get(reqId);
        
        if (handlerTickByTick != null) {
            handlerTickByTick.tickByTickHistoricalTickAllLast(reqId, ticks);
        }

        recEOM();
    }

    public interface ITickByTickDataHandler {
        void tickByTickAllLast(int reqId, int tickType, long time, double price, Decimal size, TickAttribLast tickAttribLast, String exchange, String specialConditions);
        void tickByTickBidAsk(int reqId, long time, double bidPrice, double askPrice, Decimal bidSize, Decimal askSize, TickAttribBidAsk tickAttribBidAsk);
        void tickByTickMidPoint(int reqId, long time, double midPoint);
        void tickByTickHistoricalTickAllLast(int reqId, List<HistoricalTickLast> ticks);
        void tickByTickHistoricalTickBidAsk(int reqId, List<HistoricalTickBidAsk> ticks);        
        void tickByTickHistoricalTick(int reqId, List<HistoricalTick> ticks);        
    }

    public void reqTickByTickData(Contract contract, String tickType, int numberOfTicks, boolean ignoreSize, 
            ITickByTickDataHandler handler) {
        if (!checkConnection())
            return;

        int reqId = m_reqId++;
        m_tickByTickDataMap.put( reqId, handler);
        m_client.reqTickByTickData( reqId, contract, tickType, numberOfTicks, ignoreSize);
        sendEOM();
    }

    public void cancelTickByTickData( ITickByTickDataHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey( m_tickByTickDataMap, handler);
        if (reqId != null) {
            m_client.cancelTickByTickData( reqId);
            sendEOM();
        }
    }

    @Override
    public void tickByTickAllLast(int reqId, int tickType, long time, double price, Decimal size, TickAttribLast tickAttribLast,
            String exchange, String specialConditions) {
        ITickByTickDataHandler handler = m_tickByTickDataMap.get(reqId);

        if (handler != null) {
            handler.tickByTickAllLast(reqId, tickType, time, price, size, tickAttribLast, exchange, specialConditions);
        }

        recEOM();
    }

    @Override
    public void tickByTickBidAsk(int reqId, long time, double bidPrice, double askPrice, Decimal bidSize, Decimal askSize,
            TickAttribBidAsk tickAttribBidAsk) {
        ITickByTickDataHandler handler = m_tickByTickDataMap.get(reqId);

        if (handler != null) {
            handler.tickByTickBidAsk(reqId, time, bidPrice, askPrice, bidSize, askSize, tickAttribBidAsk);
        }

        recEOM();
    }
    
    @Override
    public void tickByTickMidPoint(int reqId, long time, double midPoint) {
        ITickByTickDataHandler handler = m_tickByTickDataMap.get(reqId);

        if (handler != null) {
            handler.tickByTickMidPoint(reqId, time, midPoint);
        }

        recEOM();
    }

    @Override
    public void orderBound(long orderId, int apiClientId, int apiOrderId) {
        show( "Order bound. OrderId: " + orderId + ", apiClientId: " + apiClientId + ", apiOrderId: " + apiOrderId);
    }

    // ---------------------------------------- Completed orders ----------------------------------------
    public interface ICompletedOrdersHandler {
        void completedOrder(Contract contract, Order order, OrderState orderState);
        void completedOrdersEnd();
    }

    public void reqCompletedOrders(ICompletedOrdersHandler handler) {
        if (!checkConnection())
            return;

        m_completedOrdersHandler = handler;
        m_client.reqCompletedOrders(false);
        sendEOM();
    }

    @Override
    public void completedOrder(Contract contract, Order order, OrderState orderState) {
        if (m_completedOrdersHandler != null) {
            m_completedOrdersHandler.completedOrder(contract, order, orderState);
        }
        recEOM();
    }

    @Override
    public void completedOrdersEnd() {
        if (m_completedOrdersHandler != null) {
            m_completedOrdersHandler.completedOrdersEnd();
        }
        recEOM();
    }
    
    // ---------------------------------------- WSH Meta Data ----------------------------------------
    public interface IWshMetaDataHandler {
        void wshMetaData(int reqId, String dataJson);
    }
    
    public void reqWshMetaData(IWshMetaDataHandler handler) {
        if (!checkConnection())
            return;

        int reqId = m_reqId++;
        m_wshMetaDataMap.put(reqId, handler);
        m_client.reqWshMetaData(reqId);
        sendEOM();;
    }
    
    public void cancelWshMetaData(IWshMetaDataHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(m_wshMetaDataMap, handler);
        if (reqId != null) {
            m_client.cancelWshMetaData(reqId);
            sendEOM();
        }
    }

    @Override
    public void wshMetaData(int reqId, String dataJson) {
        IWshMetaDataHandler handler = m_wshMetaDataMap.get(reqId);

        if (handler != null) {
            handler.wshMetaData(reqId, dataJson);
        }

        recEOM(); 
    }

    // ---------------------------------------- WSH Event Data ----------------------------------------
    public interface IWshEventDataHandler {
        void wshEventData(int reqId, String dataJson);
    }

    // why is this failing at work only? pas
//    public void reqWshEventData(WshEventData wshEventData, IWshEventDataHandler handler) {
//        if (!checkConnection())
//            return;
//
//        int reqId = m_reqId++;
//        m_wshEventDataMap.put(reqId, handler);
//        m_client.reqWshEventData(reqId, wshEventData);
//        sendEOM();
//    }
    
    public void cancelWshEventData(IWshEventDataHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(m_wshEventDataMap, handler);
        if (reqId != null) {
            m_client.cancelWshMetaData(reqId);
            sendEOM();
        }
    }

    @Override
    public void wshEventData(int reqId, String dataJson) {
        IWshEventDataHandler handler = m_wshEventDataMap.get(reqId);

        if (handler != null) {
            handler.wshEventData(reqId, dataJson);
        }

        recEOM();       
    }

    // ---------------------------------------- Historical Schedule ----------------------------------------
    public interface IHistoricalScheduleHandler {
        void historicalSchedule(int reqId, String startDateTime, String endDateTime, String timeZone, List<HistoricalSession> sessions);
    }

    public void reqHistoricalSchedule(Contract contract, String endDateTime, int duration, DurationUnit durationUnit, boolean rthOnly, IHistoricalScheduleHandler handler) {
        if (!checkConnection())
            return;

        int reqId = m_reqId++;
        m_historicalScheduleMap.put(reqId, handler);
            String durationStr = duration + " " + durationUnit.toString().charAt( 0);
        m_client.reqHistoricalData(reqId, contract, endDateTime, durationStr, BarSize._1_day.toString(), WhatToShow.SCHEDULE.name(), rthOnly ? 1 : 0, 2, false, Collections.emptyList());

        sendEOM();
    }

    public void cancelHistoricalSchedule(IHistoricalScheduleHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(m_historicalScheduleMap, handler);
        if (reqId != null) {
            m_client.cancelHistoricalData(reqId);

            sendEOM();
        }
    }

    @Override public void historicalSchedule(int reqId, String startDateTime, String endDateTime, String timeZone, List<HistoricalSession> sessions) {
        IHistoricalScheduleHandler handler = m_historicalScheduleMap.get(reqId);

        if (handler != null) {
            handler.historicalSchedule(reqId, startDateTime, endDateTime, timeZone, sessions);
        }

        recEOM();
    }
    
    // ---------------------------------------- User Info handling ----------------------------------------
    public interface IUserInfoHandler {
        void userInfo(int reqId, String whiteBrandingId);
    }

    public void reqUserInfo(int reqId, IUserInfoHandler handler) {
        if (!checkConnection())
            return;

        m_userInfoHandler = handler;
        m_client.reqUserInfo(reqId);
        sendEOM();
    }

    @Override public void userInfo(int reqId, String whiteBrandingId) {
        m_userInfoHandler.userInfo(reqId, whiteBrandingId);
        recEOM();
    }

	public int mdCount() {
		return m_topMktDataMap.size();
	}

	public void tick(int reqId, double price) {
		Util.ifff( m_topMktDataMap.get( reqId), handler -> handler.tickPrice( TickType.LAST, price, null) ); 
	}

	/** simulate a file; this needs to be improved to have correct price and size */
	public void simFill(int orderId, double filled, double remaining, double avgPrice) {
		orderStatus(orderId, "Filled", Decimal.get( filled), Decimal.get( remaining), avgPrice, 0, 0, 0, 0, "", 0);
	}
}
