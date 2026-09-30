package pe.edu.upeu.cliente.service;

import pe.edu.upeu.cliente.dto.ClienteRequest;
import pe.edu.upeu.cliente.dto.ClienteResponse;

import java.util.List;

public interface ClienteService {
    List<ClienteResponse> listar();
    ClienteResponse obtener(Long id);
    ClienteResponse obtenerPorDocumento(String numeroDocumento);
    ClienteResponse crear(ClienteRequest request);
    ClienteResponse actualizar(Long id, ClienteRequest request);
    void eliminar(Long id);
}

