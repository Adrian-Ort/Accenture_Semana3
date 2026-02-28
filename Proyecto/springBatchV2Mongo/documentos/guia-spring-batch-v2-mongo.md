# Spring Batch v2 Mongo - Escribir reportes en MongoDB

## Que hay de nuevo en esta version?

En la v2 el Step 2 escribia los reportes en un archivo CSV. En esta variante **reemplazamos el Writer del Step 2** para que escriba directamente en una **coleccion de MongoDB**. Esto enseña a integrar dos bases de datos diferentes (MySQL y MongoDB) en un mismo Job de Spring Batch.

### Flujo completo

```
Step 1:  empleados.csv  →  procesar (bono + mayusculas)  →  tabla MySQL        (igual que v2)
Step 2:  tabla MySQL     →  calcular salario total        →  coleccion MongoDB  (CAMBIA)
```

### Que se aprende de nuevo

| Concepto | Donde se ve |
|----------|------------|
| Un Job puede usar **dos bases de datos diferentes** | MySQL en Step 1, MongoDB en Step 2 |
| `MongoItemWriter` como alternativa a `FlatFileItemWriter` | Bean `escribirEnMongo()` en BatchConfig |
| `@Document` convierte un POJO en documento MongoDB | Anotacion en `EmpleadoReporte` |
| Spring Boot autoconfigura `MongoTemplate` | Solo con la URI en `application.properties` |
| La **unica pieza que cambia** es el Writer | Reader, Processor y Step 1 quedan identicos |

### Analogia con la fabrica

Las dos estaciones siguen siendo las mismas. Lo unico que cambia es **donde se almacena el producto de la estacion 2**:

```
Estacion 1 (Step 1)                    Estacion 2 (Step 2)
+----------------------------+         +----------------------------+
| Materia prima: CSV         |         | Materia prima: tabla MySQL |
| Operacion: bono+mayusculas |  --->   | Operacion: salario total   |
| Producto: tabla MySQL      |         | Producto: coleccion Mongo  |  ← CAMBIA
+----------------------------+         +----------------------------+
```

En la v2 la estacion 2 empaquetaba el producto en un archivo CSV. Ahora lo guarda en un almacen diferente (MongoDB). La estacion y la operacion son las mismas — solo cambia el destino final.

---

## Prerequisitos

Todo lo de la v2, mas un contenedor de MongoDB corriendo.

Verificar que ambos contenedores estan corriendo:

```bash
docker ps
```

Debes ver `mysql-academia` y `mongo-academia` en la lista. Si no estan corriendo:

```bash
docker start mysql-academia
docker start mongo-academia
```

> **Importante:** Si ya ejecutaste la v2 y la tabla tiene datos, limpia los datos antes de empezar:
> ```sql
> DELETE FROM empleados_procesados;
> ```

---

## Que cambia respecto a la v2?

Solo hay **4 archivos modificados**. Todo lo demas es identico a la v2:

| Archivo | Cambio |
|---------|--------|
| `pom.xml` | Agrega dependencia `spring-boot-starter-data-mongodb` |
| `application.properties` | Agrega URI de conexion a MongoDB |
| `EmpleadoReporte.java` | Agrega `@Document`, `@Id` para ser documento MongoDB |
| `BatchConfig.java` | Reemplaza `FlatFileItemWriter` por `MongoItemWriter` |

Archivos que **NO cambian** respecto a la v2:

| Archivo | Motivo |
|---------|--------|
| `SpringBatchApplication.java` | Mismo punto de entrada |
| `Empleado.java` | Mismo modelo para Step 1 |
| `EmpleadoProcessor.java` | Misma logica de Step 1 |
| `ReporteProcessor.java` | Misma logica de Step 2 (la transformacion no depende del destino) |
| `empleados.csv` | Mismos datos de entrada |

---

## Estructura del proyecto

```
springBatchV2Mongo/
├── pom.xml                                          ★ MODIFICADO
└── src/main/
    ├── java/com/academia/batch/
    │   ├── SpringBatchApplication.java              (igual que v2)
    │   ├── config/
    │   │   └── BatchConfig.java                     ★ MODIFICADO
    │   ├── model/
    │   │   ├── Empleado.java                        (igual que v2)
    │   │   └── EmpleadoReporte.java                 ★ MODIFICADO
    │   └── processor/
    │       ├── EmpleadoProcessor.java               (igual que v2)
    │       └── ReporteProcessor.java                (igual que v2)
    └── resources/
        ├── application.properties                   ★ MODIFICADO
        └── empleados.csv                            (igual que v2)
```

