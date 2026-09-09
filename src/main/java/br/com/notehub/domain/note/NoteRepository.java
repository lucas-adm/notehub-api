package br.com.notehub.domain.note;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {

    @Query("""
                SELECT n FROM Note n
                JOIN n.user u
                WHERE u.username = :username AND n.name = :name
            """)
    Optional<Note> findByUserUsernameAndName(String username, String name);

    Optional<Note> findByFullName(String fullName);

    @Query("""
            SELECT DISTINCT n FROM Note n
            LEFT JOIN FETCH n.user u
            LEFT JOIN FETCH n.tags t
            WHERE (u IS NULL OR u.profilePrivate = false)
            AND n.hidden = false
            AND (
                :q IS NULL OR LOWER(n.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR
                :q IS NULL OR LOWER(n.description) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
            )
            """)
    Page<Note> searchPublicNotesByNameOrDescription(Pageable pageable, @Param("q") String q);

    @Query("""
            SELECT DISTINCT n FROM Note n
            LEFT JOIN FETCH n.user u
            LEFT JOIN FETCH n.tags t
            WHERE (u IS NULL OR u.profilePrivate = false)
            AND n.hidden = false
            AND (
                EXISTS (
                    SELECT 1 FROM Tag tag
                    JOIN tag.notes note
                    WHERE note.id = n.id
                    AND (
                    :q IS NULL OR
                    LOWER(tag.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                    )
                )
            )
            """)
    Page<Note> searchPublicNotesByTag(Pageable pageable, @Param("q") String q);

    @Query("""
            SELECT DISTINCT n FROM Note n
            LEFT JOIN FETCH n.user u
            LEFT JOIN FETCH n.tags t
            WHERE u.id = :id
            AND (
                LOWER(n.name) LIKE LOWER(CONCAT('%', :q, '%'))
                OR
                EXISTS (
                    SELECT 1 FROM Tag tag
                    JOIN tag.notes note
                    WHERE note.id = n.id AND LOWER(tag.name) LIKE LOWER(CONCAT('%', :q, '%'))
                )
            )
            """)
    Page<Note> searchPrivateNotesByNameOrTag(Pageable pageable, @Param("id") UUID id, @Param("q") String q);

    @Query("""
            SELECT DISTINCT n FROM Note n
            LEFT JOIN FETCH n.user u
            LEFT JOIN FETCH n.tags t
            WHERE
            u.id = :id
            AND
            EXISTS (
                SELECT 1 FROM Tag tag
                JOIN tag.notes note
                WHERE note.id = n.id AND LOWER(tag.name) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            """)
    Page<Note> searchPrivateNotesByTag(Pageable pageable, UUID id, String q);

    @Query("""
            SELECT DISTINCT n FROM Note n
            LEFT JOIN FETCH n.user u
            LEFT JOIN FETCH n.tags t
            WHERE u.username = :username
            AND (
                :q IS NULL OR LOWER(n.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR
                :q IS NULL OR LOWER(n.description) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
            )
            AND (:tag IS NULL OR EXISTS (
                SELECT 1 FROM Tag t
                JOIN t.notes tn
                WHERE tn.id = n.id
                AND LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:tag AS text), '%'))
            ))
            AND (:type IS NULL AND n.hidden = false OR (
                (:type = 'open' AND n.closed = false AND n.hidden = false) OR
                (:type = 'closed' AND n.closed = true AND n.hidden = false) OR
                (:type = 'hidden' AND n.hidden = true)
            ))
            """)
    Page<Note> searchUserNotesBySpecs(Pageable pageable,
                                      @Param("username") String username,
                                      @Param("q") String q,
                                      @Param("tag") String tag,
                                      @Param("type") String type);

    @EntityGraph(attributePaths = {"user", "tags"})
    Page<Note> findAllByUserId(Pageable pageable, UUID id);

    @EntityGraph(attributePaths = {"user", "tags"})
    Page<Note> findAllByUserProfilePrivateFalseAndUserUsernameAndHiddenFalse(Pageable pageable, String username);

    @EntityGraph(attributePaths = {"user", "tags"})
    Page<Note> findAllByUserUsernameAndHiddenFalse(Pageable pageable, String username);

    @Modifying
    @Query("""
                UPDATE Note n
                SET n.fullName = CONCAT(CAST(n.id AS string), '/', n.name)
                WHERE n.user.id = :uId
            """)
    void setOrphanFullNameForUser(UUID uId);

    void deleteAllByUserId(UUID uuid);

    void deleteAllByUserIdAndHiddenTrue(UUID uuid);

}