package cl.techstore.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa el evento JSON de auditoria que se publica en la cola
 * "techstore-audit-queue" cada vez que se crea, modifica o elimina
 * (borrado logico) un producto.
 *
 * Estructura exigida por la pauta de la Evaluacion 3:
 * {
 *   "accion": "CREAR / MODIFICAR / ELIMINAR",
 *   "productoId": <id_del_producto>,
 *   "nombre": "<nombre_del_producto>",
 *   "usuario": "<correo_del_usuario_obtenido_del_JWT>",
 *   "fecha": "<timestamp_ISO_8601>"
 * }
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditoriaEventoDTO {
    private String accion;
    private Long productoId;
    private String nombre;
    private String usuario;
    private String fecha;
}
