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

## Cobertura esperada

| Paquete | Antes | Después |
|---|---|---|
| `service` | ~35% | ~70% |
| `controller` | ~10% | ~65% |
| `mapper` | ~0% | ~80% |
| `utils` | ~0% | ~100% |
| **Total instrucciones** | **31%** | **~65%** |

---

## Archivos a crear

```
src/test/java/cl/eos/dipalza/
  service/
    JwtServiceTest.java                          (nuevo)
    NumeradosServiceTest.java                    (nuevo)
  mapper/
    VentaMapperTest.java                         (nuevo)
  utils/
    UtilsTest.java                               (nuevo)
  controller/
    AuthControllerTest.java                      (nuevo)
    VentaControllerTest.java                     (nuevo)
  ApplicationContextIT.java                      (nuevo)

src/test/resources/
  application-dev-sec.properties                (nuevo)
```
