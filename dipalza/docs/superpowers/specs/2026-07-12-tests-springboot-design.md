# Diseño: Tests Spring Boot — cobertura completa sin BD activa

**Fecha:** 2026-07-12  
**Proyecto:** dipalza.springboot  
**Objetivo:** Llevar cobertura de instrucciones de 31% a ~65% mediante 42 nuevos tests distribuidos en tres capas, sin depender de SQL Server activo.

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

| Capa | Tests |
|---|---|
| Unitarios (Mockito) | 22 nuevos + 8 existentes = 30 |
| Controller slice (@WebMvcTest) | 54 nuevos + 5 existentes (ProductoController) = 59 |
| Integration (@SpringBootTest) | 4 |
| **Total** | **80 tests** |

## Cobertura esperada

| Paquete | Antes | Después |
|---|---|---|
| `service` | ~35% | ~70% |
| `controller` | ~10% | ~80% |
| `mapper` | ~0% | ~80% |
| `utils` | ~0% | ~100% |
| **Total instrucciones** | **31%** | **~70%** |

---

## Archivos a crear

```
src/test/java/cl/eos/dipalza/
  service/
    JwtServiceTest.java
    NumeradosServiceTest.java
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
