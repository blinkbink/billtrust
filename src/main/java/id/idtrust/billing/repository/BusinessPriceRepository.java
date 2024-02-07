package id.idtrust.billing.repository;

import id.idtrust.billing.model.BusinessPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Table(name="business_price")
public interface BusinessPriceRepository extends JpaRepository<BusinessPrice, Long>, JpaSpecificationExecutor<BusinessPrice> {

    @Query(value = "SELECT b FROM Product p, Account a, BusinessPrice b  WHERE p.id = a.product and a.id = b.account and a.xkey=:xkey")
    List<BusinessPrice> bisnisFindByXkey(@Param("xkey") String xkey);

    @Query(value = "SELECT b FROM Product p, Account a, BusinessPrice b  WHERE p.id = a.product and a.id = b.account and a.id=:account_id")
    BusinessPrice bisnisFindByAccountId(@Param("account_id") Long account_id);


    @Query(value = "SELECT b FROM Product p, Account a, BusinessPrice b  WHERE p.id = a.product and a.id = b.account and a.xkey=:xkey and p.category=:category")
    List<BusinessPrice> bisnisFindByCategory(@Param("xkey") String xkey, @Param("category") String category);
}