package org.example.wag.regex;

import edu.stanford.nlp.ling.CoreLabel;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public abstract class Pattern {
    protected abstract boolean test(@NotNull List<CoreLabel> labels);
    protected abstract List<CoreLabel> match(@NotNull List<CoreLabel> labels);
    protected abstract int skip(@NotNull List<CoreLabel> labels);

    private static TagPattern tag(@NotNull String pattern, int i) {
        int a = pattern.indexOf('>', i);
        if (a == -1)
            throw new IllegalArgumentException("Invalid pattern");
        return new TagPattern(pattern.substring(i, a));
    }

    private static Pattern group(@NotNull String pattern, int i, boolean inGroup) {
        SequencePattern sequencePattern = new SequencePattern();

        for (int j = i; j < pattern.length(); j++) {
            char c = pattern.charAt(j);
            if (c == '<') {
                sequencePattern.add(tag(pattern, j + 1));
                j = pattern.indexOf('>', j + 1);
            } else if (c == '(') {
                sequencePattern.add(group(pattern, j + 1, true));
                j = pattern.indexOf(')', j + 1);
            } else if (c == ')' && inGroup) {
                break;
            } else if (c == '|') {
                return new AlternatePattern(sequencePattern, group(pattern, j + 1, inGroup));
            } else {
                sequencePattern.mod(c);
            }
        }
        return sequencePattern;
    }

    public List<Result> consume(@NotNull List<CoreLabel> labels) {
        List<Result> results = new ArrayList<>();
        List<CoreLabel> list = labels;
        while (!list.isEmpty()) {
            if (test(list)) {
                List<CoreLabel> match = match(list);
                results.add(new Result(match));
                list = list.subList(skip(list), list.size());
            } else {
                list = list.subList(1, list.size());
            }
        }
        return results;
    }

    public static Pattern compile(@NotNull String pattern) {
        return group(pattern, 0, false);
    }
}

class TagPattern extends Pattern {
    private final java.util.regex.Pattern tag;

    TagPattern(String regex) {
        this.tag = java.util.regex.Pattern.compile(regex);
    }

    @Override
    protected boolean test(@NotNull List<CoreLabel> labels) {
        if (labels.isEmpty())
            return false;
        return tag.matcher(labels.getFirst().tag()).find();
    }

    @Override
    protected List<CoreLabel> match(@NotNull List<CoreLabel> labels) {
        return test(labels) ? List.of(labels.getFirst()) : Collections.emptyList();
    }

    @Override
    protected int skip(@NotNull List<CoreLabel> labels) {
        return test(labels) ? 1 : 0;
    }

    @Override
    public String toString() {
        return "<" + tag.pattern() + ">";
    }
}

class ModPattern extends Pattern {
    enum Mod {
        ZERO_OR_MORE("*"),
        ONE_OR_MORE("+"),
        ZERO_OR_ONE("?");

        private final String mod;

        Mod(String mod) {
            this.mod = mod;
        }

        @Override
        public String toString() {
            return mod;
        }
    }
    private final Pattern pattern;
    private final Mod mod;

    ModPattern(Pattern pattern, String mod) {
        this.pattern = pattern;
        this.mod = switch (mod) {
            case "*" -> Mod.ZERO_OR_MORE;
            case "+" -> Mod.ONE_OR_MORE;
            case "?" -> Mod.ZERO_OR_ONE;
            default -> throw new IllegalArgumentException("Invalid mod");
        };
    }

    @Override
    protected boolean test(@NotNull List<CoreLabel> labels) {
        return switch (mod) {
            case ZERO_OR_MORE, ZERO_OR_ONE -> !labels.isEmpty();
            case ONE_OR_MORE -> pattern.test(labels);
        };
    }

    @Override
    protected List<CoreLabel> match(@NotNull List<CoreLabel> labels) {
        if (!pattern.test(labels))
            return Collections.emptyList();
        return switch (mod) {
            case ZERO_OR_ONE -> pattern.match(labels);
            case ONE_OR_MORE, ZERO_OR_MORE -> {
                List<CoreLabel> result = new ArrayList<>();
                List<CoreLabel> list = labels;
                while (pattern.test(list)) {
                    result.addAll(pattern.match(list));
                    list = list.subList(pattern.skip(list), list.size());
                }
                yield result;
            }
        };
    }

    @Override
    protected int skip(@NotNull List<CoreLabel> labels) {
        return match(labels).size();
    }

    @Override
    public String toString() {
        return pattern + mod.toString();
    }
}

class SequencePattern extends Pattern {
    private final List<Pattern> patterns = new ArrayList<>();

    SequencePattern(Pattern... patterns) {
        this.patterns.addAll(Arrays.asList(patterns));
    }

    void add(Pattern pattern) {
        patterns.add(pattern);
    }

    void mod(char mod) {
        Pattern pattern = patterns.removeLast();
        patterns.add(new ModPattern(pattern, String.valueOf(mod)));
    }

    @Override
    protected boolean test(@NotNull List<CoreLabel> labels) {
        List<CoreLabel> list = labels;
        for (Pattern pat : patterns) {
            if (!pat.test(list))
                return false;
            list = list.subList(pat.skip(list), list.size());
        }
        return true;
    }

    @Override
    protected List<CoreLabel> match(@NotNull List<CoreLabel> labels) {
        List<CoreLabel> list = labels;
        List<CoreLabel> result = new ArrayList<>();
        for (Pattern pat : patterns) {
            if (!pat.test(list))
                return Collections.emptyList();
            result.addAll(pat.match(list));
            list = list.subList(pat.skip(list), list.size());
        }
        return result;
    }

    @Override
    protected int skip(@NotNull List<CoreLabel> labels) {
        List<CoreLabel> list = labels;
        int count = 0;
        for (Pattern pat : patterns) {
            if (!pat.test(list))
                return count;
            count += pat.skip(list);
            list = list.subList(pat.skip(list), list.size());
        }
        return count;
    }

    @Override
    public String toString() {
        return patterns.stream().map(Objects::toString).collect(Collectors.joining());
    }
}

class AlternatePattern extends Pattern {
    private final Pattern a;
    private final Pattern b;

    AlternatePattern(Pattern a, Pattern b) {
        this.a = a;
        this.b = b;
    }

    @Override
    protected boolean test(@NotNull List<CoreLabel> labels) {
        return a.test(labels) || b.test(labels);
    }

    @Override
    public List<CoreLabel> match(@NotNull List<CoreLabel> labels) {
        if (a.test(labels))
            return a.match(labels);
        if (b.test(labels))
            return b.match(labels);
        return Collections.emptyList();
    }

    @Override
    protected int skip(@NotNull List<CoreLabel> labels) {
        if (a.test(labels))
            return a.skip(labels);
        if (b.test(labels))
            return b.skip(labels);
        return 0;
    }

    @Override
    public String toString() {
        return "(" + a + "|" + b + ")";
    }
}
