package searchengine.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "lemma")
@Data
public class LemmaModel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name = "site_id", nullable = false)
    private Integer siteId;

    @Column(name = "lemma", columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private Integer frequency;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "site_id", insertable = false, updatable = false, nullable = false)
    private SiteModel siteModel;
}
