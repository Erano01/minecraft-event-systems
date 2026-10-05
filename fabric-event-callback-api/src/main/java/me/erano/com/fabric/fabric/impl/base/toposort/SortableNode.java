package me.erano.com.fabric.fabric.impl.base.toposort;

import java.util.ArrayList;
import java.util.List;

// Faz grafiginin dugumu. Thread-safe degil: sadece sahibi olan event'in kilidi altinda kullanilir.
public abstract class SortableNode<N extends SortableNode<N>> {
    protected final List<N> subsequentNodes = new ArrayList<>();
    protected final List<N> previousNodes = new ArrayList<>();
    boolean visited = false;

    protected abstract String getDescription();

    protected void addSubsequentNode(N node) {
        subsequentNodes.add(node);
    }

    protected void addPreviousNode(N node) {
        previousNodes.add(node);
    }

    public static <N extends SortableNode<N>> void link(N first, N second) {
        if (first == second) {
            throw new IllegalArgumentException("Cannot link a node to itself!");
        }
        first.addSubsequentNode(second);
        second.addPreviousNode(first);
    }
}
