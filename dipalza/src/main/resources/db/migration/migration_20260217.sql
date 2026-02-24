-- DROP SCHEMA dbo;

CREATE SCHEMA dbo;
-- ventas.dbo.app_role definition

-- Drop table

-- DROP TABLE ventas.dbo.app_role;

CREATE TABLE app_role ( id bigint IDENTITY(1,1) NOT NULL, name varchar(50) COLLATE Modern_Spanish_CI_AS NOT NULL, CONSTRAINT PK__app_role__3213E83FDFD86E49 PRIMARY KEY (id), CONSTRAINT UQ__app_role__72E12F1BA02DEC54 UNIQUE (name));


-- ventas.dbo.app_user definition

-- Drop table

-- DROP TABLE ventas.dbo.app_user;

CREATE TABLE app_user ( id bigint IDENTITY(1,1) NOT NULL, username varchar(100) COLLATE Modern_Spanish_CI_AS NOT NULL, password varchar(100) COLLATE Modern_Spanish_CI_AS NOT NULL, enabled bit DEFAULT 1 NOT NULL, locked bit DEFAULT 0 NOT NULL, created_at date DEFAULT CONVERT([date],sysutcdatetime()) NOT NULL, updated_at date DEFAULT CONVERT([date],sysutcdatetime()) NOT NULL, CONSTRAINT PK__app_user__3213E83F7459B955 PRIMARY KEY (id), CONSTRAINT UQ__app_user__F3DBC572B4D6807B UNIQUE (username));


-- ventas.dbo.cliente definition

-- Drop table

-- DROP TABLE ventas.dbo.cliente;

CREATE TABLE cliente ( rut varchar(10) COLLATE Modern_Spanish_CI_AS NOT NULL, codigo varchar(3) COLLATE Modern_Spanish_CI_AS NOT NULL, razon varchar(60) COLLATE Modern_Spanish_CI_AS NULL, direccion varchar(40) COLLATE Modern_Spanish_CI_AS NULL, ciudad varchar(30) COLLATE Modern_Spanish_CI_AS NULL, giro varchar(40) COLLATE Modern_Spanish_CI_AS NULL, telefono varchar(40) COLLATE Modern_Spanish_CI_AS NULL, codigo_ruta varchar(10) COLLATE Modern_Spanish_CI_AS NULL, codigo_vendedor varchar(3) COLLATE Modern_Spanish_CI_AS NULL, CONSTRAINT cliente_pk PRIMARY KEY (rut,codigo));


-- ventas.dbo.condicionventa definition

-- Drop table

-- DROP TABLE ventas.dbo.condicionventa;

CREATE TABLE condicionventa ( codigo varchar(10) COLLATE Modern_Spanish_CI_AS NOT NULL, descripcion varchar(50) COLLATE Modern_Spanish_CI_AS NOT NULL, dias int NULL, CONSTRAINT condicionventa_pk PRIMARY KEY (codigo));


-- ventas.dbo.conduccion definition

-- Drop table

-- DROP TABLE ventas.dbo.conduccion;

CREATE TABLE conduccion ( codigo varchar(10) COLLATE Modern_Spanish_CI_AS NOT NULL, descripcion varchar(50) COLLATE Modern_Spanish_CI_AS NOT NULL, valor money NULL, CONSTRAINT conduccion_pk PRIMARY KEY (codigo));


-- ventas.dbo.configuracion definition

-- Drop table

-- DROP TABLE ventas.dbo.configuracion;

CREATE TABLE configuracion ( propiedad varchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL, valor varchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL, tipo varchar(100) COLLATE SQL_Latin1_General_CP1_CI_AS NULL, descripcion varchar(255) COLLATE SQL_Latin1_General_CP1_CI_AS NULL, CONSTRAINT configuracion_pk PRIMARY KEY (propiedad));
 CREATE UNIQUE NONCLUSTERED INDEX configuracion_propiedad_IDX ON ventas.dbo.configuracion (  propiedad ASC  )  
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- ventas.dbo.historial_posicion definition

-- Drop table

-- DROP TABLE ventas.dbo.historial_posicion;

CREATE TABLE historial_posicion ( id bigint IDENTITY(1,1) NOT NULL, vendedorId varchar(3) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL, fechaHora datetime2(0) NOT NULL, posicion geography NOT NULL, CONSTRAINT pk_historial_posicion PRIMARY KEY (id));
 CREATE NONCLUSTERED INDEX idx_histotial_vendedor_fechaHora ON ventas.dbo.historial_posicion (  vendedorId ASC  , fechaHora ASC  )  
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
CREATE INDEX sidx_historial_posicion ON ventas.dbo.historial_posicion (posicion);


-- ventas.dbo.ila definition

-- Drop table

-- DROP TABLE ventas.dbo.ila;

CREATE TABLE ila ( codigo varchar(10) COLLATE Modern_Spanish_CI_AS NOT NULL, descripcion varchar(50) COLLATE Modern_Spanish_CI_AS NOT NULL, valor money NULL, CONSTRAINT ila_pk PRIMARY KEY (codigo));


