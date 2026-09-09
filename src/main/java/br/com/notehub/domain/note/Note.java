package br.com.notehub.domain.note;

import br.com.notehub.domain.comment.Comment;
import br.com.notehub.domain.flame.Flame;
import br.com.notehub.domain.tag.Tag;
import br.com.notehub.domain.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "notes")
@NoArgsConstructor
@Data
@JsonIgnoreProperties({"user", "tags", "comments", "flames"})
@ToString(exclude = {"user", "tags", "comments", "flames"})
@EqualsAndHashCode(exclude = {"user", "tags", "comments", "flames"})
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User user;

    private Instant createdAt = Instant.now();

    private Instant modifiedAt = Instant.now();

    private String name;

    private String fullName;

    private String description;

    @Column(columnDefinition = "TEXT")
    private String markdown;

    private boolean modified = false;

    private boolean closed;

    private boolean hidden;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(name = "note_tags", joinColumns = @JoinColumn(name = "note_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<Tag> tags = new ArrayList<>();

    @OneToMany(mappedBy = "note", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();
    private int commentsCount = 0;

    @OneToMany(mappedBy = "note", orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Flame> flames = new HashSet<>();
    private int flamesCount = 0;

    public Note(User user, String name, String description, String markdown, boolean closed, boolean hidden, List<Tag> tags) {
        this.user = user;
        this.name = name;
        this.fullName = String.format("%s/%s", user.getUsername(), name);
        this.closed = closed;
        this.hidden = hidden;
        if (description != null) this.description = description;
        if (markdown != null) this.markdown = markdown;
        if (tags != null) this.tags = tags;
    }

}