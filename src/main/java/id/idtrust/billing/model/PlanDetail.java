package id.idtrust.billing.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Setter
@Getter
@Entity
@Table(name = "plan_detail")
public class PlanDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne()
    @JoinColumn(name = "product")
    private Product product;

    @Column(name = "price")
    private Integer price;

    @Column(name = "qty")
    private Integer qty;

    @Column(name = "unit_price")
    private Integer unitPrice;

    @ManyToOne()
    @JoinColumn(name = "plan")
    private Plan plan;

}