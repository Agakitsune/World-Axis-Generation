package org.example.wag;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class VocabSpace {
    private final @NotNull List<String> vocab = new ArrayList<>();
    private int @NotNull [] hashes = new int[0];

    public void addWord(@NotNull String word) {
        hashes = Arrays.copyOf(hashes, vocab.size() + 1);
        hashes[vocab.size()] = word.hashCode();
        vocab.add(word);
    }

    public void addWords(@NotNull Collection<String> words) {
        words.forEach(this::addWord);
    }

    public List<String> getVocab() {
        return vocab;
    }

    public void resize(int size) {
        hashes = Arrays.copyOf(hashes, size);
    }

    public boolean contain(@NotNull String word) {
        final int code = word.hashCode();
        return Arrays.stream(hashes).anyMatch(h -> code == h);
    }

    public void merge(@NotNull VocabSpace space) {
        space.getVocab().stream().filter(Predicate.not(this::contain)).forEach(this::addWord);
    }

    public int getIndexOf(@NotNull String word) {
        int code = word.hashCode();
        for (int i = 0; i < hashes.length; i++) {
            if (hashes[i] == code) {
                return i;
            }
        }
        return -1;
    }

    public int getIndexOf(int code) {
        for (int i = 0; i < hashes.length; i++) {
            if (hashes[i] == code) {
                return i;
            }
        }
        return -1;
    }

    public String getWord(int code) {
        int index = getIndexOf(code);
        if (index != -1) {
            return vocab.get(index);
        }
        return null;
    }

    public String wordAt(int index) {
        return vocab.get(index);
    }

    public int size() {
        return vocab.size();
    }

    @Override
    public String toString() {
        return "VocabSpace{" +
                "vocab=" + vocab +
                ", hashes=" + Arrays.toString(hashes) +
                '}';
    }
}
