package pe.utp.entrevistapro360.presentation;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.utp.entrevistapro360.application.port.in.AutenticarUsuario;
import pe.utp.entrevistapro360.application.port.in.RegistrarUsuario;
import pe.utp.entrevistapro360.presentation.dto.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final RegistrarUsuario registro;
    private final AutenticarUsuario login;
    public AuthController(RegistrarUsuario registro, AutenticarUsuario login) { this.registro = registro; this.login = login; }
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(
                registro.registrar(request.nombreCompleto(), request.email(), request.password())));
    }
    @PostMapping("/login")
    public LoginResponse autenticar(@RequestBody LoginRequest request) {
        var resultado = login.autenticar(request.email(), request.password());
        return new LoginResponse(resultado.token().accessToken(), "Bearer", resultado.token().expiresIn(), UsuarioResponse.from(resultado.usuario()));
    }
}
