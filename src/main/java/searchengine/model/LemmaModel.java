package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Table(name = "lemma", uniqueConstraints = @UniqueConstraint(columnNames = {"lemma", "site_id"}))
@NoArgsConstructor
@Data
public class LemmaModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "frequency", nullable = false)
    private int frequency;

    @Column(name = "lemma", columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(name = "site_id", nullable = false)
    private int siteId;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "site_id", insertable = false, updatable = false, nullable = false)
    private SiteModel siteModel;

}
