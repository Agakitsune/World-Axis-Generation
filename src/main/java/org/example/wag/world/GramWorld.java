package org.example.wag.world;

import edu.stanford.nlp.ling.CoreLabel;
import org.example.wag.Label;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GramWorld implements World {
    public List<CoreLabel> gram = new ArrayList<>();
    boolean useProper = false;

    public GramWorld(boolean useProper) {
        this.useProper = useProper;
    }

    @Override
    public void process(@NotNull List<CoreLabel> labels) {}

    @Override
    public void process(@NotNull List<CoreLabel> labels, int cursor) {
        int subcursor = cursor;
        int sindex = labels.get(cursor).index();

        //System.out.println("GRAM: " + labels.get(cursor) + " " + sindex);

        if (useProper) {
            for (; subcursor < labels.size(); subcursor++) {
                CoreLabel label = labels.get(subcursor);

                if ((subcursor != cursor) && (label.index() <= sindex)) {
                    break;
                }

                if (!Label.isProperNoun(label)) {
                    break;
                }

                gram.add(label);
            }
        } else {
            for (; subcursor < labels.size(); subcursor++) {
                CoreLabel label = labels.get(subcursor);

                if ((subcursor != cursor) && (label.index() <= sindex)) {
                    break;
                }

                if (!Label.isNoun(label)) {
                    break;
                }

                gram.add(label);
            }
        }
    }

    public GramWorld useProper(boolean useProper) {
        this.useProper = useProper;
        return this;
    }

    @Override
    public void reset() {
        gram.clear();
    }
}
