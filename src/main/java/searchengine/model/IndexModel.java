package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id" , nullable = false)
    private int id;

    @Column(name = "page_id", nullable = false)
    private int pageId;

    @Column(name = "lemma_id", nullable = false)
    private int lemmaId;

    @Column(name = "lemma_rank", nullable = false)
    private int lemmaCount;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "page_id", insertable = false, updatable = false, nullable = false)
    private PageModel pageModel;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "lemma_id", insertable = false, updatable = false, nullable = false)
    private LemmaModel lemmaModel;

    @Override
    public String toString() {
        return "IndexModel{" +
                "id=" + id +
                ", pageId=" + pageId +
                ", lemmaId=" + lemmaId +
                ", lemmaCount=" + lemmaCount +
                '}';
    }
}