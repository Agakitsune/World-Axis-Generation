package org.example.wag;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VectorSpace {
    private int dimensions = 0;
    private double @NotNull [] @NotNull [] data = new double[0][];
    private int @NotNull [] hashes = new int[0];

    public VectorSpace(int dimensions) {
        this.dimensions = dimensions;
    }

    public VectorSpace emplace(@NotNull String word) {
        hashes = Arrays.copyOf(hashes, hashes.length + 1);
        hashes[data.length] = word.hashCode();
        data = Arrays.copyOf(data, data.length + 1);
        data[data.length - 1] = Vector.zeros(dimensions).apply();
        return this;
    }

    public VectorSpace emplaceFromCode(int code) {
        hashes = Arrays.copyOf(hashes, hashes.length + 1);
        hashes[data.length] = code;
        data = Arrays.copyOf(data, data.length + 1);
        data[data.length - 1] = Vector.zeros(dimensions).apply();
        return this;
    }

    public VectorSpace set(@NotNull String word, double @NotNull [] vector) {
        int index = IntStream.range(0, hashes.length).filter(i -> hashes[i] == word.hashCode()).findFirst().orElse(-1);
        if (index == -1) {
            index = data.length;
            hashes = Arrays.copyOf(hashes, hashes.length + 1);
            hashes[data.length] = word.hashCode();
            data = Arrays.copyOf(data, data.length + 1);
        }
        data[index] = vector;
        return this;
    }

    public VectorSpace setFromCode(int code, double @NotNull [] vector) {
        int index = IntStream.range(0, hashes.length).filter(i -> hashes[i] == code).findFirst().orElse(-1);
        if (index == -1) {
            index = data.length;
            hashes = Arrays.copyOf(hashes, hashes.length + 1);
            hashes[data.length] = code;
            data = Arrays.copyOf(data, data.length + 1);
        }
        data[index] = vector;
        return this;
    }

    public void set(int index, double @NotNull [] vector) {
        if (index >= data.length) {
            data = Arrays.copyOf(data, index + 1);
        }
        data[index] = vector;
    }

    public double[] get(@NotNull String word) {
        int index = IntStream.range(0, hashes.length).filter(i -> hashes[i] == word.hashCode()).findFirst().orElse(-1);
        if (index == -1) {
            return null;
        }
        return data[index];
    }

    public double[] getFromCode(int code) {
        int index = IntStream.range(0, hashes.length).filter(i -> hashes[i] == code).findFirst().orElse(-1);
        if (index == -1) {
            return null;
        }
        return data[index];
    }

    public double[] get(int index) {
        if (index >= data.length) {
            return null;
        }
        return data[index];
    }

    public boolean contains(@NotNull String word) {
        return Arrays.stream(hashes).anyMatch(h -> h == word.hashCode());
    }

    public int dimensions() {
        return dimensions;
    }

    public VectorSpace resize(int dimensions) {
        this.dimensions = dimensions;
        return this;
    }

    public void equalize() {
        for (int i = 0; i < data.length; i++) {
            data[i] = Arrays.copyOf(data[i], dimensions);
        }
    }

    @Override
    public String toString() {
        return "VectorSpace{" +
                "dimensions=" + dimensions +
                ", data=" + Arrays.deepToString(data) +
                '}';
    }

    public String prettyString(@NotNull VocabSpace space) {
        return Arrays.stream(hashes).mapToObj(h -> {
            final StringBuilder sb = new StringBuilder();
            final String word = space.getWord(h);
            sb.append(word).append(": ");
            final int index = Arrays.stream(hashes).boxed().toList().indexOf(h);
            sb.append(Arrays.toString(data[index]));
            return sb.toString();
        }).collect(Collectors.joining("\n"));
    }

    public static class Vector {
        private double[] data;

        private Vector(double[] data) {
            this.data = data;
        }

        public static Vector vector(double @NotNull []data) {
            return new Vector(data);
        }

        public double get(int index) {
            if (index >= data.length) {
                return 0;
            }
            return data[index];
        }

        public Vector set(int index, double value) {
            if (index >= data.length) {
                data = Arrays.copyOf(data, index + 1);
            }
            data[index] = value;
            return this;
        }

        public Vector unit() {
            double sum = 0;
            for (double value : data) {
                sum += value * value;
            }
            double length = Math.sqrt(sum);
            return new Vector(Arrays.stream(data).map(value -> value / length).toArray());
        }

        public Vector fill(float value) {
            Arrays.fill(data, value);
            return this;
        }

        public Vector add(Vector other) {
            return fadd(other, 1.0);
        }

        public Vector add(double @NotNull [] other) {
            return fadd(other, 1.0);
        }

        public Vector fadd(Vector other, double factor) {
            for (int i = 0; i < data.length; i++) {
                data[i] = data[i] + other.data[i] * factor;
            }
            return this;
        }

        public Vector fadd(double @NotNull [] other, double factor) {
            if (other.length > data.length) {
                data = Arrays.copyOf(data, other.length);
            }
            for (int i = 0; i < data.length; i++) {
                data[i] = data[i] + other[i] * factor;
            }
            return this;
        }

        public double @NotNull [] apply() {
            return data;
        }

        public static @NotNull Vector zeros(int dimensions) {
            return Vector.vector(new double[dimensions]);
        }

        public static @NotNull Vector ones(int dimensions) {
            return Vector.vector(new double[dimensions]).fill(1);
        }

        public static double @NotNull [] add(double @NotNull [] dst, double @NotNull [] src) {
            return add(dst, src, 1.0);
        }

        public static double @NotNull [] add(double @NotNull [] dst, double @NotNull [] src, double factor) {
            for (int i = 0; i < dst.length; i++) {
                dst[i] = dst[i] + src[i] * factor;
            }
            return dst;
        }
    }
}
