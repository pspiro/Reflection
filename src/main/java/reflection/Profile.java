package reflection;

import org.json.simple.JsonObject;

public class Profile {
	private JsonObject m_object;

	public Profile(JsonObject object) {
		m_object = object;
	}
	
	String getName() {
		return m_object.getString("name");
	}

	String getAddress() {
		return m_object.getString("address");
	}

	String getEmail() {
		return m_object.getString("email");
	}

	String getPhone() {
		return m_object.getString("phone");
	}

	String getPan() {
		return m_object.getString("pan_number");
	}
	
	void validate() {
		
	}

}
