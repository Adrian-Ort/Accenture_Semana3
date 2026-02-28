# Semana 3 — Ejercicios + Proyecto

Esto es lo que se trabajó esta semana: 6 ejercicios prácticos de Java y un proyecto con Spring Batch + MongoDB.

---

## ¿Qué se vio esta semana?

La semana estuvo enfocada en **colecciones, streams, programación funcional y concurrencia** en Java, y cerró con un proyecto real usando Spring Batch conectado a dos bases de datos.

### Ejercicios prácticos

| # | Nombre | ¿Qué hace? |
|---|--------|------------|
| 1 | `ContactManager` | Agenda de contactos con `TreeSet`, `Optional` y búsquedas con streams |
| 2 | `ExpiringCache` | Caché genérico con tiempo de expiración (TTL), usando genéricos y `HashMap` |
| 3 | `ValidatorDemo` | Sistema de validaciones encadenables con interfaces funcionales y records |
| 4 | `SalesAnalyzer` | Análisis de ventas con `groupingBy`, `summingDouble`, `maxBy` y más collectors |
| 5 | `TextAnalyzer` | Analizador de texto: conteo, palabras únicas, frecuencias y agrupación |
| 6 | `ConcurrentScraper` | Scraper paralelo con `CompletableFuture`, `ExecutorService` y timeout |

### Temas cubiertos

- **Colecciones avanzadas** — `TreeSet`, `HashMap`, `Optional`, `Comparator`
- **Streams y collectors** — `groupingBy`, `summingDouble`, `averagingDouble`, `maxBy`, `counting`, `flatMap`
- **Programación funcional** — interfaces funcionales, `Predicate`, composición de validadores, records
- **Genéricos** — clases genéricas con `<K, V>` y `record` genérico
- **Concurrencia** — `CompletableFuture`, `ExecutorService`, `Executors.newFixedThreadPool`, timeout

---

## El Proyecto

**Spring Batch V2 Mongo** — un job que procesa un CSV de empleados y guarda los reportes en MongoDB.

Está en la carpeta `/Proyecto/springBatchV2Mongo/`. Tiene su propio README ahí adentro con todos los detalles.

La idea rápida:
```
CSV de empleados → MySQL (Step 1)  →  MongoDB (Step 2)
```

---

## Estructura del repo

```
EjerciciosSemana3/
├── EjerciciosPracticos/
│   └── EjerciciosSemanales/
│       └── src/
│           ├── Ejercicio1/   → ContactManager
│           ├── Ejercicio2/   → ExpiringCache
│           ├── Ejercicio3/   → ValidatorDemo
│           ├── Ejercicio4/   → SalesAnalyzer
│           ├── Ejercicio5/   → TextAnalyzer
│           └── Ejercicio6/   → ConcurrentScraper
└── Proyecto/
    └── springBatchV2Mongo/   → Proyecto Spring Boot
```
