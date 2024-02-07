package id.idtrust.billing.repository;

import id.idtrust.billing.model.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import javax.persistence.LockModeType;

public interface BatchRepository extends JpaRepository<Batch, Integer> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Transactional()
    @Query(value = "SELECT b FROM Batch b WHERE id = :id")
    Batch findPostpaidByBatchId(@Param("id") int id);

    @Transactional()
    @Query(nativeQuery = true, value = "SELECT * FROM Batch WHERE account_id = :account_id and settled is false order by id DESC LIMIT 1")
    Batch findBatchByAccount(@Param("account_id") Long account_id);

    @Transactional()
    @Query(nativeQuery = true, value = "SELECT * FROM Batch WHERE account_id = :account_id and settled is false and free is false order by id DESC")
    List<Batch> findBatchByListAccount(@Param("account_id") Long account_id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Transactional()
    @Query(value = "SELECT b FROM Batch b WHERE account_id = :account_id and settled is false")
    Batch findPostpaidByAccountRecord(@Param("account_id") Long account_id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Transactional()
    @Query(value = "SELECT b FROM Batch b WHERE account_id = :account_id and settled is false order by id DESC")
    List<Batch> findPostpaidByAccountList(@Param("account_id") Long account_id);
}