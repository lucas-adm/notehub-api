package br.com.notehub.application.controller.note;

import br.com.notehub.application.dto.request.note.*;
import br.com.notehub.application.dto.response.note.DetailNoteRES;
import br.com.notehub.application.dto.response.note.LowDetailNoteRES;
import br.com.notehub.application.dto.response.page.PageRES;
import br.com.notehub.domain.note.NoteService;
import com.auth0.jwt.JWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notes")
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Note Controller", description = "Endpoints for managing notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService service;

    private UUID getSubject(String bearerToken) {
        if (bearerToken == null) return null;
        String idFromToken = JWT.decode(bearerToken.replace("Bearer ", "")).getSubject();
        return UUID.fromString(idFromToken);
    }

    @Operation(summary = "Register a new note", description = "Creates a new note public or private.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Note registered successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @PostMapping("/new-note")
    public ResponseEntity<LowDetailNoteRES> createNote(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @Valid @RequestBody CreateNoteREQ dto
    ) {
        UUID idFromToken = getSubject(accessToken);
        LowDetailNoteRES note = service.create(idFromToken, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }

    @Operation(summary = "Edit note fields", description = "Updates note.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Note updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Invalid token."),
            @ApiResponse(responseCode = "404", description = "Note not found."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    @PutMapping("/{id}/edit-note")
    public ResponseEntity<Void> editNote(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("id") UUID idFromPath,
            @Valid @RequestBody EditNoteREQ dto
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.edit(idFromToken, idFromPath, dto.name(), dto.description(), dto.tags(), dto.closed(), dto.hidden());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Change note name", description = "Changes the note name.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Note updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Invalid token."),
            @ApiResponse(responseCode = "404", description = "Note not found."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    @PatchMapping("/{id}/change-name")
    public ResponseEntity<Void> changeNoteName(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("id") UUID idFromPath,
            @Valid @RequestBody ChangeNameREQ dto
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.changeName(idFromToken, idFromPath, dto.name());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Change note description", description = "Changes the note description.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Note updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Invalid token."),
            @ApiResponse(responseCode = "404", description = "Note not found."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    @PatchMapping("/{id}/change-description")
    public ResponseEntity<Void> changeNoteDescription(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("id") UUID idFromPath,
            @Valid @RequestBody ChangeDescriptionREQ dto
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.changeDescription(idFromToken, idFromPath, dto.description());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Change note content", description = "Changes the note markdown content.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Note markdown changed successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Invalid token."),
            @ApiResponse(responseCode = "404", description = "Note not found."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    @PatchMapping("/{id}/change-markdown")
    public ResponseEntity<Void> changeNoteMarkdown(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("id") UUID idFromPath,
            @Valid @RequestBody ChangeMarkdownREQ dto
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.changeMarkdown(idFromToken, idFromPath, dto.markdown());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Change note status", description = "Set note status to closed true or false depending on current value.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Note status changed successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Invalid token."),
            @ApiResponse(responseCode = "404", description = "Note not found."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    @PatchMapping("/{id}/change-status")
    public ResponseEntity<Void> changeNoteStatus(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("id") UUID idFromPath
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.changeClosed(idFromToken, idFromPath);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Change note visibility", description = "Set note hidden to true or false depending on current value.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Note visibility changed successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Invalid token."),
            @ApiResponse(responseCode = "404", description = "Note not found."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    @PatchMapping("/{id}/change-visibility")
    public ResponseEntity<Void> changeNoteVisibility(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("id") UUID idFromPath
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.changeHidden(idFromToken, idFromPath);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Change note tags", description = "Set a array of strings as new tag names to user's note")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Note tags updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Invalid token."),
            @ApiResponse(responseCode = "404", description = "Note not found."),
            @ApiResponse(responseCode = "500", description = "Internal server error.")
    })
    @PatchMapping("/{id}/change-tags")
    public ResponseEntity<Void> changeNoteTags(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("id") UUID idFromPath,
            @Valid @RequestBody ChangeTagsREQ dto
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.changeTags(idFromToken, idFromPath, dto.tags());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Delete note", description = "Deletes a note permanently.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Note deleted successfully."),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "404", description = "Note not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Void> deleteNote(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("id") UUID idFromPath
    ) {
        UUID idFromToken = getSubject(accessToken);
        service.delete(idFromToken, idFromPath);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Get all tags", description = "Retrieves a paginated list of all used tags.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tags retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid pageable criteria.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/tags")
    public ResponseEntity<List<String>> getAllTags() {
        List<String> tags = service.getAllTags();
        return ResponseEntity.status(HttpStatus.OK).body(tags);
    }

    @Operation(summary = "Get all tags used by user", description = "Retrieves a paginated list of all used tags by user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Used tags by user retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid pageable criteria.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/private/tags")
    public ResponseEntity<List<String>> findAllPrivateUserTags(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken
    ) {
        UUID idFromToken = getSubject(accessToken);
        List<String> tags = service.getAllPrivateUserTags(idFromToken);
        return ResponseEntity.status(HttpStatus.OK).body(tags);
    }

    @Operation(summary = "Get all tags used by username", description = "Retrieves a list of all used tags by username.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Used tags by user retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid pageable criteria.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/{username}/tags")
    public ResponseEntity<List<String>> findAllPublicUserTags(
            @Parameter(hidden = true) @RequestHeader(required = false, value = "Authorization") String accessToken,
            @PathVariable("username") String username
    ) {
        UUID idFromToken = getSubject(accessToken);
        List<String> tags = service.getAllPublicUserTags(idFromToken, username);
        return ResponseEntity.status(HttpStatus.OK).body(tags);
    }

    @Operation(summary = "Search for notes", description = "Searches notes by name or description.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/search")
    public ResponseEntity<PageRES<LowDetailNoteRES>> searchPublicNotes(
            @ParameterObject @PageableDefault(page = 0, size = 25, sort = {"flamesCount"}, direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String q
    ) {
        PageRES<LowDetailNoteRES> page = service.findPublicNotes(pageable, q);
        return ResponseEntity.status(HttpStatus.OK).body(page);
    }

    @Operation(summary = "Search for user notes", description = "Searches user notes by name or tag.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid pageable criteria.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/private/search")
    public ResponseEntity<PageRES<LowDetailNoteRES>> searchPrivateNotes(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = {"createdAt"}, direction = Sort.Direction.DESC)
            Pageable pageable,
            @NotBlank @RequestParam String q
    ) {
        UUID idFromToken = getSubject(accessToken);
        PageRES<LowDetailNoteRES> page = service.findPrivateNotes(pageable, idFromToken, q);
        return ResponseEntity.status(HttpStatus.OK).body(page);
    }

    @Operation(summary = "Search for notes", description = "Searches notes by tag.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid pageable criteria.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/search/tag")
    public ResponseEntity<PageRES<LowDetailNoteRES>> searchPublicNotesByTag(
            @ParameterObject @PageableDefault(page = 0, size = 25, sort = {"flamesCount"}, direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String q
    ) {
        PageRES<LowDetailNoteRES> page = service.findPublicNotesByTag(pageable, q);
        return ResponseEntity.status(HttpStatus.OK).body(page);
    }

    @Operation(summary = "Search for user notes", description = "Searches user notes by tag.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid pageable criteria.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/private/search/tag")
    public ResponseEntity<PageRES<LowDetailNoteRES>> searchPrivateNotesByTag(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pageable,
            @NotBlank @RequestParam String q
    ) {
        UUID idFromToken = getSubject(accessToken);
        PageRES<LowDetailNoteRES> page = service.findPrivateNotesByTag(pageable, idFromToken, q);
        return ResponseEntity.status(HttpStatus.OK).body(page);
    }

    @Operation(
            summary = "Search user notes by multiple criteria",
            description = """
                     Searches notes of a specific user combining optional filters:
                     - Username (path parameter)
                     - Text query (q) for name
                     - Tag name (tag)
                     - Type (open, closed and hidden)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid pageable criteria.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "404", description = "User not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/{username}/specs")
    public ResponseEntity<PageRES<LowDetailNoteRES>> searchUserNotesBySpecs(
            @Parameter(hidden = true) @RequestHeader(required = false, value = "Authorization") String accessToken,
            @ParameterObject @PageableDefault(page = 0, size = 25, sort = {"modifiedAt"}, direction = Sort.Direction.DESC) Pageable pageable,
            @PathVariable("username") String username,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String type
    ) {
        UUID idFromToken = getSubject(accessToken);
        PageRES<LowDetailNoteRES> page = service.findUserNotesBySpecs(idFromToken, pageable, username, q, tag, type);
        return ResponseEntity.status(HttpStatus.OK).body(page);
    }

    @Operation(summary = "Get a note details", description = "Retrieves detailed information about a note by their uuid.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully."),
            @ApiResponse(responseCode = "403", description = "Access denied.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Note not found.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("{username}/{name}")
    public ResponseEntity<DetailNoteRES> getPublicNote(
            @Parameter(hidden = true) @RequestHeader(required = false, value = "Authorization") String accessToken,
            @PathVariable("username") String username,
            @PathVariable("name") String name
    ) {
        UUID idFromToken = getSubject(accessToken);
        DetailNoteRES note = service.getNote(idFromToken, username, name);
        return ResponseEntity.status(HttpStatus.OK).body(note);
    }

    @Operation(summary = "Search for notes", description = "Searches notes by user username.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid pageable criteria.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/user/{username}")
    public ResponseEntity<PageRES<LowDetailNoteRES>> getAllUserNotesByUsername(
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = {"flamesCount"}, direction = Sort.Direction.DESC) Pageable pageable,
            @PathVariable("username") String username
    ) {
        PageRES<LowDetailNoteRES> page = service.getAllUserNotesByUsername(pageable, username);
        return ResponseEntity.status(HttpStatus.OK).body(page);
    }

    @Operation(summary = "Search for notes", description = "Searches notes by user id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid pageable criteria.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "403", description = "Invalid token.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/private")
    public ResponseEntity<PageRES<LowDetailNoteRES>> getAllUserNotesById(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = {"modifiedAt"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID idFromToken = getSubject(accessToken);
        PageRES<LowDetailNoteRES> page = service.getAllUserNotesById(pageable, idFromToken);
        return ResponseEntity.status(HttpStatus.OK).body(page);
    }

    @Operation(
            summary = "Get notes from a user with mutual connections",
            description = "Retrieves a paginated list of notes from a user that the authenticated user is following."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notes retrieved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid pageable criteria.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "403", description = "Access token is invalid or missing.", content = @Content(examples = {})),
            @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content(examples = {}))
    })
    @GetMapping("/private/following/{username}")
    public ResponseEntity<PageRES<LowDetailNoteRES>> getAllFollowingUserNotes(
            @Parameter(hidden = true) @RequestHeader("Authorization") String accessToken,
            @PathVariable("username") String username,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID idFromToken = getSubject(accessToken);
        PageRES<LowDetailNoteRES> page = service.getAllFollowedUserNotes(pageable, idFromToken, username);
        return ResponseEntity.status(HttpStatus.OK).body(page);
    }

}