# Diseño: Tests Spring Boot — cobertura completa sin BD activa

**Fecha:** 2026-07-12  
**Proyecto:** dipalza.springboot  
**Objetivo:** Cobertura completa de servicios y controllers mediante tests distribuidos en tres capas, sin depender de SQL Server activo.

---

## Arquitectura

```
Capa 1: Unit puro (Mockito)
  @ExtendWith(MockitoExtension) — sin Spring, sin BD

Capa 2: Controller slice (@WebMvcTest + @MockBean)
  Spring MVC cargado, MockMvc para HTTP, servicios mockeados

Capa 3: Integration (@SpringBootTest + @MockBean repos)
  Contexto Spring completo, repositorios mockeados, H2 en classpath
```

**Perfil activo en tests:** `dev-sec` (activa seguridad JWT y `AuthController`).  
**BD:** H2 en classpath solo para que JPA no falle al arrancar; todos los repositorios relevantes quedan como `@MockBean`.

---

## Capa 1 — Tests unitarios puros

### `JwtServiceTest` (5 tests)

Clase bajo test: `cl.eos.dipalza.service.JwtService`  
Setup: inyección de campos via reflection (`@Value` → `@InjectMocks` + setter o constructor propio en el test).

| Test | Escenario | Resultado esperado |
|---|---|---|
| `generateAccess_incluyeRoles` | User con rol ADMIN | claim "roles" presente en payload |
| `generateRefresh_noIncluyeRoles` | User cualquiera | claim "roles" ausente en payload |
| `extractUsername_retornaSubject` | Token válido generado | username correcto |
| `parse_tokenExpirado_lanzaExcepcion` | Token generado con expiración en el pasado | `JwtException` |
| `parse_tokenInvalido_lanzaExcepcion` | String basura | `JwtException` |

### `VentaMapperTest` (5 tests)

Clase bajo test: `cl.eos.dipalza.mapper.VentaMapper` (métodos estáticos, sin mocks).

| Test | Escenario | Resultado esperado |
|---|---|---|
| `toVentaDTO_ventaCompleta_mapeaTodosLosCampos` | Venta con cliente, vendedor, ruta, condición, detalles | DTO con todos los campos poblados |
| `toVentaDTO_clienteNull_noCrash` | Venta sin cliente | rutaCliente/codigoCliente null, sin NPE |
| `toVentaDTO_vendedorNull_noCrash` | Venta sin vendedor | codigoVendedor null, sin NPE |
| `toVentaDetalleDTO_conProducto_mapeaCampos` | VentaDetalle con producto | articulo e idProducto correctos |
| `toVentaDetalleDTO_productoNull_noCrash` | VentaDetalle sin producto | idProducto null, nombreProducto vacío |

### `UtilsTest` (4 tests)

Clase bajo test: `cl.eos.dipalza.utils.Utils` (métodos estáticos).

| Test | Escenario | Resultado esperado |
|---|---|---|
| `putZeroesAtBegin_rellenaConCeros` | number=5, len=3 | "005" |
| `putZeroesAtBegin_noRellena_cuandoYaTieneLargo` | number=123, len=3 | "123" |
| `putStrAtBegin_rellenaConChar` | "AB", '#', 5 | "###AB" |
| `putStrAtBegin_noRellena_cuandoYaTieneLargo` | "ABCDE", '#', 3 | "ABCDE" |

### `NumeradosServiceTest` (8 tests)

Clase bajo test: `cl.eos.dipalza.service.NumeradosService`  
Mocks: `NumeradoRepository`, `ProductoRepository`, `NumeradoMapper`.

