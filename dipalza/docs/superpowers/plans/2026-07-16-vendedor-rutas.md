# Asociación Vendedor–Rutas Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Un vendedor puede tener asociadas múltiples rutas (relación M:N); esa asociación se gestiona desde Configuración (chips multi-select) y desde el login (selección forzada si no hay ninguna configurada); la creación de venta deja de depender de una "ruta activa del vendedor" y usa la ruta del cliente facturado.

**Architecture:** Tabla `vendedor_ruta` (ya creada en dev) + entidad/repo/servicio/controller Spring Boot expuestos en `/api/vendedores/{codigo}/{tipo}/rutas` (GET/PUT, reemplazo total del set en cada guardado). En Flutter, `RutasPage` gana dos modos opcionales (`multiSelect`, `obligatorio`) reusados tanto por Configuración como por el login; un `VendedorRutaProvider` nuevo habla con el endpoint.

**Tech Stack:** Spring Boot 3 / Jakarta Persistence / Maven (`./mvnw`), JUnit 5 + Mockito + AssertJ (`@ExtendWith(MockitoExtension.class)`) para servicio, `@WebMvcTest` + MockMvc para controller. Flutter/Dart, Dio, RxDart (BLoC), SharedPreferences.

## Global Constraints

- Backend: sin manejador de excepciones global nuevo — usar `ResponseStatusException` directamente (patrón de `AuthController`).
- Backend: controllers delgados, delegan 100% al service (patrón de `RutaController`).
- Backend: constructor injection siempre, nunca `@Autowired` de campo.
- Flutter: sí existe una suite de tests automatizados (`flutter test`, baseline: 26 tests pasando en `test/unit/` y `test/widget/`) aunque no cubre páginas/providers de este estilo específico — no inventar tests nuevos para código sin ese patrón, pero **toda tarea que modifique un archivo con test existente debe correr `flutter test` completo y no dejar ningún test roto** (ver Task 7, que corrige una aserción que codificaba el bug de `ClientesModel.ruta`). Además de eso, verificar con `flutter analyze` y el checklist manual de cada task.
- Flutter: mantener retrocompatibilidad — `RutasPage()` sin argumentos debe comportarse exactamente igual que hoy (usado por `LoginPage` antes del Task 9, y como fallback en el router).
- No tocar `ClientesProvider.obtenerListaClientes`, `clientes.page.dart`, ni el getter/setter `PreferenciasUsuario.ruta` (quedan tal cual, ver spec).
- La tabla `dbo.vendedor_ruta` ya existe en dev (migración `migration_20260716.sql` ya aplicada y commiteada) — no repetir ese paso.
- Backend: todo comando `./mvnw` debe incluir `-Dfrontend.skip=true` — el `frontend-maven-plugin` del `pom.xml` apunta a `../../dipalzaSpringbootClient` (ruta relativa que no existe dentro de este worktree) y falla si no se omite.
- Este plan se ejecuta dentro de worktrees dedicados: backend en `dipalza.springboot/.worktrees/feature-vendedor-rutas/dipalza` (rama `feature/vendedor-rutas`), Flutter en `flutterDipalza/.worktrees/feature-vendedor-rutas` (rama `feature/vendedor-rutas`). Todas las rutas de archivo del plan ya están actualizadas a estas ubicaciones.

---

## Backend (`dipalza.springboot/dipalza`)

### Task 1: Entidad `VendedorRuta`, clave compuesta y repositorio

**Files:**
- Create: `dipalza/src/main/java/cl/eos/dipalza/entity/ids/VendedorRutaId.java`
- Create: `dipalza/src/main/java/cl/eos/dipalza/entity/VendedorRuta.java`
- Create: `dipalza/src/main/java/cl/eos/dipalza/repository/VendedorRutaRepository.java`

**Interfaces:**
- Produces: `VendedorRutaId(String codigoVendedor, String tipoVendedor, String codigoRuta)`, `VendedorRuta` con `getId()/setId(VendedorRutaId)`, `getVendedor()/setVendedor(Vendedor)`, `getRuta()/setRuta(Ruta)`. `VendedorRutaRepository extends JpaRepository<VendedorRuta, VendedorRutaId>` con `findByIdCodigoVendedorAndIdTipoVendedor(String, String)` y `deleteByIdCodigoVendedorAndIdTipoVendedor(String, String)`.
- No hay tests de entidad en este proyecto (mismo patrón que `Ruta`/`Vendedor`); se verifica con compilación.

- [ ] **Step 1: Crear `VendedorRutaId`**

```java
package cl.eos.dipalza.entity.ids;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class VendedorRutaId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "codigo_vendedor", length = 3, nullable = false)
    private String codigoVendedor;
    @Column(name = "tipo_vendedor", length = 1, nullable = false)
    private String tipoVendedor;
    @Column(name = "codigo_ruta", length = 10, nullable = false)
    private String codigoRuta;

    public VendedorRutaId() {}

    public VendedorRutaId(String codigoVendedor, String tipoVendedor, String codigoRuta) {
        this.codigoVendedor = codigoVendedor;
        this.tipoVendedor = tipoVendedor;
        this.codigoRuta = codigoRuta;
    }

    public String getCodigoVendedor() { return codigoVendedor; }
    public void setCodigoVendedor(String codigoVendedor) { this.codigoVendedor = codigoVendedor; }
    public String getTipoVendedor() { return tipoVendedor; }
    public void setTipoVendedor(String tipoVendedor) { this.tipoVendedor = tipoVendedor; }
    public String getCodigoRuta() { return codigoRuta; }
    public void setCodigoRuta(String codigoRuta) { this.codigoRuta = codigoRuta; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VendedorRutaId that)) return false;
        return Objects.equals(codigoVendedor, that.codigoVendedor)
                && Objects.equals(tipoVendedor, that.tipoVendedor)
                && Objects.equals(codigoRuta, that.codigoRuta);
    }

    @Override
    public int hashCode() { return Objects.hash(codigoVendedor, tipoVendedor, codigoRuta); }
}
```

- [ ] **Step 2: Crear la entidad `VendedorRuta`**

```java
package cl.eos.dipalza.entity;

import cl.eos.dipalza.entity.ids.VendedorRutaId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

    public VendedorRutaId getId() { return id; }
    public void setId(VendedorRutaId id) { this.id = id; }
    public Vendedor getVendedor() { return vendedor; }
    public void setVendedor(Vendedor vendedor) { this.vendedor = vendedor; }
    public Ruta getRuta() { return ruta; }
    public void setRuta(Ruta ruta) { this.ruta = ruta; }
}
```

- [ ] **Step 3: Crear el repositorio**

