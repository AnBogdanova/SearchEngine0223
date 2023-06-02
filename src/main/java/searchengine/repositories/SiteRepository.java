package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.models.SiteEntity;
import searchengine.models.StatusType;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity,Integer>, Serializable
{
    Optional<List<SiteEntity>> findAllByStatus(StatusType statusType);

    SiteEntity findByUrl(String url);

    boolean existsByUrl(String url);

    boolean existsByStatus(StatusType statusType);
}
