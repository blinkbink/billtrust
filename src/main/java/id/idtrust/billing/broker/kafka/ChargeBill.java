package id.idtrust.billing.broker.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChargeBill {


    /*
    {
    "xkey": "63dcaf69e5cfd2a90aed4719",
    "product": "text",
    "amount": "1",
    "items":"id transaksi/id table/id dokumen/id sms",
    "description": "IDTRA001",
     "TRACE_ID":"1-63d21121-18959e163c80fe616fb0f704,Root=1-63d21121-18959e163c80fe616fb0f704;Parent=1-63d103a0-606d21ab31c6d4a66de5b3d1"
}
     */

    @JsonProperty
    private String xkey;
    @JsonProperty
    private String product;
    @JsonProperty
    private String amount;
    @JsonProperty
    private String items;
    @JsonProperty
    private String description;
    @JsonProperty
    private String TRACE_ID;

    public String getXkey() {
        return xkey;
    }

    public void setXkey(String xkey) {
        this.xkey = xkey;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getItems() {
        return items;
    }

    public void setItems(String items) {
        this.items = items;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTRACE_ID() {
        return TRACE_ID;
    }

    public void setTRACE_ID(String TRACE_ID) {
        this.TRACE_ID = TRACE_ID;
    }
}
