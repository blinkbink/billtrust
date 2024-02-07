package id.idtrust.billing.repository;

import id.idtrust.billing.model.Invoice;
import id.idtrust.billing.model.Period;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Table(name="invoice")
public interface ReportRepository extends JpaRepository<Invoice, Integer> {

    @Query("SELECT i from Invoice i, Product p, Account a where a.product.id = p.id and i.account.id = a.id and a.xkey=:xkey and p.pkey=:pkey order by i.createdDate DESC")
    Page<Invoice> findDefault(@Param("xkey") String xkey, @Param("pkey") String pkey, Pageable pageable);

    @Query("SELECT i from Invoice i, Product p, Account a where a.product.id = p.id and i.account.id = a.id and a.xkey=:xkey and p.pkey=:pkey and (DATE(i.createdDate) >= DATE(:start_date) and DATE(i.createdDate) <= DATE(:end_date)) order by i.createdDate DESC")
    Page<Invoice> findWithRangeDate(@Param("xkey") String xkey, @Param("pkey") String pkey, @Param("start_date") String start_date, @Param("end_date") String end_date, Pageable pageable);

    @Query(nativeQuery = true, value = "SELECT date_part('week', date_trunc('week', created_date) + interval '1 week') AS txn, abs(SUM(amount)) as total, date_part('year', created_date) AS txn_year, extract(month from created_date) as month from invoices where trx=2 and account_id = :account_id group by account_id, txn, txn_year, month order by txn ASC")
    List<Object[]> findWeekly(@Param("account_id") Long account_id);

    @Query(nativeQuery = true, value = "SELECT date_part('month', created_date) AS txn, abs(SUM(amount)) as total, date_part('year', created_date) AS txn_year from invoices where trx=2 and account_id = :account_id group by txn, account_id, txn_year order by txn ASC")
    List<Object[]> findMonthly(@Param("account_id") Long account_id);

    @Query(nativeQuery = true, value = "SELECT date_part('quarter', created_date) AS txn, abs(SUM(amount)) as total, date_part('year', created_date) AS txn_year from invoices where trx=2 and account_id = :account_id group by account_id, txn, txn_year order by txn ASC")
    List<Object[]> findQuarterly(@Param("account_id") Long account_id);

    @Query(nativeQuery = true, value = "SELECT date_part('year', created_date) AS txn, abs(SUM(amount)) as total from invoices where trx=2 and account_id = :account_id group by account_id, txn order by txn ASC")
    List<Object[]> findYearly(@Param("account_id") Long account_id);

}