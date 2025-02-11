package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "page", indexes = @Index(name = "path_index", columnList = "path"))
@NoArgsConstructor
@Getter
@Setter
public class PageModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "site_id", nullable = false)
    private int siteId;

    @Column(name = "path", columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;

    @Column(name = "code", nullable = false)
    private int code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @ManyToOne()
    @JoinColumn(name = "site_id", nullable = false, insertable = false, updatable = false)
    private SiteModel siteModel;
}
