package id.idtrust.billing.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Setter
@Getter
@Entity
@Table(name = "topup_product")
public class TopupProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne()
    @JoinColumn(name = "topup")
    private Topup topup;

    @ManyToOne()
    @JoinColumn(name = "product")
    private Product product;

    @Column(name = "qty")
    private Integer qty;

    @Transient
    private Integer unit_price;

    @Column(name = "price")
    private Integer price;

    @ManyToOne()
    @JoinColumn(name = "account")
    private Account account;


}