-- ventas.dbo.posicion definition

-- Drop table

-- DROP TABLE ventas.dbo.posicion;

CREATE TABLE posicion ( vendedorId varchar(3) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL, posicion geography NOT NULL, ultimaActualizacion datetime2(0) NOT NULL, CONSTRAINT PK__posicion__F20B1251C4F1F60D PRIMARY KEY (vendedorId));


-- ventas.dbo.producto definition

-- Drop table

-- DROP TABLE ventas.dbo.producto;

CREATE TABLE producto ( Articulo varchar(255) COLLATE Modern_Spanish_CI_AS NOT NULL, Descripcion nvarchar(200) COLLATE Modern_Spanish_CI_AS NOT NULL, VentaNeto money NOT NULL, PorcIla decimal(5,2) NOT NULL, PorcCarne decimal(5,2) NOT NULL, Unidad varchar(10) COLLATE Modern_Spanish_CI_AS NOT NULL, Stock money NULL, CodigoIla varchar(20) COLLATE Modern_Spanish_CI_AS NULL, last_update date DEFAULT CONVERT([date],sysutcdatetime()) NOT NULL, rv timestamp NOT NULL, numbered bit DEFAULT 0 NULL, pieces decimal(5,2) DEFAULT 0 NULL, stockVentas money DEFAULT 0 NULL, piezasVentas decimal(19,2) DEFAULT 0 NULL, CONSTRAINT PK_Producto PRIMARY KEY (Articulo));


-- ventas.dbo.ruta definition

-- Drop table

-- DROP TABLE ventas.dbo.ruta;

CREATE TABLE ruta ( codigo varchar(10) COLLATE Modern_Spanish_CI_AS NOT NULL, descripcion varchar(50) COLLATE Modern_Spanish_CI_AS NOT NULL, conduccion_id varchar(100) COLLATE Modern_Spanish_CI_AS NULL, CONSTRAINT ruta_pk PRIMARY KEY (codigo));


-- ventas.dbo.vendedor definition

-- Drop table

-- DROP TABLE ventas.dbo.vendedor;

CREATE TABLE vendedor ( rut varchar(10) COLLATE Modern_Spanish_CI_AS NULL, codigo varchar(3) COLLATE Modern_Spanish_CI_AS NOT NULL, tipo varchar(1) COLLATE Modern_Spanish_CI_AS NOT NULL, nombre varchar(40) COLLATE Modern_Spanish_CI_AS NULL, ciudad varchar(30) COLLATE Modern_Spanish_CI_AS NULL, comuna varchar(30) COLLATE Modern_Spanish_CI_AS NULL, direccion varchar(40) COLLATE Modern_Spanish_CI_AS NULL, telefono varchar(40) COLLATE Modern_Spanish_CI_AS NULL, CONSTRAINT vendedor_pk PRIMARY KEY (codigo,tipo));


-- ventas.dbo.app_refresh_token definition

-- Drop table

-- DROP TABLE ventas.dbo.app_refresh_token;

CREATE TABLE app_refresh_token ( id bigint IDENTITY(1,1) NOT NULL, user_id bigint NOT NULL, token_hash varchar(200) COLLATE Modern_Spanish_CI_AS NOT NULL, expires_at datetime NOT NULL, revoked bit DEFAULT 0 NOT NULL, created_at datetime DEFAULT getdate() NOT NULL, CONSTRAINT PK__app_refr__3213E83F04E9AA5A PRIMARY KEY (id), CONSTRAINT UQ__app_refr__80488ABFE301C05E UNIQUE (user_id,token_hash), CONSTRAINT FK__app_refre__user___748F2482 FOREIGN KEY (user_id) REFERENCES app_user(id));


-- ventas.dbo.app_user_roles definition

-- Drop table

-- DROP TABLE ventas.dbo.app_user_roles;

CREATE TABLE app_user_roles ( user_id bigint NOT NULL, role_id bigint NOT NULL, CONSTRAINT PK_app_user_roles PRIMARY KEY (user_id,role_id), CONSTRAINT FK__app_user___role___70BE939E FOREIGN KEY (role_id) REFERENCES app_role(id), CONSTRAINT FK__app_user___user___6FCA6F65 FOREIGN KEY (user_id) REFERENCES app_user(id));


-- ventas.dbo.numerados definition

-- Drop table

-- DROP TABLE ventas.dbo.numerados;

CREATE TABLE numerados ( id bigint IDENTITY(1,1) NOT NULL, articulo varchar(255) COLLATE Modern_Spanish_CI_AS NOT NULL, numero int NOT NULL, peso decimal(19,4) NOT NULL, estado char(1) COLLATE Modern_Spanish_CI_AS DEFAULT 'D' NOT NULL, creado_en date DEFAULT CONVERT([date],sysutcdatetime()) NOT NULL, actualizado_en date DEFAULT CONVERT([date],sysutcdatetime()) NOT NULL, CONSTRAINT PK_InventarioNumerados PRIMARY KEY (id), CONSTRAINT FK_InvNum_producto FOREIGN KEY (articulo) REFERENCES producto(Articulo));
ALTER TABLE ventas.dbo.numerados WITH NOCHECK ADD CONSTRAINT CK_InvNum_estado CHECK (([estado]='A' OR [estado]='V' OR [estado]='R' OR [estado]='D'));
ALTER TABLE ventas.dbo.numerados WITH NOCHECK ADD CONSTRAINT CK_InvNum_peso CHECK (([peso]>=(0)));


