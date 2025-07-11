/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.simple.JsonObject;

import com.ib.client.Types.Action;
import com.ib.client.Types.AlgoStrategy;
import com.ib.client.Types.HedgeType;
import com.ib.client.Types.Method;
import com.ib.client.Types.OcaType;
import com.ib.client.Types.ReferencePriceType;
import com.ib.client.Types.Rule80A;
import com.ib.client.Types.TimeInForce;
import com.ib.client.Types.TriggerMethod;
import com.ib.client.Types.VolatilityType;

public class Order {
    final public static int 	CUSTOMER = 0;
    final public static int 	FIRM = 1;
    final public static char    OPT_UNKNOWN='?';
    final public static char    OPT_BROKER_DEALER='b';
    final public static char    OPT_CUSTOMER ='c';
    final public static char    OPT_FIRM='f';
    final public static char    OPT_ISEMM='m';
    final public static char    OPT_FARMM='n';
    final public static char    OPT_SPECIALIST='y';
    final public static int 	AUCTION_MATCH = 1;
    final public static int 	AUCTION_IMPROVEMENT = 2;
    final public static int 	AUCTION_TRANSPARENT = 3;
    final public static String  EMPTY_STR = "";
    final public static double COMPETE_AGAINST_BEST_OFFSET_UP_TO_MID = Double.POSITIVE_INFINITY;

    // order id's
    private int  m_clientId;
    private int  m_orderId;
    private int  m_permId;
    private int  m_parentId; // Parent order Id, to associate Auto STP or TRAIL orders with the original order.

    // primary attributes
    private Action		m_action = Action.Buy;
    private int         m_roundedQty;  // quantity submitted to exchange
    private int         m_displaySize;
    private String      m_orderType = "LMT";
    private double      m_lmtPrice = Double.MAX_VALUE;
    private double      m_auxPrice = Double.MAX_VALUE;
    private String      m_tif = "DAY"; // "Time in Force" - DAY, GTC, etc.

    // Clearing info
    private String   m_account; // IB account
    private String   m_settlingFirm;
    private String   m_clearingAccount; // True beneficiary of the order
    private String   m_clearingIntent; // "" (Default), "IB", "Away", "PTA" (PostTrade)
    
    // secondary attributes
    private boolean m_allOrNone;
    private boolean m_blockOrder;
    private boolean m_hidden;
    private boolean m_outsideRth;
    private boolean m_sweepToFill;
    private double  m_percentOffset = Double.MAX_VALUE;   // for Relative orders; specify the decimal, e.g. .04 not 4
    private double  m_trailingPercent = Double.MAX_VALUE; // for Trailing Stop orders; specify the percentage, e.g. 3, not .03
    private double  m_trailStopPrice = Double.MAX_VALUE;  // stop price for Trailing Stop orders
    private int     m_minQty = Integer.MAX_VALUE;
    private String  m_goodAfterTime; // FORMAT: 20060505 08:00:00 EST
    private String  m_goodTillDate;  // FORMAT: 20060505 08:00:00 EST or 20060505
    private String  m_ocaGroup; // one cancels all group name
    private String  m_orderRef;
    private String  m_rule80A = "";
    private int     m_ocaType = 0; // None
    private int     m_triggerMethod = 0; // Default

    // extended order fields
    private String m_activeStartTime; // GTC orders
    private String m_activeStopTime;  // GTC orders

    // advisor allocation orders
    private String m_faGroup;
    private String m_faMethod = ""; // None;
    private String m_faPercentage;
    private String m_faProfile;

    // volatility orders
    private double  m_volatility = Double.MAX_VALUE;  // enter percentage not decimal, e.g. 2 not .02
    private int     m_volatilityType = Integer.MAX_VALUE; // None;
    private int     m_continuousUpdate;
    private int     m_referencePriceType = Integer.MAX_VALUE; // None;
    private String  m_deltaNeutralOrderType = ""; // None;
    private double  m_deltaNeutralAuxPrice = Double.MAX_VALUE;
    private int     m_deltaNeutralConId;
    private String  m_deltaNeutralOpenClose;
    private boolean m_deltaNeutralShortSale;
    private int     m_deltaNeutralShortSaleSlot;
    private String  m_deltaNeutralDesignatedLocation;

    // scale orders
    private int     m_scaleInitLevelSize = Integer.MAX_VALUE;
    private int     m_scaleSubsLevelSize = Integer.MAX_VALUE;
    private double  m_scalePriceIncrement = Double.MAX_VALUE;
    private double  m_scalePriceAdjustValue = Double.MAX_VALUE;
    private int     m_scalePriceAdjustInterval = Integer.MAX_VALUE;
    private double  m_scaleProfitOffset = Double.MAX_VALUE;
    private boolean m_scaleAutoReset;
    private int     m_scaleInitPosition = Integer.MAX_VALUE;
    private int     m_scaleInitFillQty = Integer.MAX_VALUE;
    private boolean m_scaleRandomPercent;
    private String  m_scaleTable;

