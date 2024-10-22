package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Table(name = "index_search")
@NoArgsConstructor
@Setter
@Getter
public class IndexModel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name = "page_id", nullable = false)
    private Integer pageId;

    @Column(name = "lemma_id", nullable = false)
    private Integer lemmaId;

    @Column(name = "lemma_rank", nullable = false)
    private Float lemmaRank;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "page_id", insertable = false, updatable = false, nullable = false)
    private PageModel pageModel;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "lemma_id", insertable = false, updatable = false, nullable = false)
    private LemmaModel lemmaModel;
}