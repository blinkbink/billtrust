package id.idtrust.billing.repository;

import id.idtrust.billing.model.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.Table;

@Repository
@Table(name="balance")
public interface BalanceRepository extends JpaRepository<Balance, Long>, JpaSpecificationExecutor<Balance> {

    @Query( value = "SELECT b FROM Balance b WHERE account_id = :account_id")
    Balance saldo(@Param("account_id") Long AccountId);
}
