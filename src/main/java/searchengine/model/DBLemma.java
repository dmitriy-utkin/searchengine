package searchengine.model;

import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.context.annotation.DependsOn;

import javax.persistence.*;
import javax.transaction.Transactional;

@Entity
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"dbSite"})
@Transactional
@Table(name = "lemmas")
@DependsOn("sites")
@DynamicUpdate
public class DBLemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private DBSite dbSite;

    @Column(name = "lemma", columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;

}
