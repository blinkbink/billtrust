package id.idtrust.billing.repository;

import id.idtrust.billing.model.Channel;
import id.idtrust.billing.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.Table;
import java.util.List;

@Repository
@Table(name="channel")
public interface ChannelRepository extends JpaRepository<Channel, Long> {
    @Query( value = "SELECT c FROM Channel c WHERE type != 'MANUAL'")
    List<Channel> getAll();

    @Query( value = "SELECT c FROM Channel c WHERE channel_code=:channel")
    Channel findChannel(@Param ("channel") String channel);

}