| Test | Escenario | Resultado esperado |
|---|---|---|
| `findAll_listaVacia_retornaListaVacia` | repo devuelve `[]` | `List.of()` |
| `findAll_conDatos_mapea` | repo devuelve 2 numerados | lista de 2 DTOs |
| `findById_noEncontrado_retornaNull` | `findById` vacío | `null` |
| `findById_encontrado_retornaDTO` | `findById` presente | DTO mapeado |
| `findPrecioPromedio_listaVacia_retornaCero` | repo vacío | `0f` |
| `findPrecioPromedio_conDatos_calculaPromedio` | pesos [10.0, 20.0] | `15.0f` |
| `save_productoNoExiste_retornaNull` | `findByArticulo` → null | `null` |
| `save_numeradoNuevo_creaYRetornaDTO` | numerado no existe en repo | numerado creado con datos del DTO |

### `ClienteServiceTest` (7 tests)

Mocks: `ClienteRepository`, `ClienteMapper`.

| Test | Escenario | Resultado esperado |
|---|---|---|
| `getAllClientes_listaVacia_retornaVacia` | repo vacío | `List.of()` |
| `getAllClientes_conDatos_mapea` | repo devuelve 2 clientes | lista de 2 DTOs |
| `getClientesByRuta_retornaFiltrados` | repo devuelve clientes de ruta | lista mapeada |
| `getClienteById_encontrado_retornaDTO` | `findById` presente | `Optional` con DTO |
| `getClienteById_noEncontrado_retornaVacio` | `findById` vacío | `Optional.empty()` |
| `createOrUpdate_guardaYRetornaDTO` | mapper + repo.save | DTO guardado |
| `delete_encontrado_retornaTrue` | `existsById` true | true + deleteById invocado |
| `delete_noEncontrado_retornaFalse` | `existsById` false | false, sin deleteById |

### `CondicionVentaServiceTest` (5 tests)

Mocks: `CondicionVentaRepository`, `CondicionVentaMapper`.

| Test | Escenario | Resultado esperado |
|---|---|---|
| `getAll_retornaListaMapeada` | repo devuelve 2 entidades | lista de 2 DTOs |
| `getById_encontrado` | `findById` presente | `Optional` con DTO |
| `getById_noEncontrado` | `findById` vacío | `Optional.empty()` |
| `createOrUpdate_guardaYRetorna` | repo.save OK | DTO guardado |
| `delete_encontrado_retornaTrue` | `existsById` true | true |
| `delete_noEncontrado_retornaFalse` | `existsById` false | false |

### `ConduccionServiceTest` (5 tests)

Mocks: `ConduccionRepository`, `ConduccionMapper`. Misma estructura que `CondicionVentaServiceTest`.

| Test | Escenario | Resultado esperado |
|---|---|---|
| `getAll_retornaListaMapeada` | repo devuelve 2 entidades | lista de 2 DTOs |
| `getById_encontrado` | `findById` presente | `Optional` con DTO |
| `getById_noEncontrado` | `findById` vacío | `Optional.empty()` |
| `createOrUpdate_guardaYRetorna` | repo.save OK | DTO guardado |
| `delete_encontrado_retornaTrue` | `existsById` true | true |

### `IlaServiceTest` (5 tests)

Mocks: `IlaRepository`, `IlaMapper`. Mismo patrón CRUD.

| Test | Escenario | Resultado esperado |
|---|---|---|
| `findAllByOrderByDescripcionAsc_mapea` | repo devuelve ordenados | lista mapeada |
| `getById_encontrado` | presente | `Optional` con DTO |
| `getById_noEncontrado` | vacío | `Optional.empty()` |
| `createOrUpdate_guardaYRetorna` | repo.save OK | DTO guardado |
| `delete_encontrado_retornaTrue` | `existsById` true | true |

### `RutaServiceTest` (6 tests)

Mocks: `RutaRepository`, `RutaMapper`, `ConduccionRepository`.

| Test | Escenario | Resultado esperado |
|---|---|---|
| `getAll_retornaListaMapeada` | repo devuelve 2 rutas | lista de 2 DTOs |
| `getRutaById_encontrado` | presente | `Optional` con DTO |
| `getRutaById_noEncontrado` | vacío | `Optional.empty()` |
| `createOrUpdate_usaConduccionMatcheada` | conduccion con código igual al DTO | ruta guardada con esa conduccion |
| `createOrUpdate_usaPrimeraConduccion_cuandoNoHayMatch` | código no existe en lista | usa `getFirst()` |
| `delete_noEncontrado_retornaFalse` | `existsById` false | false |