```java
package cl.eos.dipalza.repository;

import cl.eos.dipalza.entity.VendedorRuta;
import cl.eos.dipalza.entity.ids.VendedorRutaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendedorRutaRepository extends JpaRepository<VendedorRuta, VendedorRutaId> {
    List<VendedorRuta> findByIdCodigoVendedorAndIdTipoVendedor(String codigoVendedor, String tipoVendedor);
    void deleteByIdCodigoVendedorAndIdTipoVendedor(String codigoVendedor, String tipoVendedor);
}
```

- [ ] **Step 4: Compilar**

Run: `cd /Users/cursor/Dev/dipalza/dipalza.springboot/.worktrees/feature-vendedor-rutas/dipalza && ./mvnw -q compile -Dfrontend.skip=true`
Expected: termina sin errores (sin salida, `-q` solo imprime en caso de fallo).

- [ ] **Step 5: Commit**

```bash
cd /Users/cursor/Dev/dipalza/dipalza.springboot/.worktrees/feature-vendedor-rutas/dipalza
git add src/main/java/cl/eos/dipalza/entity/ids/VendedorRutaId.java \
        src/main/java/cl/eos/dipalza/entity/VendedorRuta.java \
        src/main/java/cl/eos/dipalza/repository/VendedorRutaRepository.java
git commit -m "feat: agrega entidad VendedorRuta y su repositorio

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

### Task 2: `VendedorRutaService` (TDD)

**Files:**
- Test: `dipalza/src/test/java/cl/eos/dipalza/service/VendedorRutaServiceTest.java`
- Create: `dipalza/src/main/java/cl/eos/dipalza/service/VendedorRutaService.java`

**Interfaces:**
- Consumes: `VendedorRutaRepository` (Task 1), `VendedorRepository.findById(VendedorId)`, `RutaRepository.findById(String)`, `RutaMapper.toDTO(Ruta)` (ya existen en el proyecto).
- Produces: `VendedorRutaService(VendedorRutaRepository, VendedorRepository, RutaRepository, RutaMapper)` con `List<RutaDTO> getRutasByVendedor(String codigo, String tipo)` y `List<RutaDTO> asignarRutas(String codigo, String tipo, List<String> codigosRuta)` (lanza `ResponseStatusException` 404 si el vendedor o alguna ruta no existen).

- [ ] **Step 1: Escribir el test (debe fallar — la clase no existe aún)**

```java
package cl.eos.dipalza.service;

