package br.com.notehub.domain.note;

import br.com.notehub.application.dto.request.note.CreateNoteREQ;
import br.com.notehub.application.dto.response.note.DetailNoteRES;
import br.com.notehub.application.dto.response.note.LowDetailNoteRES;
import br.com.notehub.application.dto.response.page.PageRES;
import br.com.notehub.domain.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface NoteService {

    LowDetailNoteRES create(UUID idFromToken, CreateNoteREQ req);

    void edit(UUID idFromToken, UUID idFromPath, String name, String description, List<String> tags, boolean closed, boolean hidden);

    void changeName(UUID idFromToken, UUID idFromPath, String name);

    void changeDescription(UUID idFromToken, UUID idFromPath, String description);

    void changeMarkdown(UUID idFromToken, UUID idFromPath, String markdown);

    void changeClosed(UUID idFromToken, UUID idFromPath);

    void changeHidden(UUID idFromToken, UUID idFromPath);

    void changeTags(UUID idFromToken, UUID idFromPath, List<String> tags);

    void setOrphanFullNameForUser(UUID uId);

    void delete(UUID idFromToken, UUID idFromPath);

    void deleteAllUserNotes(User user);

    void deleteAllUserHiddenNotes(User user);

    List<String> getAllTags();

    List<String> getAllPublicUserTags(UUID idFromToken, String username);

    List<String> getAllPrivateUserTags(UUID idFromToken);

    PageRES<LowDetailNoteRES> findPublicNotes(Pageable pageable, String q);

    PageRES<LowDetailNoteRES> findPrivateNotes(Pageable pageable, UUID idFromToken, String q);

    PageRES<LowDetailNoteRES> findPublicNotesByTag(Pageable pageable, String tag);

    PageRES<LowDetailNoteRES> findPrivateNotesByTag(Pageable pageable, UUID idFromToken, String tag);

    PageRES<LowDetailNoteRES> findUserNotesBySpecs(UUID idFromToken, Pageable pageable, String username, String q, String tag, String type);

    DetailNoteRES getNote(UUID idFromToken, String username, String name);

    PageRES<LowDetailNoteRES> getAllUserNotesByUsername(Pageable pageable, String username);

    PageRES<LowDetailNoteRES> getAllUserNotesById(Pageable pageable, UUID idFromToken);

    PageRES<LowDetailNoteRES> getAllFollowedUserNotes(Pageable pageable, UUID idFromToken, String username);

}