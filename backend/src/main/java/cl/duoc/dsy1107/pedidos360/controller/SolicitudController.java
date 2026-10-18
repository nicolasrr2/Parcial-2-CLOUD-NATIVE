package cl.duoc.dsy1107.pedidos360.controller;

import cl.duoc.dsy1107.pedidos360.dto.DecisionRequest;
import cl.duoc.dsy1107.pedidos360.dto.SolicitudRequest;
import cl.duoc.dsy1107.pedidos360.dto.SolicitudResponse;
import cl.duoc.dsy1107.pedidos360.service.SolicitudService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/solicitudes")
public class SolicitudController {

    private final SolicitudService service;

    public SolicitudController(SolicitudService service) {
        this.service = service;
    }

    @GetMapping("/mias")
    @PreAuthorize("hasRole('SOLICITANTES') and hasAuthority('SCOPE_solicitudes/read')")
    public List<SolicitudResponse> mias(Authentication authentication) {
        return service.listarMias(authentication.getName());
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('APROBADORES') and hasAuthority('SCOPE_solicitudes/read')")
    public List<SolicitudResponse> pendientes() {
        return service.listarPendientes();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_solicitudes/read')")
    public SolicitudResponse buscar(@PathVariable @NonNull Long id, Authentication authentication) {
        return service.buscar(id, authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SOLICITANTES') and hasAuthority('SCOPE_solicitudes/write')")
    public SolicitudResponse crear(@Valid @RequestBody SolicitudRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return service.crear(request, jwt.getSubject(), jwt.getClaimAsString("email"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SOLICITANTES') and hasAuthority('SCOPE_solicitudes/write')")
    public SolicitudResponse actualizar(@PathVariable @NonNull Long id, @Valid @RequestBody SolicitudRequest request,
            Authentication authentication) {
        return service.actualizar(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SOLICITANTES') and hasAuthority('SCOPE_solicitudes/write')")
    public void eliminar(@PathVariable @NonNull Long id, Authentication authentication) {
        service.eliminar(id, authentication.getName());
    }

    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasRole('APROBADORES') and hasAuthority('SCOPE_solicitudes/approve')")
    public SolicitudResponse decidir(@PathVariable @NonNull Long id, @Valid @RequestBody DecisionRequest request) {
        return service.decidir(id, request);
    }
}