    // hedge orders
    private String  m_hedgeType = "";
    private String  m_hedgeParam; // beta value for beta hedge (in range 0-1), ratio for pair hedge

    // algo orders
    private String              m_algoStrategy = "";
    private List<TagValue> m_algoParams = new ArrayList<>();
    private String              m_algoId;

    // combo orders
    private List<TagValue>       m_smartComboRoutingParams = new ArrayList<>();
    private List<OrderComboLeg>  m_orderComboLegs = new ArrayList<>();

    // processing control
    private boolean m_whatIf;
    private boolean m_transmit = true; // if false, order will be sent to TWS but not transmitted to server
    private boolean m_overridePercentageConstraints;
    
    // Institutional orders only
    private String m_openClose;       // O=Open, C=Close
    private int    m_origin;          // 0=Customer, 1=Firm
    private int    m_shortSaleSlot;   // 1 if you hold the shares, 2 if they will be delivered from elsewhere.  Only for Action="SSHORT
    private String m_designatedLocation; // set when slot=2 only.
    private int    m_exemptCode = -1;
    private String m_deltaNeutralSettlingFirm;
    private String m_deltaNeutralClearingAccount;
    private String m_deltaNeutralClearingIntent;
    
    // SMART routing only
    private double  m_discretionaryAmt = Double.MAX_VALUE;
    private boolean m_optOutSmartRouting;

    // BOX or VOL ORDERS ONLY
    private int     m_auctionStrategy; // 1=AUCTION_MATCH, 2=AUCTION_IMPROVEMENT, 3=AUCTION_TRANSPARENT

    // BOX ORDERS ONLY
    private double m_startingPrice;
    private double m_stockRefPrice;
    private double m_delta = Double.MAX_VALUE;

    // pegged to stock or VOL orders
    private double m_stockRangeLower;
    private double m_stockRangeUpper;

    // COMBO ORDERS ONLY
    private double m_basisPoints;      // EFP orders only, download only
    private int    m_basisPointsType;  // EFP orders only, download only

    // Not Held
    private boolean  m_notHeld;

    // order misc options
    private List<TagValue> m_orderMiscOptions;
    
    //order algo id
	private boolean m_solicited;
	
	private boolean m_randomizeSize;
	private boolean m_randomizePrice;
	
	//VER PEG2BENCH fields:
	private int m_referenceContractId;
	private double m_peggedChangeAmount;
	private boolean m_isPeggedChangeAmountDecrease;
	private double m_referenceChangeAmount;
	private String m_referenceExchangeId;
	private OrderType m_adjustedOrderType;
	private double m_triggerPrice = Double.MAX_VALUE;
	private double m_adjustedStopPrice = Double.MAX_VALUE;
	private double m_adjustedStopLimitPrice = Double.MAX_VALUE;
	private double m_adjustedTrailingAmount = Double.MAX_VALUE;
	private int m_adjustableTrailingUnit;
	private double m_lmtPriceOffset = Double.MAX_VALUE;
	
	private List<OrderCondition> m_conditions = new ArrayList<>();
	private boolean m_conditionsCancelOrder;
	private boolean m_conditionsIgnoreRth;
    
    // models
    private String m_modelCode;
    
	private String m_extOperator;
	private SoftDollarTier m_softDollarTier;
	
	// native cash quantity
	private double m_cashQty = Double.MAX_VALUE;
	
	private String m_mifid2DecisionMaker = EMPTY_STR;
	private String m_mifid2DecisionAlgo = EMPTY_STR;
    private String m_mifid2ExecutionTrader = EMPTY_STR;
    private String m_mifid2ExecutionAlgo = EMPTY_STR;	
    
    // don't use auto price for hedge
    private boolean m_dontUseAutoPriceForHedge;
    
    private boolean m_isOmsContainer;
    private boolean m_discretionaryUpToLimitPrice;
    
    private String  m_autoCancelDate;
    private Decimal m_filledQuantity;
    private int     m_refFuturesConId;
    private boolean m_autoCancelParent;
    private String  m_shareholder;
    private boolean m_imbalanceOnly;
    private boolean m_routeMarketableToBbo;
    private long    m_parentPermId;

