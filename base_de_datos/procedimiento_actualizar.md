# Define los pasos a seguir para grabar en la base de datos DIPALZA

## Obtener ID de base de datos

El Id, que es el correlativo en la base de datos y por el cual se asocia la venta. La tabla PARAMETROS en su campo FolioDocumento está el último ID que se ha generado.

```sql
select FolioDocumento from PARAMETROS
```
Este Id debe tener largo 10 y con ceros a la izquierda, por ejemplo: 0000318834

Este identificador lo debo cambiar para asegurar que el número está tomado.


## Obtener el número de factura con el que voy a reconocer en SII la venta.

* Cuando se utiliza factura electrónica
* Cuando no se utiliza factura electrónica
     

```sql
# Con factura electrónica
Select max(numero) from folios where tipo = '06' and tipo1='E'

# Sin factura electrónica
Select max(numero) from folios where tipo = '06' and tipo1=' '
```

Este número debe tener largo 7 y con ceros a la izquierda, por ejemplo: 0013958

## Agregar una venta

Para agregar una venta hay varias tablas que se deben actualizar.

* ctadocto
* totaldocumento
* encabezadocumento
* detalledocumento
* datoscliente
* MSOSTVENTASILA
* folios
* artxlocal

### Encabezado de la venta

Hay que insertar el encabezado de venta en la tabla **encabezadocumento**

```sql
# afectoexento='A'
# local = '000'
# tipo = '06'
# tipo1 = 'E' o ' ' (factura electrónica o normal)

insert into encabezadocumento (fecha, vence, afectoexento, rut, local, id, tipo, numero, codigo, tipo1, publicadonro ) values  (?, ?, ?, ?, ?, ?, ?, ?, ?,?,?)

```

### Detalle de Ventas

Por cada detalle se debe agregar un registro en la tabla **detalledocumento**.

Cosas a tener en consideración:

* Variación corresponde al descueto multiplicado por (-1)
* Hay que tener en consideración que el registro asociado a la conducción debe ir al final
* El campo linea es de largo 3 con 0 adelante y no debe ser mayor que 25 (sino, se crea una nueva factura)
* Cuando el producto es numerado, se deben agregar los número como parte del nombre


```sql
# paridad = 1.0F
# tipoid = '06'
# local = '000'

insert into detalledocumento (precioventa, totallinea, paridad, preciocosto, cantidad, id, linea, tipoid, local, articulo, descripcion, variacion) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)

```

### Total Documento

AL finalizar todo el proceso se debe agregar un registro en la tabla **totaldocumento**

```sql
# tipoid = '06'

insert into totaldocumento (totaldetalle, totaliva, totalila, totalneto, total, id, tipoid) values (?, ?, ?, ?, ?, ?, ?)

```

### Cuenta Documento

Hay que agregar un registro en la tabla **ctadocto**

```sql
# TIPO1 = 'E' o ' ' según sea factura electrónica o no
# fecha_vencimiento corresponde al valor del día + los días de la condición de venta

insert into ctadocto (rut_cliente, fecha_vencimiento, comision, fecha_ingreso, vendedor, valor_bruto, valor_iva, valor_neto, tipo, numero, codigo_cliente, local_venta, valor_ila, TIPO1) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)

```

### Folios

Hay que agregar un registro en la tabla **folios**

```sql
# TIPO = '06'
# TIPO1 = 'E' o ' ' según sea factura electrónica o no
# NUMERO corresponde al número de factura

INSERT INTO folios (NUMERO, TIPO, TIPO1) VALUES (? , ?, ?)

```

# ILA

Hay que insertar registros en la tabla **MSOSTVENTASILA**, uno por cada tipo de ILA aplicado a los productos.
Cada registro corresponde a un código de ila y la suma de todos los productos que tienen asociado dicho código.

```sql
#numero corresponde al número de factura

insert into MSOSTVENTASILA (tipo, TIPO1, codigo, valor, numero, ila) values (?, ?, ?, ?, ?, ?)
```

### Actualizar el ID de la base de datos

Cuando se graba un encabezado de ventas, este recibe el ID obtenido desde PARAMETROS  + 1, por lo tanto corresponde actualizar la tabla PARAMETROS con el nuevo valor.