---

## Paso 1: Crear el proyecto en Eclipse

1. **File** > **New** > **Maven Project**
2. Marca la casilla **Create a simple project (skip archetype selection)**
3. Llena los campos:
   - **Group Id:** `com.academia`
   - **Artifact Id:** `spring-batch-v2-mongo`
   - **Version:** `1.0.0`
   - **Packaging:** `jar`
4. Clic en **Finish**

Crea los mismos 4 paquetes que en la v2:

```
com.academia.batch
com.academia.batch.model
com.academia.batch.processor
com.academia.batch.config
```

---

## Paso 2: pom.xml (MODIFICADO)

Respecto a la v2, se agrega **una sola dependencia**: `spring-boot-starter-data-mongodb`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.2</version>
        <relativePath/>
    </parent>

    <groupId>com.academia</groupId>
    <artifactId>spring-batch-v2-mongo</artifactId>
    <version>1.0.0</version>
    <name>spring-batch-v2-mongo</name>
    <description>Spring Batch con dos Steps: CSV a MySQL y MySQL a MongoDB</description>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-batch</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

**Que hace `spring-boot-starter-data-mongodb`?**

Este starter trae todo lo necesario para trabajar con MongoDB desde Spring Boot:
- El driver de MongoDB para Java
- Spring Data MongoDB (repositorios, `MongoTemplate`, anotaciones como `@Document`)
- Autoconfiguracion: Spring Boot crea automaticamente un `MongoTemplate` a partir de la URI que pongas en `application.properties`

Es el mismo starter que usamos en el proyecto REST con MongoDB (`restSpringMongo`). La diferencia es que aqui no creamos repositorios ni controladores — solo usamos el `MongoTemplate` que Spring Batch necesita para su `MongoItemWriter`.

Despues de pegar: clic derecho en el proyecto > **Maven** > **Update Project** (o `Alt+F5`).

---

## Paso 3: application.properties (MODIFICADO)

Se mantiene toda la configuracion de MySQL (el Step 1 la necesita) y se agrega la URI de MongoDB:

```properties
# Conexion a MySQL (contenedor docker mysql-academia)
spring.datasource.url=jdbc:mysql://localhost:3306/academia
spring.datasource.username=alumno
spring.datasource.password=alumno123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Spring Batch crea automaticamente las tablas BATCH_* en MySQL
spring.batch.jdbc.initialize-schema=always

# Ejecutar el Job automaticamente al iniciar la aplicacion
spring.batch.job.enabled=true

# Conexion a MongoDB (contenedor docker mongo-academia)
spring.data.mongodb.uri=mongodb://root:root123@localhost:27017/academia?authSource=admin
```

**La linea nueva:**

```properties
spring.data.mongodb.uri=mongodb://root:root123@localhost:27017/academia?authSource=admin
```

| Parte | Significado |
|-------|-------------|
| `root:root123` | Usuario y contraseña del contenedor MongoDB |
| `localhost:27017` | Host y puerto (27017 es el puerto por defecto de MongoDB) |
| `academia` | Nombre de la base de datos donde se guardaran los documentos |
| `authSource=admin` | Base de datos donde esta registrado el usuario root |

Con esta sola linea, Spring Boot autoconfigura un `MongoTemplate` listo para usar. No necesitas crear ningun `@Bean` adicional para la conexion.

**Dos bases de datos en una sola app:** Nota que ahora tenemos configuracion para MySQL (`spring.datasource.*`) y para MongoDB (`spring.data.mongodb.*`) en el mismo archivo. Spring Boot las maneja de forma independiente — cada una tiene su propia conexion.

---

## Paso 4: Archivos sin cambios (copiar de v2)

Los siguientes archivos son **identicos** a la v2. Si ya seguiste esa guia, puedes copiarlos directamente.

### empleados.csv (en `src/main/resources/`)

```csv
nombre,departamento,salario
Juan Perez,Ventas,25000
Maria Lopez,TI,35000
Carlos Garcia,RRHH,28000
Ana Martinez,Ventas,27000
Pedro Sanchez,TI,32000
Laura Diaz,RRHH,30000
Roberto Flores,Ventas,26000
Sofia Ramirez,TI,38000
Diego Torres,RRHH,29000
Fernanda Rios,Ventas,31000
```

