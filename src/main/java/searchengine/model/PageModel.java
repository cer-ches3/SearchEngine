package searchengine.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "page",  indexes = @Index(columnList = "path"))
@Data
public class PageModel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Integer id;

    @Column(name = "path", columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;

    @Column(name = "code", nullable = false)
    private Integer code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @Column(name = "site_id", nullable = false)
    private Integer siteId;
}
