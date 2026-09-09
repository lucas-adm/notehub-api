package br.com.notehub.application.dto.request.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangeNameREQ(
        @NotBlank(message = "Não pode ser vazio")
        @Pattern(
                regexp = "^[a-zA-Z0-9_.-]+$",
                message = "Apenas letras, números, _ e ."
        )
        @Size(min = 1, max = 255, message = "Tamanho inválido")
        String name
) {
}