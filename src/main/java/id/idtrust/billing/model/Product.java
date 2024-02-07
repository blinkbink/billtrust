package id.idtrust.billing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "pkey", nullable = false)
    private String pkey;

    @Column(name = "created_date")
    private Date createdDate;


    @Column(name = "name_product")
    private String nameProduct;


    @Column(name = "name_product_en")
    private String nameProductEn;


    @Column(name = "detail_id")
    private String detailId;

    @Column(name = "category")
    private String category;

    @Column(name = "detail_en")
    private String detailEn;

    @Column(name = "base_price")
    private Integer base_price;

    @Column(name = "personal_base_price")
    private Integer personal_base_price;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private Set<Account> accounts = new LinkedHashSet<>();



}