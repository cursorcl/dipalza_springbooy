# Diseño: Asociación Vendedor–Rutas

**Fecha:** 2026-07-16
**Proyectos afectados:** `dipalza.springboot/dipalza` (backend), `dipalza.springboot/base_de_datos` (scripts SQL), `flutterDipalza` (app móvil)
**Objetivo:** Permitir que un vendedor tenga asociadas múltiples rutas que cubre (relación muchos a muchos), y que esa selección se gestione desde la pantalla de Configuración de la app móvil mediante chips seleccionables.

---

## Alcance y decisiones previas

- Cardinalidad: **muchos a muchos**. Un vendedor cubre varias rutas; una ruta puede ser cubierta por varios vendedores.
- La tabla de asociación es un **vínculo simple** (sin estado activo/inactivo, sin fechas de auditoría adicionales).
- La opción "Ruta" (single-select) que hoy existe en la pantalla de Configuración del vendedor **se reemplaza** por "Rutas" (multi-select, chips), representando esta nueva asociación maestra.
- **Fuera de alcance / sin cambios:** el flujo de `LoginPage` (mantiene su propio picker single-select y sigue seteando `prefs.ruta`), la creación de venta (`venta.encabezado.edicion.page.dart`, no muestra la ruta al usuario, sigue enviando `prefs.ruta`) y `ClientesProvider.obtenerListaClientes` (ya trae todos los clientes del vendedor sin filtrar por ruta). Estos tres puntos consumen `prefs.ruta`, que sigue existiendo y sin relación con la nueva asociación vendedor-rutas.

---

## 1. Base de datos (`dipalza.springboot/base_de_datos`)

### Tabla nueva: `vendedor_ruta`

`vendedor` tiene PK compuesta (`codigo`, `tipo`), por lo que la tabla de asociación referencia ambas columnas. Sigue el patrón de nombres ya usado en `app_user_roles` (`PK_<tabla>`, `FK_<tabla>_<referenciada>`).

```sql
CREATE TABLE dbo.vendedor_ruta (
    codigo_vendedor varchar(3)  COLLATE Modern_Spanish_CI_AS NOT NULL,
    tipo_vendedor   varchar(1)  COLLATE Modern_Spanish_CI_AS NOT NULL,
    codigo_ruta     varchar(10) COLLATE Modern_Spanish_CI_AS NOT NULL,
    CONSTRAINT PK_vendedor_ruta PRIMARY KEY (codigo_vendedor, tipo_vendedor, codigo_ruta),
    CONSTRAINT FK_vendedor_ruta_vendedor FOREIGN KEY (codigo_vendedor, tipo_vendedor) REFERENCES dbo.vendedor(codigo, tipo),
    CONSTRAINT FK_vendedor_ruta_ruta FOREIGN KEY (codigo_ruta) REFERENCES dbo.ruta(codigo)
);
```

### Dónde se agrega

1. `db/install_dipalza_sync.sql` — DDL canónico. Se inserta justo después de las definiciones de `vendedor` y `ruta` (líneas ~157-174).
2. `archive/migration/migration_20260716.sql` — script incremental nuevo, mismo estilo que `migration_20260529.sql`:

```sql
-- Agrega tabla de asociación vendedor-rutas (rutas que cubre cada vendedor)
CREATE TABLE dbo.vendedor_ruta (
    codigo_vendedor varchar(3)  COLLATE Modern_Spanish_CI_AS NOT NULL,
    tipo_vendedor   varchar(1)  COLLATE Modern_Spanish_CI_AS NOT NULL,
    codigo_ruta     varchar(10) COLLATE Modern_Spanish_CI_AS NOT NULL,
    CONSTRAINT PK_vendedor_ruta PRIMARY KEY (codigo_vendedor, tipo_vendedor, codigo_ruta),
    CONSTRAINT FK_vendedor_ruta_vendedor FOREIGN KEY (codigo_vendedor, tipo_vendedor) REFERENCES dbo.vendedor(codigo, tipo),
    CONSTRAINT FK_vendedor_ruta_ruta FOREIGN KEY (codigo_ruta) REFERENCES dbo.ruta(codigo)
);
```

---

## 2. Backend Spring Boot (`dipalza.springboot/dipalza`, paquete `cl.eos.dipalza`)

### Clave compuesta: `entity/ids/VendedorRutaId.java`

`@Embeddable implements Serializable`, mismo estilo que `VendedorId`:

