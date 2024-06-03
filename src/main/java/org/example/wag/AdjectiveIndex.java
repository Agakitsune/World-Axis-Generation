package org.example.wag;

import edu.stanford.nlp.ling.CoreLabel;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class AdjectiveIndex {
    private int @NotNull [] lemma_hashes = new int[0];
    private int @NotNull [] adjective_hashes = new int[0];
    private int @NotNull [] entries = new int[0];

    public AdjectiveIndex add(@NotNull String adjective, @NotNull String lemma) {
        int lindex = Arrays.stream(lemma_hashes).boxed().toList().indexOf(lemma.hashCode());
        int aindex;
        if (lindex == -1) {
            lindex = lemma_hashes.length;
            aindex = adjective_hashes.length;
            lemma_hashes = Arrays.copyOf(lemma_hashes, lemma_hashes.length + 1);
            lemma_hashes[lemma_hashes.length - 1] = lemma.hashCode();
            adjective_hashes = Arrays.copyOf(adjective_hashes, adjective_hashes.length + 1);
            adjective_hashes[adjective_hashes.length - 1] = adjective.hashCode();
            entries = Arrays.copyOf(entries, entries.length + 1);
            entries[aindex] = lindex;
        } else {
            aindex = Arrays.stream(adjective_hashes).boxed().toList().indexOf(adjective.hashCode());
            if (aindex == -1) {
                aindex = adjective_hashes.length;
                adjective_hashes = Arrays.copyOf(adjective_hashes, adjective_hashes.length + 1);
                adjective_hashes[adjective_hashes.length - 1] = adjective.hashCode();
                entries = Arrays.copyOf(entries, entries.length + 1);
                entries[aindex] = lindex;
            }
        }
        return this;
    }

    public AdjectiveIndex add(@NotNull CoreLabel label) {
        add(label.word(), label.lemma());
        return this;
    }

    public boolean hasLemma(@NotNull String lemma) {
        return Arrays.stream(lemma_hashes).anyMatch(h -> h == lemma.hashCode());
    }

    public boolean hasLemmaFromCode(int code) {
        return Arrays.stream(lemma_hashes).anyMatch(h -> h == code);
    }

    public boolean hasAdjective(@NotNull String adjective) {
        return Arrays.stream(adjective_hashes).anyMatch(h -> h == adjective.hashCode());
    }

    public boolean hasAdjectiveFromCode(int code) {
        return Arrays.stream(adjective_hashes).anyMatch(h -> h == code);
    }

    public int getLemmaIndex(@NotNull String lemma) {
        return Arrays.stream(lemma_hashes).boxed().toList().indexOf(lemma.hashCode());
    }
    public int getLemmaIndexFromCode(int code) {
        return Arrays.stream(lemma_hashes).boxed().toList().indexOf(code);
    }
    public int getAdjectiveIndex(@NotNull String adjective) {
        return Arrays.stream(adjective_hashes).boxed().toList().indexOf(adjective.hashCode());
    }
    public int getAdjectiveIndexFromCode(int code) {
        return Arrays.stream(adjective_hashes).boxed().toList().indexOf(code);
    }
    public int getLemmaIndexFromAdjective(@NotNull String adjective) {
        return entries[getAdjectiveIndex(adjective)];
    }
    public int getLemmaIndexFromAdjectiveFromCode(int code) {
        return entries[getAdjectiveIndexFromCode(code)];
    }
    public int @NotNull [] getAdjectivesFromLemma(@NotNull String lemma) {
        int index = getLemmaIndex(lemma);
        return Arrays.stream(entries).filter(i -> i == index).map(i -> adjective_hashes[i]).toArray();
    }
    public int getLemmaHashFromIndex(int index) {
        return lemma_hashes[index];
    }
    public int getAdjectiveHashFromIndex(int index) {
        return adjective_hashes[index];
    }

    public void merge(@NotNull AdjectiveIndex other) {
        int @NotNull [] new_lemma_hashes = Arrays.stream(other.lemma_hashes).filter((IntPredicate) Predicate.not(this::hasLemmaFromCode)).toArray();
        int @NotNull [] new_adjective_hashes = Arrays.stream(other.adjective_hashes).filter((IntPredicate) Predicate.not(this::hasAdjectiveFromCode)).toArray();
        int @NotNull [] new_entries = IntStream.range(0, new_adjective_hashes.length).map(i -> Arrays.stream(other.entries).boxed().toList().indexOf(i)).toArray();
        if (new_lemma_hashes.length > 0) {
            lemma_hashes = Arrays.copyOf(lemma_hashes, lemma_hashes.length + new_lemma_hashes.length);
            System.arraycopy(new_lemma_hashes, 0, lemma_hashes, lemma_hashes.length - new_lemma_hashes.length, new_lemma_hashes.length);
        }
        if (new_adjective_hashes.length > 0) {
            adjective_hashes = Arrays.copyOf(adjective_hashes, adjective_hashes.length + new_adjective_hashes.length);
            System.arraycopy(new_adjective_hashes, 0, adjective_hashes, adjective_hashes.length - new_adjective_hashes.length, new_adjective_hashes.length);
            entries = Arrays.copyOf(entries, entries.length + new_entries.length);
            System.arraycopy(new_entries, 0, entries, entries.length - new_entries.length, new_entries.length);
        }
    }

    @Override
    public String toString() {
        return "AdjectiveIndex{" +
                "lemma_hashes=" + Arrays.toString(lemma_hashes) +
                ", adjective_hashes=" + Arrays.toString(adjective_hashes) +
                ", entries=" + Arrays.toString(entries) +
                '}';
    }
}