-- ventas.dbo.venta definition

-- Drop table

-- DROP TABLE ventas.dbo.venta;

CREATE TABLE venta ( id bigint IDENTITY(1,1) NOT NULL, rut_cliente varchar(10) COLLATE Modern_Spanish_CI_AS NOT NULL, codigo_cliente varchar(3) COLLATE Modern_Spanish_CI_AS NOT NULL, codigo_vendedor varchar(3) COLLATE Modern_Spanish_CI_AS NOT NULL, tipo_vendedor varchar(1) COLLATE Modern_Spanish_CI_AS NOT NULL, fecha datetime NOT NULL, codigo_ruta varchar(10) COLLATE Modern_Spanish_CI_AS NOT NULL, total_descuento money NULL, total_iva money NULL, total_ila money NULL, total money NULL, estado varchar(20) COLLATE Modern_Spanish_CI_AS NULL, condicion_venta varchar(10) COLLATE Modern_Spanish_CI_AS NULL, total_neto money NULL, CONSTRAINT venta_pk PRIMARY KEY (id), CONSTRAINT venta_cliente_FK FOREIGN KEY (rut_cliente,codigo_cliente) REFERENCES cliente(rut,codigo) ON DELETE CASCADE ON UPDATE CASCADE, CONSTRAINT venta_condicionventa_FK FOREIGN KEY (condicion_venta) REFERENCES condicionventa(codigo) ON DELETE CASCADE ON UPDATE CASCADE, CONSTRAINT venta_ruta_FK FOREIGN KEY (codigo_ruta) REFERENCES ruta(codigo), CONSTRAINT venta_vendedor_FK FOREIGN KEY (codigo_vendedor,tipo_vendedor) REFERENCES vendedor(codigo,tipo));
 CREATE NONCLUSTERED INDEX venta_codigo_vendedor_IDX ON ventas.dbo.venta (  codigo_vendedor ASC  , tipo_vendedor ASC  )  
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX venta_fecha_IDX ON ventas.dbo.venta (  fecha ASC  )  
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX venta_rut_cliente_IDX ON ventas.dbo.venta (  rut_cliente ASC  )  
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- ventas.dbo.venta_detalle definition

-- Drop table

-- DROP TABLE ventas.dbo.venta_detalle;

CREATE TABLE venta_detalle ( id bigint IDENTITY(1,1) NOT NULL, venta_id bigint NOT NULL, producto_id varchar(255) COLLATE Modern_Spanish_CI_AS NOT NULL, cantidad decimal(18,4) NOT NULL, precio_unitario decimal(18,6) NOT NULL, porc_descuento decimal(18,2) NOT NULL, total_descuento decimal(18,2) NOT NULL, porc_ila decimal(18,2) NOT NULL, total_ila decimal(18,2) NOT NULL, porc_iva decimal(18,2) NOT NULL, total_iva decimal(18,2) NOT NULL, total_linea decimal(18,2) NOT NULL, piezas int DEFAULT 0 NOT NULL, unidad varchar(10) COLLATE SQL_Latin1_General_CP1_CI_AS NULL, CONSTRAINT venta_detalle_pk PRIMARY KEY (id), CONSTRAINT FK_vd_producto FOREIGN KEY (producto_id) REFERENCES producto(Articulo), CONSTRAINT FK_vd_venta FOREIGN KEY (venta_id) REFERENCES venta(id));
 CREATE NONCLUSTERED INDEX IX_vd_producto ON ventas.dbo.venta_detalle (  producto_id ASC  )  
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;
 CREATE NONCLUSTERED INDEX IX_vd_venta ON ventas.dbo.venta_detalle (  venta_id ASC  )  
	 WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
	 ON [PRIMARY ] ;


-- ventas.dbo.venta_detalle_pieza definition

-- Drop table

-- DROP TABLE ventas.dbo.venta_detalle_pieza;

CREATE TABLE venta_detalle_pieza ( id bigint IDENTITY(1,1) NOT NULL, id_detalle_venta bigint NULL, inv_id_pieza bigint NULL, peso decimal(19,4) NOT NULL, creado_en date DEFAULT CONVERT([date],sysutcdatetime()) NOT NULL, CONSTRAINT PK_venta_detalle_pieza PRIMARY KEY (id), CONSTRAINT venta_detalle_pieza_numerados_FK FOREIGN KEY (inv_id_pieza) REFERENCES numerados(id) ON DELETE CASCADE ON UPDATE CASCADE, CONSTRAINT venta_detalle_pieza_venta_detalle_FK FOREIGN KEY (id_detalle_venta) REFERENCES venta_detalle(id) ON DELETE CASCADE ON UPDATE CASCADE);