```java
@Embeddable
public class VendedorRutaId implements Serializable {
    @Column(name = "codigo_vendedor", length = 3, nullable = false)
    private String codigoVendedor;
    @Column(name = "tipo_vendedor", length = 1, nullable = false)
    private String tipoVendedor;
    @Column(name = "codigo_ruta", length = 10, nullable = false)
    private String codigoRuta;
    // getters/setters, equals, hashCode
}
```

### Entidad: `entity/VendedorRuta.java`

```java
@Entity
@Table(name = "vendedor_ruta", schema = "dbo")
public class VendedorRuta {
    @EmbeddedId
    private VendedorRutaId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "codigo_vendedor", referencedColumnName = "codigo", insertable = false, updatable = false),
        @JoinColumn(name = "tipo_vendedor", referencedColumnName = "tipo", insertable = false, updatable = false)
    })
    private Vendedor vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_ruta", referencedColumnName = "codigo", insertable = false, updatable = false)
    private Ruta ruta;
    // getters/setters
}
```

### Repositorio: `repository/VendedorRutaRepository.java`

```java
@Repository
public interface VendedorRutaRepository extends JpaRepository<VendedorRuta, VendedorRutaId> {
    List<VendedorRuta> findByIdCodigoVendedorAndIdTipoVendedor(String codigoVendedor, String tipoVendedor);
    void deleteByIdCodigoVendedorAndIdTipoVendedor(String codigoVendedor, String tipoVendedor);
}
```

### Servicio: `service/VendedorRutaService.java`

Constructor injection (`VendedorRutaRepository`, `VendedorRepository`, `RutaRepository`, `RutaMapper`).

- `List<RutaDTO> getRutasByVendedor(String codigo, String tipo)`: busca asociaciones existentes y mapea la `Ruta` de cada una con `RutaMapper`.
- `List<RutaDTO> asignarRutas(String codigo, String tipo, List<String> codigosRuta)`, `@Transactional`:
  1. Verifica que el vendedor exista (`VendedorRepository.findById`); si no, `ResponseStatusException(HttpStatus.NOT_FOUND, "Vendedor no encontrado")`.
  2. Verifica que cada código de ruta exista (`RutaRepository.findById`); si alguno no existe, `ResponseStatusException(HttpStatus.NOT_FOUND, "Ruta <codigo> no encontrada")`.
  3. **Reemplaza el set completo**: `deleteByIdCodigoVendedorAndIdTipoVendedor(...)` seguido de `saveAll(...)` con las nuevas asociaciones. Se elige reemplazo total (no altas/bajas incrementales) porque la UI de Flutter envía la selección completa de chips en cada guardado.
  4. Retorna `getRutasByVendedor(codigo, tipo)`.

Sin manejador de excepciones global nuevo — se sigue el patrón existente de `AuthController` (lanzar `ResponseStatusException` directamente desde el controller/servicio).

### Controlador: `controller/VendedorRutaController.java`

No existe hoy un `VendedorController`; se crea este controlador dedicado a la asociación:

```java
@RestController
@RequestMapping("/api/vendedores/{codigo}/{tipo}/rutas")
public class VendedorRutaController {
    // constructor injection de VendedorRutaService

    @GetMapping
    public List<RutaDTO> getRutas(@PathVariable String codigo, @PathVariable String tipo) { ... }

    @PutMapping
    public List<RutaDTO> setRutas(@PathVariable String codigo, @PathVariable String tipo,
                                   @RequestBody List<String> codigosRuta) { ... }
}
```

Delegación directa al servicio (controller delgado, igual que `RutaController`).

---

## 3. Flutter (`flutterDipalza`)

Se mantiene `prefs.ruta` (single) intacto para login/venta/clientes (ver "Fuera de alcance"). El nuevo flujo multi-select es independiente.

### `RutasPage` (`lib/src/page/rutas/rutas.page.dart`)

Se agregan parámetros opcionales, retrocompatibles con el uso actual desde `LoginPage`:

```dart
class RutasPage extends StatefulWidget {
  final bool multiSelect;
  final List<RutasModel> seleccionInicial;
  const RutasPage({Key? key, this.multiSelect = false, this.seleccionInicial = const []}) : super(key: key);
}
```

- Si `multiSelect == false` (default): comportamiento actual sin cambios (tap en fila → `AppNavigator.pop(ruta)` con un solo `RutasModel`).
- Si `multiSelect == true`: cada fila muestra un `Checkbox` (estado en `Set<String> _codigosSeleccionados`, inicializado desde `seleccionInicial`); el `AppBar` agrega una acción "Guardar" (ícono check) que hace `AppNavigator.pop(...)` con la `List<RutasModel>` filtrada por códigos seleccionados.

