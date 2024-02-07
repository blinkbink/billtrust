package id.idtrust.billing.repository;

import id.idtrust.billing.model.Topup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import javax.persistence.Table;

@Repository
@Table(name = "topup")
public interface TopupRepository extends PagingAndSortingRepository<Topup, Long>, JpaSpecificationExecutor<Topup> {

    @Query( value = "SELECT t FROM Topup t WHERE id=:id")
    Topup findById(@Param("id") long id);

    @Query( value = "SELECT t FROM Topup t WHERE id=:id")
    Topup findByIds(@Param("id") long id);

    @Query(nativeQuery = true, value = "SELECT * FROM topup t, accounts a WHERE a.id = t.account and a.xkey=:xkey and t.plan is not Null and t.status=:status order by t.created_date DESC")
    Page<Topup> findByAccountPaging(@Param("xkey") String xkey, @Param("status") String status, Pageable pageable);

    @Query(nativeQuery = true, value = "SELECT * FROM topup t, accounts a WHERE a.id = t.account and a.xkey=:xkey and t.plan is Null and t.status=:status order by t.created_date DESC")
    Page<Topup> findByBisnisPaging(@Param("xkey") String xkey, @Param("status") String status, Pageable pageable);

    @Query( value = "SELECT t FROM Topup t WHERE account.id=:id and plan != null")
    List<Topup> findByAccount(@Param("id") long id);

    @Query( value = "SELECT t FROM Topup t WHERE account.id=:id and plan != null and account.product.pkey = 'emeterai'")
    List<Topup> findByAccountEmeterai(@Param("id") long id);

    @Query(nativeQuery = true, value = "SELECT t FROM accounts a, topup t WHERE a.xkey=:xkey and t.plan is not null and a.product.pkey = 'emeterai' and t.status=:status")
    Page<Topup> findByAccountEmeteraiPaging(@Param("xkey") String xkey, @Param("status") String status, Pageable pageable);

    @Query(nativeQuery = true, value = "SELECT t FROM accounts a, topup t WHERE a.xkey=:xkey and t.plan is null and a.product.pkey = 'emeterai' and t.status=:status")
    Page<Topup> findByBisnisEmeteraiPaging(@Param("xkey") String xkey, @Param("status") String status, Pageable pageable);

    @Query( value = "SELECT t FROM Topup t WHERE t.account.xkey=:xkey and t.status!='VOID' and t.plan.type = 'starter'")
    List<Topup> userHasTopupStarter(@Param("xkey") String xkey);

    @Query( value = "SELECT t FROM Topup t WHERE t.account.xkey=:xkey and t.plan.type = 'starter' and t.status = 'WAITING_PAYMENT'")
    List<Topup> userHasTopupStarterWaitingPayment(@Param("xkey") String xkey);

    @Query(nativeQuery = true, value = "SELECT * FROM topup(:id, :amount)")
    int SPTopup(@Param("id") long id, @Param("amount") int amount);
}
