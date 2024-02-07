package id.idtrust.billing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "plan")
@Setter
@Getter
@Transactional(rollbackFor = Exception.class)
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "xkey")
    private String xkey;

    @Column(name = "name")
    private String name;
    @Column(name = "total")
    private Integer total;

    @Column(name = "ppn")
    private Integer ppn;

    @Column(name = "admin_fee")
    private Integer admin_fee;

    @Column(name = "title_id")
    private String title_id;

    @Column(name = "title_en")
    private String title_en;

    @Column(name = "type")
    private String type;

    @Column(name = "bundle_price")
    private Integer bundlePrice;

    @Column(name = "free")
    private boolean free;

    @Column(name= "recommended")
    private Boolean recommended;

    @OneToMany(mappedBy = "plan")
    @JsonIgnore
    private Set<Topup> topups = new LinkedHashSet<>();

    @OneToMany(mappedBy = "plan")
    @JsonIgnore
    private Set<PlanDetail> planDetails = new LinkedHashSet<>();

}