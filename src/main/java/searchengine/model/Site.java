package searchengine.model;

import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import javax.transaction.Transactional;
import java.util.Date;

@Entity
@Getter
@Setter
@Builder
@EqualsAndHashCode(of ={"id", "url"})
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"statusTime", "lastError"})
@Transactional
@Table(name = "sites")
@DynamicUpdate
@Cacheable
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private Status status;

    @Column(name = "status_time", nullable = false)
    private Date statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;
}


