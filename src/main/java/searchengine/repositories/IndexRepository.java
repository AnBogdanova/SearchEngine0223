package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.models.SearchIndex;

import javax.transaction.Transactional;
import java.io.Serializable;

@Repository
public interface IndexRepository extends JpaRepository<SearchIndex, Long>, Serializable {

    boolean existsByPageId(int pageId);

    @Query("SELECT sum(s.rank) FROM SearchIndex s WHERE s.page.id = :pageId")
    double absoluteRelevanceByPageId(int pageId);

    @Transactional
    @Modifying
    void deleteAllByPageId(int pageId);
}
