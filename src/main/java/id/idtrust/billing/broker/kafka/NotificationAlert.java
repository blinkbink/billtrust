package id.idtrust.billing.broker.kafka;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationAlert {
	
	 @JsonProperty
	 public String name;
	 @JsonProperty
	 public String email;
	 @JsonProperty
	 public String user_id;
	 @JsonProperty
	 public String timestamp;
	 @JsonProperty
	 public List<DataNotification> data;
	 @JsonProperty
	 public String TRACE_ID;
	 @JsonProperty
	 private String traceInformation;


	 
	public String getTraceInformation() {
		return traceInformation;
	}
	public void setTraceInformation(String traceInformation) {
		this.traceInformation = traceInformation;
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
	

	public List<DataNotification> getData() {
		return data;
	}
	public void setData(List<DataNotification> data) {
		this.data = data;
	}


	
	public String getTRACE_ID() {
		return TRACE_ID;
	}
	public void setTRACE_ID(String tRACE_ID) {
		TRACE_ID = tRACE_ID;
	}


	public String getUser_id() {
		return user_id;
	}
	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	@Override
	public String toString() {
		return "NotificationAlert [name=" + name + ", email=" + email + ", user_id="
				+ user_id + ", timestamp=" + timestamp + ", data=" + data + ", TRACE_ID=" + TRACE_ID + "]";
	}

	
}