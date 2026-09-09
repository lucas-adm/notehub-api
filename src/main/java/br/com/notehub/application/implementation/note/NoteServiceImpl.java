package br.com.notehub.application.implementation.note;

import br.com.notehub.application.counter.Counter;
import br.com.notehub.application.dto.request.note.CreateNoteREQ;
import br.com.notehub.application.dto.response.note.DetailNoteRES;
import br.com.notehub.application.dto.response.note.LowDetailNoteRES;
import br.com.notehub.application.dto.response.page.PageRES;
import br.com.notehub.domain.feed.FeedService;
import br.com.notehub.domain.follow.FollowService;
import br.com.notehub.domain.note.Note;
import br.com.notehub.domain.note.NoteRepository;
import br.com.notehub.domain.note.NoteService;
import br.com.notehub.domain.tag.Tag;
import br.com.notehub.domain.tag.TagRepository;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserRepository;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final NoteRepository repository;
    private final FollowService followService;
    private final Counter counter;
    private final FeedService feeder;

    private void validateAccess(@Nullable UUID idFromToken, UUID idFromRequested) {
        if (idFromToken == null) throw new AccessDeniedException("Usuário sem permissão.");
        if (!Objects.equals(idFromToken, idFromRequested)) {
            throw new AccessDeniedException("Usuário sem permissão.");
        }
    }

    private void deleteNoteAndFlush(Note note) {
        repository.delete(note);
        repository.flush();
    }

    private void removeOrphanTags(List<String> names) {
        List<Tag> tags = tagRepository.findAllByNameIn(names);
        tags.stream().filter(tag -> tag.getNotes().isEmpty()).forEach(tagRepository::delete);
    }

    private List<Tag> findOrCreateTags(List<String> tags) {
        if (tags == null) return null;
        List<Tag> immutableList = tags.stream()
                .map(String::toLowerCase).distinct()
                .map(tag -> tagRepository.findByName(tag).orElseGet(() -> new Tag(tag.toLowerCase())))
                .toList();
        return new ArrayList<>(immutableList);
    }

    private void changeField(UUID idFromToken, UUID idFromPath, Consumer<Note> setter) {
        Note note = repository.findById(idFromPath).orElseThrow(EntityNotFoundException::new);
        validateAccess(idFromToken, note.getUser().getId());
        setter.accept(note);
        note.setModifiedAt(Instant.now());
        note.setModified(true);
        repository.saveAndFlush(note);
    }

    private String createFullName(Note note) {
        return String.format("%s/%s", note.getUser().getUsername(), note.getName());
    }

    public Note mapToNote(UUID idFromToken, CreateNoteREQ req) {
        User user = userRepository.findById(idFromToken).orElseThrow(EntityNotFoundException::new);
        List<Tag> tags = findOrCreateTags(req.tags());
        return new Note(user, req.name(), req.description(), req.markdown(), req.closed(), req.hidden(), tags);
    }

    @Transactional
    @Override
    public LowDetailNoteRES create(UUID idFromToken, CreateNoteREQ req) {
        Note note = mapToNote(idFromToken, req);
        repository.save(note);
        counter.updateNotesCount(note.getUser(), true);
        feeder.onNoteCreated(note.getId());
        return new LowDetailNoteRES(note);
    }

    @Transactional
    @Override
    public void edit(UUID idFromToken, UUID idFromPath, String name, String description, List<String> tags, boolean closed, boolean hidden) {
        changeField(idFromToken, idFromPath, note -> {
            List<String> oldTags = note.getTags().stream().map(Tag::getName).toList();
            note.setName(name);
            note.setFullName(createFullName(note));
            note.setDescription(description);
            note.setTags(findOrCreateTags(tags));
            note.setClosed(closed);
            note.setHidden(hidden);
            removeOrphanTags(oldTags);
        });
        feeder.onNoteHidden(idFromPath);
    }

    @Transactional
    @Override
    public void changeName(UUID idFromToken, UUID idFromPath, String name) {
        changeField(idFromToken, idFromPath, note -> {
            note.setName(name);
            note.setFullName(createFullName(note));
        });
    }

    @Transactional
    @Override
    public void changeDescription(UUID idFromToken, UUID idFromPath, String description) {
        changeField(idFromToken, idFromPath, note -> note.setDescription(description));
    }

    @Transactional
    @Override
    public void changeMarkdown(UUID idFromToken, UUID idFromPath, String markdown) {
        changeField(idFromToken, idFromPath, note -> note.setMarkdown(markdown));
    }

    @Transactional
    @Override
    public void changeClosed(UUID idFromToken, UUID idFromPath) {
        changeField(idFromToken, idFromPath, note -> note.setClosed(!note.isClosed()));
    }

    @Transactional
    @Override
    public void changeHidden(UUID idFromToken, UUID idFromPath) {
        changeField(idFromToken, idFromPath, note -> note.setHidden(!note.isHidden()));
        feeder.onNoteHidden(idFromPath);
    }

    @Transactional
    @Override
    public void changeTags(UUID idFromToken, UUID idFromPath, List<String> tags) {
        changeField(idFromToken, idFromPath, note -> {
            List<String> oldTagNames = note.getTags().stream().map(Tag::getName).toList();
            note.setTags(findOrCreateTags(tags));
            removeOrphanTags(oldTagNames);
        });
    }

    @Transactional
    @Override
    public void setOrphanFullNameForUser(UUID uId) {
        repository.setOrphanFullNameForUser(uId);
    }

    @Transactional
    @Override
    public void delete(UUID idFromToken, UUID idFromPath) {
        Note note = repository.findById(idFromPath).orElseThrow(EntityNotFoundException::new);
        validateAccess(idFromToken, note.getUser().getId());
        List<String> oldTagNames = note.getTags().stream().map(Tag::getName).toList();
        deleteNoteAndFlush(note);
        removeOrphanTags(oldTagNames);
        counter.updateNotesCount(note.getUser(), false);
    }

    @Transactional
    @Override
    public void deleteAllUserNotes(User user) {
        List<String> tags = tagRepository.findAllByNotesUserId(user.getId()).stream().map(Tag::getName).toList();
        repository.deleteAllByUserId(user.getId());
        repository.flush();
        removeOrphanTags(tags);
    }

    @Transactional
    @Override
    public void deleteAllUserHiddenNotes(User user) {
        List<String> tags = tagRepository.findAllByNotesUserIdAndNotesHiddenTrue(user.getId()).stream().map(Tag::getName).toList();
        repository.deleteAllByUserIdAndHiddenTrue(user.getId());
        repository.flush();
        removeOrphanTags(tags);
    }

    @Override
    public List<String> getAllTags() {
        return tagRepository.findAll().stream().map(Tag::getName).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<String> getAllPublicUserTags(UUID idFromToken, String username) {
        User requesting = (idFromToken != null) ? userRepository.findById(idFromToken).orElseThrow(EntityNotFoundException::new) : null;
        User requested = userRepository.findByUsername(username).orElseThrow(EntityNotFoundException::new);
        if (requested.isProfilePrivate()) followService.validateBidirectionalFollowAccess(requesting, requested);
        return tagRepository.findAllByNotesUserUsernameAndNotesHiddenFalseOrderByNameAsc(username).stream().map(Tag::getName).toList();
    }

    @Override
    public List<String> getAllPrivateUserTags(UUID idFromToken) {
        return tagRepository.findAllByNotesUserId(idFromToken).stream().map(Tag::getName).toList();
    }

    @Override
    public PageRES<LowDetailNoteRES> findPublicNotes(Pageable pageable, String q) {
        Page<LowDetailNoteRES> page = repository.searchPublicNotesByNameOrDescription(pageable, q).map(LowDetailNoteRES::new);
        return new PageRES<>(page);
    }

    @Override
    public PageRES<LowDetailNoteRES> findPrivateNotes(Pageable pageable, UUID idFromToken, String q) {
        Page<LowDetailNoteRES> page = repository.searchPrivateNotesByNameOrTag(pageable, idFromToken, q).map(LowDetailNoteRES::new);
        return new PageRES<>(page);
    }

    @Override
    public PageRES<LowDetailNoteRES> findPublicNotesByTag(Pageable pageable, String tag) {
        Page<LowDetailNoteRES> page = repository.searchPublicNotesByTag(pageable, tag).map(LowDetailNoteRES::new);
        return new PageRES<>(page);
    }

    @Override
    public PageRES<LowDetailNoteRES> findPrivateNotesByTag(Pageable pageable, UUID idFromToken, String tag) {
        Page<LowDetailNoteRES> page = repository.searchPrivateNotesByTag(pageable, idFromToken, tag).map(LowDetailNoteRES::new);
        return new PageRES<>(page);
    }

    @Transactional(readOnly = true)
    @Override
    public PageRES<LowDetailNoteRES> findUserNotesBySpecs(UUID idFromToken, Pageable pageable, String username, String q, String tag, String type) {
        User requesting = (idFromToken != null) ? userRepository.findById(idFromToken).orElseThrow(EntityNotFoundException::new) : null;
        User requested = userRepository.findByUsername(username).orElseThrow(EntityNotFoundException::new);
        if (Objects.equals(type, "hidden")) validateAccess(idFromToken, requested.getId());
        if (requested.isProfilePrivate()) followService.validateBidirectionalFollowAccess(requesting, requested);
        Page<LowDetailNoteRES> page = repository.searchUserNotesBySpecs(pageable, username, q, tag, type).map(LowDetailNoteRES::new);
        return new PageRES<>(page);
    }

    @Transactional(readOnly = true)
    @Override
    public DetailNoteRES getNote(UUID idFromToken, String username, String name) {
        User requesting = (idFromToken != null) ? userRepository.findById(idFromToken).orElseThrow(EntityNotFoundException::new) : null;
        Note requested = repository.findByUserUsernameAndName(username, name)
                .or(() -> repository.findByFullName(String.format("%s/%s", username, name)))
                .orElseThrow(EntityNotFoundException::new);
        User author = requested.getUser();
        if (author != null) {
            if (requested.isHidden()) validateAccess(idFromToken, author.getId());
            if (author.isProfilePrivate()) followService.validateBidirectionalFollowAccess(requesting, author);
        }
        return new DetailNoteRES(requested);
    }

    @Override
    public PageRES<LowDetailNoteRES> getAllUserNotesByUsername(Pageable pageable, String username) {
        Page<Note> notes = repository.findAllByUserProfilePrivateFalseAndUserUsernameAndHiddenFalse(pageable, username.toLowerCase());
        Page<LowDetailNoteRES> page = notes.map(LowDetailNoteRES::new);
        return new PageRES<>(page);
    }

    @Override
    public PageRES<LowDetailNoteRES> getAllUserNotesById(Pageable pageable, UUID idFromToken) {
        Page<LowDetailNoteRES> page = repository.findAllByUserId(pageable, idFromToken).map(LowDetailNoteRES::new);
        return new PageRES<>(page);
    }

    @Override
    public PageRES<LowDetailNoteRES> getAllFollowedUserNotes(Pageable pageable, UUID idFromToken, String username) {
        User requesting = userRepository.findById(idFromToken).orElseThrow(EntityNotFoundException::new);
        User requested = userRepository.findByUsername(username).orElseThrow(EntityNotFoundException::new);
        if (requested.isProfilePrivate()) followService.validateBidirectionalFollowAccess(requesting, requested);
        Page<LowDetailNoteRES> page = repository.findAllByUserUsernameAndHiddenFalse(pageable, username).map(LowDetailNoteRES::new);
        return new PageRES<>(page);
    }

}