### Empleado.java (en `com.academia.batch.model`)

```java
package com.academia.batch.model;

public class Empleado {

    private String nombre;
    private String departamento;
    private double salario;
    private double bono;

    public Empleado() {
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public double getSalario() {
        return salario;
    }

    public void setSalario(double salario) {
        this.salario = salario;
    }

    public double getBono() {
        return bono;
    }

    public void setBono(double bono) {
        this.bono = bono;
    }

    @Override
    public String toString() {
        return nombre + " | " + departamento + " | Salario: " + salario + " | Bono: " + bono;
    }
}
```

### EmpleadoProcessor.java (en `com.academia.batch.processor`)

```java
package com.academia.batch.processor;

import com.academia.batch.model.Empleado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

public class EmpleadoProcessor implements ItemProcessor<Empleado, Empleado> {

    private static final Logger log = LoggerFactory.getLogger(EmpleadoProcessor.class);

    @Override
    public Empleado process(Empleado empleado) {
        empleado.setNombre(empleado.getNombre().toUpperCase());
        empleado.setBono(empleado.getSalario() * 0.10);

        log.info("Step 1 - Procesando: {}", empleado);
        return empleado;
    }
}
```

### ReporteProcessor.java (en `com.academia.batch.processor`)

```java
package com.academia.batch.processor;

import com.academia.batch.model.Empleado;
import com.academia.batch.model.EmpleadoReporte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

public class ReporteProcessor implements ItemProcessor<Empleado, EmpleadoReporte> {

    private static final Logger log = LoggerFactory.getLogger(ReporteProcessor.class);

    @Override
    public EmpleadoReporte process(Empleado empleado) {
        EmpleadoReporte reporte = new EmpleadoReporte();
        reporte.setNombre(empleado.getNombre());
        reporte.setDepartamento(empleado.getDepartamento());
        reporte.setSalario(empleado.getSalario());
        reporte.setBono(empleado.getBono());
        reporte.setSalarioTotal(empleado.getSalario() + empleado.getBono());

        log.info("Step 2 - Reporte: {}", reporte);
        return reporte;
    }
}
```

### SpringBatchApplication.java (en `com.academia.batch`)

```java
package com.academia.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBatchApplication.class, args);
    }
}
```

---

## Paso 5: EmpleadoReporte.java (MODIFICADO)

Este es el modelo que el Step 2 escribe como salida. En la v2 era un POJO simple. Ahora le agregamos anotaciones de Spring Data MongoDB para que pueda guardarse como documento.

Crea la clase `EmpleadoReporte.java` en el paquete `com.academia.batch.model`:

```java
package com.academia.batch.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// Modelo usado en el Step 2: agrega el campo salarioTotal para el reporte
// En esta version se persiste como documento en la coleccion "reportes" de MongoDB
@Document(collection = "reportes")
public class EmpleadoReporte {

    @Id
    private String id;
    private String nombre;
    private String departamento;
    private double salario;
    private double bono;
    private double salarioTotal;

    public EmpleadoReporte() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public double getSalario() {
        return salario;
    }

    public void setSalario(double salario) {
        this.salario = salario;
    }

    public double getBono() {
        return bono;
    }

    public void setBono(double bono) {
        this.bono = bono;
    }

    public double getSalarioTotal() {
        return salarioTotal;
    }

    public void setSalarioTotal(double salarioTotal) {
        this.salarioTotal = salarioTotal;
    }

    @Override
    public String toString() {
        return nombre + " | " + departamento + " | Salario: " + salario
             + " | Bono: " + bono + " | Total: " + salarioTotal;
    }
}
```

### Que cambio respecto a la v2?

Se agregaron **3 cosas**:

**1. `@Document(collection = "reportes")`**

Le dice a Spring Data MongoDB: "esta clase representa un documento de la coleccion `reportes`". Es el equivalente MongoDB de `@Entity` + `@Table(name = "reportes")` en JPA/MySQL.

| MySQL (JPA) | MongoDB (Spring Data) |
|---|---|
| `@Entity` | `@Document` |
| `@Table(name = "reportes")` | `@Document(collection = "reportes")` |
| Tabla con filas | Coleccion con documentos |

**2. `@Id` y el campo `private String id`**

Todo documento en MongoDB necesita un campo `_id`. Con `@Id`, Spring Data MongoDB genera automaticamente un `ObjectId` unico al guardar el documento. Usamos `String` porque MongoDB internamente maneja los IDs como `ObjectId` y Spring los convierte a `String` automaticamente.

