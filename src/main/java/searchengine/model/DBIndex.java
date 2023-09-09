package searchengine.model;

import lombok.*;
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
@ToString(exclude = {"dbPage", "dbLemma"})
@Transactional
@Table(name = "indexes")
@DependsOn({"pages", "lemmas"})
public class DBIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private DBPage dbPage;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private DBLemma dbLemma;

    @Column(name = "lemma_rank", nullable = false)
    private int rank;
}
