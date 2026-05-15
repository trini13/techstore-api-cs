package cl.techstore.api.controller;

import cl.techstore.api.dto.LoginRequest;
import cl.techstore.api.dto.LoginResponse;
import cl.techstore.api.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    private final String USER = "admin@techstore.cl";
    private final String PASS = "Admin1234";

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (USER.equals(request.getUsername()) && PASS.equals(request.getPassword())) {
            String token = jwtUtil.generarToken(request.getUsername());
            return ResponseEntity.ok(new LoginResponse(token, "Bearer", "3600"));
        }
        return ResponseEntity.status(401).body("Credenciales inválidas");
    }
}