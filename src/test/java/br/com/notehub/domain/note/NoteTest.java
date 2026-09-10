package br.com.notehub.domain.note;

import br.com.notehub.domain.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
public class NoteTest {

    @Test
    void shouldSetFullName_whenCreatingNote() {
        User owner = User.signup("owner@notehub.com.br", "owner", "OWNER", "123");
        Note note = new Note(owner, "my-note", null, "# md", false, false, null);
        assertThat(note.getFullName()).isEqualTo("owner/my-note");
    }

}