package org.example.wag.world;

import edu.stanford.nlp.ling.CoreLabel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface World {

    default void process(@NotNull List<CoreLabel> labels) {}
    default void process(@NotNull List<CoreLabel> labels, int cursor) {}
    default void process(@NotNull List<CoreLabel> labels, int cursor, @NotNull String origin) {}
    default void reset() {}
}
