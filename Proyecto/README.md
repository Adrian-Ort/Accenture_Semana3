# Spring Batch V2 Mongo

Proyecto de Spring Batch que lee un CSV de empleados, los procesa y guarda los resultados en **dos bases de datos distintas**: MySQL y MongoDB.

---

## ¿Qué hace exactamente?

Tiene dos pasos (Steps) que corren uno después del otro:

```
empleados.csv  →  [Step 1: procesar]  →  tabla MySQL
tabla MySQL    →  [Step 2: reportear] →  colección MongoDB
```

**Step 1** lee el CSV, pone los nombres en mayúsculas y calcula un bono del 10%, y guarda todo en MySQL.

**Step 2** lee lo que quedó en MySQL, calcula el salario total (salario + bono) y guarda un reporte en MongoDB.

---

## Lo que necesitas antes de correr esto

- Java 17+
- Maven
- Docker con dos contenedores corriendo:

```bash
# Si no tienes el contenedor de MySQL:
docker run -d --name mysql-academia -e MYSQL_ROOT_PASSWORD=root123 -e MYSQL_DATABASE=academia -e MYSQL_USER=alumno -e MYSQL_PASSWORD=alumno123 -p 3306:3306 mysql:8

# Si no tienes el contenedor de MongoDB:
docker run -d --name mongo-academia -e MONGO_INITDB_ROOT_USERNAME=root -e MONGO_INITDB_ROOT_PASSWORD=root123 -p 27017:27017 mongo:latest
```

Verifica que los dos estén corriendo:
```bash
docker ps
```

---

## Preparar MySQL (solo la primera vez)

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

> MongoDB no necesita preparación — la colección `reportes` se crea sola.

---

## Cómo correrlo

Desde Eclipse: clic derecho en `SpringBatchApplication.java` → **Run As → Java Application**

Desde terminal:
```bash
mvn spring-boot:run
```

Al terminar debes ver en consola:
```
Job: [procesarEmpleadosJob] completed with status: [COMPLETED]
```

---

## Verificar que funcionó

**MySQL (resultado del Step 1):**
```bash
docker exec -it mysql-academia mysql -u alumno -palumno123 academia -e "SELECT * FROM empleados_procesados;"
```

**MongoDB (resultado del Step 2):**
```bash
docker exec -it mongo-academia mongosh -u root -p root123
```
```javascript
use academia
db.reportes.find()
db.reportes.countDocuments()  // debe ser 10
```

---

## Estructura del proyecto

```
springBatchV2Mongo/
├── pom.xml
└── src/main/
    ├── java/com/academia/batch/
    │   ├── SpringBatchApplication.java   → punto de entrada
    │   ├── config/
    │   │   └── BatchConfig.java          → define los Steps y el Job
    │   ├── model/
    │   │   ├── Empleado.java             → modelo para Step 1
    │   │   └── EmpleadoReporte.java      → modelo para Step 2 (@Document MongoDB)
    │   └── processor/
    │       ├── EmpleadoProcessor.java    → bono + mayúsculas
    │       └── ReporteProcessor.java     → calcula salario total
    └── resources/
        ├── application.properties        → config MySQL + MongoDB
        └── empleados.csv                 → datos de entrada (10 empleados)
```

---

## Las dependencias clave

```xml
<!-- Batch -->
<dependency>spring-boot-starter-batch</dependency>

<!-- MySQL -->
<dependency>mysql-connector-j</dependency>

<!-- MongoDB (lo nuevo en esta versión) -->
<dependency>spring-boot-starter-data-mongodb</dependency>
```

---

## Lo interesante de este proyecto

Lo que cambia respecto a una versión anterior es **solo el Writer del Step 2**. En lugar de escribir un CSV, ahora usa `MongoItemWriter`:

```java
@Bean
public MongoItemWriter<EmpleadoReporte> escribirEnMongo(MongoTemplate mongoTemplate) {
    return new MongoItemWriterBuilder<EmpleadoReporte>()
            .template(mongoTemplate)
            .collection("reportes")
            .build();
}
```

El `MongoTemplate` lo inyecta Spring Boot solo, a partir de la URI en `application.properties`. Sin `@Bean` extra, sin configuración adicional.

Esto muestra una cosa importante de Spring Batch: puedes **cambiar el destino de los datos sin tocar el Reader ni el Processor**.

---

## Para volver a correr (limpiar datos)

```bash
# MySQL
docker exec -it mysql-academia mysql -u alumno -palumno123 academia -e "DELETE FROM empleados_procesados;"

# MongoDB
docker exec -it mongo-academia mongosh -u root -p root123 --eval "use academia; db.reportes.drop();"
```

---

## Problemas comunes

| Error | Qué hacer |
|-------|-----------|
| `Communications link failure` | `docker start mysql-academia` |
| `Connection refused` en 27017 | `docker start mongo-academia` |
| `Exception authenticating MongoCredential` | Revisa la URI en `application.properties` |
| Documentos duplicados al re-ejecutar | Borra la colección con `db.reportes.drop()` |
| `A job instance already exists` | Asegúrate que `BatchConfig` tenga `.incrementer(new RunIdIncrementer())` |
