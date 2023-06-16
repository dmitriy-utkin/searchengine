package searchengine.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@EqualsAndHashCode
@Table(name = "pages", indexes = {@Index(name = "path_site_index", columnList = "path, site_id", unique = true)})
public class DBPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    //TODO: не работает индекс
    @Column(name = "path", columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private DBSite DBSite;

    @Column(nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;
}
