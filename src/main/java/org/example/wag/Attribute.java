package org.example.wag;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

public class Attribute {
    private final int name;
    private int @NotNull [] amplifiers = new int[0];
    private int @NotNull [] descriptors = new int[0];
    private boolean isNegative = false;

    public Attribute(@NotNull String name) {
        this.name = name.hashCode();
    }
    public Attribute addAmplifier(@NotNull String amplifier) {
        amplifiers = Arrays.copyOf(amplifiers, amplifiers.length + 1);
        amplifiers[amplifiers.length - 1] = amplifier.hashCode();
        return this;
    }
    public Attribute addAmplifier(@NotNull Collection<String> amplifiers) {
        this.amplifiers = Arrays.copyOf(this.amplifiers, this.amplifiers.length + amplifiers.size());
        for (String amplifier : amplifiers) {
            this.amplifiers[this.amplifiers.length - 1] = amplifier.hashCode();
        }
        return this;
    }
    public Attribute addDescription(@NotNull String descriptor) {
        descriptors = Arrays.copyOf(descriptors, descriptors.length + 1);
        descriptors[descriptors.length - 1] = descriptor.hashCode();
        return this;
    }
    public Attribute addDescription(@NotNull Collection<String> descriptors) {
        this.descriptors = Arrays.copyOf(this.descriptors, this.descriptors.length + descriptors.size());
        for (String descriptor : descriptors) {
            this.descriptors[this.descriptors.length - 1] = descriptor.hashCode();
        }
        return this;
    }
    public Attribute setNegative(boolean negative) {
        isNegative = negative;
        return this;
    }

    public int getName() {
        return name;
    }
    public int @NotNull [] getAmplifiers() {
        return amplifiers;
    }
    public int @NotNull [] getDescription() {
        return descriptors;
    }

    @Override
    public String toString() {
        return "Attribute{" +
                "name='" + name + '\'' +
                ", amplifier=" + Arrays.toString(amplifiers) +
                ", descriptors=" + Arrays.toString(descriptors) +
                '}';
    }
}
