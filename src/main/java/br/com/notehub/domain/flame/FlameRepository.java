package br.com.notehub.domain.flame;

import br.com.notehub.domain.note.Note;
import br.com.notehub.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlameRepository extends JpaRepository<Flame, UUID> {

    Optional<Flame> findByUserIdAndNoteId(UUID userId, UUID noteId);

    boolean existsByUserAndNote(User user, Note note);

    @Query("""
            SELECT DISTINCT f FROM Flame f
            LEFT JOIN FETCH f.user u
            LEFT JOIN FETCH f.note n
            LEFT JOIN FETCH n.user nu
            LEFT JOIN FETCH n.tags t
            WHERE u.username = :username
            AND n.hidden = false
            AND (
                (:q IS NULL OR LOWER(nu.username) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
                OR (:q IS NULL OR LOWER(nu.displayName) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
                OR (:q IS NULL OR LOWER(n.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
                OR (:q IS NULL OR LOWER(n.description) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
                OR (:q IS NULL OR EXISTS (
                    SELECT 1 FROM Tag t
                    JOIN t.notes tn
                    WHERE tn.id = n.id
                    AND LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                   ))
            )
            """)
    Page<Flame> getUserFlames(Pageable pageable, @Param("username") String username, @Param("q") String q);

}