    private Boolean m_usePriceMgmtAlgo;
    private int     m_duration;
    private int     m_postToAts;
    private String  m_advancedErrorOverride;
    private String  m_manualOrderTime;
    private int     m_minTradeQty;
    private int     m_minCompeteSize;
    private double  m_competeAgainstBestOffset;
    private double  m_midOffsetAtWhole;
    private double  m_midOffsetAtHalf;
	private OrderStatus m_status = OrderStatus.Unknown;
	private Date m_placedAt;
	
	// getters
    public Action  action()                         { return m_action; }
    public boolean allOrNone()                      { return m_allOrNone; }
    public boolean blockOrder()                     { return m_blockOrder; }
    public boolean hidden()                         { return m_hidden; }
    public boolean outsideRth()                     { return m_outsideRth; }
    public boolean notHeld()                        { return m_notHeld; }
    public boolean solicited()                      { return m_solicited; }
    public boolean optOutSmartRouting()             { return m_optOutSmartRouting; }
    public boolean scaleAutoReset()                 { return m_scaleAutoReset; }
    public boolean scaleRandomPercent()             { return m_scaleRandomPercent; }
    public boolean sweepToFill()                    { return m_sweepToFill; }
    public boolean transmit()                       { return m_transmit; }
    public boolean whatIf()                         { return m_whatIf; }
    public double auxPrice()                        { return m_auxPrice; }
    public double delta()                           { return m_delta; }
    public double deltaNeutralAuxPrice()            { return m_deltaNeutralAuxPrice; }
    public double discretionaryAmt()                { return m_discretionaryAmt; }
    public double lmtPrice()                        { return m_lmtPrice; }
    public double percentOffset()                   { return m_percentOffset; }
    public double scalePriceAdjustValue()           { return m_scalePriceAdjustValue; }
    public double scalePriceIncrement()             { return m_scalePriceIncrement; }
    public double scaleProfitOffset()               { return m_scaleProfitOffset; }
    public double startingPrice()                   { return m_startingPrice; }
    public double stockRangeLower()                 { return m_stockRangeLower; }
    public double stockRangeUpper()                 { return m_stockRangeUpper; }
    public double stockRefPrice()                   { return m_stockRefPrice; }
    public double basisPoints()                     { return m_basisPoints; }
    public int    basisPointsType()                 { return m_basisPointsType; }
    public double trailingPercent()                 { return m_trailingPercent; }
    public double trailStopPrice()                  { return m_trailStopPrice; }
    public double volatility()                      { return m_volatility; }
    public int clientId()                           { return m_clientId; }
    public int continuousUpdate()                   { return m_continuousUpdate; }
    public int deltaNeutralConId()                  { return m_deltaNeutralConId; }
    public String deltaNeutralOpenClose()           { return m_deltaNeutralOpenClose; }
    public boolean deltaNeutralShortSale()          { return m_deltaNeutralShortSale; }
    public int deltaNeutralShortSaleSlot()          { return m_deltaNeutralShortSaleSlot; }
    public String deltaNeutralDesignatedLocation()  { return m_deltaNeutralDesignatedLocation; }
    public int displaySize()                        { return m_displaySize; }
    public int minQty()                             { return m_minQty; }
    public int orderId()                            { return m_orderId; }
    public int parentId()                           { return m_parentId; }
    public int scaleInitFillQty()                   { return m_scaleInitFillQty; }
    public int scaleInitLevelSize()                 { return m_scaleInitLevelSize; }
    public int scaleInitPosition()                  { return m_scaleInitPosition; }
    public int scalePriceAdjustInterval()           { return m_scalePriceAdjustInterval; }
    public int scaleSubsLevelSize()                 { return m_scaleSubsLevelSize; }
    public int permId()                             { return m_permId; }
    public Method faMethod()                        { return Method.get(m_faMethod); }
    public String getFaMethod()                     { return m_faMethod; }
    public OcaType ocaType()                        { return OcaType.get(m_ocaType); }
    public int getOcaType()                         { return m_ocaType; }
    public OrderType orderType()                    { return OrderType.get(m_orderType); }
    public String getOrderType()                    { return m_orderType; }
    public Rule80A rule80A()                        { return Rule80A.get(m_rule80A); }
    public String getRule80A()                      { return m_rule80A; }
    public String account()                         { return m_account; }
    public String settlingFirm()                    { return m_settlingFirm; }
    public String clearingAccount()                 { return m_clearingAccount; }
    public String clearingIntent()                  { return m_clearingIntent; }
    public AlgoStrategy algoStrategy()              { return AlgoStrategy.get(m_algoStrategy); }
    public String getAlgoStrategy()                 { return m_algoStrategy; }
    public String algoId()                          { return m_algoId; }
    public String faGroup()                         { return m_faGroup; }
    public String faPercentage()                    { return m_faPercentage; }
    public String faProfile()                       { return m_faProfile; }
    public String goodAfterTime()                   { return m_goodAfterTime; }
    public String goodTillDate()                    { return m_goodTillDate; }
    public String hedgeParam()                      { return m_hedgeParam; }
    public HedgeType hedgeType()                    { return HedgeType.get(m_hedgeType); }
    public String getHedgeType()                    { return m_hedgeType; }
    public String ocaGroup()                        { return m_ocaGroup; }
    public String orderRef()                        { return m_orderRef; }
    public TimeInForce tif()                        { return TimeInForce.get(m_tif); }
    public String getTif()                          { return m_tif; }
    public String scaleTable()                      { return m_scaleTable; }
    public int auctionStrategy()                    { return m_auctionStrategy; }
    public VolatilityType volatilityType()          { return VolatilityType.get(m_volatilityType); }
    public int getVolatilityType()                  { return m_volatilityType; }
    public TriggerMethod triggerMethod()            { return TriggerMethod.get(m_triggerMethod); }
    public int getTriggerMethod()                   { return m_triggerMethod; }
    public String activeStartTime()                 { return m_activeStartTime; }
    public String activeStopTime()                  { return m_activeStopTime; }
    public OrderType deltaNeutralOrderType()        { return OrderType.get(m_deltaNeutralOrderType); }
    public String getDeltaNeutralOrderType()        { return m_deltaNeutralOrderType; }
    public List<OrderComboLeg> orderComboLegs()     { return m_orderComboLegs; }
    public boolean overridePercentageConstraints()  { return m_overridePercentageConstraints; }
    public String openClose()                       { return m_openClose; }
    public int    origin()                          { return m_origin; }
    public int    shortSaleSlot()                   { return m_shortSaleSlot; }
    public String designatedLocation()              { return m_designatedLocation; }
    public int    exemptCode()                      { return m_exemptCode; }
    public String deltaNeutralSettlingFirm()        { return m_deltaNeutralSettlingFirm; }
    public String deltaNeutralClearingAccount()     { return m_deltaNeutralClearingAccount; }
    public String deltaNeutralClearingIntent()      { return m_deltaNeutralClearingIntent; }
    public ReferencePriceType referencePriceType()  { return ReferencePriceType.get(m_referencePriceType); }
    public int getReferencePriceType()              { return m_referencePriceType; }
    public List<TagValue> smartComboRoutingParams() { return m_smartComboRoutingParams; }
    public List<TagValue> orderMiscOptions()        { return m_orderMiscOptions; }
    public boolean randomizeSize()                  { return m_randomizeSize; }
    public boolean randomizePrice()                 { return m_randomizePrice; }
    public int referenceContractId()                { return m_referenceContractId; }
    public boolean isPeggedChangeAmountDecrease()   { return m_isPeggedChangeAmountDecrease; }
    public double peggedChangeAmount()              { return m_peggedChangeAmount; }
	public double referenceChangeAmount()           { return m_referenceChangeAmount; }
	public String referenceExchangeId()             { return m_referenceExchangeId; }
	public OrderType adjustedOrderType()            { return m_adjustedOrderType; }
	public double triggerPrice()                    { return m_triggerPrice; }
	public double adjustedStopPrice()               { return m_adjustedStopPrice; }
	public double adjustedStopLimitPrice()          { return m_adjustedStopLimitPrice; }
	public double adjustedTrailingAmount()          { return m_adjustedTrailingAmount; }
	public int adjustableTrailingUnit()             { return m_adjustableTrailingUnit; }
	public double lmtPriceOffset()                  { return m_lmtPriceOffset; }
	public List<OrderCondition> conditions()        { return m_conditions; }
	public boolean conditionsIgnoreRth()            { return m_conditionsIgnoreRth; }
	public boolean conditionsCancelOrder()          { return m_conditionsCancelOrder; }
    public String modelCode()                       { return m_modelCode; }
    public String extOperator()                     { return m_extOperator; }
    public SoftDollarTier softDollarTier()          { return m_softDollarTier; }
    public double cashQty()                         { return m_cashQty; }
    public String mifid2DecisionMaker()             { return m_mifid2DecisionMaker; }
    public String mifid2DecisionAlgo()              { return m_mifid2DecisionAlgo; }
    public String mifid2ExecutionTrader()           { return m_mifid2ExecutionTrader; }
    public String mifid2ExecutionAlgo()             { return m_mifid2ExecutionAlgo; }
    public boolean dontUseAutoPriceForHedge()       { return m_dontUseAutoPriceForHedge; }
    public boolean isOmsContainer()                 { return m_isOmsContainer; }
    public boolean discretionaryUpToLimitPrice()    { return m_discretionaryUpToLimitPrice; }
    public String autoCancelDate()                  { return m_autoCancelDate; }
    public Decimal filledQuantity()                 { return m_filledQuantity; }
    public int refFuturesConId()                    { return m_refFuturesConId; }
    public boolean autoCancelParent()               { return m_autoCancelParent; }
    public String shareholder()                     { return m_shareholder; }
    public boolean imbalanceOnly()                  { return m_imbalanceOnly; }
    public boolean routeMarketableToBbo()           { return m_routeMarketableToBbo; }
    public long parentPermId()                      { return m_parentPermId; }
    public Boolean usePriceMgmtAlgo()               { return m_usePriceMgmtAlgo; }
    public int duration()                           { return m_duration; }
    public int postToAts()                          { return m_postToAts; }
    public String advancedErrorOverride()           { return m_advancedErrorOverride; }
    public String manualOrderTime()                 { return m_manualOrderTime; }
    public int minTradeQty()                        { return m_minTradeQty; }
    public int minCompeteSize()                     { return m_minCompeteSize; }
    public double competeAgainstBestOffset()        { return m_competeAgainstBestOffset; }
    public boolean isCompeteAgainstBestOffsetUpToMid() { return m_competeAgainstBestOffset == COMPETE_AGAINST_BEST_OFFSET_UP_TO_MID; }
    public double midOffsetAtWhole()                { return m_midOffsetAtWhole; }
    public double midOffsetAtHalf()                 { return m_midOffsetAtHalf; }
  