### `ProductoServiceTest` (6 tests)

Mocks: `ProductoRepository`, `ProductoMapper`.

| Test | Escenario | Resultado esperado |
|---|---|---|
| `getAllProductos_mapea` | repo devuelve 3 | lista de 3 DTOs |
| `getProductosByDescripcion_filtra` | repo devuelve 1 | lista de 1 DTO |
| `findById_encontrado` | presente | `Optional` con DTO |
| `findById_noEncontrado` | vacío | `Optional.empty()` |
| `createOrUpdate_guardaYRetorna` | save OK | DTO guardado |
| `delete_encontrado_retornaTrue` | `existsById` true | true |

### `VentaDetalleServiceTest` (2 tests)

Mocks: `VentaDetalleRepository`.

| Test | Escenario | Resultado esperado |
|---|---|---|
| `listarDetallesOptimized_conDatos_mapea` | repo devuelve 2 detalles | lista de 2 DTOs |
| `listarDetallesOptimized_listaVacia` | repo vacío | lista vacía |

### `ConfiguracionServiceTest` (7 tests)

Mocks: `ConfiguracionRepository`. Sin Spring (lógica de cache pura).

| Test | Escenario | Resultado esperado |
|---|---|---|
| `getString_claveExiste_retornaValor` | cache con clave "K" → "val" | "val" |
| `getString_claveNoExiste_retornaVacio` | cache sin la clave | "" |
| `getInt_valorNumerico_retornaInt` | cache "42" | 42 |
| `getInt_valorNoNumerico_retornaCero` | cache "abc" | 0 |
| `getDouble_valorDecimal_retornaDouble` | cache "3.14" | 3.14 |
| `getBoolean_true_retornaTrue` | cache "true" | true |
| `actualizarConfig_claveExiste_actualizaCacheYRepo` | repo devuelve config existente | cache actualizado, repo.save invocado |

### `RefreshTokenServiceTest` (2 tests)

Mocks: `RefreshTokenRepo`.

| Test | Escenario | Resultado esperado |
|---|---|---|
| `purgeExpiredTokens_invocaDeleteByExpiresAtBefore` | método scheduled ejecutado | `deleteByExpiresAtBefore(Instant)` invocado |
| `purgeExpiredTokens_pasaInstantAnteriorOIgualAhora` | el `Instant` pasado no es futuro | argumento capturado ≤ `Instant.now()` |

### `PosicionServiceTest` (5 tests)

Mocks: `PosicionRepository`, `HistorialPosicionRepository`, `VendedorRepository`, `SimpMessagingTemplate`.

| Test | Escenario | Resultado esperado |
|---|---|---|
| `obtenerActuales_mapea` | repo devuelve 2 posiciones | lista de 2 DTOs |
| `obtenerActuales_vacio` | repo vacío | lista vacía |
| `registrarUbicacion_nuevaPosicion_creaYGuarda` | `findByVendedorId` null | nueva `Posicion` creada + historial insertado |
| `registrarUbicacion_posicionExistente_actualizaYGuarda` | `findByVendedorId` retorna entidad | posición actualizada + historial insertado |
| `registrarUbicacion_enviaWebSocket` | cualquier registro | `messagingTemplate.convertAndSend` invocado |

---

## Capa 2 — Controller slice (@WebMvcTest)

Anotación base: `@WebMvcTest(value = XController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)`

### `AuthControllerTest` (7 tests)

Mocks: `UserRepo`, `VendedorRepository`, `PasswordEncoder`, `JwtService`, `RefreshTokenRepo`.  
Activa perfil `dev-sec`.

