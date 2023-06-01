package reflection;

import java.util.Random;

import org.json.simple.JSONObject;

// probably this should be a JSONObject
class LiveOrder {
	enum LiveOrderStatus { Working, Filled, Failed };

	private long m_finished;  // don't keep them forever. pas
	private String m_description;  // either order description or error description
	private String m_action;
	private boolean m_done;
	private LiveOrderStatus m_status;
	private int m_progress;
	private RefCode m_errorCode;
	private String m_id = Util.id(5);

	LiveOrder(String action, String description) {
		m_action = action;
		m_description = description;
		m_status = LiveOrderStatus.Working;
	}
	
	void failed(Exception e) {
		m_status = LiveOrderStatus.Failed;

		m_description = e.getMessage();
		if (e instanceof RefException) {
			m_errorCode = ((RefException)e).code();
		}
		m_finished = System.currentTimeMillis();
	}

	void filled(double stockTokenQty) {
		m_status = LiveOrderStatus.Filled;
		m_description = m_description
				.replace("Buy", "Bought")
				.replace("Sell", "Sold");
		m_finished = System.currentTimeMillis();
	}

//	public String action() {
//		return m_action;
//	}
//
//	public String description() {
//		return m_description;
//	}

	public int progress() {
		return 25;
	}

	public String msgType() {
		return m_status == LiveOrderStatus.Failed ? "error" : "message";
	}

	public LiveOrderStatus status() {
		return m_status;
	}

//	public RefCode errorCode() {
//		return m_errorCode;
//	}
//
	public String id() {
		return m_id;
	}

	static Random r = new Random();
	public JSONObject getWorkingOrder() {
		JSONObject order = new JSONObject();
		order.put( "id", m_id);
		order.put( "action", m_action);
		order.put( "description", m_description);
		order.put( "progress", r.nextInt(101) ); //m_progress);
		return order;
	}

	public Object getCompletedOrder() {
		JSONObject order = new JSONObject();
		order.put( "id", m_id);
		order.put( "type", msgType() );   
		order.put( "text", m_description);
		order.put( "status", m_status.toString() );
		if (m_errorCode != null) {
			order.put( "errorCode", m_errorCode.toString() );
		}
		return order;
	}
}
