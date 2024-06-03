package org.example.wag;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

public class Subject {

    public enum Gender {
        NEUTRAL,
        MALE,
        FEMALE;

        public static boolean similar(Gender a, Gender b) {
            if (a == NEUTRAL || b == NEUTRAL) {
                return true;
            } else {
                return a == b;
            }
        }
    }

    private int mainName;
    private int @NotNull [] hashes;
    private final @NotNull GraphSpace gspace = new GraphSpace(); // each subject has its own graph space because it is unique, must store kind of vocab in there to keep track of the words to avoid duplicate
    private @NotNull Gender gender = Gender.NEUTRAL;

    public Subject(@NotNull String name) {
        this.mainName = name.hashCode();
        this.hashes = new int[0];
        gspace.emplace(name);
    }

    public Subject(@NotNull String name, @NotNull Gender gender) {
        this.mainName = name.hashCode();
        this.gender = gender;
        this.hashes = new int[0];
        gspace.emplace(name);
    }

    public Subject(@NotNull String name, @NotNull Collection<String> aliases) {
        final Object @NotNull [] array = aliases.toArray();

        this.mainName = name.hashCode();
        this.hashes = new int[aliases.size()];

        IntStream.range(0, aliases.size()).forEach(i -> this.hashes[i] = array[i].hashCode());
        gspace.emplace(name);
    }

    public Subject(@NotNull String name, @NotNull Collection<String> aliases, @NotNull Gender gender) {
        final Object @NotNull [] array = aliases.toArray();

        this.mainName = name.hashCode();
        this.hashes = new int[aliases.size()];
        this.gender = gender;

        IntStream.range(0, aliases.size()).forEach(i -> this.hashes[i] = array[i].hashCode());
        gspace.emplace(name);
    }

    public @NotNull Gender getGender() {
        return gender;
    }
    public Subject setGender(@NotNull Gender gender) {
        this.gender = gender;
        return this;
    }
    public int getName() {
        return mainName;
    }
    public void setName(@NotNull String name) {
        this.mainName = name.hashCode();
    }
    public Subject addAlias(@NotNull String alias) {
        this.hashes = Arrays.copyOf(hashes, hashes.length + 1);
        this.hashes[hashes.length - 1] = alias.hashCode();
        return this;
    }
    public Subject addAliases(@NotNull Collection<String> aliases) {
        this.hashes = Arrays.copyOf(hashes, hashes.length + aliases.size());
        final Object @NotNull [] array = aliases.toArray();
        IntStream.range(0, aliases.size()).forEach(i -> this.hashes[i] = array[i].hashCode());
        return this;
    }
    public Subject addAliases(@NotNull String... aliases) {
        return addAliases(List.of(aliases));
    }
    public Subject addAttribute(@NotNull Attribute attribute) {
        /*if (!hgspace.contains(attribute.getName())) {
            hgspace.emplace(attribute.getName());
        }
        for (String a : attribute.getAmplifiers()) {
            if (!hgspace.contains(a)) {
                hgspace.emplace(a);
            }
            hgspace.setLink(attribute.getName(), a, 1);
        }
        for (String d : attribute.getDescription()) {
            if (!hgspace.contains(d)) {
                hgspace.emplace(d);
            }
            hgspace.setLink(attribute.getName(), d, 1);
        }
        hgspace.setLink(mainName, attribute.getName(), 1);*/
        return this;
    }
    public int @NotNull []getAliases() {
        return hashes;
    }
    public @NotNull GraphSpace getGraphSpace() {
        return gspace;
    }
    public boolean match(@NotNull String name) {
        final int code = name.hashCode();
        return mainName == code || Arrays.stream(hashes).anyMatch(h -> h == code);
    }
    public boolean matches(@NotNull Collection<String> names) {
        return names.stream().anyMatch(this::match);
    }
    public int matchWeight(@NotNull Collection<String> names) {
        return names.stream().filter(this::match).map(s -> s.split(" ").length).max(Integer::compareTo).orElse(0);
    }
    public boolean containsAttribute(@NotNull String attribute) {
        return gspace.contains(attribute);
    }

    @Override
    public String toString() {
        return "Subject{" +
                "name='" + mainName + '\'' +
                ", aliases=" + Arrays.toString(hashes) +
                ", gspace=" + gspace +
                ", gender=" + gender +
                '}';
    }

    public String prettyString(final @NotNull VocabSpace space) {
        return "{\n" +
                "name: '" + space.getWord(mainName) + "'\n" +
                "aliases: " + Arrays.toString(Arrays.stream(hashes).mapToObj(space::getWord).toArray()) + '\n' +
                gender + '\n' +
                "gspace: \n" +
                gspace.prettyString(space) + "\n}";
    }

    public static Gender findGenderFromName(@NotNull String name) {
        Gender gender = Gender.NEUTRAL;

        try {
            if (Files.lines(Path.of("male.txt")).anyMatch(l -> l.equalsIgnoreCase(name))) {
                gender = Gender.MALE;
            } else if (Files.lines(Path.of("female.txt")).anyMatch(l -> l.equalsIgnoreCase(name))) {
                gender = Gender.FEMALE;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return gender;
    }

    public static Gender findGenderFromPronoun(@NotNull String value) {
        final byte[][] male = new byte[][] {
                "he".getBytes(),
                "him".getBytes(),
                "his".getBytes(),
                "himself".getBytes()
        };

        final byte[][] female = new byte[][] {
                "she".getBytes(),
                "her".getBytes(),
                "hers".getBytes(),
                "herself".getBytes()
        };

        final byte[][] neutral = new byte[][] {
                "it".getBytes(),
                "its".getBytes(),
                "itself".getBytes()
        };

        if (Arrays.stream(male).anyMatch(p -> Arrays.equals(value.toLowerCase().getBytes(), p))) {
            return Gender.MALE;
        } else if (Arrays.stream(female).anyMatch(p -> Arrays.equals(value.toLowerCase().getBytes(), p))) {
            return Gender.FEMALE;
        } else if (Arrays.stream(neutral).anyMatch(p -> Arrays.equals(value.toLowerCase().getBytes(), p))) {
            return Gender.NEUTRAL;
        }
        throw new IllegalArgumentException("Unknown pronoun: " + value);
    }

    public static Subject subject(@NotNull String name) {
        return new Subject(name, findGenderFromName(name));
    }
}
