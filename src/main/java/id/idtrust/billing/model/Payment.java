package id.idtrust.billing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne()
    @JoinColumn(name = "channel")
    private Channel channel;

    @Column(name = "payment_date")
    private Date paymentDate;


    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "payment_code")
    private String paymentCode;


    @Column(name = "transaction_id")
    private String transactionId;


    @Column(name = "source")
    private String source;

    @OneToMany(mappedBy = "payment")
    @JsonIgnore
    private Set<Topup> topups = new HashSet<>();

    @OneToMany(mappedBy = "payment")
    @JsonIgnore
    private Set<ProofOfPayment> proofOfPayments = new LinkedHashSet<>();

    @OneToMany(mappedBy = "payment")
    @JsonIgnore
    private Set<Bill> bills = new LinkedHashSet<>();


}