package id.idtrust.billing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "batch")
public class Batch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne()
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "open_date")
    private Date openDate;

    @Column(name = "closing_date")
    private Date closingDate;

    @Column(name = "name_batch")
    private String nameBatch;

    @Column(name = "free")
    private boolean free;

    @Column(name = "settled")
    private Boolean settled;

    @Column(name = "usage")
    private Integer usage;

    @Column(name = "quota")
    private Integer quota;

    @Column(name = "remaining_balance")
    private Integer remainingBalance;

    @Column(name = "price")
    private Integer price;

    @Column(name = "expired")
    private Date expired;

    @OneToMany(mappedBy = "batch")
    @JsonIgnore
    private Set<Bill> bills = new LinkedHashSet<>();

    @OneToMany(mappedBy = "batch")
    @JsonIgnore
    private Set<Invoice> invoice = new LinkedHashSet<>();
}