| Test | HTTP | Escenario | Status |
|---|---|---|---|
| `login_credencialesValidas_retornaTokens` | POST /auth/login | user enabled, password OK | 200 + body con `accessToken` |
| `login_usuarioNoEncontrado_retorna401` | POST /auth/login | `findByUsername` vacío | 401 |
| `login_cuentaDeshabilitada_retorna401` | POST /auth/login | `isEnabled()` = false | 401 |
| `login_cuentaBloqueada_retorna401` | POST /auth/login | `isLocked()` = true | 401 |
| `login_passwordIncorrecto_retorna401` | POST /auth/login | `enc.matches` = false | 401 |
| `refresh_tokenValido_retornaTokens` | POST /auth/refresh | token en repo, no revocado, no expirado | 200 |
| `refresh_tokenRevocado_retorna401` | POST /auth/refresh | token.isRevoked() = true | 401 |

### `VentaControllerTest` (9 tests)

Mock: `VentaService`, `NumeradoRepository`.

| Test | HTTP | Escenario | Status |
|---|---|---|---|
| `listarVentas_sinFiltros_retorna200` | GET /api/ventas | servicio devuelve lista vacía | 200 + `[]` |
| `listarPending_porVendedor_retorna200` | GET /api/ventas/pending/V01 | servicio devuelve lista | 200 |
| `grabarVenta_idNull_crea` | POST /api/ventas | id null → `crearVenta` | 200 + DTO |
| `grabarVenta_idExiste_actualiza` | POST /api/ventas | id != -1 → `actualizarVenta` | 200 + DTO |
| `eliminarVenta_encontrado_retorna204` | DELETE /api/ventas/1 | servicio devuelve true | 204 |
| `eliminarVenta_noEncontrado_retorna404` | DELETE /api/ventas/99 | servicio devuelve false | 404 |
| `eliminarVenta_facturada_retorna409` | DELETE /api/ventas/1 | servicio lanza `IllegalStateException` | 409 |
| `updateEstadoVenta_estadoValido_retorna200` | POST /api/ventas/updateEstadoVenta | estado "OPENED" | 200 |
| `updateEstadoVenta_estadoInvalido_retorna400` | POST /api/ventas/updateEstadoVenta | estado "GARBAGE" | 400 |

### `ClienteControllerTest` (10 tests)

Mock: `ClienteService`.

| Test | HTTP | Escenario | Status |
|---|---|---|---|
| `getAll_retornaLista` | GET /api/clientes | servicio devuelve 2 clientes | 200 + lista |
| `getByRuta_retornaLista` | GET /api/clientes/ruta/R01 | filtrado por ruta | 200 + lista |
| `getById_encontrado_retorna200` | GET /api/clientes/12345678 | `getClienteById` presente | 200 + DTO |
| `getById_noEncontrado_retorna404` | GET /api/clientes/99999999 | `getClienteById` vacío | 404 |
| `getByVendedor_retornaLista` | GET /api/clientes/vendedor?codigoVendedor=V01 | filtrado por vendedor | 200 + lista |
| `create_retornaDTO` | POST /api/clientes | `createOrUpdateCliente` devuelve DTO | 200 |
| `update_encontrado_retorna200` | PUT /api/clientes/12345678/001 | cliente existe → actualiza | 200 |
| `update_noEncontrado_retorna404` | PUT /api/clientes/99999999/001 | cliente no existe | 404 |
| `delete_encontrado_retorna204` | DELETE /api/clientes/12345678/001 | servicio devuelve true | 204 |
| `delete_noEncontrado_retorna404` | DELETE /api/clientes/99999999/001 | servicio devuelve false | 404 |

### `NumeradosControllerTest` (8 tests)

Mock: `NumeradosService`.

| Test | HTTP | Escenario | Status |
|---|---|---|---|
| `getAll_retornaLista` | GET /api/numerados | lista de DTOs | 200 |
| `getByProduct_retornaLista` | GET /api/numerados/byProduct?codigoProducto=ART01 | filtrado por producto | 200 |
| `getResumen_retornaLista` | GET /api/numerados/resumen | lista agrupada | 200 |
| `getByEstado_retornaLista` | GET /api/numerados/estados?estado=D | filtrado por estado | 200 |
| `create_retornaDTO` | POST /api/numerados | save crea nuevo | 200 |
| `update_retornaDTO` | PUT /api/numerados | save actualiza existente | 200 |
| `delete_ejecutaSinError` | DELETE /api/numerados | deleteById invocado | 200 |
| `pesoPorArticulo_retornaFloat` | GET /api/numerados/pesopromedio/ART01 | promedio calculado | 200 |

