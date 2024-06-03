package org.example.wag;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GraphSpace {
    private int @NotNull [] @NotNull [] weights = new int[0][];
    private int @NotNull [] hashes = new int[0];

    public GraphSpace emplace(@NotNull String word) {
        hashes = Arrays.copyOf(hashes, hashes.length + 1);
        hashes[hashes.length - 1] = word.hashCode();
        weights = Arrays.copyOf(weights, weights.length + 1);
        weights[weights.length - 1] = new int[0];
        return this;
    }

    public GraphSpace emplace(@NotNull String word, int initialCapacity) {
        hashes = Arrays.copyOf(hashes, hashes.length + 1);
        hashes[hashes.length - 1] = word.hashCode();
        weights = Arrays.copyOf(weights, weights.length + 1);
        weights[weights.length - 1] = new int[initialCapacity];
        return this;
    }

    public boolean contains(@NotNull String word) {
        int hash = word.hashCode();
        for (int h : hashes) {
            if (h == hash) {
                return true;
            }
        }
        return false;
    }

    public void setLink(@NotNull String word1, @NotNull String word2, int weight) {
        final int windex = Arrays.stream(hashes).boxed().toList().indexOf(word1.hashCode());
        final int index = Arrays.stream(hashes).boxed().toList().indexOf(word2.hashCode());
        int max = Math.max(windex, index);
        if (max >= weights.length) {
            weights = Arrays.copyOf(weights, max + 1);
        }
        if (index >= weights[windex].length) {
            weights[windex] = Arrays.copyOf(weights[windex], index + 1);
        }
        if (windex >= weights[index].length) {
            weights[index] = Arrays.copyOf(weights[index], windex + 1);
        }
        weights[windex][index] = weight;
        weights[index][windex] = weight;
    }

    public void setLink(int windex, @NotNull String word2, int weight) {
        final int index = Arrays.stream(hashes).boxed().toList().indexOf(word2.hashCode());
        int max = Math.max(windex, index);
        if (max >= weights.length) {
            weights = Arrays.copyOf(weights, max + 1);
        }
        if (index >= weights[windex].length) {
            weights[windex] = Arrays.copyOf(weights[windex], index + 1);
        }
        if (windex >= weights[index].length) {
            weights[index] = Arrays.copyOf(weights[index], windex + 1);
        }
        weights[windex][index] = weight;
        weights[index][windex] = weight;
    }

    public void setLink(@NotNull String word1, int index, int weight) {
        final int windex = Arrays.stream(hashes).boxed().toList().indexOf(word1.hashCode());
        int max = Math.max(windex, index);
        if (max >= weights.length) {
            weights = Arrays.copyOf(weights, max + 1);
        }
        if (index >= weights[windex].length) {
            weights[windex] = Arrays.copyOf(weights[windex], index + 1);
        }
        if (windex >= weights[index].length) {
            weights[index] = Arrays.copyOf(weights[index], windex + 1);
        }
        weights[windex][index] = weight;
        weights[index][windex] = weight;
    }

    public void setLink(int windex, int index, int weight) {
        int max = Math.max(windex, index);
        if (max >= weights.length) {
            weights = Arrays.copyOf(weights, max + 1);
        }
        if (index >= weights[windex].length) {
            weights[windex] = Arrays.copyOf(weights[windex], index + 1);
        }
        if (windex >= weights[index].length) {
            weights[index] = Arrays.copyOf(weights[index], windex + 1);
        }
        weights[windex][index] = weight;
        weights[index][windex] = weight;
    }

    public int[] getWeights(@NotNull String word) {
        int index = Arrays.stream(hashes).boxed().toList().indexOf(word.hashCode());
        if (index >= weights.length) {
            return null;
        }
        return weights[index];
    }

    public int getIndex(@NotNull String word) {
        return Arrays.stream(hashes).boxed().toList().indexOf(word.hashCode());
    }

    public int size() {
        return hashes.length;
    }

    @Override
    public String toString() {
        return "HashGraphSpace{" +
                "hashes=" + Arrays.toString(hashes) +
                ", weights=" + Arrays.deepToString(weights) +
                '}';
    }

    public String prettyString(@NotNull VocabSpace space) {
        return Arrays.stream(hashes).mapToObj(h -> {
            final StringBuilder sb = new StringBuilder();
            final String word = space.getWord(h);
            sb.append(word).append(": ");
            final int index = Arrays.stream(hashes).boxed().toList().indexOf(h);
            sb.append(IntStream.range(0, weights[index].length).filter(i -> weights[index][i] != 0).mapToObj(i -> weights[index][i] == -1 ? "not " + space.getWord(hashes[i]) : space.getWord(hashes[i])).collect(Collectors.joining(", ", "[", "]")));
            return sb.toString();
        }).collect(Collectors.joining("\n"));
    }
}
