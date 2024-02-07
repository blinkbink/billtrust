package id.idtrust.billing.repository;

import id.idtrust.billing.model.InvoiceFiles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.stereotype.Repository;

@Repository
@Table(name="invoice_file")
public interface InvoiceFilesRepository extends JpaRepository<InvoiceFiles, Long>, JpaSpecificationExecutor<InvoiceFiles> {

}