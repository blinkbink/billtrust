package id.idtrust.billing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "channel")
public class Channel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;


    @Column(name = "channel_code", length = 10)
    private String channelCode;

    @Column(name = "type")
    private String type;

    @Column(name = "category")
    private String category;

    @Column(name = "image")
    private String image;


    @Column(name = "pixel")
    private String pixel;

    @OneToMany(mappedBy = "channel")
    @JsonIgnore
    private Set<Payment> payments = new LinkedHashSet<>();



}