	// setters
	public void referenceContractId(int m_referenceContractId)          { this.m_referenceContractId = m_referenceContractId; }
    public void account(String v)                                       { m_account = v; }
    public void settlingFirm(String v)                                  { m_settlingFirm = v; }
    public void clearingAccount(String v)                               { m_clearingAccount = v; }
    public void clearingIntent(String v)                                { m_clearingIntent = v; }
    public void action(Action v)                                        { m_action = v; }
    public void algoStrategy(AlgoStrategy v)                            { m_algoStrategy = ( v == null ) ? null : v.getApiString(); }
    public void algoStrategy(String v)                                  { m_algoStrategy = v; }
    public void algoId(String v)                                        { m_algoId = v; }
    public void allOrNone(boolean v)                                    { m_allOrNone = v; }
    public void auxPrice(double v)                                      { m_auxPrice = v; }
    public void blockOrder(boolean v)                                   { m_blockOrder = v; }
    public void clientId(int v)                                         { m_clientId = v; }
    public void continuousUpdate(int v)                                 { m_continuousUpdate = v; }
    public void delta(double v)                                         { m_delta = v; }
    public void deltaNeutralAuxPrice(double v)                          { m_deltaNeutralAuxPrice = v; }
    public void deltaNeutralConId(int v)                                { m_deltaNeutralConId = v; }
    public void deltaNeutralOpenClose(String v)                         { m_deltaNeutralOpenClose = v; }
    public void deltaNeutralShortSale(boolean v)                        { m_deltaNeutralShortSale = v; }
    public void deltaNeutralShortSaleSlot(int v)                        { m_deltaNeutralShortSaleSlot = v; }
    public void deltaNeutralDesignatedLocation(String v)                { m_deltaNeutralDesignatedLocation = v; }
    public void deltaNeutralOrderType(OrderType v)                      { m_deltaNeutralOrderType = ( v == null ) ? null : v.getApiString(); }
    public void deltaNeutralOrderType(String v)                         { m_deltaNeutralOrderType = v; }
    public void discretionaryAmt(double v)                              { m_discretionaryAmt = v; }
    public void displaySize(int v)                                      { m_displaySize = v; }
    public void faGroup(String v)                                       { m_faGroup = v; }
    public void faMethod(Method v)                                      { m_faMethod = ( v == null ) ? null : v.getApiString(); }
    public void faMethod(String v)                                      { m_faMethod = v; }
    public void faPercentage(String v)                                  { m_faPercentage = v; }
    public void faProfile(String v)                                     { m_faProfile = v; }
    public void goodAfterTime(String v)                                 { m_goodAfterTime = v; }
    public void goodTillDate(String v)                                  { m_goodTillDate = v; }
    public void hedgeParam(String v)                                    { m_hedgeParam = v; }
    public void hedgeType(HedgeType v)                                  { m_hedgeType = ( v == null ) ? null : v.getApiString(); }
    public void hedgeType(String v)                                     { m_hedgeType = v; }
    public void hidden(boolean v)                                       { m_hidden = v; }
    public void lmtPrice(double v)                                      { m_lmtPrice = v; }
    public void minQty(int v)                                           { m_minQty = v; }
    public void notHeld(boolean v)                                      { m_notHeld = v; }
    public void solicited(boolean v)                                    { m_solicited = v; }
    public void ocaGroup(String v)                                      { m_ocaGroup = v; }
    public void ocaType(OcaType v)                                      { m_ocaType = ( v == null ) ? 0 : v.ordinal(); }
    public void ocaType(int v)                                          { m_ocaType = v; }
    public void optOutSmartRouting(boolean v)                           { m_optOutSmartRouting = v; }
    public void orderId(int v)                                          { m_orderId = v; }
    public void orderRef(String v)                                      { m_orderRef = v; }
    public void orderType(OrderType v)                                  { m_orderType = ( v == null ) ? null : v.getApiString(); }
    public void orderType(String v)                                     { m_orderType = v; }
    public void outsideRth(boolean v)                                   { m_outsideRth = v; }
    public void overridePercentageConstraints(boolean v)                { m_overridePercentageConstraints = v; }
    public void openClose(String v)                                     { m_openClose = v; }
    public void origin(int v)                                           { m_origin = v; }
    public void shortSaleSlot(int v)                                    { m_shortSaleSlot = v; }
    public void designatedLocation(String v)                            { m_designatedLocation = v; }
    public void exemptCode(int v)                                       { m_exemptCode = v; }
    public void parentId(int v)                                         { m_parentId = v; }
    public void percentOffset(double v)                                 { m_percentOffset = v; }
    public void permId(int v)                                           { m_permId = v; }
    public void referencePriceType(ReferencePriceType v)                { m_referencePriceType = ( v == null ) ? 0 : v.ordinal(); }
    public void referencePriceType(int v)                               { m_referencePriceType = v; }
    public void rule80A(Rule80A v)                                      { m_rule80A = ( v == null ) ? null : v.getApiString(); }
    public void rule80A(String v)                                       { m_rule80A = v; }
    public void scaleAutoReset(boolean v)                               { m_scaleAutoReset = v; }
    public void scaleInitFillQty(int v)                                 { m_scaleInitFillQty = v; }
    public void scaleInitLevelSize(int v)                               { m_scaleInitLevelSize = v; }
    public void scaleInitPosition(int v)                                { m_scaleInitPosition = v; }
    public void scalePriceAdjustInterval(int v)                         { m_scalePriceAdjustInterval = v; }
    public void scalePriceAdjustValue(double v)                         { m_scalePriceAdjustValue = v; }
    public void scalePriceIncrement(double v)                           { m_scalePriceIncrement = v; }
    public void scaleProfitOffset(double v)                             { m_scaleProfitOffset = v; }
    public void scaleRandomPercent(boolean v)                           { m_scaleRandomPercent = v; }
    public void scaleSubsLevelSize(int v)                               { m_scaleSubsLevelSize = v; }
    public void startingPrice(double v)                                 { m_startingPrice = v; }
    public void stockRangeLower(double v)                               { m_stockRangeLower = v; }
    public void stockRangeUpper(double v)                               { m_stockRangeUpper = v; }
    public void stockRefPrice(double v)                                 { m_stockRefPrice = v; }
    public void basisPoints(double v)                                   { m_basisPoints = v; }
    public void basisPointsType(int v)                                  { m_basisPointsType = v; }
    public void sweepToFill(boolean v)                                  { m_sweepToFill = v; }
    public void tif(TimeInForce v)                                      { m_tif = ( v == null ) ? null : v.getApiString(); }
    public void tif(String v)                                           { m_tif = v; }
    public void trailingPercent(double v)                               { m_trailingPercent = v; }
    public void trailStopPrice(double v)                                { m_trailStopPrice = v; }
    public void transmit(boolean v)                                     { m_transmit = v; }
    public void triggerMethod(TriggerMethod v)                          { m_triggerMethod = ( v == null ) ? 0 : v.val(); }
    public void triggerMethod(int v)                                    { m_triggerMethod = v; }
    public void activeStartTime(String v)                               { m_activeStartTime = v; }
    public void activeStopTime(String v)                                { m_activeStopTime = v; }
    public void algoParams(List<TagValue> v)                            { m_algoParams = v; }
    public void volatility(double v)                                    { m_volatility = v; }
    public void volatilityType(VolatilityType v)                        { m_volatilityType = ( v == null ) ? 0 : v.ordinal(); }
    public void volatilityType(int v)                                   { m_volatilityType = v; }
    public void whatIf(boolean v)                                       { m_whatIf = v; }
    public void scaleTable(String v)                                    { m_scaleTable = v; }
    public void auctionStrategy(int v)                                  { m_auctionStrategy = v; }
    public void orderComboLegs(List<OrderComboLeg> v)                   { m_orderComboLegs = v; }
    public void deltaNeutralSettlingFirm(String v)                      { m_deltaNeutralSettlingFirm = v; }
    public void deltaNeutralClearingAccount(String v)                   { m_deltaNeutralClearingAccount = v; }
    public void deltaNeutralClearingIntent(String v)                    { m_deltaNeutralClearingIntent = v; }
    public void smartComboRoutingParams(List<TagValue> v)               { m_smartComboRoutingParams = v; }
    public void orderMiscOptions(List<TagValue> v)                      { m_orderMiscOptions = v; }
    public void randomizeSize(boolean v)                                { m_randomizeSize = v; }
    public void randomizePrice(boolean v)                               { m_randomizePrice = v; }
    public void modelCode(String v)                                     { m_modelCode = v; }
    public void isPeggedChangeAmountDecrease(boolean v)                 { m_isPeggedChangeAmountDecrease = v; }
	public void peggedChangeAmount(double m_peggedChangeAmount)         { this.m_peggedChangeAmount = m_peggedChangeAmount; }
	public void referenceChangeAmount(double m_referenceChangeAmount)   { this.m_referenceChangeAmount = m_referenceChangeAmount; }
	public void referenceExchangeId(String m_referenceExchangeId)       { this.m_referenceExchangeId = m_referenceExchangeId; }
	public void adjustedOrderType(OrderType v)                          { m_adjustedOrderType = v; }
	public void triggerPrice(double v)                                  { m_triggerPrice = v; }
	public void adjustedStopPrice(double v)                             { m_adjustedStopPrice = v; }
	public void adjustedStopLimitPrice(double v)                        { m_adjustedStopLimitPrice = v; }
	public void adjustedTrailingAmount(double v)                        { m_adjustedTrailingAmount = v; }
	public void adjustableTrailingUnit(int v)                           { m_adjustableTrailingUnit = v; }
	public void lmtPriceOffset(double v)                                { m_lmtPriceOffset = v; }
	public void conditions(List<OrderCondition> v)                      { m_conditions = v; }
	public void conditionsIgnoreRth(boolean v)                          { m_conditionsIgnoreRth = v; }
	public void conditionsCancelOrder(boolean v)                        { m_conditionsCancelOrder = v; }
	public void extOperator(String v)                                   { m_extOperator = v; }
	public void softDollarTier(SoftDollarTier v)                        { m_softDollarTier = v; }
	public void cashQty(double v)                                       { m_cashQty = v; }
    public void mifid2DecisionMaker(String v)                           { m_mifid2DecisionMaker = v; }
    public void mifid2DecisionAlgo(String v)                            { m_mifid2DecisionAlgo = v; }
    public void mifid2ExecutionTrader(String v)                         { m_mifid2ExecutionTrader = v; }
    public void mifid2ExecutionAlgo(String v)                           { m_mifid2ExecutionAlgo = v; }
    public void dontUseAutoPriceForHedge(boolean v)                     { m_dontUseAutoPriceForHedge = v; }
    public void isOmsContainer(boolean v)                               { m_isOmsContainer = v; }
    public void discretionaryUpToLimitPrice(boolean v)                  { m_discretionaryUpToLimitPrice = v; }
    public void autoCancelDate(String v)                                { m_autoCancelDate = v; }
    public void filledQuantity(Decimal v)                               { m_filledQuantity = v; }
    public void refFuturesConId(int v)                                  { m_refFuturesConId = v; }
    public void autoCancelParent(boolean v)                             { m_autoCancelParent = v; }
    public void shareholder(String v)                                   { m_shareholder = v; }
    public void imbalanceOnly(boolean v)                                { m_imbalanceOnly = v; }
    public void routeMarketableToBbo(boolean v)                         { m_routeMarketableToBbo = v; }
    public void parentPermId(long v)                                    { m_parentPermId = v; }
    public void usePriceMgmtAlgo(Boolean v)                             { m_usePriceMgmtAlgo = v; }
    public void duration(int v)                                         { m_duration = v; }
    public void postToAts(int v)                                        { m_postToAts = v; }
    public void advancedErrorOverride(String v)                         { m_advancedErrorOverride = v; }
    public void manualOrderTime(String v)                               { m_manualOrderTime = v; }
    public void minTradeQty(int v)                                      { m_minTradeQty = v; }
    public void minCompeteSize(int v)                                   { m_minCompeteSize = v; }
    public void competeAgainstBestOffset(double v)                      { m_competeAgainstBestOffset = v; }
    public void setCompeteAgainstBestOffsetUpToMid()                    { m_competeAgainstBestOffset = COMPETE_AGAINST_BEST_OFFSET_UP_TO_MID; }
    public void midOffsetAtWhole(double v)                              { m_midOffsetAtWhole = v; }
    public void midOffsetAtHalf(double v)                               { m_midOffsetAtHalf = v; }


