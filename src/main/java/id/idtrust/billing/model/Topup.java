package id.idtrust.billing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "topup")
public class Topup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Access( AccessType.PROPERTY )
    private Long id;

    @Column(name = "created_date")
    private Date createdDate;

    @Column(name = "expired_date")
    private Date expiredDate;

    @Column(name = "price")
    private Integer price;

    @Column(name = "total")
    private Integer total;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "payment")
    private Payment payment;

    @ManyToOne()
    @JoinColumn(name = "account")
    private Account account;

    @ManyToOne()
    @JoinColumn(name = "plan")
    private Plan plan;

    @OneToMany(mappedBy = "topup")
    @JsonIgnore
    private Set<TopupProduct> topupProducts = new LinkedHashSet<>();

    @OneToMany(mappedBy = "topup")
    @JsonIgnore
    private Set<Invoice> invoices = new LinkedHashSet<>();

    @OneToMany(mappedBy = "topup")
    @JsonIgnore
    private Set<InvoiceFiles> invoiceFile = new LinkedHashSet<>();


}