En MySQL el `id` era un `INT AUTO_INCREMENT` definido en la tabla. En MongoDB no necesitas crear la coleccion ni definir el ID manualmente — se genera solo.

**3. Getter y setter del id**

Se agregan `getId()` y `setId()` para el nuevo campo.

**Lo que NO cambio:** Los campos `nombre`, `departamento`, `salario`, `bono`, `salarioTotal` y sus getters/setters son identicos. El `ReporteProcessor` no necesita cambios porque el sigue creando un `EmpleadoReporte` normal — son las anotaciones las que le dicen a MongoDB como guardarlo.

---

## Paso 6: BatchConfig.java (MODIFICADO)

Esta es la clase con el cambio principal. **Solo cambia el Writer del Step 2** — todo lo demas es identico.

Crea la clase `BatchConfig.java` en el paquete `com.academia.batch.config`:

```java
package com.academia.batch.config;

import com.academia.batch.model.Empleado;
import com.academia.batch.model.EmpleadoReporte;
import com.academia.batch.processor.EmpleadoProcessor;
import com.academia.batch.processor.ReporteProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class BatchConfig {

    // =====================================================================
    //  STEP 1: Lee CSV → procesa (bono + mayusculas) → escribe en MySQL
    //  (identico a la v2)
    // =====================================================================

    @Bean
    public FlatFileItemReader<Empleado> leerCsv() {
        return new FlatFileItemReaderBuilder<Empleado>()
                .name("empleadoReader")
                .resource(new ClassPathResource("empleados.csv"))
                .delimited()
                .names("nombre", "departamento", "salario")
                .targetType(Empleado.class)
                .linesToSkip(1)
                .build();
    }

    @Bean
    public EmpleadoProcessor procesarEmpleado() {
        return new EmpleadoProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<Empleado> escribirEnBd(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Empleado>()
                .sql("INSERT INTO empleados_procesados (nombre, departamento, salario, bono) "
                   + "VALUES (:nombre, :departamento, :salario, :bono)")
                .dataSource(dataSource)
                .beanMapped()
                .build();
    }

    @Bean
    public Step paso1(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager,
                      FlatFileItemReader<Empleado> leerCsv,
                      EmpleadoProcessor procesarEmpleado,
                      JdbcBatchItemWriter<Empleado> escribirEnBd) {

        return new StepBuilder("paso1", jobRepository)
                .<Empleado, Empleado>chunk(3, transactionManager)
                .reader(leerCsv)
                .processor(procesarEmpleado)
                .writer(escribirEnBd)
                .build();
    }

    // =====================================================================
    //  STEP 2: Lee MySQL → calcula salario total → escribe en MongoDB
    //  (el Writer CAMBIA respecto a la v2)
    // =====================================================================

    @Bean
    public JdbcCursorItemReader<Empleado> leerDeBd(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<Empleado>()
                .name("empleadoDbReader")
                .dataSource(dataSource)
                .sql("SELECT nombre, departamento, salario, bono FROM empleados_procesados")
                .rowMapper((rs, rowNum) -> {
                    Empleado empleado = new Empleado();
                    empleado.setNombre(rs.getString("nombre"));
                    empleado.setDepartamento(rs.getString("departamento"));
                    empleado.setSalario(rs.getDouble("salario"));
                    empleado.setBono(rs.getDouble("bono"));
                    return empleado;
                })
                .build();
    }

    @Bean
    public ReporteProcessor procesarReporte() {
        return new ReporteProcessor();
    }

    @Bean
    public MongoItemWriter<EmpleadoReporte> escribirEnMongo(MongoTemplate mongoTemplate) {
        return new MongoItemWriterBuilder<EmpleadoReporte>()
                .template(mongoTemplate)
                .collection("reportes")
                .build();
    }

    @Bean
    public Step paso2(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager,
                      JdbcCursorItemReader<Empleado> leerDeBd,
                      ReporteProcessor procesarReporte,
                      MongoItemWriter<EmpleadoReporte> escribirEnMongo) {

        return new StepBuilder("paso2", jobRepository)
                .<Empleado, EmpleadoReporte>chunk(3, transactionManager)
                .reader(leerDeBd)
                .processor(procesarReporte)
                .writer(escribirEnMongo)
                .build();
    }

    // =====================================================================
    //  JOB: ejecuta paso1 y luego paso2
    // =====================================================================

    @Bean
    public Job procesarEmpleadosJob(JobRepository jobRepository, Step paso1, Step paso2) {
        return new JobBuilder("procesarEmpleadosJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(paso1)
                .next(paso2)
                .build();
    }
}
```

