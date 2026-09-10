package br.com.notehub.implementation.note;

import br.com.notehub.application.counter.Counter;
import br.com.notehub.application.implementation.note.NoteServiceImpl;
import br.com.notehub.domain.feed.FeedService;
import br.com.notehub.domain.follow.FollowService;
import br.com.notehub.domain.note.Note;
import br.com.notehub.domain.note.NoteRepository;
import br.com.notehub.domain.tag.TagRepository;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractNoteServiceTest {

    @InjectMocks
    protected NoteServiceImpl service;

    @Mock
    protected UserRepository userRepository;

    @Mock
    protected TagRepository tagRepository;

    @Mock
    protected NoteRepository repository;

    @Mock
    protected FollowService followService;

    @Mock
    protected Counter counter;

    @Mock
    protected FeedService feeder;

    protected User owner;
    protected Note note;

    @BeforeEach
    void baseSetup() {
        owner = User.signup("owner@notehub.com.br", "owner", "OWNER", "123");
        owner.setId(UUID.randomUUID());

        note = new Note(owner, "old-name", null, "# md", false, false, null);
        note.setId(UUID.randomUUID());
    }

}