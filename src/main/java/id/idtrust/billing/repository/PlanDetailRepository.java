package id.idtrust.billing.repository;

import id.idtrust.billing.model.Plan;
import id.idtrust.billing.model.PlanDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.Table;
import java.util.List;

@Repository
@Table(name="plan_detail")
public interface PlanDetailRepository extends JpaRepository<PlanDetail, Integer> {

    @Query( value = "SELECT pd FROM PlanDetail pd WHERE plan.id=:plan")
    List<PlanDetail> findById(@Param("plan") Long plan);

    @Query( value = "SELECT pd FROM PlanDetail pd WHERE plan.id=:plan")
    List<PlanDetail> findByIds(@Param("plan") Long plan);
    @Query( value = "SELECT pd FROM PlanDetail pd WHERE product.id=:product")
    List<PlanDetail> findByProduct(@Param("product") Long product);
}