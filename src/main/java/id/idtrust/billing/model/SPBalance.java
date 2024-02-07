package id.idtrust.billing.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

@Setter
@Getter
public class SPBalance {

    @Column(name = "newbalance")
    private Integer newbalance;


}