### El cambio del Writer, explicado en detalle

Lo unico que cambio del Step 2 es el Writer. Veamos la comparacion lado a lado:

**v2 — FlatFileItemWriter (escribir CSV):**
```java
@Bean
public FlatFileItemWriter<EmpleadoReporte> escribirCsv() {
    return new FlatFileItemWriterBuilder<EmpleadoReporte>()
            .name("reporteWriter")
            .resource(new FileSystemResource("reporte-empleados.csv"))
            .headerCallback(writer -> writer.write("nombre,departamento,salario,bono,salario_total"))
            .delimited()
            .names("nombre", "departamento", "salario", "bono", "salarioTotal")
            .build();
}
```

**v2Mongo — MongoItemWriter (escribir en MongoDB):**
```java
@Bean
public MongoItemWriter<EmpleadoReporte> escribirEnMongo(MongoTemplate mongoTemplate) {
    return new MongoItemWriterBuilder<EmpleadoReporte>()
            .template(mongoTemplate)
            .collection("reportes")
            .build();
}
```

**Observa lo simple que es el `MongoItemWriter`:**

| | FlatFileItemWriter (v2) | MongoItemWriter (v2Mongo) |
|---|---|---|
| Destino | `FileSystemResource("reporte-empleados.csv")` | `.collection("reportes")` |
| Mapeo de campos | `.names("nombre", "departamento", ...)` | **Automatico** — guarda todos los campos del objeto |
| Encabezados | `.headerCallback(...)` | No aplica (MongoDB no tiene encabezados) |
| Conexion | No necesita (es un archivo local) | `.template(mongoTemplate)` |
| Lineas de codigo | 7 | 4 |

**`MongoTemplate mongoTemplate`** — este parametro lo inyecta Spring automaticamente. Spring Boot lo crea a partir de la URI que pusimos en `application.properties` (`spring.data.mongodb.uri`). No necesitas definir ningun `@Bean` para el `MongoTemplate`.

**`.collection("reportes")`** — indica en que coleccion guardar los documentos. Coincide con `@Document(collection = "reportes")` del modelo. Si omites esta linea, usaria el nombre de la coleccion definido en `@Document`.

**Mapeo automatico** — a diferencia del `FlatFileItemWriter` donde tienes que listar cada campo con `.names()`, el `MongoItemWriter` convierte automaticamente el objeto Java a documento BSON (el formato de MongoDB). Cada campo del objeto se convierte en un campo del documento.

### Que cambio en los imports

Se quitaron los imports del Writer CSV:
```java
// ELIMINADOS (ya no se usan):
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.core.io.FileSystemResource;
```

Se agregaron los imports del Writer MongoDB:
```java
// NUEVOS:
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.data.mongodb.core.MongoTemplate;
```

### Que NO cambio

- **Step 1 completo** (Reader CSV, Processor, Writer MySQL) — identico
- **Reader del Step 2** (`JdbcCursorItemReader`) — sigue leyendo de MySQL
- **Processor del Step 2** (`ReporteProcessor`) — sigue calculando `salarioTotal`
- **Definicion del Job** — sigue siendo `paso1` → `paso2`

Esto demuestra el poder de la arquitectura Reader-Processor-Writer: puedes **cambiar solo una pieza** sin afectar las demas.

---

## Paso 7: Ejecutar la aplicacion

### Desde Eclipse

Clic derecho en `SpringBatchApplication.java` > **Run As** > **Java Application**

### Desde la terminal

Desde la carpeta raiz del proyecto (`springBatchV2Mongo/`):

```bash
mvn spring-boot:run
```

### Resultado esperado en consola

La salida es practicamente igual a la v2. Los logs de ambos Steps son identicos porque el Processor no cambio:

