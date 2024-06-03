package org.example.wag;

import org.jetbrains.annotations.NotNull;

public class Relationship {
    private @NotNull Subject subject1;
    private @NotNull Subject subject2;
    private @NotNull String relationship;

    public Relationship(Subject subject1, Subject subject2, String relationship) {
        this.subject1 = subject1;
        this.subject2 = subject2;
        this.relationship = relationship;
    }

    @Override
    public String toString() {
        return "Relationship{" +
                "subject1=" + subject1 +
                ", subject2=" + subject2 +
                ", relationship='" + relationship + '\'' +
                '}';
    }
}
