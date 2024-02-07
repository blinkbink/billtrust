package id.idtrust.billing.repository;

import id.idtrust.billing.model.Invoice;
import id.idtrust.billing.model.Plan;
import id.idtrust.billing.model.SPBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Table
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query(value = "SELECT i FROM Invoice i where invoice_id = :invoiceid and trx='2' order by id asc")
    List<Invoice> findByInvoiceId(@Param("invoiceid") String invoiceid);

    @Query(value = "SELECT COUNT(i) FROM Invoice i, Account a, Product p where a.product.id = p.id and i.account.id = a.id and i.referral=:referral and p.id = :product and i.trx='1'")
    Integer Verification(@Param("referral") String referral, @Param("product") Long product);

    @Query(value = "SELECT i FROM Invoice i where invoice_id = :invoiceid AND trx = '3'")
    List<Invoice> findTransactionReversal(@Param("invoiceid") String invoiceid);

    @Query(value = "SELECT i FROM Invoice i WHERE item_id=:item and trx='2' ORDER BY created_date DESC")
    List<Invoice> getItemcheck(@Param("item") Long item);

    @Query(value = "SELECT i FROM  Invoice i WHERE item_id=:item and trx='2' and invoice_id=:invoiceid ORDER BY created_date DESC")
    List<Invoice> getItem(@Param("item") Long item, @Param("invoiceid") String invoiceid);

    @Query(value = "SELECT i FROM  Invoice i WHERE item_id=:item and trx='3' and invoice_id=:invoiceid ORDER BY created_date DESC")
    List<Invoice> getItemReversal(@Param("item") Long item, @Param("invoiceid") String invoiceid);

    @Query(value = "SELECT i FROM Invoice i WHERE item_id=:item and trx='3' ORDER BY created_date DESC")
    List<Invoice> getItemReversalcheck(@Param("item") Long item);

    @Query(nativeQuery = true,value = "SELECT * FROM testfunction2(:xkey, :amount)")
    String SPBalanceMines(@Param("xkey") Long item, @Param("amount") int amount);

    @Query(nativeQuery = true,value = "SELECT * FROM testfunction3(:xkey, :amount, :batch)")
    String SPBalancePlus(@Param("xkey") Long item, @Param("amount") int amount, @Param("batch") int batch);

}