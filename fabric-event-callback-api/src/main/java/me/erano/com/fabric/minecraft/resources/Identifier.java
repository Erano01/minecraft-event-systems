package me.erano.com.fabric.minecraft.resources;

// Minecraft'in Identifier'i yerine gecen stub ("namespace:path"). Faz siralamasinda esitlik bozucu
// olarak karsilastirildigi icin Comparable.
public record Identifier(String namespace, String path) implements Comparable<Identifier> {
    public static Identifier fromNamespaceAndPath(String namespace, String path) {
        return new Identifier(namespace, path);
    }

    @Override
    public int compareTo(Identifier other) {
        int cmp = path.compareTo(other.path);
        return cmp != 0 ? cmp : namespace.compareTo(other.namespace);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
