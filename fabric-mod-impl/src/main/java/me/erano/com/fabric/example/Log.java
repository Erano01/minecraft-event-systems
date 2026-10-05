package me.erano.com.fabric.example;

final class Log {
    private Log() {
    }

    static void line(String message) {
        System.out.printf("  [%s] %s%n", Thread.currentThread().getName(), message);
    }
}