    public Order() {
        m_openClose = EMPTY_STR;
        m_activeStartTime = EMPTY_STR;
        m_activeStopTime = EMPTY_STR;
    	m_outsideRth = false;
        m_origin = CUSTOMER;
        m_designatedLocation = EMPTY_STR;
        m_optOutSmartRouting = false;
        m_startingPrice = Double.MAX_VALUE;
        m_stockRefPrice = Double.MAX_VALUE;
        m_stockRangeLower = Double.MAX_VALUE;
        m_stockRangeUpper = Double.MAX_VALUE;
        m_deltaNeutralConId = 0;
        m_deltaNeutralSettlingFirm = EMPTY_STR;
        m_deltaNeutralClearingAccount = EMPTY_STR;
        m_deltaNeutralClearingIntent = EMPTY_STR;
        m_deltaNeutralOpenClose = EMPTY_STR;
        m_deltaNeutralShortSale = false;
        m_deltaNeutralShortSaleSlot = 0;
        m_deltaNeutralDesignatedLocation = EMPTY_STR;
        m_basisPoints = Double.MAX_VALUE;
        m_basisPointsType = Integer.MAX_VALUE;
        m_scaleAutoReset = false;
        m_scaleRandomPercent = false;
        m_scaleTable = EMPTY_STR;
        m_whatIf = false;
        m_notHeld = false;
        m_algoId = EMPTY_STR;
        m_solicited = false;
        m_randomizeSize = false;
        m_randomizePrice = false;
        m_extOperator = EMPTY_STR;
        m_softDollarTier = new SoftDollarTier(EMPTY_STR, EMPTY_STR, EMPTY_STR);
        m_dontUseAutoPriceForHedge = false;
        m_isOmsContainer = false;
        m_discretionaryUpToLimitPrice = false;
        m_autoCancelDate = EMPTY_STR;
        m_filledQuantity = Decimal.INVALID;
        m_refFuturesConId = 0;
        m_autoCancelParent = false;
        m_shareholder = EMPTY_STR;
        m_imbalanceOnly = false;
        m_routeMarketableToBbo = false;
        m_parentPermId = 0;
        m_usePriceMgmtAlgo = null;
        m_duration = Integer.MAX_VALUE;
        m_postToAts = Integer.MAX_VALUE;
        m_advancedErrorOverride = EMPTY_STR;
        m_manualOrderTime = EMPTY_STR;
        m_minTradeQty = Integer.MAX_VALUE;
        m_minCompeteSize = Integer.MAX_VALUE;
        m_competeAgainstBestOffset = Double.MAX_VALUE;
        m_midOffsetAtWhole = Double.MAX_VALUE;
        m_midOffsetAtHalf = Double.MAX_VALUE;
        
        // custom fields
        m_placedAt = new Date();
    }

