package id.idtrust.billing.broker.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Payment {

	@JsonProperty
	public String name;
	@JsonProperty
	public String email;

	@JsonProperty
	public DataProduct data;
	@JsonProperty
	public String TRACE_ID;
	@JsonProperty
	public String user_id;
	@JsonProperty
	public String timestamp;


	public String getTRACE_ID() {
		return TRACE_ID;
	}

	public void setTRACE_ID(String tRACE_ID) {
		TRACE_ID = tRACE_ID;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	

	public DataProduct getData() {
		return data;
	}

	public void setData(DataProduct data) {
		this.data = data;
	}


	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getUser_id() {
		return user_id;
	}

	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}

	@Override
	public String toString() {
		return "Payment [name=" + name + ", email=" + email + ", data=" + data
				+ ", TRACE_ID=" + TRACE_ID + ", user_id=" + user_id + ", timestamp=" + timestamp + "]";
	}

	
	
}