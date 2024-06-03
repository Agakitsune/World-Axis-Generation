package org.example.wag;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Group {

    private @NotNull List<Subject> subjects;

    public Group(Subject... subjects) {
        this.subjects = List.of(subjects);
    }

    public Group(@NotNull Collection<Subject> subjects) {
        this.subjects = List.copyOf(subjects);
    }

    private Group(@NotNull List<Subject> subjects) {
        this.subjects = subjects;
    }

    @Override
    public String toString() {
        return "Group" + subjects.stream().map(s -> s.getName() + " " + s.getGender()).collect(Collectors.joining(", ", "{", "}"));
    }

    public static Group move(@NotNull List<Subject> subjects) {
        return new Group(subjects);
    }
}