### `PingControllerTest` (1 test)

Sin mock (sin dependencias).

| Test | HTTP | Escenario | Status |
|---|---|---|---|
| `ping_retornaStatusUp` | GET /ping | respuesta con `status: UP` | 200 |

### `RutaControllerTest` (3 tests)

Mock: `RutaService`.

| Test | HTTP | Escenario | Status |
|---|---|---|---|
| `getAll_retornaLista` | GET /api/rutas | lista de DTOs | 200 |
| `getById_encontrado_retorna200` | GET /api/rutas/R01 | ruta presente | 200 |
| `getById_noEncontrado_retorna404` | GET /api/rutas/X99 | vacío | 404 |

### `CondicionVentaControllerTest` (3 tests)

Mock: `CondicionVentaService`.

| Test | HTTP | Escenario | Status |
|---|---|---|---|
| `getAll_retornaLista` | GET /api/condicionventa | lista de DTOs | 200 |
| `getById_encontrado_retorna200` | GET /api/condicionventa/C01 | presente | 200 |
| `getById_noEncontrado_retorna404` | GET /api/condicionventa/X99 | vacío | 404 |

### `ConduccionControllerTest` (3 tests)

Mock: `ConduccionService`.

| Test | HTTP | Escenario | Status |
|---|---|---|---|
| `getAll_retornaLista` | GET /api/conduccion | lista de DTOs | 200 |
| `getById_encontrado_retorna200` | GET /api/conduccion/C01 | presente | 200 |
| `getById_noEncontrado_retorna404` | GET /api/conduccion/X99 | vacío | 404 |

### `IlaControllerTest` (3 tests)

Mock: `IlaService`.

| Test | HTTP | Escenario | Status |
|---|---|---|---|
| `getAll_retornaLista` | GET /api/ila | lista de DTOs | 200 |
| `getById_encontrado_retorna200` | GET /api/ila/I01 | presente | 200 |
| `getById_noEncontrado_retorna404` | GET /api/ila/X99 | vacío | 404 |

### `PosicionControllerTest` (3 tests)

Mock: `PosicionService`.

| Test | HTTP | Escenario | Status |
|---|---|---|---|
| `obtenerPosiciones_retorna200` | GET /api/posicion | lista de DTOs | 200 |
| `obtenerHistorico_retorna200` | POST /api/posicion/historico | filtro en body | 200 |
| `registrarPosicion_retorna202` | POST /api/posicion | body con posición | 202 |

### `VentaDetalleControllerTest` (2 tests)

Mock: `VentaDetalleService`.

| Test | HTTP | Escenario | Status |
|---|---|---|---|
| `listarDetalles_retornaLista` | GET /api/ventadetalle/1 | lista de DTOs | 200 |
| `listarDetalles_listaVacia_retorna200` | GET /api/ventadetalle/99 | servicio devuelve `[]` | 200 |

### `FacturacionControllerTest` (2 tests)

Mock: `FacturacionService`.

| Test | HTTP | Escenario | Status |
|---|---|---|---|
| `facturar_conResultados_retorna200` | POST /api/facturacion | servicio devuelve lista | 200 |
| `facturar_sinResultados_retorna204` | POST /api/facturacion | servicio devuelve lista vacía | 204 |

---

## Capa 3 — Integration (@SpringBootTest)

Clase: `ApplicationContextIT`  
Anotaciones: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, perfil `dev-sec`.  
`@MockBean`: todos los repositorios JPA + servicios que acceden a BD.  
H2 en `scope=test` en `pom.xml` para que el contexto JPA arranque.

