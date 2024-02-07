package id.idtrust.billing.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Setter
@Getter
@Entity
@Table(name = "bill")
public class Bill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne()
    @JoinColumn(name = "batch")
    private Batch batch;

    @ManyToOne()
    @JoinColumn(name = "payment")
    private Payment payment;

    @Column(name = "total")
    private Integer total;

    @Column(name = "due_date")
    private Date dueDate;


    @Column(name = "status")
    private String status;

    @ManyToOne()
    @JoinColumn(name = "account_id")
    private Account account;

}