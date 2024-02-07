package id.idtrust.billing.broker.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;


public class DataNotification {
	
	 @JsonProperty
	 public String type_balance;
	 @JsonProperty
	 public String type_balance_en;
	 @JsonProperty
	 public Integer balance;


	public String getType_balance() {
		return type_balance;
	}
	public void setType_balance(String type_balance) {
		this.type_balance = type_balance;
	}
	public String getType_balance_en() {
		return type_balance_en;
	}
	public void setType_balance_en(String type_balance_en) {
		this.type_balance_en = type_balance_en;
	}

	public Integer getBalance() {
		return balance;
	}

	public void setBalance(Integer balance) {
		this.balance = balance;
	}

	@Override
	public String toString() {
		return "DataNotification [type_balance=" + type_balance + ", type_balance_en=" + type_balance_en + ", balance="
				+ balance + "]";
	}
	 
	 
	 
}