| Test | Qué valida |
|---|---|
| `contextLoads` | El contexto Spring arranca sin excepción |
| `endpointsProtegidos_sinToken_retorna401` | `GET /api/ventas` sin Authorization → 401 |
| `pingEndpoint_esPublico` | `GET /ping` → 200 sin token |
| `loginConCredencialesValidas_retornaTokens` | Flujo completo: POST /auth/login con mocks configurados → 200 + `accessToken` presente |

---

## Cambios en pom.xml

1. H2 ya agregado como `scope=test` (necesario para contexto JPA).  
   JaCoCo ya configurado (sesión anterior).

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

2. Properties de test en `src/test/resources/application-dev-sec.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
security.jwt.secret=test-secret-key-that-is-long-enough-for-hs256
security.jwt.issuer=test-issuer
security.jwt.access-minutes=60
security.jwt.refresh-hr=24
```

---

## Resumen de tests

| Capa | Clase | Tests nuevos |
|---|---|---|
| **Unitario — Servicios** | JwtServiceTest | 5 |
| | NumeradosServiceTest | 8 |
| | ClienteServiceTest | 8 |
| | CondicionVentaServiceTest | 6 |
| | ConduccionServiceTest | 5 |
| | IlaServiceTest | 5 |
| | RutaServiceTest | 6 |
| | ProductoServiceTest | 6 |
| | VentaDetalleServiceTest | 2 |
| | ConfiguracionServiceTest | 7 |
| | RefreshTokenServiceTest | 2 |
| | PosicionServiceTest | 5 |
| **Unitario — Mappers/Utils** | VentaMapperTest | 5 |
| | UtilsTest | 4 |
| **Controller slice** | AuthControllerTest | 7 |
| | VentaControllerTest | 9 |
| | ClienteControllerTest | 10 |
| | NumeradosControllerTest | 8 |
| | PingControllerTest | 1 |
| | RutaControllerTest | 3 |
| | CondicionVentaControllerTest | 3 |
| | ConduccionControllerTest | 3 |
| | IlaControllerTest | 3 |
| | PosicionControllerTest | 3 |
| | VentaDetalleControllerTest | 2 |
| | FacturacionControllerTest | 2 |
| **Integration** | ApplicationContextIT | 4 |
| **Ya existentes** | VentaServiceTotalesTest | 8 |
| | ProductoControllerTest | 5 |
| | FacturacionServiceTest/IT | ~10 |
| | VentaItemProcessor*Test | ~15 |
| **TOTAL NUEVOS** | | **128** |
| **TOTAL PROYECTO** | | **~165** |

## Cobertura esperada

| Paquete | Antes | Después |
|---|---|---|
| `service` | ~35% | ~80% |
| `controller` | ~10% | ~85% |
| `mapper` | ~0% | ~80% |
| `utils` | ~0% | ~100% |
| **Total instrucciones** | **31%** | **~75%** |

---

## Archivos a crear

```
src/test/java/cl/eos/dipalza/
  service/
    JwtServiceTest.java
    NumeradosServiceTest.java
    ClienteServiceTest.java
    CondicionVentaServiceTest.java
    ConduccionServiceTest.java
    IlaServiceTest.java
    RutaServiceTest.java
    ProductoServiceTest.java
    VentaDetalleServiceTest.java
    ConfiguracionServiceTest.java
    RefreshTokenServiceTest.java
    PosicionServiceTest.java
  mapper/
    VentaMapperTest.java
  utils/
    UtilsTest.java
  controller/
    AuthControllerTest.java
    VentaControllerTest.java
    ClienteControllerTest.java
    NumeradosControllerTest.java
    PingControllerTest.java
    RutaControllerTest.java
    CondicionVentaControllerTest.java
    ConduccionControllerTest.java
    IlaControllerTest.java
    PosicionControllerTest.java
    VentaDetalleControllerTest.java
    FacturacionControllerTest.java
  ApplicationContextIT.java

src/test/resources/
  application-dev-sec.properties
```