import cl.eos.dipalza.entity.Ruta;
import cl.eos.dipalza.entity.Vendedor;
import cl.eos.dipalza.entity.VendedorRuta;
import cl.eos.dipalza.entity.ids.VendedorId;
import cl.eos.dipalza.entity.ids.VendedorRutaId;
import cl.eos.dipalza.mapper.RutaMapper;
import cl.eos.dipalza.model.RutaDTO;
import cl.eos.dipalza.repository.RutaRepository;
import cl.eos.dipalza.repository.VendedorRepository;
import cl.eos.dipalza.repository.VendedorRutaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendedorRutaServiceTest {

    @Mock VendedorRutaRepository vendedorRutaRepo;
    @Mock VendedorRepository vendedorRepo;
    @Mock RutaRepository rutaRepo;
    @Mock RutaMapper rutaMapper;
    @InjectMocks VendedorRutaService service;

    private VendedorRuta asociacion(String codigoRuta) {
        VendedorRuta vr = new VendedorRuta();
        vr.setId(new VendedorRutaId("001", "V", codigoRuta));
        Ruta ruta = new Ruta();
        ruta.setCodigo(codigoRuta);
        vr.setRuta(ruta);
        return vr;
    }

    private RutaDTO dto(String codigo) {
        RutaDTO d = new RutaDTO();
        d.setCodigo(codigo);
        return d;
    }

    @Test
    void getRutasByVendedor_conAsociaciones_retornaListaMapeada() {
        VendedorRuta vr = asociacion("R01");
        when(vendedorRutaRepo.findByIdCodigoVendedorAndIdTipoVendedor("001", "V"))
                .thenReturn(List.of(vr));
        when(rutaMapper.toDTO(vr.getRuta())).thenReturn(dto("R01"));

        List<RutaDTO> result = service.getRutasByVendedor("001", "V");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCodigo()).isEqualTo("R01");
    }

    @Test
    void getRutasByVendedor_sinAsociaciones_retornaListaVacia() {
        when(vendedorRutaRepo.findByIdCodigoVendedorAndIdTipoVendedor("001", "V"))
                .thenReturn(List.of());

        assertThat(service.getRutasByVendedor("001", "V")).isEmpty();
    }

    @Test
    void asignarRutas_vendedorNoExiste_lanza404() {
        when(vendedorRepo.findById(new VendedorId("001", "V"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.asignarRutas("001", "V", List.of("R01")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Vendedor no encontrado");

        verifyNoInteractions(rutaRepo, vendedorRutaRepo);
    }

    @Test
    void asignarRutas_rutaNoExiste_lanza404() {
        when(vendedorRepo.findById(new VendedorId("001", "V"))).thenReturn(Optional.of(new Vendedor()));
        when(rutaRepo.findById("R99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.asignarRutas("001", "V", List.of("R99")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("R99");

        verify(vendedorRutaRepo, never()).deleteByIdCodigoVendedorAndIdTipoVendedor(any(), any());
    }

    @Test
    void asignarRutas_datosValidos_reemplazaSetCompleto() {
        when(vendedorRepo.findById(new VendedorId("001", "V"))).thenReturn(Optional.of(new Vendedor()));
        when(rutaRepo.findById("R01")).thenReturn(Optional.of(new Ruta()));
        when(rutaRepo.findById("R02")).thenReturn(Optional.of(new Ruta()));
        when(vendedorRutaRepo.findByIdCodigoVendedorAndIdTipoVendedor("001", "V"))
                .thenReturn(List.of(asociacion("R01"), asociacion("R02")));
        when(rutaMapper.toDTO(any())).thenReturn(dto("R01"), dto("R02"));

        List<RutaDTO> result = service.asignarRutas("001", "V", List.of("R01", "R02"));

        verify(vendedorRutaRepo).deleteByIdCodigoVendedorAndIdTipoVendedor("001", "V");
        verify(vendedorRutaRepo).saveAll(anyList());
        assertThat(result).hasSize(2);
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla (no compila: `VendedorRutaService` no existe)**

Run: `cd /Users/cursor/Dev/dipalza/dipalza.springboot/.worktrees/feature-vendedor-rutas/dipalza && ./mvnw test -Dfrontend.skip=true -Dtest=VendedorRutaServiceTest`
Expected: `COMPILATION ERROR` — `cannot find symbol: class VendedorRutaService`

- [ ] **Step 3: Implementar `VendedorRutaService`**

```java
package cl.eos.dipalza.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.eos.dipalza.entity.VendedorRuta;
import cl.eos.dipalza.entity.ids.VendedorId;
import cl.eos.dipalza.entity.ids.VendedorRutaId;
import cl.eos.dipalza.mapper.RutaMapper;
import cl.eos.dipalza.model.RutaDTO;
import cl.eos.dipalza.repository.RutaRepository;
import cl.eos.dipalza.repository.VendedorRepository;
import cl.eos.dipalza.repository.VendedorRutaRepository;

@Service
public class VendedorRutaService {

    private final VendedorRutaRepository vendedorRutaRepository;
    private final VendedorRepository vendedorRepository;
    private final RutaRepository rutaRepository;
    private final RutaMapper rutaMapper;

    public VendedorRutaService(VendedorRutaRepository vendedorRutaRepository,
                                VendedorRepository vendedorRepository,
                                RutaRepository rutaRepository,
                                RutaMapper rutaMapper) {
        this.vendedorRutaRepository = vendedorRutaRepository;
        this.vendedorRepository = vendedorRepository;
        this.rutaRepository = rutaRepository;
        this.rutaMapper = rutaMapper;
    }

    public List<RutaDTO> getRutasByVendedor(String codigo, String tipo) {
        return vendedorRutaRepository.findByIdCodigoVendedorAndIdTipoVendedor(codigo, tipo)
                .stream()
                .map(vr -> rutaMapper.toDTO(vr.getRuta()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<RutaDTO> asignarRutas(String codigo, String tipo, List<String> codigosRuta) {
        if (vendedorRepository.findById(new VendedorId(codigo, tipo)).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendedor no encontrado");
        }
        for (String codigoRuta : codigosRuta) {
            if (rutaRepository.findById(codigoRuta).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ruta " + codigoRuta + " no encontrada");
            }
        }

        vendedorRutaRepository.deleteByIdCodigoVendedorAndIdTipoVendedor(codigo, tipo);

        List<VendedorRuta> nuevas = codigosRuta.stream()
                .map(codigoRuta -> {
                    VendedorRuta vr = new VendedorRuta();
                    vr.setId(new VendedorRutaId(codigo, tipo, codigoRuta));
                    return vr;
                })
                .collect(Collectors.toList());
        vendedorRutaRepository.saveAll(nuevas);

        return getRutasByVendedor(codigo, tipo);
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `cd /Users/cursor/Dev/dipalza/dipalza.springboot/.worktrees/feature-vendedor-rutas/dipalza && ./mvnw test -Dfrontend.skip=true -Dtest=VendedorRutaServiceTest`
Expected: `Tests run: 5, Failures: 0, Errors: 0` — `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
cd /Users/cursor/Dev/dipalza/dipalza.springboot/.worktrees/feature-vendedor-rutas/dipalza
git add src/test/java/cl/eos/dipalza/service/VendedorRutaServiceTest.java \
        src/main/java/cl/eos/dipalza/service/VendedorRutaService.java
git commit -m "feat: agrega VendedorRutaService (asignación y consulta de rutas por vendedor)

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

### Task 3: `VendedorRutaController` (TDD, `@WebMvcTest`)

**Files:**
- Test: `dipalza/src/test/java/cl/eos/dipalza/controller/VendedorRutaControllerTest.java`
- Create: `dipalza/src/main/java/cl/eos/dipalza/controller/VendedorRutaController.java`

**Interfaces:**
- Consumes: `VendedorRutaService.getRutasByVendedor(String, String)`, `VendedorRutaService.asignarRutas(String, String, List<String>)` (Task 2).
- Produces: `GET /api/vendedores/{codigo}/{tipo}/rutas` → `List<RutaDTO>`; `PUT /api/vendedores/{codigo}/{tipo}/rutas` (body `List<String>`) → `List<RutaDTO>`.

- [ ] **Step 1: Escribir el test (debe fallar — el controller no existe aún)**

```java
package cl.eos.dipalza.controller;

import cl.eos.dipalza.model.RutaDTO;
import cl.eos.dipalza.service.VendedorRutaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = VendedorRutaController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class VendedorRutaControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean VendedorRutaService service;

    private RutaDTO dto(String codigo) {
        RutaDTO d = new RutaDTO();
        d.setCodigo(codigo);
        d.setDescripcion("Ruta " + codigo);
        return d;
    }

    @Test
    void getRutas_retornaListaDelVendedor() throws Exception {
        when(service.getRutasByVendedor("001", "V")).thenReturn(List.of(dto("R01")));

        mockMvc.perform(get("/api/vendedores/001/V/rutas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo", is("R01")));
    }

    @Test
    void setRutas_datosValidos_retornaListaActualizada() throws Exception {
        when(service.asignarRutas("001", "V", List.of("R01", "R02")))
                .thenReturn(List.of(dto("R01"), dto("R02")));

        mockMvc.perform(put("/api/vendedores/001/V/rutas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of("R01", "R02"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void setRutas_vendedorNoExiste_retorna404() throws Exception {
        when(service.asignarRutas("999", "V", List.of("R01")))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendedor no encontrado"));

        mockMvc.perform(put("/api/vendedores/999/V/rutas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of("R01"))))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd /Users/cursor/Dev/dipalza/dipalza.springboot/.worktrees/feature-vendedor-rutas/dipalza && ./mvnw test -Dfrontend.skip=true -Dtest=VendedorRutaControllerTest`
Expected: `COMPILATION ERROR` — `cannot find symbol: class VendedorRutaController`

- [ ] **Step 3: Implementar el controller**

```java
package cl.eos.dipalza.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.eos.dipalza.model.RutaDTO;
import cl.eos.dipalza.service.VendedorRutaService;

@RestController
@RequestMapping("/api/vendedores/{codigo}/{tipo}/rutas")
public class VendedorRutaController {

    private final VendedorRutaService vendedorRutaService;

    public VendedorRutaController(VendedorRutaService vendedorRutaService) {
        this.vendedorRutaService = vendedorRutaService;
    }

    @GetMapping
    public List<RutaDTO> getRutas(@PathVariable String codigo, @PathVariable String tipo) {
        return vendedorRutaService.getRutasByVendedor(codigo, tipo);
    }

    @PutMapping
    public List<RutaDTO> setRutas(@PathVariable String codigo, @PathVariable String tipo,
                                   @RequestBody List<String> codigosRuta) {
        return vendedorRutaService.asignarRutas(codigo, tipo, codigosRuta);
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `cd /Users/cursor/Dev/dipalza/dipalza.springboot/.worktrees/feature-vendedor-rutas/dipalza && ./mvnw test -Dfrontend.skip=true -Dtest=VendedorRutaControllerTest`
Expected: `Tests run: 3, Failures: 0, Errors: 0` — `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
cd /Users/cursor/Dev/dipalza/dipalza.springboot/.worktrees/feature-vendedor-rutas/dipalza
git add src/test/java/cl/eos/dipalza/controller/VendedorRutaControllerTest.java \
        src/main/java/cl/eos/dipalza/controller/VendedorRutaController.java
git commit -m "feat: agrega VendedorRutaController (GET/PUT /api/vendedores/{codigo}/{tipo}/rutas)

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

## Flutter (`flutterDipalza`)

### Task 4: `RutasPage` con modos `multiSelect`/`obligatorio` + `app_router.dart`

**Files:**
- Modify: `lib/src/page/rutas/rutas.page.dart` (reescritura completa)
- Modify: `lib/src/share/app_router.dart:38-39` (case `AppRoutes.rutas`)

**Interfaces:**
- Produces: `RutasPage({bool multiSelect = false, List<RutasModel> seleccionInicial = const [], bool obligatorio = false})`. Sin argumentos, hace `pop(RutasModel)` igual que hoy. Con `multiSelect: true`, hace `pop(List<RutasModel>)`. Con `obligatorio: true` (solo junto a `multiSelect: true`), bloquea el back y exige ≥1 selección antes de habilitar "Guardar".

- [ ] **Step 1: Reescribir `rutas.page.dart` completo**

```dart
// Archivo: rutas.page.dart
// ignore_for_file: unused_field

import 'package:dipalza_movil/src/model/rutas_model.dart';
import 'package:dipalza_movil/src/share/app.navigator.dart';
import 'package:flutter/material.dart';

import '../../bloc/rutas_bloc.dart';
import '../../utils/utils.dart';

class RutasPage extends StatefulWidget {
  final bool multiSelect;
  final List<RutasModel> seleccionInicial;
  final bool obligatorio;

  const RutasPage({
    Key? key,
    this.multiSelect = false,
    this.seleccionInicial = const [],
    this.obligatorio = false,
  }) : super(key: key);

  @override
  _RutasPageState createState() => _RutasPageState();
}

class _RutasPageState extends State<RutasPage> {
  final RutasBloc rutasBloc = RutasBloc();

  late List<RutasModel> _rutasFiltradas;
  final TextEditingController _searchController = TextEditingController();
  bool _verBuscar = false;
  late Set<String> _codigosSeleccionados;

  @override
  void initState() {
    super.initState();
    _codigosSeleccionados =
        widget.seleccionInicial.map((r) => r.codigo).toSet();
    _searchController.addListener(_filtrarRutas);

    // Reintenta si no hay datos
    if (rutasBloc.listaRutas.isEmpty) {
      rutasBloc.obtenerListaRutas();
    }
  }

  @override
  void dispose() {
    _searchController.dispose();
    //rutasBloc.dispose();
    super.dispose();
  }

  void _filtrarRutas() {
    final query = _searchController.text.toLowerCase();
    rutasBloc.filtrarRutas(query);
  }

  void _guardarSeleccion() {
    final seleccionadas = rutasBloc.listaRutas
        .where((r) => _codigosSeleccionados.contains(r.codigo))
        .toList();
    AppNavigator.pop(seleccionadas);
  }

  void _alternarSeleccion(String codigo) {
    setState(() {
      if (_codigosSeleccionados.contains(codigo)) {
        _codigosSeleccionados.remove(codigo);
      } else {
        _codigosSeleccionados.add(codigo);
      }
    });
  }

  _card(RutasModel ruta) {
    final seleccionada = _codigosSeleccionados.contains(ruta.codigo);
    return Card(
      child: ListTile(
        leading: CircleAvatar(
          radius: 25,
          child: const Icon(Icons.add_box_outlined),
          backgroundColor: colorRojoBase(),
          foregroundColor: Colors.white,
        ),
        title: Text(ruta.descripcion,
            style: const TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 13.0,
                color: Colors.black)),
        subtitle: Row(
          children: <Widget>[
            Text(ruta.codigo,
                style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 10.0,
                    color: Colors.grey)),
          ],
        ),
        trailing: widget.multiSelect
            ? Checkbox(
                value: seleccionada,
                onChanged: (_) => _alternarSeleccion(ruta.codigo),
              )
            : IconButton(
                icon: const Icon(Icons.arrow_forward_ios),
                onPressed: () {
                  AppNavigator.pop(ruta);
                }),
        onTap: widget.multiSelect
            ? () => _alternarSeleccion(ruta.codigo)
            : () {
                AppNavigator.pop(ruta);
              },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final scaffold = Scaffold(
        appBar: AppBar(
          backgroundColor: colorRojoBase(),
          automaticallyImplyLeading: !widget.obligatorio,
          title: Text(widget.multiSelect ? 'Seleccionar Rutas' : 'Seleccionar Ruta'),
          actions: <Widget>[
            if (widget.multiSelect)
              IconButton(
                icon: const Icon(Icons.check),
                tooltip: 'Guardar',
                onPressed: (widget.obligatorio && _codigosSeleccionados.isEmpty)
                    ? null
                    : _guardarSeleccion,
              ),
            IconButton(
              icon: const Icon(Icons.search),
              tooltip: 'Buscar',
              onPressed: () {
                setState(() {
                  _verBuscar = true;
                });
              },
            ),
          ],
          bottom: _verBuscar
              ? PreferredSize(
                  preferredSize: const Size.fromHeight(56.0),
                  child: Padding(
                    padding: const EdgeInsets.all(8.0),
                    child: TextField(
                      controller: _searchController,
                      decoration: InputDecoration(
                        hintText: 'Buscar...',
                        prefixIcon:
                            const Icon(Icons.search, color: Colors.white),
                        filled: true,
                        fillColor: Colors.white24,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(20),
                          borderSide: BorderSide.none,
                        ),
                        contentPadding: const EdgeInsets.symmetric(vertical: 0),
                        suffixIcon: new IconButton(
                          color: Colors.white,
                          icon: const Icon(Icons.cancel),
                          onPressed: () {
                            _searchController.clear();
                            rutasBloc
                                .limpiarFiltro(); // ← Usar BLoC para limpiar
                            setState(() {
                              _verBuscar = false;
                            });
                          },
                        ),
                      ),
                      style: const TextStyle(color: Colors.white),
                    ),
                  ),
                )
              : null,
        ),
        body: RefreshIndicator(
          onRefresh: () => rutasBloc.obtenerListaRutas(),
          child: StreamBuilder<List<RutasModel>>(
            stream: rutasBloc.rutasStream,
            builder: (context, snapshot) {
              if (snapshot.hasError) {
                return SingleChildScrollView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  child: SizedBox(
                    height: MediaQuery.of(context).size.height * 0.7,
                    child: Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const Icon(Icons.error, color: Colors.red, size: 64),
                          const SizedBox(height: 16),
                          Text('Error: ${snapshot.error}'),
                          ElevatedButton(
                            onPressed: () => rutasBloc
                                .limpiarFiltro(), //  rutasBloc.cargarRutas(widget.listaRutas),
                            child: const Text('Reintentar'),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              }
              if (!snapshot.hasData) {
                return const Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      CircularProgressIndicator(),
                      SizedBox(height: 16),
                      Text('Cargando rutas...'),
                    ],
                  ),
                );
              }
              final rutasFiltradas = snapshot.data!; // ← Los datos del stream
              if (snapshot.data!.isEmpty) {
                return SingleChildScrollView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  child: SizedBox(
                    height: MediaQuery.of(context).size.height * 0.7,
                    child: const Center(
                      child: Text('No se encontraron rutas.'),
                    ),
                  ),
                );
              }
              return ListView.builder(
                itemCount: rutasFiltradas.length,
                itemBuilder: (context, index) {
                  final ruta = rutasFiltradas[index];
                  return _card(ruta);
                },
              );
            },
          ),
        ));

    if (widget.obligatorio) {
      return PopScope(canPop: false, child: scaffold);
    }
    return scaffold;
  }
}
```

- [ ] **Step 2: Actualizar `app_router.dart`**

Ubicar el case actual (`lib/src/share/app_router.dart:38-39`):

```dart
      case AppRoutes.rutas:
        return MaterialPageRoute(builder: (_) => const RutasPage());
```

Reemplazar por:

```dart
      case AppRoutes.rutas:
        if (args is Map<String, dynamic>) {
          return MaterialPageRoute(
            builder: (_) => RutasPage(
              multiSelect: args['multiSelect'] as bool? ?? false,
              seleccionInicial:
                  (args['seleccionInicial'] as List<RutasModel>?) ?? const [],
              obligatorio: args['obligatorio'] as bool? ?? false,
            ),
          );
        }
        return MaterialPageRoute(builder: (_) => const RutasPage());
```

Y agregar el import faltante junto a los demás (`lib/src/share/app_router.dart:1-17`):

```dart
import '../model/rutas_model.dart';
```

- [ ] **Step 3: Verificar con `flutter analyze`**

Run: `cd /Users/cursor/Dev/dipalza/flutterDipalza/.worktrees/feature-vendedor-rutas && flutter analyze lib/src/page/rutas/rutas.page.dart lib/src/share/app_router.dart`
Expected: `No issues found!`

- [ ] **Step 4: Commit**

```bash
cd /Users/cursor/Dev/dipalza/flutterDipalza/.worktrees/feature-vendedor-rutas
git add lib/src/page/rutas/rutas.page.dart lib/src/share/app_router.dart
git commit -m "feat: RutasPage soporta selección múltiple y modo obligatorio

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

### Task 5: `VendedorRutaProvider` + `PreferenciasUsuario.rutasAsignadas`

**Files:**
- Create: `lib/src/provider/vendedor_ruta_provider.dart`
- Modify: `lib/src/share/prefs_usuario.dart` (agregar getter/setter, junto al de `ruta`)

**Interfaces:**
- Produces: `VendedorRutaProvider().obtenerRutasAsignadas(String codigo, String tipo) -> Future<List<RutasModel>>`, `VendedorRutaProvider().guardarRutasAsignadas(String codigo, String tipo, List<String> codigosRuta) -> Future<List<RutasModel>>`. `PreferenciasUsuario().rutasAsignadas` (getter/setter `List<String>`).

- [ ] **Step 1: Crear el provider**

```dart
import 'package:dipalza_movil/src/model/rutas_model.dart';

import '../services/api_client.dart';

class VendedorRutaProvider {
  final _dio = ApiClient().dio;

  Future<List<RutasModel>> obtenerRutasAsignadas(
      String codigo, String tipo) async {
    try {
      final res = await _dio.get('/api/vendedores/$codigo/$tipo/rutas');
      final List<dynamic> data = res.data;
      return data.map((j) => RutasModel.fromJson(j)).toList();
    } catch (error) {
      print(error.toString());
      return [];
    }
  }

  Future<List<RutasModel>> guardarRutasAsignadas(
      String codigo, String tipo, List<String> codigosRuta) async {
    try {
      final res = await _dio.put('/api/vendedores/$codigo/$tipo/rutas',
          data: codigosRuta);
      final List<dynamic> data = res.data;
      return data.map((j) => RutasModel.fromJson(j)).toList();
    } catch (error) {
      print(error.toString());
      return [];
    }
  }
}
```

Guardar en: `lib/src/provider/vendedor_ruta_provider.dart`

- [ ] **Step 2: Agregar el getter/setter en `PreferenciasUsuario`**

En `lib/src/share/prefs_usuario.dart`, justo después del bloque `get ruta`/`set ruta`:

```dart
  String get ruta {
    return _prefs.getString('ruta') ?? '';
  }

  set ruta(String value) {
    _prefs.setString('ruta', value);
  }

  List<String> get rutasAsignadas {
    return _prefs.getStringList('rutasAsignadas') ?? [];
  }

  set rutasAsignadas(List<String> value) {
    _prefs.setStringList('rutasAsignadas', value);
  }
```

- [ ] **Step 3: Verificar con `flutter analyze`**

Run: `cd /Users/cursor/Dev/dipalza/flutterDipalza/.worktrees/feature-vendedor-rutas && flutter analyze lib/src/provider/vendedor_ruta_provider.dart lib/src/share/prefs_usuario.dart`
Expected: `No issues found!`

- [ ] **Step 4: Commit**

```bash
cd /Users/cursor/Dev/dipalza/flutterDipalza/.worktrees/feature-vendedor-rutas
git add lib/src/provider/vendedor_ruta_provider.dart lib/src/share/prefs_usuario.dart
git commit -m "feat: agrega VendedorRutaProvider y caché local de rutas asignadas

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

### Task 6: `ConfiguracionPage` — sección "Rutas" con chips

**Files:**
- Modify: `lib/src/page/config/preferences.page.dart`

**Interfaces:**
- Consumes: `VendedorRutaProvider` (Task 5), `RutasPage(multiSelect:, seleccionInicial:)` vía `AppRoutes.rutas` (Task 4).

- [ ] **Step 1: Agregar el import del provider**

En `lib/src/page/config/preferences.page.dart:1-18`, junto a los demás imports:

```dart
import '../../provider/vendedor_ruta_provider.dart';
```

- [ ] **Step 2: Reemplazar el campo de estado**

Cambiar (línea 33):

```dart
  RutasModel? _rutaSeleccionada;
```

por:

```dart
  List<RutasModel> _rutasAsignadas = [];
```

- [ ] **Step 3: Hidratar la selección en `initState`**

Cambiar:

```dart
  @override
  void initState() {
    super.initState();
    _urlController = TextEditingController(text: _prefs.urlServicio);
    // Estado inicial: si hay URL, no asumimos conectividad hasta probar
    _status = (_prefs.urlServicio.isEmpty)
        ? ConnectionStatus.unknown
        : ConnectionStatus.unknown;
  }
```

por:

```dart
  @override
  void initState() {
    super.initState();
    _urlController = TextEditingController(text: _prefs.urlServicio);
    // Estado inicial: si hay URL, no asumimos conectividad hasta probar
    _status = (_prefs.urlServicio.isEmpty)
        ? ConnectionStatus.unknown
        : ConnectionStatus.unknown;
    _cargarRutasAsignadas();
  }

  Future<void> _cargarRutasAsignadas() async {
    final rutas = await VendedorRutaProvider()
        .obtenerRutasAsignadas(_prefs.vendedor, _prefs.tipo);
    if (!mounted) return;
    setState(() => _rutasAsignadas = rutas);
  }
```

- [ ] **Step 4: Reemplazar `_pickRuta` por `_pickRutas`**

Cambiar:

```dart
  Future<void> _pickRuta() async {
    final seleccion = await AppNavigator.pushNamed(
        AppRoutes.rutas); // RutasPage(listaRutas: listaRutas)),
    if (seleccion != null) {
      setState(() => _rutaSeleccionada = seleccion);
      _prefs.ruta = seleccion.codigo; // mismo setter que usa LoginPage
    }
  }
```

por:

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

- [ ] **Step 5: Reemplazar la `ListTile` "Ruta" por la sección "Rutas" con chips**

Cambiar (dentro del `Card` de "Preferencias"):

```dart
                // Ruta
                ListTile(
                  leading: const Icon(Icons.map_outlined),
                  title: const Text('Ruta'),
                  subtitle: Text(
                    _rutaSeleccionada?.descripcion ??
                        _rutaSeleccionada?.codigo ??
                        'Seleccione una ruta',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: _pickRuta,
                ),
                const Divider(height: 0),
```

por:

```dart
                // Rutas
                ListTile(
                  leading: const Icon(Icons.map_outlined),
                  title: const Text('Rutas'),
                  subtitle: _rutasAsignadas.isEmpty
                      ? const Text('Sin rutas asignadas')
                      : null,
                  trailing: const Icon(Icons.chevron_right),
                  onTap: _pickRutas,
                ),
                if (_rutasAsignadas.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                    child: Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: _rutasAsignadas
                          .map((r) => Chip(label: Text(r.descripcion)))
                          .toList(),
                    ),
                  ),
                const Divider(height: 0),
```

- [ ] **Step 6: Verificar con `flutter analyze`**

Run: `cd /Users/cursor/Dev/dipalza/flutterDipalza/.worktrees/feature-vendedor-rutas && flutter analyze lib/src/page/config/preferences.page.dart`
Expected: `No issues found!`

- [ ] **Step 7: Verificación manual**

Levantar la app (`flutter run`), ir a Configuración → tocar "Rutas" → marcar 2-3 chips → Guardar → confirmar que aparecen como chips en la pantalla y que persisten al salir y volver a entrar a Configuración.

- [ ] **Step 8: Commit**

```bash
cd /Users/cursor/Dev/dipalza/flutterDipalza/.worktrees/feature-vendedor-rutas
git add lib/src/page/config/preferences.page.dart
git commit -m "feat: Configuración usa selección múltiple de rutas (chips)

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

### Task 7: Corrección `ClientesModel` + origen de `codigoRuta` en venta

**Files:**
- Modify: `lib/src/model/clientes_model.dart:41`
- Modify: `lib/src/page/ventas/venta.encabezado.edicion.page.dart:250`
- Modify: `test/unit/clientes_model_test.dart:27` — **existe una suite de tests automatizados** (`flutter test`, 26 tests) que el resto del plan no contemplaba. Este archivo en particular codifica el bug actual: envía `'ruta': 'R01'` en el JSON de entrada pero espera `cliente.ruta == ''` — hay que corregir esa aserción para que refleje el comportamiento correcto.

- [ ] **Step 1: Corregir el typo en `ClientesModel.fromJson`**

Cambiar (línea 41):

```dart
      ruta: json["tuta"] ?? "");
```

por:

```dart
      ruta: json["ruta"] ?? "");
```

- [ ] **Step 2: Corregir el test que codificaba el bug**

En `test/unit/clientes_model_test.dart`, dentro de `test('fromJson creates correct model', ...)`, cambiar (línea 27):

```dart
      expect(cliente.ruta, '');
```

por:

```dart
      expect(cliente.ruta, 'R01');
```

(el JSON de entrada de ese mismo test, línea 15, ya trae `'ruta': 'R01'` — la aserción pasaba a `''` únicamente por el typo que se corrige en el Step 1).

- [ ] **Step 3: Usar la ruta del cliente en `saveVenta()`**

Cambiar (línea 250):

```dart
        codigoRuta: pref.ruta,
```

por:

```dart
        codigoRuta: _clienteSeleccionado!.ruta,
```

- [ ] **Step 4: Ejecutar la suite de tests y verificar que pasa**

Run: `cd /Users/cursor/Dev/dipalza/flutterDipalza/.worktrees/feature-vendedor-rutas && flutter test test/unit/clientes_model_test.dart`
Expected: `All tests passed!` (5 tests)

- [ ] **Step 5: Verificar con `flutter analyze`**

Run: `cd /Users/cursor/Dev/dipalza/flutterDipalza/.worktrees/feature-vendedor-rutas && flutter analyze lib/src/model/clientes_model.dart lib/src/page/ventas/venta.encabezado.edicion.page.dart`
Expected: `No issues found!`

- [ ] **Step 6: Verificación manual**

Crear una venta nueva para un cliente conocido y confirmar (log/debug o inspección de la respuesta del backend) que `codigoRuta` corresponde a la ruta real del cliente, no a un valor vacío ni al de otro cliente.

- [ ] **Step 7: Commit**

```bash
cd /Users/cursor/Dev/dipalza/flutterDipalza/.worktrees/feature-vendedor-rutas
git add lib/src/model/clientes_model.dart lib/src/page/ventas/venta.encabezado.edicion.page.dart test/unit/clientes_model_test.dart
git commit -m "fix: venta usa la ruta del cliente seleccionado, corrige typo tuta->ruta

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

### Task 8: `LoginBloc` y `Validators` — quitar validación de ruta

**Files:**
- Modify: `lib/src/bloc/login_bloc.dart`
- Modify: `lib/src/page/login/login_validacion.dart`

**Interfaces:**
- Produces: `LoginBloc.formValidStream` ahora depende solo de `usuarioStream`/`passwordStream`. `LoginBloc` ya no expone `rutaStream`, `changeRuta`, `ruta`.

- [ ] **Step 1: Reescribir `login_bloc.dart`**

```dart
import 'dart:async';

import 'package:rxdart/rxdart.dart';

import 'login_validacion.dart';

class LoginBloc with Validators {
  final _usuarioController = BehaviorSubject<String>.seeded('');
  final _passwordController = BehaviorSubject<String>.seeded('');

  Stream<String> get usuarioStream =>
      _usuarioController.stream.transform(validarUsuario).distinct();
  Stream<String> get passwordStream =>
      _passwordController.stream.transform(validarPassword).distinct();

  Stream<bool> get formValidStream =>
      Rx.combineLatest2(usuarioStream, passwordStream, (a, b) => true);

  Function(String) get changeUsuario => _usuarioController.sink.add;
  Function(String) get changePassword => _passwordController.sink.add;

  String get usuario => _usuarioController.value;
  String get password => _passwordController.value;

  dispose() {
    _usuarioController.close();
    _passwordController.close();
  }
}
```

- [ ] **Step 2: Reescribir `login_validacion.dart`**

```dart
import 'dart:async';

class Validators {
  final validarUsuario = StreamTransformer<String, String>.fromHandlers(handleData: (usuario, sink) {
    if (usuario.length >= 3) {
      sink.add(usuario);
    } else {
      sink.addError('El usuario minimo 3 caracteres.');
    }
  });

  final validarPassword = StreamTransformer<String, String>.fromHandlers(handleData: (password, sink) {
    if (password.length >= 6) {
      sink.add(password);
    } else {
      sink.addError('La contraseña debe mayor a 6 caracteres.');
    }
  });
}
```

- [ ] **Step 3: Verificar con `flutter analyze`**

Run: `cd /Users/cursor/Dev/dipalza/flutterDipalza/.worktrees/feature-vendedor-rutas && flutter analyze lib/src/bloc/login_bloc.dart lib/src/page/login/login_validacion.dart`
Expected: aparecerán errores en `login.page.dart` (Task 9 aún no aplicado) referenciando `bloc.changeRuta`/`bloc.ruta` — eso es esperado en este punto intermedio; confirmar que `login_bloc.dart` y `login_validacion.dart` en sí no tienen errores propios.

- [ ] **Step 4: Ejecutar la suite completa de tests**

Run: `cd /Users/cursor/Dev/dipalza/flutterDipalza/.worktrees/feature-vendedor-rutas && flutter test`
Expected: `All tests passed!` (26 tests) — ningún test existente (`test/widget_test.dart`, `test/widget/login_page_test.dart`) importa `login.page.dart` directamente, así que el estado intermedio de Step 3 no afecta esta corrida.

- [ ] **Step 5: Commit**

```bash
cd /Users/cursor/Dev/dipalza/flutterDipalza/.worktrees/feature-vendedor-rutas
git add lib/src/bloc/login_bloc.dart lib/src/page/login/login_validacion.dart
git commit -m "refactor: login ya no valida ni exige una ruta para habilitar Ingresar

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

### Task 9: `LoginPage` — quitar picker de ruta, agregar selección forzada post-login

**Files:**
- Modify: `lib/src/page/login/login.page.dart`

**Interfaces:**
- Consumes: `VendedorRutaProvider` (Task 5), `RutasPage(multiSelect:true, obligatorio:true)` vía `AppRoutes.rutas` (Task 4), `LoginBloc` sin ruta (Task 8).

- [ ] **Step 1: Agregar el import del provider**

En `lib/src/page/login/login.page.dart:1-23`, junto a los demás imports:

```dart
import 'package:dipalza_movil/src/provider/vendedor_ruta_provider.dart';
```

- [ ] **Step 2: Quitar el campo `_rutaSeleccionada`**

Cambiar (línea 38):

```dart
  RutasModel? _rutaSeleccionada;
```

Eliminar esa línea por completo (ya no se usa un `RutasModel` único en esta página).

- [ ] **Step 3: Quitar el widget selector de ruta de `_loginForm`**

Cambiar:

```dart
              AbsorbPointer(
                absorbing: status != ServerStatus.online,
                child: _crearPassword(bloc),
              ),
              const SizedBox(height: 20.0),
              AbsorbPointer(
                absorbing: status != ServerStatus.online,
                child: _crearSelectorRutas(context, bloc),
              ),
              const SizedBox(height: 20.0),
              AbsorbPointer(
                absorbing: status != ServerStatus.online,
                child: _crearSelectorFechaFacturacion(context),
              ),
```

por:

```dart
              AbsorbPointer(
                absorbing: status != ServerStatus.online,
                child: _crearPassword(bloc),
              ),
              const SizedBox(height: 20.0),
              AbsorbPointer(
                absorbing: status != ServerStatus.online,
                child: _crearSelectorFechaFacturacion(context),
              ),
```

- [ ] **Step 4: Eliminar el método `_crearSelectorRutas`**

Borrar completo el método (ubicado entre `_crearPassword` y `_crearSelectorFechaFacturacion`):

```dart
  Widget _crearSelectorRutas(BuildContext context, LoginBloc bloc) {
    return InkWell(
      onTap: _isLoading
          ? null
          : () async {
              final dynamic resultado = await AppNavigator.pushNamed(AppRoutes.rutas);
              if (resultado != null && resultado is RutasModel) {
                setState(() => _rutaSeleccionada = resultado);
                bloc.changeRuta(_rutaSeleccionada!.codigo);
              }
            },
      borderRadius: BorderRadius.circular(10.0),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 15.0, horizontal: 12.0),
        decoration: BoxDecoration(
          border: Border.all(color: Colors.grey.shade400),
          borderRadius: BorderRadius.circular(10.0),
        ),
        child: Row(
          children: [
            Icon(Icons.map_outlined, color: colorRojoBase()),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                _rutaSeleccionada?.descripcion ?? 'Seleccione una ruta',
                style: TextStyle(
                  fontSize: 16,
                  color: _rutaSeleccionada == null ? Colors.grey[700] : Colors.black,
                ),
              ),
            ),
            const Icon(Icons.arrow_forward_ios, size: 16, color: Colors.grey),
          ],
        ),
      ),
    );
  }
```

- [ ] **Step 5: Quitar el reseteo de ruta en `_crearBotonesSecundarios`**

Cambiar:

```dart
        TextButton(
          onPressed: () async {
            final urlAntes = prefs.urlServicio;
            await AppNavigator.pushNamed(AppRoutes.config);
            if (!mounted) return;
            if (prefs.urlServicio != urlAntes) {
              final bloc = context.read<LoginBloc>();
              setState(() {
                _rutaSeleccionada = null;
              });
              bloc.changeRuta('');
            }
          },
          child: const Text('Configurar'),
        ),
```

por:

```dart
        TextButton(
          onPressed: () async {
            await AppNavigator.pushNamed(AppRoutes.config);
          },
          child: const Text('Configurar'),
        ),
```

- [ ] **Step 6: Actualizar `_login()` — quitar `prefs.ruta` y agregar la verificación/forzado post-login**

Cambiar:

```dart
    if (resp.status == 200 && mounted) {
      LoginResponseModel response = LoginResponseModel.fromJson(resp.detalle);
      prefs.vendedor = response.codigo;
      prefs.name = response.nombre;
      prefs.userName = bloc.usuario;
      prefs.password = bloc.password;
      prefs.access_token = response.accessToken;
      prefs.refreshToken = response.refreshToken;
      prefs.tipo = response.tipo;

      if (_rutaSeleccionada != null) prefs.ruta = _rutaSeleccionada!.codigo;

      // Se guarda como String en formato ISO 8601 (estándar y robusto)
      if (_fechaFacturacion != null) {
        prefs.fechaFacturacion = _fechaFacturacion!;
      }

      AppNavigator.pushReplacementNamed(AppRoutes.home);
    } else if (mounted) {
      final mensaje = resp.detalle['error']?.toString() ?? 'No se pudo iniciar sesión. Intente nuevamente.';
      alertUtil.showAlertDialog(context, mensaje, Icons.error_outline);
    }
```

por:

```dart
    if (resp.status == 200 && mounted) {
      LoginResponseModel response = LoginResponseModel.fromJson(resp.detalle);
      prefs.vendedor = response.codigo;
      prefs.name = response.nombre;
      prefs.userName = bloc.usuario;
      prefs.password = bloc.password;
      prefs.access_token = response.accessToken;
      prefs.refreshToken = response.refreshToken;
      prefs.tipo = response.tipo;

      // Se guarda como String en formato ISO 8601 (estándar y robusto)
      if (_fechaFacturacion != null) {
        prefs.fechaFacturacion = _fechaFacturacion!;
      }

      try {
        final rutas = await VendedorRutaProvider()
            .obtenerRutasAsignadas(response.codigo, response.tipo);

        if (rutas.isEmpty && mounted) {
          final seleccion = await AppNavigator.pushNamed(
            AppRoutes.rutas,
            arguments: {'multiSelect': true, 'obligatorio': true},
          );
          final nuevas = List<RutasModel>.from(seleccion);
          await VendedorRutaProvider().guardarRutasAsignadas(response.codigo,
              response.tipo, nuevas.map((r) => r.codigo).toList());
        }

        if (mounted) AppNavigator.pushReplacementNamed(AppRoutes.home);
      } catch (e) {
        if (mounted) {
          alertUtil.showAlertDialog(context,
              'No se pudieron obtener las rutas del vendedor. Intente nuevamente.',
              Icons.error_outline);
        }
      }
    } else if (mounted) {
      final mensaje = resp.detalle['error']?.toString() ?? 'No se pudo iniciar sesión. Intente nuevamente.';
      alertUtil.showAlertDialog(context, mensaje, Icons.error_outline);
    }
```

- [ ] **Step 7: Verificar con `flutter analyze`**

Run: `cd /Users/cursor/Dev/dipalza/flutterDipalza/.worktrees/feature-vendedor-rutas && flutter analyze`
Expected: `No issues found!` (ahora sin los errores intermedios de Task 8)

- [ ] **Step 7b: Ejecutar la suite completa de tests**

Run: `cd /Users/cursor/Dev/dipalza/flutterDipalza/.worktrees/feature-vendedor-rutas && flutter test`
Expected: `All tests passed!` (26 tests)

- [ ] **Step 8: Verificación manual**

1. Login con un vendedor que **no tiene** rutas configuradas → tras autenticar debe aparecer la pantalla de selección múltiple sin flecha de "volver" ni gesto de back habilitado, con "Guardar" deshabilitado hasta marcar al menos 1 ruta → al guardar, navega a Home.
2. Login con un vendedor que **ya tiene** rutas configuradas → navega directo a Home sin mostrar la pantalla de rutas.
3. Confirmar que el botón "Ingresar" se habilita solo con usuario+contraseña válidos, sin depender de ninguna ruta.

- [ ] **Step 9: Commit**

```bash
cd /Users/cursor/Dev/dipalza/flutterDipalza/.worktrees/feature-vendedor-rutas
git add lib/src/page/login/login.page.dart
git commit -m "feat: login no pide ruta; fuerza selección solo si el vendedor no tiene ninguna

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

## Self-Review Notes

- **Cobertura de la spec:** Tabla/migración ya aplicadas (fuera de este plan). Backend: Tasks 1-3 cubren entidad/repo/servicio/controller. Flutter: Task 4 cubre `RutasPage`+router, Task 5 el provider+prefs, Task 6 Configuración, Task 7 el hallazgo de `codigoRuta`/typo, Tasks 8-9 el login. Manejo de errores y testing de la spec quedan reflejados en los pasos de verificación de cada task.
- **Placeholders:** ninguno — cada step trae código completo listo para pegar.
- **Consistencia de tipos:** `VendedorRutaService`/`VendedorRutaController` usan los mismos nombres (`getRutasByVendedor`, `asignarRutas`) en spec, tests e implementación. `RutasPage(multiSelect, seleccionInicial, obligatorio)` se usa igual en Tasks 4, 6 y 9.
- **Orden de tasks:** Task 9 (login) depende de Tasks 4, 5 y 8 — por eso va al final. Task 7 es independiente del resto y podría reordenarse antes si se prefiere, pero no bloquea nada estando al final del bloque Flutter.
