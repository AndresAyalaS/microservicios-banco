# Prueba Técnica – Microservicios

Solución compuesta por dos microservicios Spring Boot que se comunican entre sí y comparten una base de datos PostgreSQL, desplegables con Docker Compose y probables con la colección de Postman incluida.

| Microservicio          | Responsabilidad                                    | Puerto | Java |
|------------------------|----------------------------------------------------|--------|------|
| `ms-cliente-persona`   | Gestión de personas y clientes                     |  8080  |  21  |
| `ms-cuenta-movimiento` | Cuentas, movimientos y reportes (estado de cuenta) |  8081  |  17  |

## Contenido

1. [Estructura del repositorio](#estructura-del-repositorio)
2. [Arquitectura](#arquitectura)
3. [Requisitos](#requisitos)
4. [Despliegue con Docker](#despliegue-con-docker)
5. [Ejecución local](#ejecución-local-sin-docker)
6. [API de ms-cliente-persona](#api-de-ms-cliente-persona-puerto-8080)
7. [API de ms-cuenta-movimiento](#api-de-ms-cuenta-movimiento-puerto-8081)
8. [Colección de Postman](#colección-de-postman)
9. [Pruebas automatizadas](#pruebas-automatizadas)
10. [Solución de problemas](#solución-de-problemas)

## Estructura del repositorio

```
devsu/
├── ms-cliente-persona/          # Microservicio de clientes
├── ms-cuenta-movimiento/        # Microservicio de cuentas, movimientos y reportes
├── BaseDatos.sql                # Script de base de datos
├── docker-compose.yml           # PostgreSQL + los 2 microservicios
├── postman_collection.json      # Colección de Postman con todos los endpoints
└── README.md
```

## Arquitectura

```
                  ┌─────────────────────┐
  Cliente HTTP ─► │ ms-cuenta-movimiento │ ──(HTTP)──► ms-cliente-persona
                  │       :8081          │               :8080
                  └──────────┬──────────┘                   │
                             └────────► PostgreSQL ◄────────┘
                                          (devsudb)
```

- `ms-cuenta-movimiento` valida que el cliente exista consultando a `ms-cliente-persona` (Feign) y obtiene de él el nombre del cliente para los reportes (RestTemplate). Si el servicio de clientes no responde, la creación de cuentas devuelve `503`, y el reporte usa el nombre `"Cliente {id}"`.
- Ambos microservicios se conectan a la misma base `devsudb` de PostgreSQL.

## Requisitos

- **Docker** y **Docker Compose** (para el despliegue).
- **Java 17 y 21** y **Maven 3.9+** solo si se ejecuta o prueba fuera de Docker.
- **Postman** (opcional) para probar la API con la colección incluida.

## Despliegue con Docker

Desde la raíz del proyecto:

```powershell
docker compose up --build -d
docker compose ps
docker compose logs -f
```

Cada microservicio compila su jar dentro de la imagen (Dockerfile multi-etapa), por lo que **no** es necesario ejecutar Maven antes.

| Servicio                   | URL / acceso                                      |
|----------------------------|---------------------------------------------------|
| ms-cliente-persona         | http://localhost:8080                             |
| ms-cuenta-movimiento       | http://localhost:8081                             |
| PostgreSQL (desde el host) | `localhost:5433` · BD `devsudb` · usuario `devsu` |

Para detener:

```powershell
docker compose down        # detiene y elimina los contenedores
docker compose down -v     # además borra los datos de la base (parte de cero)
```

### Variables de entorno

| Variable                                                    | Servicio          | Descripción                                                                                                    |
|-------------------------------------------------------------|-------------------|----------------------------------------------------------------------------------------------------------------|
| `SPRING_DATASOURCE_URL`                                     | ambos             | `jdbc:postgresql://postgres:5432/devsudb`                                                                      |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | ambos             | Credenciales de PostgreSQL                                                                                     |
| `SPRING_JPA_HIBERNATE_DDL_AUTO`                             | ambos             | `update` (crea/actualiza las tablas al arrancar)                                                               |
| `CLIENTE_SERVICE_URL`                                       | cuenta-movimiento | URL base del servicio de clientes dentro de la red de Docker (`http://ms-cliente-persona:8080/api/v1/clients`) |

> Dentro de Docker **no** se usa `localhost` para comunicar contenedores: se usa el nombre del servicio (`postgres`, `ms-cliente-persona`).

## Ejecución local (sin Docker)

```powershell
cd ms-cliente-persona
mvn spring-boot:run

# en otra terminal
cd ms-cuenta-movimiento
mvn spring-boot:run
```

`ms-cliente-persona` usa H2 en memoria por defecto. Para PostgreSQL basta con definir `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`. En ejecución local, `ms-cuenta-movimiento` necesita que la propiedad `cliente.service.url` apunte a `http://localhost:8080/api/v1/clients`.

---

## API de ms-cliente-persona (puerto 8080)

Ruta base: `/api/v1/clients`

| Método | Ruta                   | Descripción               |
|--------|------------------------|---------------------------|
| POST   | `/api/v1/clients`      | Crea un cliente           |
| GET    | `/api/v1/clients`      | Lista los clientes        |
| GET    | `/api/v1/clients/{id}` | Obtiene un cliente por id |
| PUT    | `/api/v1/clients/{id}` | Actualiza un cliente      |
| DELETE | `/api/v1/clients/{id}` | Elimina un cliente        |

Cuerpo para crear o actualizar:

```json
{
  "name": "Jose Lema",
  "gender": "M",
  "age": 30,
  "documentId": "1710000001",
  "address": "Otavalo sn y principal",
  "phone": "098254785",
  "password": "1234",
  "status": true
}
```

| Campo        | Tipo     | Descripción            |
|--------------|----------|------------------------|
| `name`       | texto    | Nombre completo        |
| `gender`     | texto    | `M` / `F`              |
| `age`        | número   | Edad                   |
| `documentId` | texto    | Identificación         |
| `address`    | texto    | Dirección              |
| `phone`      | texto    | Teléfono               |
| `password`   | texto    | Contraseña del cliente |
| `status`     | booleano | Estado del cliente     |

---

## API de ms-cuenta-movimiento (puerto 8081)

### Cuentas – `/api/v1/cuentas`

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| POST | `/api/v1/cuentas` | Crea una cuenta (valida que el cliente exista) | 201, 400, 404, 409, 503 |
| GET | `/api/v1/cuentas` | Lista todas las cuentas | 200 |
| GET | `/api/v1/cuentas/{id}` | Obtiene una cuenta | 200, 404 |
| PUT | `/api/v1/cuentas/{id}` | Actualización completa. Requiere `accountNumber`, `accountType`, `status` y `clientId`; `initialBalance` se ignora | 200, 400, 404, 409 |
| PATCH | `/api/v1/cuentas/{id}` | Actualización parcial: solo cambia los campos enviados | 200, 404, 409 |

```json
{
  "accountNumber": "478758",
  "accountType": "Ahorros",
  "initialBalance": 2000,
  "status": true,
  "clientId": 1
}
```

Reglas:
- `accountNumber` es único (si ya existe: `409` "Cuenta ya existe").
- El cliente debe existir en `ms-cliente-persona` (si no: `404` "Cliente no encontrado"; si el servicio no responde: `503`).
- El saldo de una cuenta solo cambia mediante movimientos.

### Movimientos – `/api/v1/movimientos`

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| POST | `/api/v1/movimientos` | Registra un movimiento | 201, 400, 404 |
| GET | `/api/v1/movimientos` | Lista los movimientos. Filtro opcional: `?accountId=2` | 200 |
| GET | `/api/v1/movimientos/{id}` | Obtiene un movimiento | 200, 404 |
| PUT | `/api/v1/movimientos/{id}` | Cambia el valor y recalcula los saldos posteriores | 200, 400, 404 |

```json
{ "accountId": 1, "value": -575 }
```

Reglas de negocio:
- Valor **positivo** = *Deposito*; valor **negativo** = *Retiro*. El tipo se calcula automáticamente a partir del signo (`movementType` en el cuerpo se ignora).
- El valor no puede ser nulo ni cero.
- Si el saldo resultante es negativo se responde `400` con el mensaje **"Saldo no disponible"**.
- Al actualizar un movimiento se reajustan el saldo de ese movimiento y el de los posteriores de la misma cuenta; si alguno quedara negativo, se rechaza con `400`. No se puede cambiar la cuenta de un movimiento.

### Reportes (estado de cuenta) – `/api/v1/reportes`

```
GET /api/v1/reportes?clientId=2&startDate=2026-10-01&endDate=2026-10-31
```

| Parámetro | Formato | Descripción |
|---|---|---|
| `clientId` | número | Cliente a consultar |
| `startDate` | `yyyy-MM-dd` | Fecha inicial (inclusive) |
| `endDate` | `yyyy-MM-dd` | Fecha final (inclusive, hasta las 23:59:59) |

Devuelve una fila por movimiento de todas las cuentas del cliente en el rango:

```json
[
  {
    "fecha": "2026-10-26T20:15:30",
    "cliente": "Marianela Montalvo",
    "numeroCuenta": "225487",
    "tipo": "Corriente",
    "saldoInicial": 700.0,
    "estado": true,
    "movimiento": 600.0,
    "saldoDisponible": 700.0
  }
]
```

### Formato de errores

Todas las respuestas de error de `ms-cuenta-movimiento` tienen la forma `{"mensaje": "..."}`:

| Código | Causa |
|---|---|
| 400 | Regla de negocio, "Saldo no disponible", cuerpo JSON inválido o parámetro inválido |
| 404 | Cuenta, movimiento o cliente no encontrado |
| 409 | Número de cuenta duplicado |
| 503 | Servicio de clientes no disponible |
| 500 | Error interno no controlado |

---

## Colección de Postman

El archivo `postman_collection.json` (**Prueba Tecnica - Microservicios **) contiene todos los endpoints de ambos microservicios, agrupados en cuatro carpetas: *Clientes*, *Cuentas*, *Movimientos* y *Reportes*.

### Importación

1. En Postman: **Import** → seleccionar `postman_collection.json`.
2. La colección ya define estas variables (se pueden editar en la pestaña *Variables*):

| Variable | Valor por defecto |
|---|---|
| `clientes_url` | `http://localhost:8080/api/v1` |
| `cuentas_url` | `http://localhost:8081/api/v1` |

### Orden de ejecución recomendado

Los ids de las peticiones (`/clients/1`, `accountId: 2`, etc.) asumen una base de datos **vacía** (`docker compose down -v` y volver a levantar) y que las carpetas se ejecutan **en orden**, de arriba hacia abajo: *Clientes → Cuentas → Movimientos → Reportes*.

**Clientes**

| Petición | Resultado esperado |
|---|---|
| Crear cliente – Jose Lema / Marianela Montalvo / Juan Osorio | Clientes con id 1, 2 y 3 |
| Listar clientes · Obtener por id · Actualizar (PUT) | Consultas y actualización del cliente 1 |
| Eliminar cliente (id 3) | Elimina a Juan Osorio. Se recomienda ejecutarla al final de toda la colección |

**Cuentas**

| Petición | Resultado esperado |
|---|---|
| Crear cuenta 478758 – Jose Lema (Ahorros, 2000) | `201`, id 1 |
| Crear cuenta 225487 – Marianela Montalvo (Corriente, 100) | `201`, id 2 |
| Crear cuenta 495878 – Juan Osorio (Ahorros, 0) | `201`, id 3 |
| Crear cuenta 496825 – Marianela Montalvo (Ahorros, 540) | `201`, id 4 |
| Crear cuenta corriente 585545 – Jose Lema (Corriente, 1000) | `201`, id 5 |
| Crear cuenta con cliente inexistente | `404` "Cliente no encontrado" |
| Crear cuenta duplicada | `409` "Cuenta ya existe" |
| Obtener cuenta inexistente (9999) | `404` "Cuenta no encontrada" |
| Listar · Obtener por id · Actualizar (PUT) · Actualizar parcialmente (PATCH) | `200` |

**Movimientos**

| Petición | Saldo resultante |
|---|---|
| Retiro de 575 – cuenta 478758 | 1425 |
| Depósito de 600 – cuenta 225487 | 700 |
| Depósito de 150 – cuenta 495878 | 150 |
| Retiro de 540 – cuenta 496825 | 0 |
| Retiro sin saldo (cuenta 496825, −100) | `400` "Saldo no disponible" |
| Actualizar movimiento 1 (retiro de 575 a 600) | `200`, saldo 1400 |
| Actualizar movimiento 4 dejando saldo negativo | `400` "Saldo no disponible" |
| Obtener movimiento inexistente (9999) | `404` "Movimiento no encontrado" |
| Listar · Listar por cuenta (`?accountId=2`) · Obtener por id | `200` |

**Reportes**

| Petición | Resultado |
|---|---|
| Estado de cuenta – Marianela Montalvo (cliente 2) | Movimientos de las cuentas 225487 y 496825 |
| Estado de cuenta – Jose Lema (cliente 1) | Movimientos de las cuentas 478758 y 585545 |

> Los reportes de la colección usan el rango `2026-10-01` a `2026-10-31`. Si los movimientos se crean en otra fecha, ajuste `startDate` y `endDate` para que los incluyan.

---

## Pruebas automatizadas

Cada microservicio se prueba por separado con `mvn test` desde su carpeta.

### ms-cliente-persona

Ubicadas en `src/test/java/com/devsu/cliente`:

| Clase | Tipo | Qué valida |
|---|---|---|
| `ClientUnitTest` | Unitaria | Comportamiento de la clase de dominio `Client` |
| `service/ClientServiceTest` | Unitaria (servicio) | Lógica de negocio de `ClientService` con dependencias simuladas |
| `controller/ClientControllerIntegrationTest` | Integración | Endpoints `/api/v1/clients` de punta a punta (controller, servicio y persistencia) |

```powershell
cd ms-cliente-persona
mvn test
```

### ms-cuenta-movimiento

Ubicadas en `src/test/java/com/devsu/cuenta`:

| Clase | Tipo | Qué valida |
|---|---|---|
| `service/AccountServiceTest` | Unitaria (Mockito) | Crear, PUT y PATCH de cuentas; cliente inexistente, servicio de clientes caído, cuenta duplicada |
| `service/MovementServiceTest` | Unitaria (Mockito) | Depósitos y retiros, saldo insuficiente, recálculo de saldos al actualizar un movimiento |
| `service/ReportServiceTest` | Unitaria (Mockito) | Filas del reporte, rango de fechas, varias cuentas y nombre de respaldo del cliente |
| `controller/AccountControllerTest`, `MovementControllerTest`, `ReportControllerTest` | Controller (MockMvc) | Códigos HTTP, cuerpo de respuesta y validación de parámetros |
| `exception/GlobalExceptionHandlerTest` | Unitaria (MockMvc) | Mapeo de cada excepción a su código HTTP y mensaje `{"mensaje": ...}` |
| `repository/AccountRepositoryTest`, `MovementRepositoryTest` | Repositorio (`@DataJpaTest`, H2) | Consultas derivadas, unicidad del número de cuenta y orden de los movimientos |
| `CuentaIntegrationTest` | Integración | Carga del contexto de Spring |
| `CuentaFlowIntegrationTest` | Integración | Flujo completo: cuenta, movimientos y reporte, con los servicios de clientes simulados |

```powershell
cd ms-cuenta-movimiento
mvn test
```

Las pruebas usan H2 en memoria y no requieren Docker ni PostgreSQL.

## Solución de problemas

| Síntoma | Causa probable y solución |
|---|---|
| El contenedor `ms-cliente-persona` falla con `Driver org.h2.Driver claims to not accept jdbcUrl` | El `application.properties` no debe fijar `spring.datasource.driverClassName`; Spring Boot lo deduce de la URL. |
| `POST /cuentas` responde `503` | `CLIENTE_SERVICE_URL` apunta a `localhost` o a una ruta incorrecta. Debe ser `http://ms-cliente-persona:8080/api/v1/clients`. |
| `POST /cuentas` responde `404` "Cliente no encontrado" | El cliente aún no existe: ejecute primero la carpeta *Clientes*. |
| Los cambios de código no se reflejan en Docker | Reconstruya: `docker compose up -d --build`. |
| Las peticiones de Postman devuelven datos o ids inesperados | Hay datos previos en el volumen: `docker compose down -v` y levante de nuevo. |
| Un microservicio arranca antes que PostgreSQL | El compose ya usa `depends_on` con `service_healthy`; revise los logs con `docker compose logs postgres`. |

## Tecnologías

Spring Boot, Spring Data JPA, Spring Cloud OpenFeign, PostgreSQL, H2, Lombok, JUnit 5, Mockito, Docker y Docker Compose.
