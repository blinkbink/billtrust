package id.idtrust.billing.repository;

import id.idtrust.billing.model.TopupProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.Table;
import java.util.List;
@Repository
@Table(name = "topup_product")
public interface TopupProductRepository extends JpaRepository<TopupProduct, Long>, JpaSpecificationExecutor<TopupProduct> {

    @Query( value = "SELECT tp FROM TopupProduct tp WHERE topup.id=:id")
    List<TopupProduct> findByTopup(@Param("id") long id);


}