    public List<TagValue> algoParams() {
        if( m_algoParams == null ) {
            m_algoParams = new ArrayList<>();
        }
        return m_algoParams; 
    }


    @Override
    public int hashCode() {
        // Use m_permId only due to the definition of equals.
        return (int) (m_permId ^ (m_permId >>> 32));
    }

	public JsonObject getJsonLog(Contract contract) {
		return common.Util.toJson(
				"id", m_orderId,
				"permId", m_permId,
				"action", m_action, 
				"roundedQty", m_roundedQty, 
				"conid", contract.conid(), 
				"lmtPrice", m_lmtPrice,
				"exchange", contract.exchange(),
				"tif", m_tif,
				"outsideRth", m_outsideRth,
				"orderRef", m_orderRef,
				"all-or-none", m_allOrNone
				);
	}

    public int roundedQty() {
		return m_roundedQty;
	}
    
    public void roundedQty( int v) {
    	m_roundedQty = v;
    }

    public void flipSide() {
    	action( action() == Action.Buy ? Action.Sell : Action.Buy); 
	}
    
    public boolean isBuy() {
    	return action() == Action.Buy;
    }
	public void status(OrderStatus status) {
		m_status = status;
	}
	public OrderStatus status() {
		return m_status;
	}
	public Date placedAt() {
		return m_placedAt;
	}
	
}
