package searchengine.model.sql;

import lombok.*;
import org.springframework.context.annotation.DependsOn;

import javax.persistence.*;
import javax.persistence.Index;
import javax.transaction.Transactional;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"site", "code", "content"})
@EqualsAndHashCode(of = {"id", "path"})
@Transactional
@Table(name = "pages", indexes = {@Index(name = "path_site_index", columnList = "site_id, path", unique = true)})
@DependsOn("sites")
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "path", columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;
}
