package pe.edu.upeu.cliente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRequest {

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Size(max = 10, message = "El tipo de documento no debe superar 10 caracteres")
    private String tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20, message = "El número de documento no debe superar 20 caracteres")
    private String numeroDocumento;

    @Size(max = 100, message = "Los nombres no deben superar 100 caracteres")
    private String nombres;

    @Size(max = 100, message = "Los apellidos no deben superar 100 caracteres")
    private String apellidos;

    @Size(max = 150, message = "La razón social no debe superar 150 caracteres")
    private String razonSocial;

    @Size(max = 100, message = "El email no debe superar 100 caracteres")
    private String email;

    @Size(max = 20, message = "El teléfono no debe superar 20 caracteres")
    private String telefono;

    @Size(max = 200, message = "La dirección no debe superar 200 caracteres")
    private String direccion;

    @Builder.Default
    private Boolean activo = true;
}

