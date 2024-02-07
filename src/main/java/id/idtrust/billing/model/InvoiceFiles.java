package id.idtrust.billing.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Setter
@Getter
@Entity
@Table(name = "invoice_file")
public class InvoiceFiles {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "path")
    private String path;

    @ManyToOne()
    @JoinColumn(name = "topup")
    private Topup topup;

}