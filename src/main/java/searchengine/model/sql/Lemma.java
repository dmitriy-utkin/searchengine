package searchengine.model.sql;

import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.context.annotation.DependsOn;

import javax.persistence.*;
import javax.transaction.Transactional;

@Entity
@Getter
@Setter
@Builder
@EqualsAndHashCode(of = {"id", "lemma"})
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"site"})
@Transactional
@Table(name = "lemmas")
@DependsOn("sites")
@DynamicUpdate
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "lemma", columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;

}
