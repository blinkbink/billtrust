package id.idtrust.billing.repository;

import id.idtrust.billing.model.Account;
import id.idtrust.billing.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.Table;
import java.util.List;

@Repository
@Table(name="plan")
public interface PlanRepository extends JpaRepository<Plan, Long> {

    @Query( value = "SELECT p FROM Plan p WHERE id=:plan")
    List<Plan> findByIdList(@Param("plan") Long plan);

    @Query( value = "SELECT p FROM Plan p WHERE id=:plan")
    Plan findByIds(@Param("plan") Long plan);

    @Query( value = "SELECT p FROM Plan p WHERE xkey=:xkey")
    List<Plan> findByXkey(@Param("xkey") String xkey);

    @Query( value = "SELECT p FROM Plan p WHERE type=:type")
    List<Plan> findByType(@Param("type") String type);
}