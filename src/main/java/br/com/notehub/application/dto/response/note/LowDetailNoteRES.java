package br.com.notehub.application.dto.response.note;

import br.com.notehub.application.dto.response.user.DetailUserRES;
import br.com.notehub.domain.note.Note;
import br.com.notehub.domain.tag.Tag;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record LowDetailNoteRES(
        UUID id,
        String name,
        String full_name,
        String description,
        List<String> tags,
        DetailUserRES user,
        String created_at,
        String modified_at,
        boolean modified,
        boolean closed,
        boolean hidden,
        int comments_count,
        int flames_count
) {
    public LowDetailNoteRES(Note note) {
        this(
                note.getId(),
                note.getName(),
                note.getFullName(),
                note.getDescription(),
                note.getTags().stream().map(Tag::getName).toList(),
                note.getUser() != null ? new DetailUserRES(note.getUser()) : null,
                note.getCreatedAt().atZone(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("d/M/yy HH:mm", Locale.of("pt-BR"))),
                note.getModifiedAt().atZone(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("d/M/yy HH:mm", Locale.of("pt-BR"))),
                note.isModified(),
                note.isClosed(),
                note.isHidden(),
                note.getCommentsCount(),
                note.getFlamesCount()
        );
    }
}