### `app_router.dart`

El case `AppRoutes.rutas` lee `settings.arguments` como `Map<String, dynamic>?` (mismo patrón usado en `ventaDetalle`/`ventaItemEdicion`) para extraer `multiSelect` y `seleccionInicial`; si no hay argumentos, construye `RutasPage()` como hoy.

### Nuevo `VendedorRutaProvider` (`lib/src/provider/vendedor_ruta_provider.dart`)

```dart
class VendedorRutaProvider {
  final _dio = ApiClient().dio;

  Future<List<RutasModel>> obtenerRutasAsignadas(String codigo, String tipo) async {
    final res = await _dio.get('/api/vendedores/$codigo/$tipo/rutas');
    return (res.data as List).map((j) => RutasModel.fromJson(j)).toList();
  }

  Future<List<RutasModel>> guardarRutasAsignadas(String codigo, String tipo, List<String> codigosRuta) async {
    final res = await _dio.put('/api/vendedores/$codigo/$tipo/rutas', data: codigosRuta);
    return (res.data as List).map((j) => RutasModel.fromJson(j)).toList();
  }
}
```

Manejo de errores por try/catch con `print` + retorno de lista vacía, igual que `ClientesProvider`.

### `PreferenciasUsuario` (`lib/src/share/prefs_usuario.dart`)

Nuevo par getter/setter para cachear localmente los códigos de las rutas asignadas (evita depender solo de la llamada de red al reabrir la pantalla):

```dart
List<String> get rutasAsignadas => _prefs.getStringList('rutasAsignadas') ?? [];
set rutasAsignadas(List<String> value) => _prefs.setStringList('rutasAsignadas', value);
```

### `ConfiguracionPage` (`lib/src/page/config/preferences.page.dart`)

- Reemplaza `RutasModel? _rutaSeleccionada` por `List<RutasModel> _rutasAsignadas = []`.
- `initState`: dispara `VendedorRutaProvider().obtenerRutasAsignadas(_prefs.vendedor, _prefs.tipo)` para hidratar `_rutasAsignadas`.
- Reemplaza el método `_pickRuta` por `_pickRutas`:

```dart
Future<void> _pickRutas() async {
  final seleccion = await AppNavigator.pushNamed(
    AppRoutes.rutas,
    arguments: {'multiSelect': true, 'seleccionInicial': _rutasAsignadas},
  );
  if (seleccion != null) {
    final nuevas = List<RutasModel>.from(seleccion);
    setState(() => _rutasAsignadas = nuevas);
    _prefs.rutasAsignadas = nuevas.map((r) => r.codigo).toList();
    await VendedorRutaProvider()
        .guardarRutasAsignadas(_prefs.vendedor, _prefs.tipo, _prefs.rutasAsignadas);
  }
}
```

- Reemplaza la `ListTile` "Ruta" (líneas 511-524) por una sección "Rutas" cuyo subtítulo/cuerpo muestra las seleccionadas como chips (`Wrap` de `Chip`, igual look que la sección "Recientes" ya existente en la misma pantalla), y cuyo `onTap` (en la fila o en un ícono de edición) llama a `_pickRutas`.

---

## Manejo de errores

- Backend: `ResponseStatusException` con `HttpStatus.NOT_FOUND` para vendedor o ruta inexistente al guardar asociaciones — sin handler global, siguiendo convención actual del proyecto.
- Flutter: los providers nuevos siguen el patrón try/catch + lista vacía de `ClientesProvider`/`VendedorProvider`; errores no bloquean la UI, se puede reintentar reabriendo el picker.

## Testing

- Backend: tests de servicio (`VendedorRutaServiceTest`, Mockito puro) cubriendo: vendedor no encontrado, ruta no encontrada, reemplazo correcto del set completo (delete + saveAll), listado vacío. Tests de controller (`@WebMvcTest`) verificando `GET`/`PUT` y códigos de estado, siguiendo la Capa 1/Capa 2 documentadas en `2026-07-12-tests-springboot-design.md`.
- Flutter: no hay suite de tests automatizados existente para providers/páginas de este estilo en el repo; verificación manual del flujo (abrir Configuración → seleccionar rutas con chips → guardar → reabrir y confirmar persistencia) queda como paso de verificación funcional, no como test automatizado nuevo.
