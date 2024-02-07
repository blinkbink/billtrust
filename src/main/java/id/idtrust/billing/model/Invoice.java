package id.idtrust.billing.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Setter
@Getter
@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne()
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "referral")
    private String referral;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "created_date")
    private Date createdDate;

    @Column(name = "description")
    private String description;

    @NotNull
    @Column(name = "invoice_id", nullable = false)
    private String invoiceId;

//    @Column(name = "type")
//    private String type;

    @Column(name = "trx")
    private Integer trx;

    @ManyToOne()
    @JoinColumn(name = "batch")
    private Batch batch;

    @Column(name = "current_balance")
    private Integer currentBalance;

    @Column(name = "item_id")
    private String itemId;

    @ManyToOne()
    @JoinColumn(name = "plan")
    private Plan plan;

    @ManyToOne(cascade=CascadeType.ALL)
    @JoinColumn(name = "topup")
    private Topup topup;

}