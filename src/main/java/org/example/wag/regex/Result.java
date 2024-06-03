package org.example.wag.regex;

import edu.stanford.nlp.ling.CoreLabel;

import java.util.List;
import java.util.stream.Stream;

public class Result {
    private final List<CoreLabel> matches;

    public Result(List<CoreLabel> matches) {
        this.matches = matches;
    }

    public Stream<CoreLabel> stream() {
        return matches.stream();
    }

    public List<CoreLabel> list() {
        return matches;
    }

    public String match() {
        return matches.stream().map(CoreLabel::word).reduce("", (a, b) -> a + " " + b).trim();
    }

    public String match(int i) {
        return matches.get(i).word();
    }

    public int size() {
        return matches.size();
    }

    public CoreLabel get(int i) {
        return matches.get(i);
    }

    public CoreLabel first() {
        return matches.getFirst();
    }

    public CoreLabel last() {
        return matches.getLast();
    }

    public CoreLabel[] toArray() {
        return matches.toArray(new CoreLabel[0]);
    }

    @Override
    public String toString() {
        return "Result{" +
                "matches=" + matches +
                '}';
    }
}
