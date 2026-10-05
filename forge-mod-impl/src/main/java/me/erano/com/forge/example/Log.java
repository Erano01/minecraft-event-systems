package me.erano.com.forge.example;

final class Log {
    private Log() {
    }

    static void line(String message) {
        System.out.printf("  [%s] %s%n", Thread.currentThread().getName(), message);
    }

    // Agir bir setup isini (asset/config okuma vb.) taklit eder; kisa isler havuzda tek worker'da
    // biteceginden paralelligi gorunur kilmak icin.
    static void simulateWork() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
