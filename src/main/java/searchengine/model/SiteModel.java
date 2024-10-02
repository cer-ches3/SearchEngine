package searchengine.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "site")
@Data
public class SiteModel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusIndexing status;

    @Column(name = "status_time", columnDefinition = "DATETIME", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError = null;

    @Column(name = "url", columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

    @Column(name = "name", columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "site_id")
    private List<PageModel> pages;
}
