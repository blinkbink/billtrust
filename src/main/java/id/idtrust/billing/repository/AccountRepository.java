package id.idtrust.billing.repository;

import id.idtrust.billing.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;
@Repository
@Table(name="account")
public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {

    @Query( value = "SELECT a FROM Account a WHERE xkey=:xkey")
    List<Account> findAccount(@Param("xkey") String xkey);

    @Query( value = "SELECT a FROM Account a, Topup t, Plan p where a.id = t.account.id and t.plan.id = p.id and a.xkey=:xkey and t.plan.type='starter'")
    List<Account> findAccountStarter(@Param("xkey") String xkey);

//    @Query( value = "SELECT a FROM Account a, Batch b, Product p WHERE xkey=:xkey and a.id = b.account.id and a.product.id = p.id and p.pkey != 'emeterai'")
//    List<Account> findAccountStarter(@Param("xkey") String xkey);

    @Query( value = "SELECT a FROM Account a WHERE xkey=:xkey")
    List<Account> findSingleAccount(@Param("xkey") String xkey);

    @Query( value = "SELECT a FROM Account a WHERE xkey=:xkey AND product.id=:product")
    Account findAccountByProduct(@Param("xkey") String xkey, @Param("product") Long product);

    @Query( value = "SELECT a FROM Account a WHERE xkey=:xkey AND product.id=:product")
    List<Account> findAccountByProductList(@Param("xkey") String xkey, @Param("product") Long product);

    @Query( value = "SELECT a FROM Account a WHERE xkey=:xkey AND product.id in :product")
    List<Account> findAccountByProductInList(@Param("xkey") String xkey, @Param("product") List<Long> product);

    @Query( value = "SELECT a FROM Account a WHERE xkey=:xkey AND product.category in :category")
    List<Account> findAccountByCategoryInList(@Param("xkey") String xkey, @Param("category") List<String> category);

    @Query( value = "SELECT a FROM Account a, Product p WHERE a.product = p.id AND xkey=:xkey AND p.category = :category")
    List<Account> findAccountByCategoryList(@Param("xkey") String xkey, @Param("category") String category);

    @Query( value = "SELECT a FROM Account a, Product p WHERE a.product.id=p.id and a.xkey=:xkey AND p.id=:product and a.active is true and p.pkey!='sms'")
    Account findAccountActiveByProduct(@Param("xkey") String xkey, @Param("product") Long product);

    @Query( value = "SELECT a FROM Account a WHERE xkey=:xkey")
    List<Account> findAccountByList(@Param("xkey") String xkey);

    @Query( value = "SELECT a FROM Account a WHERE xkey=:xkey AND product.id=:product")
    Account findByExternalKey(@Param("xkey") String xkey, @Param("product") Long product);

    @Query( value = "SELECT a FROM Account a WHERE xkey=:xkey AND (product.pkey='verification' or product.pkey='certificate' or product.pkey='sms')")
    List<Account> findByCertificateVerification(@Param("xkey") String xkey);

    @Query( value = "SELECT a FROM Account a WHERE xkey=:xkey AND product.id=:product and active is true")
    Account findByExternalKeyActive(@Param("xkey") String xkey, @Param("product") Long product);

    @Query( value = "SELECT a FROM Account a WHERE id=:id")
    Account findByIds(@Param("id") long id);
}