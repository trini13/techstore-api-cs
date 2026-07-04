import json


def lambda_handler(event, context):
    """
    Funcion serverless "techstore-audit-logger".

    Se dispara automaticamente por un trigger de Amazon SQS sobre la cola
    "techstore-audit-queue". Cada vez que techstore-api publica un evento
    de auditoria (CREAR/MODIFICAR/ELIMINAR un producto), esta funcion:
      1. Recibe el/los mensaje(s) dentro de event["Records"].
      2. Extrae y deserializa el "body" de cada registro.
      3. Imprime un log estructurado en Amazon CloudWatch Logs.
    """
    print("[FaaS Audit] Funcion Serverless de Auditoria iniciada...")

    records = event.get("Records", [])
    print(f"[FaaS Audit] Mensajes recibidos desde SQS: {len(records)}")

    for record in records:
        body = record.get("body", "{}")
        try:
            auditoria = json.loads(body)
        except json.JSONDecodeError as e:
            print(f"[FaaS Audit] ERROR: no se pudo parsear el mensaje: {e}")
            print(f"[FaaS Audit] Body crudo: {body}")
            continue

        accion = auditoria.get("accion", "DESCONOCIDA")
        producto_id = auditoria.get("productoId", "N/A")
        nombre = auditoria.get("nombre", "N/A")
        usuario = auditoria.get("usuario", "N/A")
        fecha = auditoria.get("fecha", "N/A")

        print("=======================================================")
        print("[FaaS Audit] NUEVA AUDITORIA DE PRODUCTO DETECTADA EN SQS")
        print("=======================================================")
        print(f"Accion Realizada: {accion}")
        print(f"ID Producto: {producto_id}")
        print(f"Nombre Producto: {nombre}")
        print(f"Usuario Operador: {usuario}")
        print(f"Fecha Operacion: {fecha}")
        print("=======================================================")

    return {
        "statusCode": 200,
        "body": json.dumps(
            f"Procesamiento de auditoria de TechStore finalizado con exito. "
            f"Mensajes procesados: {len(records)}"
        ),
    }
