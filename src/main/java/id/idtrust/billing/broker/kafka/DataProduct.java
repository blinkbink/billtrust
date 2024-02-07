package id.idtrust.billing.broker.kafka;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DataProduct {
	
	 @JsonProperty
	 public Integer price;
	 @JsonProperty
	 public String transaction_date;
	 @JsonProperty
	 public String package_id;
	 @JsonProperty
	 public String package_name;
	 @JsonProperty
	 public String payment_method;
	 @JsonProperty
	 public List<DetailDataProduct> package_detail;
	 @JsonProperty
	 public String transaction_id;
	 @JsonProperty
	 public String expired_date;
	 @JsonProperty
	 public String link_xendit;
	 @JsonProperty
	 public String transaction_user_agent;
	 @JsonProperty
	 public String transaction_ip;
	 
	 
	 public String getTransaction_user_agent() {
		return transaction_user_agent;
	}
	public void setTransaction_user_agent(String transaction_user_agent) {
		this.transaction_user_agent = transaction_user_agent;
	}
	public String getTransaction_ip() {
		return transaction_ip;
	}
	public void setTransaction_ip(String transaction_ip) {
		this.transaction_ip = transaction_ip;
	}
	public String getLink_xendit() {
		return link_xendit;
	}
	public void setLink_xendit(String link_xendit) {
		this.link_xendit = link_xendit;
	}
	public String getExpired_date() {
		return expired_date;
	}
	public void setExpired_date(String expired_date) {
		this.expired_date = expired_date;
	}
	public String getTransaction_id() {
		return transaction_id;
	}
	public void setTransaction_id(String transaction_id) {
		this.transaction_id = transaction_id;
	}
	public Integer getPrice() {
			return price;
		}
	public void setPrice(Integer price) {
		this.price = price;
	}
	
	public String getTransaction_date() {
		return transaction_date;
	}
	public void setTransaction_date(String transaction_date) {
		this.transaction_date = transaction_date;
	}
	public String getPackage_id() {
		return package_id;
	}
	public void setPackage_id(String package_id) {
		this.package_id = package_id;
	}
	public String getPackage_name() {
		return package_name;
	}
	public void setPackage_name(String package_name) {
		this.package_name = package_name;
	}
	public String getPayment_method() {
		return payment_method;
	}
	public void setPayment_method(String payment_method) {
		this.payment_method = payment_method;
	}
	
	
	public List<DetailDataProduct> getPackage_detail() {
		return package_detail;
	}
	public void setPackage_detail(List<DetailDataProduct> package_detail) {
		this.package_detail = package_detail;
	}
	@Override
	public String toString() {
		return "DataProduct [price=" + price + ", transaction_date=" + transaction_date + ", package_id=" + package_id
				+ ", package_name=" + package_name + ", payment_method=" + payment_method + ", package_detail="
				+ package_detail + ", transaction_id=" + transaction_id + ", expired_date=" + expired_date
				+ ", link_xendit=" + link_xendit + ", transaction_user_agent=" + transaction_user_agent
				+ ", transaction_ip=" + transaction_ip + "]";
	}
	


}