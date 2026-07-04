package cl.techstore.api.service;

import cl.techstore.api.dto.ProductoDTO;
import cl.techstore.api.model.Producto;
import cl.techstore.api.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository repo;

    @Autowired
    private AuditoriaPublisherService auditoriaPublisher;

    public List<Producto> listarTodos() {
        return repo.findAll();
    }

    public Producto crear(ProductoDTO dto) {
        Producto p = new Producto();
        p.setNombre(dto.getNombre());
        p.setDescripcion(dto.getDescripcion());
        p.setPrecio(dto.getPrecio());
        p.setStock(dto.getStock());
        p.setCategoria(dto.getCategoria());
        p.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        Producto guardado = repo.save(p);

        auditoriaPublisher.publicarEvento("CREAR", guardado.getId(), guardado.getNombre(), usuarioActual());
        return guardado;
    }

    public Producto modificar(Long id, ProductoDTO dto) {
        Producto p = repo.findById(id).orElseThrow();
        p.setNombre(dto.getNombre());
        p.setDescripcion(dto.getDescripcion());
        p.setPrecio(dto.getPrecio());
        p.setStock(dto.getStock());
        p.setCategoria(dto.getCategoria());
        if (dto.getActivo() != null) p.setActivo(dto.getActivo());
        Producto actualizado = repo.save(p);

        auditoriaPublisher.publicarEvento("MODIFICAR", actualizado.getId(), actualizado.getNombre(), usuarioActual());
        return actualizado;
    }

    public void eliminar(Long id) {
        Producto p = repo.findById(id).orElseThrow();
        p.setActivo(false);
        repo.save(p);

        auditoriaPublisher.publicarEvento("ELIMINAR", p.getId(), p.getNombre(), usuarioActual());
    }

    /**
     * Obtiene el correo del usuario autenticado a partir del contexto de
     * seguridad, el cual fue poblado por el JwtFilter con el "subject" del
     * token JWT (el correo usado en el login).
     */
    private String usuarioActual() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "desconocido";
    }
}