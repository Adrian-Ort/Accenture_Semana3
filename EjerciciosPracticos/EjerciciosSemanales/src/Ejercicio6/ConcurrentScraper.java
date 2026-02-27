package Ejercicio6;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

record PageResult(String url, int statusCode, String title, long responseTimeMs) {
    @Override
    public String toString() {
        return String.format("[%d] %s (%dms)", statusCode, title, responseTimeMs);
    }
}

public class ConcurrentScraper {
    private final ExecutorService executor;

    public ConcurrentScraper(int threadCount) {
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    private PageResult fetchPage(String url) {
        long start = System.currentTimeMillis();
        try {
            Thread.sleep((long) (200 + Math.random() * 1300));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long elapsed = System.currentTimeMillis() - start;
        return new PageResult(url, 200, "Page: " + url, elapsed);
    }

    public List<PageResult> fetchAll(List<String> urls) {
        List<CompletableFuture<PageResult>> futures = urls.stream()
                .map(url -> CompletableFuture.supplyAsync(
                        () -> fetchPage(url), executor))
                .collect(Collectors.toList());

        CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])).join();

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    public List<PageResult> fetchWithTimeout(List<String> urls, long timeoutMs) {
        List<CompletableFuture<PageResult>> futures = urls.stream()
                .map(url -> CompletableFuture.supplyAsync(
                                () -> fetchPage(url), executor)
                        .completeOnTimeout(
                                new PageResult(url, 408, "TIMEOUT: " + url, timeoutMs),
                                timeoutMs, TimeUnit.MILLISECONDS))
                .collect(Collectors.toList());

        CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])).join();

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    public void printReport(List<PageResult> results) {
        System.out.println("--- Resultados ---");
        results.forEach(System.out::println);

        double avg = results.stream()
                .mapToLong(PageResult::responseTimeMs)
                .average().orElse(0);
        System.out.printf("\nTiempo promedio: %.0fms%n", avg);

        results.stream()
                .min(Comparator.comparingLong(PageResult::responseTimeMs))
                .ifPresent(p -> System.out.println("Mas rapida: " + p));

        results.stream()
                .max(Comparator.comparingLong(PageResult::responseTimeMs))
                .ifPresent(p -> System.out.println("Mas lenta: " + p));

        Map<Integer, Long> byStatus = results.stream()
                .collect(Collectors.groupingBy(
                        PageResult::statusCode, Collectors.counting()));
        System.out.println("Por status: " + byStatus);
    }

    public void shutdown() {
        executor.shutdown();
    }

    public static void main(String[] args) {
        ConcurrentScraper scraper = new ConcurrentScraper(4);

        List<String> urls = List.of(
                "example.com", "google.com", "github.com", "stackoverflow.com"
        );

        System.out.println("=== Fetch All (paralelo) ===");
        long start = System.currentTimeMillis();
        List<PageResult> results = scraper.fetchAll(urls);
        long elapsed = System.currentTimeMillis() - start;
        scraper.printReport(results);
        System.out.printf("Tiempo total (paralelo): %dms%n", elapsed);

        System.out.println("\n=== Fetch con Timeout (500ms) ===");
        List<PageResult> resultsTimeout = scraper.fetchWithTimeout(urls, 500);
        scraper.printReport(resultsTimeout);

        scraper.shutdown();
    }
}
