package id.idtrust.billing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "xkey")
    private String xkey;

    @Column(name = "internal_email")
    private String internal_email;
    @Column(name = "email")
    private String email;


    @Column(name = "name")
    private String name;


    @Column(name = "address")
    private String address;


    @Column(name = "company")
    private String company;

    @Column(name = "type")
    private Integer type;

    @Column(name = "created_date")
    private Date createdDate;

    @Column(name = "updated_date")
    private Date updatedDate;

    @ManyToOne()
    @JoinColumn(name = "product")
    private Product product;

    @Column(name = "threshold")
    private Integer threshold;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "alert_balance")
    private Boolean alertBalance;

    @Column(name = "is_alert_balance")
    private Boolean isAlertBalance;

    @Column(name = "subscription")
    private String subscription;

    @Column(name = "bill_period")
    private Integer billPeriod;

    @Column(name = "parent")
    private Long parent;

    @OneToMany(mappedBy = "account")
    @JsonIgnore
    private Set<Bill> bills = new LinkedHashSet<>();

//    @OneToMany(mappedBy = "account")
//    @JsonIgnore
//    private Set<BusinessPrice> businessPrices = new LinkedHashSet<>();

    public Set<Bill> getBills() {
        return bills;
    }

    public void setBills(Set<Bill> bills) {
        this.bills = bills;
    }
}