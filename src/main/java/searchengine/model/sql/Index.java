package searchengine.model.sql;

import lombok.*;
import org.springframework.context.annotation.DependsOn;

import javax.persistence.*;
import javax.transaction.Transactional;

@Entity
@Getter
@Setter
@Builder
@EqualsAndHashCode(of = {"id"})
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"page", "lemma"})
@Transactional
@Table(name = "indexes", indexes = {@javax.persistence.Index(name = "lemma_index", columnList = "lemma_id")})
@DependsOn({"pages", "lemmas"})
@NamedEntityGraph(name = "indexWithPageAndSite", attributeNodes = {@NamedAttributeNode(value = "page",
                                                    subgraph = "pagesWithSites")},
                                                    subgraphs = {@NamedSubgraph(name = "pagesWithSites",
                                                            attributeNodes = @NamedAttributeNode(value = "site"))})
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @Column(name = "lemma_rank", nullable = false)
    private int rank;
}
