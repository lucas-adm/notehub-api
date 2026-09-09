package br.com.notehub.application.dto.request.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateNoteREQ(
        @NotBlank(message = "Não pode ser vazio")
        @Pattern(
                regexp = "^[a-zA-Z0-9_.-]+$",
                message = "Apenas letras, números, _ e ."
        )
        @Size(min = 1, max = 255, message = "Tamanho inválido")
        String name,

        @Pattern(
                regexp = "^(?!.*[\\u00A0\\u2007\\u202F]).*$",
                message = "👀"
        )
        @Size(max = 255, message = "Além do limite")
        String description,

        String markdown,

        boolean closed,

        boolean hidden,

        @Size(max = 12, message = "Capacidade máxima excedida")
        List<
                @NotBlank(message = "Não pode ser vazio")
                @Pattern(
                        regexp = "^(?!.*[\\p{Zs}\\u00A0\\u2007\\u202F]).*$",
                        message = "Não use espaços"
                )
                @Size(min = 2, max = 20, message = "Tamanho inválido")
                        String> tags
) {
}