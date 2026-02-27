package Ejercicio4;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

record Sale(String product, String category, double amount,
            String region, LocalDate date) {}

public class SalesAnalyzer {
    public static void main(String[] args) {
        List<Sale> sales = List.of(
                new Sale("Laptop",  "Tech",   25000, "CDMX", LocalDate.of(2026, 1, 15)),
                new Sale("Phone",   "Tech",   15000, "GDL",  LocalDate.of(2026, 1, 20)),
                new Sale("Desk",    "Office",  8000, "CDMX", LocalDate.of(2026, 2, 1)),
                new Sale("Chair",   "Office",  5000, "MTY",  LocalDate.of(2026, 2, 10)),
                new Sale("Monitor", "Tech",   12000, "CDMX", LocalDate.of(2026, 2, 15)),
                new Sale("Tablet",  "Tech",   10000, "GDL",  LocalDate.of(2026, 1, 25)),
                new Sale("Lamp",    "Office",  2000, "MTY",  LocalDate.of(2026, 1, 30))
        );

        double total = sales.stream()
                .mapToDouble(Sale::amount)
                .sum();
        System.out.println("=== Ingreso Total ===");
        System.out.printf("$%,.2f%n", total);

        Map<String, Double> byCategory = sales.stream()
                .collect(groupingBy(Sale::category, summingDouble(Sale::amount)));
        System.out.println("\n=== Ingresos por Categoria ===");
        byCategory.forEach((cat, sum) ->
                System.out.printf("  %s: $%,.2f%n", cat, sum));

        System.out.println("\n=== Top 3 Productos ===");
        sales.stream()
                .sorted(Comparator.comparingDouble(Sale::amount).reversed())
                .limit(3)
                .forEach(s -> System.out.printf("  %s: $%,.2f%n", s.product(), s.amount()));

        System.out.println("\n=== Ingresos por Region ===");
        sales.stream()
                .collect(groupingBy(Sale::region, summingDouble(Sale::amount)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(e -> System.out.printf("  %s: $%,.2f%n", e.getKey(), e.getValue()));

        Map<String, Double> avgByCategory = sales.stream()
                .collect(groupingBy(Sale::category, averagingDouble(Sale::amount)));
        System.out.println("\n=== Promedio por Categoria ===");
        avgByCategory.forEach((cat, avg) ->
                System.out.printf("  %s: $%,.2f%n", cat, avg));

        System.out.println("\n=== Producto mas Caro por Region ===");
        Map<String, Optional<Sale>> bestPerRegion = sales.stream()
                .collect(groupingBy(Sale::region,
                        maxBy(Comparator.comparingDouble(Sale::amount))));
        bestPerRegion.forEach((region, sale) ->
                sale.ifPresent(s -> System.out.printf("  %s: %s ($%,.2f)%n",
                        region, s.product(), s.amount())));

        Map<Integer, Long> byMonth = sales.stream()
                .collect(groupingBy(s -> s.date().getMonthValue(), counting()));
        System.out.println("\n=== Ventas por Mes ===");
        byMonth.forEach((month, count) ->
                System.out.printf("  Mes %d: %d ventas%n", month, count));
    }
}