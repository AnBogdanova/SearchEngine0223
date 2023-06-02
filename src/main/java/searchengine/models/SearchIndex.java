package searchengine.models;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@Table(name = "search_index")
public class SearchIndex
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Integer id;

    @OneToOne
    @JoinColumn(name = "page_id")
    private PageEntity page;

    @OneToOne
    @JoinColumn(name = "lemma_id")
    private Lemma lemma;

    @Column(name = "lemma_rank")
    private float rank;

}