```
Job: [SimpleJob: [name=procesarEmpleadosJob]] launched with the following parameters: [{run.id=1}]

Executing step: [paso1]
Step 1 - Procesando: JUAN PEREZ | Ventas | Salario: 25000.0 | Bono: 2500.0
Step 1 - Procesando: MARIA LOPEZ | TI | Salario: 35000.0 | Bono: 3500.0
Step 1 - Procesando: CARLOS GARCIA | RRHH | Salario: 28000.0 | Bono: 2800.0
...
Step: [paso1] executed in XXms

Executing step: [paso2]
Step 2 - Reporte: JUAN PEREZ | Ventas | Salario: 25000.0 | Bono: 2500.0 | Total: 27500.0
Step 2 - Reporte: MARIA LOPEZ | TI | Salario: 35000.0 | Bono: 3500.0 | Total: 38500.0
Step 2 - Reporte: CARLOS GARCIA | RRHH | Salario: 28000.0 | Bono: 2800.0 | Total: 30800.0
...
Step: [paso2] executed in XXms

Job: [SimpleJob: [name=procesarEmpleadosJob]] completed with the following parameters: [{run.id=1}] and the following status: [COMPLETED]
```

La diferencia no se ve en los logs — se ve en **donde se guardaron los datos**. En la v2 se generaba un archivo CSV. Ahora los datos estan en MongoDB.

---

## Paso 8: Verificar los resultados

### 8.1 En MySQL (resultado del Step 1 — igual que v2)

Abre DBeaver conectado como **alumno** y ejecuta:

```sql
SELECT * FROM empleados_procesados;
```

Resultado esperado (mismos 10 registros que en la v2):

| id | nombre | departamento | salario | bono |
|----|--------|-------------|---------|------|
| 1 | JUAN PEREZ | Ventas | 25000.00 | 2500.00 |
| 2 | MARIA LOPEZ | TI | 35000.00 | 3500.00 |
| ... | ... | ... | ... | ... |
| 10 | FERNANDA RIOS | Ventas | 31000.00 | 3100.00 |

### 8.2 En MongoDB (resultado del Step 2 — NUEVO)

Conectate a MongoDB dentro del contenedor:

```bash
docker exec -it mongo-academia mongosh -u root -p root123
```

Dentro de la consola de MongoDB:

```javascript
use academia
db.reportes.find()
```

Resultado esperado (10 documentos):

```javascript
{
  _id: ObjectId('...'),
  nombre: 'JUAN PEREZ',
  departamento: 'Ventas',
  salario: 25000,
  bono: 2500,
  salarioTotal: 27500,
  _class: 'com.academia.batch.model.EmpleadoReporte'
}
{
  _id: ObjectId('...'),
  nombre: 'MARIA LOPEZ',
  departamento: 'TI',
  salario: 35000,
  bono: 3500,
  salarioTotal: 38500,
  _class: 'com.academia.batch.model.EmpleadoReporte'
}
...
```

**Cada documento tiene:**
- `_id` — generado automaticamente por MongoDB (ObjectId unico)
- `nombre`, `departamento`, `salario`, `bono`, `salarioTotal` — los campos del objeto Java
- `_class` — campo que Spring Data MongoDB agrega automaticamente para saber a que clase Java corresponde el documento

Para contar los documentos:

```javascript
db.reportes.countDocuments()
```

Resultado: `10`

Para buscar por departamento (ejemplo):

```javascript
db.reportes.find({ departamento: "TI" })
```

Resultado: 3 documentos (MARIA LOPEZ, PEDRO SANCHEZ, SOFIA RAMIREZ).

Para salir de la consola de MongoDB:

```javascript
exit
```

### 8.3 Estadisticas de ambos Steps en el JobRepository

```sql
SELECT STEP_NAME, READ_COUNT, WRITE_COUNT, COMMIT_COUNT, STATUS
FROM BATCH_STEP_EXECUTION;
```

| STEP_NAME | READ_COUNT | WRITE_COUNT | COMMIT_COUNT | STATUS |
|-----------|-----------|-------------|-------------|--------|
| paso1 | 10 | 10 | 4 | COMPLETED |
| paso2 | 10 | 10 | 4 | COMPLETED |

Mismas estadisticas que la v2. Spring Batch no distingue si escribio en CSV o en MongoDB — reporta los mismos contadores.

---

## Resumen comparativo v2 vs v2Mongo

| | v2 | v2Mongo |
|---|---|---|
| Step 1 | CSV → MySQL | CSV → MySQL **(igual)** |
| Step 2 Reader | MySQL | MySQL **(igual)** |
| Step 2 Processor | `ReporteProcessor` | `ReporteProcessor` **(igual)** |
| Step 2 Writer | `FlatFileItemWriter` (CSV) | **`MongoItemWriter`** (MongoDB) |
| Dependencias | batch + mysql | batch + mysql + **mongodb** |
| Modelo reporte | POJO simple | POJO + **`@Document` + `@Id`** |
| Resultado Step 2 | Archivo `reporte-empleados.csv` | Coleccion `reportes` en MongoDB |
| Bases de datos | 1 (MySQL) | **2** (MySQL + MongoDB) |

