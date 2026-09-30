# pagatu — Sistema Distribuido de Comercio Electrónico

Proyecto del curso **Desarrollo de Aplicaciones Distribuidas (DIST)**, UPeU 2026-2.

---

## 📊 Estado Actual del Proyecto (Sesión 6 Completada de Punta a Punta)

El proyecto cuenta con **todos los componentes de la Sesión 6 (S6) implementados y compilados con éxito**:
1. Infraestructura base (Config Server, Eureka Server, API Gateway con balanceo y enrutamiento dinámico).
2. Microservicio de Catálogo (`pagatu-catalogo-ms`).
3. Microservicio de Órdenes (`pagatu-orden-ms`) con cliente OpenFeign y Circuit Breaker (Resilience4j) en bean desacoplado.
4. Microservicio de Clientes (`pagatu-cliente-ms` - Actividad Autónoma) con CRUD completo, validaciones y migración Flyway.

### Tablero de Control de Microservicios

| Módulo / Componente | Sesión | Tipo | Estado en Código | Puerto DEV | Base de Datos (Host) |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **`infra/pagatu-config`** | S2 | Infra |  **Completado** | `18888` | Local (`config-repo/`) |
| **`infra/pagatu-eureka`** | S3 | Infra |  **Completado** | `18761` | Eureka Server |
| **`infra/pagatu-gateway`** | S4/S6 | Infra |  **Completado** | `18080` | Spring Cloud Gateway |
| **`services/pagatu-catalogo-ms`** | S1 | Negocio |  **Completado** | `8080` | `15432` (`pagatu_catalogo_db`) |
| **`services/pagatu-orden-ms`** | **S6** | Negocio |  **Completado** | `8082` | `15434` (`pagatu_orden_db`) |
| **`services/pagatu-cliente-ms`** | **S6 (Autónomo)** | Negocio |  **Completado** | `8081` | `15433` (`pagatu_cliente_db`) |
| **`obs/`** (Prometheus/Grafana) | S3 | Observabilidad |  **Configurado** | `9090` / `3000` | N/A |

---

## 🏗️ Arquitectura Implementada en la Sesión 6

### 1. `pagatu-orden-ms` (Comunicación Resiliente con Feign + Resilience4j)
* **OpenFeign Declarativo:** Consulta a `pagatu-catalogo-ms` vía Eureka sin direcciones fijas:
  ```java
  @FeignClient(name = "pagatu-catalogo-ms")
  public interface ProductoClient {
      @GetMapping("/api/v1/productos/{id}")
      ProductoDto findById(@PathVariable("id") Long id);
  }
  ```
* **Separación de Bean Anti Self-Invocation:**
  La protección `@CircuitBreaker(name = "catalogo", fallbackMethod = "fallbackProducto")` se ubica en el servicio dedicado `ProductoConsultaService`, asegurando que el interceptor AOP de Spring actúe correctamente al ser invocado desde `OrdenServiceImpl`.
* **Degradación Elegante:**
  * Si catálogo está en línea: orden en `PENDIENTE_PAGO` con precios reales y total calculado.
  * Si catálogo cae o el circuito abre: orden en `CARRITO`, precios y total en `null`, registrando advertencia en log y evitando el error HTTP 500.
* **Configuración del Circuito en `pagatu-orden-ms-dev.yml`:**
  * Ventana deslizante (`sliding-window-size`): 5 llamadas.
  * Tasa de fallo (`failure-rate-threshold`): 50%.
  * Espera en `OPEN` (`wait-duration-in-open-state`): 10s.
  * Llamadas de prueba en `HALF_OPEN`: 3 llamadas.

### 2. `pagatu-cliente-ms` (Actividad Autónoma S6)
* **Base de datos:** `postgres:16-alpine` en puerto host `15433` (`pagatu_cliente_db`).
* **Migraciones Flyway:**
  * `V1__create_cliente_tables.sql`: tabla `clientes` (`tipo_documento`, `numero_documento` UNIQUE, etc.).
  * `V2__seed_clientes.sql`: datos de prueba precargados.
* **Capa Java completa:** `Cliente`, DTOs (`ClienteRequest`/`ClienteResponse`), `ClienteRepository` con `findByNumeroDocumento`, `ClienteServiceImpl` con CRUD y `ClienteController` en `/api/v1/clientes`.
* **Configuración centralizada y enrutamiento:** Archivo `pagatu-cliente-ms-dev.yml` (puerto `8081`) y ruta en el Gateway (`/api/v1/clientes/**`).

---

## 🚀 Guía de Ejecución en DEV

