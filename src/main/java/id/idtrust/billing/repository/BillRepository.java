package id.idtrust.billing.repository;

import id.idtrust.billing.model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillRepository extends JpaRepository<Bill, Integer> {

    @Query(value = "SELECT b FROM Bill b WHERE account_id = :account_id and status='UNPAID'")
    Bill findByAccountId(@Param("account_id") Long account_id);

    @Query(value = "SELECT b FROM Bill b WHERE id = :id and status='UNPAID'")
    Bill findById(@Param("id") int id);
}