**Conclusion:** Cambiar el destino de un Step se reduce a cambiar el Writer y sus dependencias. El Reader y el Processor no se enteran de a donde van los datos.

---

## Para re-ejecutar

Gracias al `RunIdIncrementer`, puedes ejecutar la app multiples veces sin limpiar las tablas `BATCH_*`.

Para evitar duplicados, limpia ambas bases de datos:

```sql
-- MySQL
DELETE FROM empleados_procesados;
```

```bash
# MongoDB
docker exec -it mongo-academia mongosh -u root -p root123 --eval "use academia; db.reportes.drop()"
```

> **Nota:** `db.reportes.drop()` elimina toda la coleccion. La proxima ejecucion la recrea automaticamente. Esto es una ventaja de MongoDB: no necesitas crear la coleccion manualmente como en MySQL donde si necesitas crear la tabla.

---

## Ejecucion completa desde linea de comando

### 1. Verificar prerequisitos

```bash
# Java 17 o superior
java -version

# Maven
mvn -version

# Ambos contenedores corriendo
docker ps
```

Si no estan corriendo:

```bash
docker start mysql-academia
docker start mongo-academia
```

### 2. Preparar MySQL (si no lo has hecho antes)

```bash
docker exec -it mysql-academia mysql -u root -proot123
```

```sql
GRANT ALL PRIVILEGES ON academia.* TO 'alumno'@'%';
FLUSH PRIVILEGES;

USE academia;

CREATE TABLE IF NOT EXISTS empleados_procesados (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    departamento VARCHAR(50) NOT NULL,
    salario DECIMAL(10,2) NOT NULL,
    bono DECIMAL(10,2) NOT NULL
);

exit;
```

> **Nota:** No necesitas preparar nada en MongoDB. La coleccion `reportes` se crea automaticamente la primera vez que el `MongoItemWriter` guarda un documento.

### 3. Compilar el proyecto

```bash
mvn clean package -DskipTests
```

### 4. Ejecutar

```bash
mvn spring-boot:run
```

### 5. Verificar resultados

**MySQL (Step 1):**

```bash
docker exec -it mysql-academia mysql -u alumno -palumno123 academia -e "
SELECT * FROM empleados_procesados;
"
```

**MongoDB (Step 2):**

```bash
docker exec -it mongo-academia mongosh -u root -p root123 --eval "
use academia;
db.reportes.find().pretty();
"
```

**Contar documentos:**

```bash
docker exec -it mongo-academia mongosh -u root -p root123 --eval "
use academia;
db.reportes.countDocuments();
"
```

### 6. Limpiar datos para re-ejecutar

```bash
# MySQL
docker exec -it mysql-academia mysql -u alumno -palumno123 academia -e "
DELETE FROM empleados_procesados;
"

# MongoDB
docker exec -it mongo-academia mongosh -u root -p root123 --eval "
use academia;
db.reportes.drop();
"
```

---

## Problemas comunes

### "Communications link failure" (MySQL)
El contenedor MySQL no esta corriendo:
```bash
docker start mysql-academia
```

### "Exception authenticating MongoCredential" (MongoDB)
Verifica que la URI en `application.properties` coincida con el usuario y contraseña del contenedor:
```properties
spring.data.mongodb.uri=mongodb://root:root123@localhost:27017/academia?authSource=admin
```

### "Connection refused" en puerto 27017
El contenedor MongoDB no esta corriendo:
```bash
docker start mongo-academia
```

### Los documentos se duplican al re-ejecutar
Cada ejecucion inserta 10 documentos nuevos (MongoDB no tiene restriccion de unicidad por defecto). Limpia la coleccion antes de re-ejecutar:
```bash
docker exec -it mongo-academia mongosh -u root -p root123 --eval "use academia; db.reportes.drop()"
```

### "Table 'empleados_procesados' doesn't exist"
No creaste la tabla en MySQL. Sigue los pasos de la seccion "Preparar MySQL".

### "A job instance already exists"
Verifica que `BatchConfig.java` tenga `.incrementer(new RunIdIncrementer())` en la definicion del Job.
