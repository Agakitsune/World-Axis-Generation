package org.example.wag.world;

import edu.stanford.nlp.ling.CoreLabel;
import org.example.wag.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SubjectWorld implements World {
    public int cursor = 0;
    private final @NotNull List<Subject> subjects;
    private final @NotNull VocabSpace vocab;
    public final @NotNull VectorSpace vspace;
    public final @NotNull GraphSpace gspace;
    public final @NotNull AdjectiveIndex index;

    public SubjectWorld(@NotNull List<Subject> subjects, @NotNull VocabSpace vocab, @NotNull VectorSpace vspace, @NotNull GraphSpace gspace, @NotNull AdjectiveIndex index) {
        this.subjects = subjects;
        this.vocab = vocab;
        this.vspace = vspace;
        this.gspace = gspace;
        this.index = index;
    }

    public List<Subject> findSubjects(@NotNull List<String> aliases) {
        return subjects.stream().filter(s -> s.matches(aliases)).toList();
    }

    public void addWordToVocab(@NotNull String word) {
        if (!vocab.contain(word)) {
            vocab.addWord(word);
        }
    }

    public void addWordToVectorSpace(@NotNull String word) {
        if (!this.vspace.contains(word)) {
            final int d = this.vspace.dimensions();
            this.vspace.set(word, VectorSpace.Vector.zeros(d).set(d - 1, 1).apply());
            this.vspace.equalize();
        }
    }

    public void addWordToGraphSpace(@NotNull String word) {
        if (!this.gspace.contains(word)) {
            this.gspace.emplace(word);
        }
    }

    public void addWordToGraphSpace(@NotNull GraphSpace gspace, @NotNull String word) {
        if (!gspace.contains(word)) {
            gspace.emplace(word);
        }
    }

    public void addWordToAll(@NotNull String word) {
        addWordToVocab(word);
        addWordToVectorSpace(word);
        addWordToGraphSpace(word);
    }

    @Override
    public void process(@NotNull List<CoreLabel> labels, int cursor) {
        int subcursor = cursor;
        final int sindex = labels.get(cursor).index() + 1;
        boolean hasNoble;
        boolean hasNoun = false;
        boolean adjective = false;
        boolean negate = false;

        final @NotNull Subject subject;

        final @NotNull String main;
        final @NotNull String fullname;

        final @NotNull GramWorld world = new GramWorld(true);
        final @NotNull GraphSpace lgspace;

        @NotNull CoreLabel label;

        @NotNull List<String> aliases = new ArrayList<>();
        final @NotNull List<Subject> matches;
        final @NotNull List<String> accumulator = new ArrayList<>();

        world.process(labels, subcursor);
        subcursor += world.gram.size();

        aliases.add(world.gram.getFirst().word());

        if (world.gram.size() >= 2) {
            fullname = world.gram.stream().map(CoreLabel::word).collect(Collectors.joining(" "));
            aliases.add(fullname);
            if (world.gram.size() > 2) {
                hasNoble = world.gram.stream().anyMatch(Label::isNobiliaryParticle);
                Stream<CoreLabel> stream = world.gram.stream();
                if (hasNoble) {
                    stream = stream.filter(Predicate.not(Label::isNobiliaryParticle));
                }
                List<String> reducedNames = stream.map(CoreLabel::word).toList();
                if (reducedNames.size() > 2) {
                    String reducedName = String.join(" ", reducedNames);
                    String simpleName = reducedNames.getFirst() + " " + reducedNames.getLast();

                    for (int i = 1; i < reducedNames.size() - 1; i++) {
                        String altName = reducedNames.get(i) + " " + reducedNames.getLast();
                        aliases.add(altName);
                        aliases.add(reducedNames.get(i));
                    }

                    if (hasNoble) {
                        aliases.add(reducedName);
                    }
                    aliases.add(simpleName);
                } else {
                    String simpleName = reducedNames.getFirst() + " " + reducedNames.getLast();
                    aliases.add(simpleName);
                }
            }
        }

        matches = findSubjects(aliases);
        if (matches.isEmpty()) {
            aliases.removeFirst();
            subjects.add(Subject.subject(world.gram.getFirst().word()).addAliases(aliases));
            addWordToVocab(world.gram.getFirst().word());
            subject = subjects.getLast();

            main = world.gram.getFirst().word();
        } else {
            List<String> finalAliases = aliases;
            subject = matches.stream().max(Comparator.comparingInt(a -> a.matchWeight(finalAliases))).orElse(null);
            aliases = aliases.stream().filter(Predicate.not(subject::match)).toList();
            subject.addAliases(aliases);

            main = vocab.getWord(subject.getName());
        }
        lgspace = subject.getGraphSpace();
        aliases.forEach(this::addWordToVocab);

        if (subcursor >= labels.size()) {
            this.cursor = subcursor;
            return;
        }

        label = labels.get(subcursor);

        // TODO: implement the rest of the code

        if (Label.isVerb(label)) {
            if (label.lemma().equalsIgnoreCase("be")) {
                subcursor++;
            }

            world.useProper(false);

            // subprocess
            // TODO: subprocess is similar to the one used with Noun, we are just using another GraphSpace
            for (; subcursor < labels.size(); subcursor++) {
                label = labels.get(subcursor);

                if ((subcursor != cursor) && (label.index() <= sindex)) {
                    break;
                }

                if (Label.isNoun(label)) {
                    world.reset();
                    world.process(labels, subcursor);

                    subcursor += world.gram.size() - 1;
                    final String gram = world.gram.stream().map(CoreLabel::lemma).collect(Collectors.joining(" "));
                    addWordToAll(gram);

                    lgspace.emplace(gram);
                    lgspace.setLink(main, gram, negate ? -1 : 1);

                    accumulator.forEach(a -> lgspace.setLink(gram, a, 1));

                    hasNoun = false; // reset
                } else if (Label.isAdjective(label)) {
                    addWordToVocab(label.word());
                    addWordToVocab(label.lemma());
                    addWordToGraphSpace(label.word());
                    addWordToGraphSpace(label.lemma());

                    index.add(label);
                    adjective = true; // crossed an adjective

                    lgspace.emplace(label.word());
                    lgspace.setLink(main, label.word(), negate ? -1 : 1);

                    if (hasNoun) {
                        accumulator.add(label.word());
                    }
                } else if (Label.isAdverb(label)) {
                    negate = label.lemma().equalsIgnoreCase("not");
                } else if (Label.isConjunction(label)) {
                    final @NotNull String cc = label.lemma();
                    if (cc.equalsIgnoreCase("and")) {
                        // TODO: introduce sub-block
                    } else if (cc.equalsIgnoreCase("but")) {
                        negate = false;
                    } else if (cc.equalsIgnoreCase("nor")) {
                        // TODO: introduce sub-block
                    } else if (cc.equalsIgnoreCase("neither")) {
                        negate = true;
                    }
                } else if (Label.isPrepositionOrSubordinatingConjunction(label)) {
                    final @NotNull String pp = label.lemma();
                    if (pp.equalsIgnoreCase("with")) {
                        subcursor = attributeProcess(main, world, lgspace, labels, subcursor);
                    } else if (pp.equalsIgnoreCase("of")) {

                    } else if (pp.equalsIgnoreCase("in")) {

                    }
                } else if (Label.isDeterminer(label)) {
                    accumulator.clear();
                    hasNoun = true;
                } else if (Label.isWhPronoun(label)) {
                    subcursor = attributeProcess(main, world, lgspace, labels, subcursor);
                }
            }
        }

        //System.out.println(subject.prettyString(vocab));
        //System.out.println(subject.getGraphSpace().prettyString(vocab));

        /*if (Label.isVerb(label)) {
            if (label.lemma().equalsIgnoreCase("be")) {
                subcursor++;
            }

            final @NotNull List<String> processed;
            boolean[] negates = new boolean[0];

            NounWorld.SubNounWorld subworld = new NounWorld.SubNounWorld(negates, world.useProper(false), index);
            subworld.process(labels, subcursor);
            System.out.println(subworld);

            negates = subworld.negates;
            processed = subworld.vocab.getVocab();

            processed.forEach(this::addWord);
            /*addWord(subject.getName());

            final int mainIndex = vocab.getIndexOf(subject.getName());
            for (int i = 0; i < subworld.stackes.size(); i++) {
                List<String> stack = subworld.stackes.get(i);
                final int gramIndex = vocab.getIndexOf(stack.getFirst());
                final int subGramIndex = subworld.vocab.getIndexOf(stack.getFirst());
                //if (stack.size() > 1) {
                    int subIndex;
                    int nextSubIndex = gramIndex;
                    for (int j = 0; j < stack.size() - 1; j++) {
                        subIndex = vocab.getIndexOf(stack.get(j));
                        nextSubIndex = vocab.getIndexOf(stack.get(j + 1));
                        gspace.setLink(mainIndex, subIndex, negates[subGramIndex] ? -1 : 1);
                        gspace.setLink(subIndex, nextSubIndex, 1);
                    }
                    gspace.setLink(mainIndex, nextSubIndex, negates[subGramIndex] ? -1 : 1);
                //} else {
                    //gspace.setLink(mainIndex, gramIndex, negates[subGramIndex] ? -1 : 1);
                //}
            }

            subcursor = subworld.cursor;

            if (subcursor >= labels.size()) {
                this.cursor = subcursor;
                return;
            }

            label = labels.get(subcursor);
            if (Label.isPrepositionOrSubordinatingConjunction(label)) {
                if (label.lemma().equalsIgnoreCase("with")) {
                    addWord("have"); // since it's status verb 'be', meaning that everything after that is an attribute
                    subcursor++;

                    AttributeWorld attributeWorld = new AttributeWorld(world);
                    attributeWorld.process(labels, subcursor);
                    for (Attribute attr : attributeWorld.attributes) {
                        subject.addAttribute(attr);
                    }
                    System.out.println(subject);
                }
            }
        } else if (Label.isConjunction(label)) {
            if (label.lemma().equalsIgnoreCase("and")) {
                subcursor++;
            }
        }*/

        this.cursor = subcursor;
    }

    private int attributeProcess(final @NotNull String main, final @NotNull GramWorld world, final @NotNull GraphSpace gspace, final @NotNull List<CoreLabel> labels, final int cursor) {
        int subcursor = cursor;
        final int sindex = labels.get(cursor).index();

        CoreLabel label;

        @NotNull String attributeVerb = "have"; // default to 'have', if attribute use another verb, we will find it
        String attribute = null;

        final @NotNull List<String> pre = new ArrayList<>();
        final @NotNull List<String> adjective = new ArrayList<>();
        final @NotNull List<String> post = new ArrayList<>();

        for (;subcursor < labels.size(); subcursor++) {
            label = labels.get(subcursor);

            if ((subcursor != cursor) && (label.index() <= sindex)) {
                break;
            }

            if (Label.isNoun(label)) {
                world.reset();
                world.process(labels, subcursor);

                final @NotNull String gram = world.gram.stream().map(CoreLabel::lemma).collect(Collectors.joining(" "));
                subcursor += world.gram.size() - 1;

                if (attribute == null) {
                    attribute = gram;
                } else {
                    post.add(gram);
                }
            } else if (Label.isAdjective(label)) {
                addWordToVocab(label.word());
                addWordToVocab(label.lemma());
                addWordToGraphSpace(label.word());
                addWordToGraphSpace(label.lemma());

                index.add(label);
                adjective.add(label.word());
            } else if (Label.isAdverb(label)) {

            } else if (Label.isPrepositionOrSubordinatingConjunction(label)) {
                final @NotNull String pp = label.lemma();
                if (pp.equalsIgnoreCase("of")) {
                    pre.add(attribute);
                    attribute = null;
                }
            } else if (Label.isVerb(label)) {
                attributeVerb = label.lemma(); // found it
            } else if (Label.isConjunction(label)) {
                final @NotNull String cc = label.lemma();
                if (cc.equalsIgnoreCase("and")) {
                    final String finalAttribute = attribute;
                    addWordToVocab(attributeVerb);
                    pre.forEach(this::addWordToVocab);
                    post.forEach(this::addWordToVocab);

                    addWordToGraphSpace(gspace, attribute);
                    addWordToGraphSpace(gspace, attributeVerb);
                    pre.forEach(p -> addWordToGraphSpace(gspace, p));
                    post.forEach(p -> addWordToGraphSpace(gspace, p));
                    adjective.forEach(a -> addWordToGraphSpace(gspace, a));

                    gspace.setLink(main, attributeVerb, 1);
                    gspace.setLink(attributeVerb, attribute, 1);

                    adjective.forEach(a -> gspace.setLink(finalAttribute, a, 1));
                    pre.forEach(p -> gspace.setLink(finalAttribute, p, 1));
                    post.forEach(p -> gspace.setLink(finalAttribute, p, 1));

                    adjective.clear();
                    pre.clear();
                    post.clear();
                    attribute = null;
                }
            }
        }

        final String finalAttribute = attribute;
        addWordToVocab(attributeVerb);
        pre.forEach(this::addWordToVocab);
        post.forEach(this::addWordToVocab);

        addWordToGraphSpace(gspace, attribute);
        addWordToGraphSpace(gspace, attributeVerb);
        pre.forEach(p -> addWordToGraphSpace(gspace, p));
        post.forEach(p -> addWordToGraphSpace(gspace, p));
        adjective.forEach(a -> addWordToGraphSpace(gspace, a));

        gspace.setLink(main, attributeVerb, 1);
        gspace.setLink(attributeVerb, attribute, 1);

        adjective.forEach(a -> gspace.setLink(finalAttribute, a, 1));
        pre.forEach(p -> gspace.setLink(finalAttribute, p, 1));
        post.forEach(p -> gspace.setLink(finalAttribute, p, 1));

        System.out.println(gspace.prettyString(vocab));

        return subcursor;
    }

    @Override
    public String toString() {
        return "SubjectWorld{" +
                "cursor=" + cursor +
                ", subjects=" + subjects +
                ", vocab=" + vocab +
                ", vspace=" + vspace +
                ", gspace=" + gspace +
                ", index=" + index +
                '}';
    }

    static class SubjectDescriptionWorld implements World {
        public int cursor = 0;
        private final @NotNull Subject subject;
        private final @NotNull VocabSpace vocab;
        public final @NotNull VectorSpace vspace;
        public final @NotNull GraphSpace gspace;
        public final @NotNull AdjectiveIndex index;
        public @NotNull List<List<String>> stackes = new ArrayList<>();

        public SubjectDescriptionWorld(@NotNull Subject subject, @NotNull VocabSpace vocab, @NotNull VectorSpace vspace, @NotNull GraphSpace gspace, @NotNull AdjectiveIndex index) {
            this.subject = subject;
            this.vocab = vocab;
            this.vspace = vspace;
            this.gspace = gspace;
            this.index = index;
            stackes.add(new ArrayList<>());
        }

        @Override
        public void process(@NotNull List<CoreLabel> labels, int cursor) {
            int subcursor = cursor;
            int sindex = labels.get(cursor).index();
            boolean negate = false;

            CoreLabel label;
            for (; subcursor < labels.size(); subcursor++) {
                label = labels.get(subcursor);

                if ((subcursor != cursor) && (label.index() <= sindex)) {
                    break;
                }

                if (Label.isAdverb(label)) {
                } else if (Label.isAdjective(label)) {

                }
            }

            this.cursor = subcursor;
        }
    }
}
