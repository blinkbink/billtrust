package id.idtrust.billing.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;

@Setter
@Getter
public class Period {

    @Column(name = "txn")
    private String txn;

    @Column(name = "account_id")
    private String account_id;

    @Column(name = "total")
    private String total;

}