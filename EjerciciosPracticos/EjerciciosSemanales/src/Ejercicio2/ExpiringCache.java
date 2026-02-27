package Ejercicio2;

import java.util.*;

public class ExpiringCache<K, V> {
    private final Map<K, CacheEntry<V>> store = new HashMap<>();
    private final long defaultTtlMillis;

    private record CacheEntry<V>(V value, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    public ExpiringCache(long defaultTtlMillis) {
        this.defaultTtlMillis = defaultTtlMillis;
    }

    public void put(K key, V value) {
        put(key, value, defaultTtlMillis);
    }

    public void put(K key, V value, long ttlMillis) {
        long expiresAt = System.currentTimeMillis() + ttlMillis;
        store.put(key, new CacheEntry<>(value, expiresAt));
    }

    public Optional<V> get(K key) {
        CacheEntry<V> entry = store.get(key);
        if (entry == null || entry.isExpired()) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    public void evictExpired() {
        store.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    public int size() {
        evictExpired();
        return store.size();
    }

    @Override
    public String toString() {
        evictExpired();
        StringBuilder sb = new StringBuilder("Cache{");
        store.forEach((k, v) -> sb.append(k).append("=").append(v.value()).append(", "));
        if (!store.isEmpty()) sb.setLength(sb.length() - 2);
        return sb.append("}").toString();
    }

    public static void main(String[] args) throws InterruptedException {
        ExpiringCache<String, String> cache = new ExpiringCache<>(5000);

        System.out.println("=== Operaciones Basicas ===");
        cache.put("user:1", "Ana");
        cache.put("user:2", "Luis");
        cache.put("user:3", "Maria");
        System.out.println("Cache: " + cache);
        System.out.println("Size: " + cache.size());

        System.out.println("\nget user:1 = " + cache.get("user:1"));
        System.out.println("get noexiste = " + cache.get("noexiste"));

        System.out.println("\n=== TTL Personalizado (200ms) ===");
        cache.put("temp", "dato temporal", 200);
        System.out.println("Antes de esperar: get temp = " + cache.get("temp"));
        Thread.sleep(300);
        System.out.println("Despues de 300ms: get temp = " + cache.get("temp"));

        System.out.println("\n=== Expiracion Total (1s TTL) ===");
        ExpiringCache<Integer, String> cache2 = new ExpiringCache<>(500);
        cache2.put(1, "uno");
        cache2.put(2, "dos");
        cache2.put(3, "tres");
        System.out.println("Antes: size = " + cache2.size());
        Thread.sleep(600);
        cache2.evictExpired();
        System.out.println("Despues de 600ms: size = " + cache2.size());
    }
}