### Paso 1: Levantar Bases de Datos con Docker Compose
Abre Docker Desktop en tu sistema y ejecuta:
```powershell
# BD Catálogo (puerto 15432)
docker compose -f services/pagatu-catalogo-ms/compose-dev.yml up -d

# BD Cliente (puerto 15433)
docker compose -f services/pagatu-cliente-ms/compose-dev.yml up -d

# BD Orden (puerto 15434)
docker compose -f services/pagatu-orden-ms/compose-dev.yml up -d
```

### Paso 2: Levantar Infraestructura (en terminales independientes)
```powershell
# Terminal 1: Servidor de Configuración
cd infra/pagatu-config
.\mvnw.cmd spring-boot:run

# Terminal 2: Eureka Discovery Server
cd infra/pagatu-eureka
.\mvnw.cmd spring-boot:run

# Terminal 3: API Gateway
cd infra/pagatu-gateway
.\mvnw.cmd spring-boot:run
```

### Paso 3: Levantar Microservicios de Negocio
```powershell
# Terminal 4: Catálogo
cd services/pagatu-catalogo-ms
.\mvnw.cmd spring-boot:run

# Terminal 5: Clientes
cd services/pagatu-cliente-ms
.\mvnw.cmd spring-boot:run

# Terminal 6: Órdenes
cd services/pagatu-orden-ms
.\mvnw.cmd spring-boot:run
```

---

## 📸 Guía para Capturar las 4 Evidencias del Informe (S06)

> [!IMPORTANT]
> Recuerda que cada captura debe mostrar **sin recortar** el reloj del sistema (fecha y hora) y tu usuario o foto de perfil en Windows / VS Code.

### Evidencia 1: Comunicación Feign Exitosa (`PENDIENTE_PAGO`)
Con `pagatu-catalogo-ms` y `pagatu-orden-ms` encendidos, crea una orden a través del Gateway:
```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:18080/api/v1/ordenes" `
  -ContentType "application/json" `
  -Body '{"idCliente": 1, "metodoPago": "YAPE_PLIN", "detalles": [{"idProducto": 1, "cantidad": 2}]}'
```
* **Resultado esperado:** HTTP 201, `estado: "PENDIENTE_PAGO"`, `precioUnitario: 89.90`, `subtotal: 179.80` y `total: 179.80` obtenidos dinámicamente de catálogo.

### Evidencia 2: Resiliencia ante Catálogo Caído (`CARRITO`)
Detén `pagatu-catalogo-ms` (`Ctrl+C` en su terminal) y repite la misma petición:
```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:18080/api/v1/ordenes" `
  -ContentType "application/json" `
  -Body '{"idCliente": 1, "metodoPago": "YAPE_PLIN", "detalles": [{"idProducto": 1, "cantidad": 2}]}'
```
* **Resultado esperado:** HTTP 201 (no 500), `estado: "CARRITO"`, `precioUnitario: null` y `total: null`. El log muestra: `[CATALOGO] Fallback activado para idProducto 1`.

### Evidencia 3: Apertura del Circuito (`OPEN`)
Con catálogo aún detenido, ejecuta una ráfaga de 5 llamadas consecutivas para superar el umbral:
```powershell
1..5 | ForEach-Object {
    Invoke-RestMethod -Method Post -Uri "http://localhost:18080/api/v1/ordenes" `
      -ContentType "application/json" `
      -Body '{"idCliente": 1, "metodoPago": "YAPE_PLIN", "detalles": [{"idProducto": 1, "cantidad": 2}]}' | Select-Object id, estado
}
```
Inmediatamente consulta el endpoint Actuator de salud:
```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8082/actuator/health"
```
* **Resultado esperado:** Captura del bloque `circuitBreakers.details.catalogo.details.state: "OPEN"`.

### Evidencia 4: Microservicio Autónomo de Clientes Operativo
Prueba `pagatu-cliente-ms` a través del Gateway:
```powershell
# Listar clientes iniciales
Invoke-RestMethod -Method Get -Uri "http://localhost:18080/api/v1/clientes"

# Crear un nuevo cliente
Invoke-RestMethod -Method Post -Uri "http://localhost:18080/api/v1/clientes" `
  -ContentType "application/json" `
  -Body '{"tipoDocumento": "DNI", "numeroDocumento": "76543210", "nombres": "Carlos", "apellidos": "Mendoza", "email": "carlos.m@example.com", "telefono": "987654321", "direccion": "Av. Brasil 450", "activo": true}'
```
* **Resultado esperado:** HTTP 200 con la lista de clientes y HTTP 201 con el cliente registrado.
