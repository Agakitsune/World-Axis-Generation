package org.example.wag.world;

import edu.stanford.nlp.ling.CoreLabel;
import org.example.wag.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NounWorld implements World {
    public int cursor = 0;
    private final @NotNull VocabSpace vocab;
    public final @NotNull VectorSpace vspace;
    public final @NotNull GraphSpace gspace;
    public final @NotNull AdjectiveIndex index;

    public NounWorld(@NotNull VocabSpace vocab, @NotNull VectorSpace vspace, @NotNull GraphSpace gspace, @NotNull AdjectiveIndex index) {
        this.vocab = vocab;
        this.vspace = vspace;
        this.gspace = gspace;
        this.index = index;
    }

    public boolean contain(@NotNull String word) {
        return vocab.contain(word);
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

    public void addWordToAll(@NotNull String word) {
        addWordToVocab(word);
        addWordToVectorSpace(word);
        addWordToGraphSpace(word);
    }

    public void bringCloser(@NotNull String word1, @NotNull String word2, float factor) {
        VectorSpace.Vector vec1 = VectorSpace.Vector.vector(vspace.get(word1));
        VectorSpace.Vector vec2 = VectorSpace.Vector.vector(vspace.get(word2));
        vec1.fadd(vec2, factor);
        vec2.fadd(vec1, factor);
        vspace.set(word1, vec1.unit().apply());
        vspace.set(word2, vec2.unit().apply());
    }

    public double[] getVector(@NotNull String word) {
        int index = vocab.getIndexOf(word);
        if (index != -1) {
            return vspace.get(index);
        }
        return null;
    }

    @Override
    public void process(@NotNull List<CoreLabel> labels, int cursor) {
        int subcursor = cursor;
        final int sindex = labels.get(cursor).index() + 1;
        boolean negate = false;
        boolean hasNoun = false;

        //System.out.println("NOUN: " + labels.get(cursor) + " " + sindex);

        final GramWorld world = new GramWorld(false);
        final List<Agglomerate> agglomerates = new ArrayList<>();
        CoreLabel label;
        String main;
        String last = null;

        world.process(labels, subcursor);

        main = world.gram.stream().map(CoreLabel::lemma).collect(Collectors.joining(" "));
        addWordToAll(main);

        subcursor += world.gram.size();

        if (subcursor >= labels.size()) {
            this.cursor = subcursor;
            return;
        }

        label = labels.get(subcursor);
        if (Label.isVerb(label)) {
            if (label.lemma().equalsIgnoreCase("be")) {
                subcursor++;
            }
        } else {
            this.cursor = subcursor;
            return;
        }

        // subprocess
        for (; subcursor < labels.size(); subcursor++) {
            label = labels.get(subcursor);

            //System.out.println("in loop: " + label + " " + label.index() + " " + sindex);
            if ((subcursor != cursor) && (label.index() <= sindex)) {
                //System.out.println("BREAK");
                break;
            }

            if (Label.isNoun(label)) {
                world.reset();
                world.process(labels, subcursor);

                final String gram = world.gram.stream().map(CoreLabel::lemma).collect(Collectors.joining(" "));
                addWordToAll(gram);
                last = gram;

                subcursor += world.gram.size() - 1;

                gspace.setLink(main, gram, negate ? -1 : 1);

                bringCloser(main, gram, negate ? -1 : 1);
                hasNoun = false; // reset
                boolean negateWord = agglomerates.stream().anyMatch(a -> !a.negate);
                agglomerates.forEach(a -> a.execute(this, gspace, gram, !negateWord, false));
                agglomerates.clear();
            } else if (Label.isAdjective(label)) {
                addWordToVocab(label.word());
                addWordToVocab(label.lemma());
                addWordToGraphSpace(label.word());
                addWordToGraphSpace(label.lemma());

                index.add(label);
                Agglomerate agglomerate = agglomerateProcess(labels, subcursor, sindex + 1, gspace.getIndex(main), negate);
                if (hasNoun) {
                    agglomerates.add(agglomerate);
                } else if (last != null) {
                    agglomerate.execute(this, gspace, last, false, true);
                } else {
                    agglomerate.execute(this, gspace);
                }
                subcursor = agglomerate.cursor;
            } else if (Label.isAdverb(label)) {
                negate = label.lemma().equalsIgnoreCase("not");
            } else if (Label.isConjunction(label)) {
                final String cc = label.lemma();
                if (cc.equalsIgnoreCase("but")) {
                    // TODO
                    //  introduce sub-block by reset the state
                    negate = false; // reset state
                } else if (cc.equalsIgnoreCase("either")) {
                    // TODO: no particular state change
                    //  there must be 'or' in the sub-block
                } else if (cc.equalsIgnoreCase("neither")) {
                    // TODO
                    //  introduce sub-block by negating it
                    //  there must be 'nor' in the sub-block
                    negate = true; // state is negated, similar to 'not'
                } else if (cc.equalsIgnoreCase("and")) {
                    // TODO: no particular state change
                    //  introduce sub-block
                } else if (cc.equalsIgnoreCase("or")) {
                    // TODO: no particular state change
                    //  introduce sub-block
                } else if (cc.equalsIgnoreCase("nor")) {
                    // TODO: no particular state change
                    //  introduce sub-block
                }
            } else if (Label.isDeterminer(label)) {
                // TODO: implement this
                //  there is a word in the sub-block
                hasNoun = true;
            } else if (Label.isPunctuation(label)) {
                if (label.lemma().equalsIgnoreCase(",")) {
                    // reset state
                    negate = false;
                }
            }
        }

        this.cursor = subcursor;
    }

    static abstract class Agglomerate {
        public final boolean negate;
        public final int negateIndex;
        public final int mainIndex;
        public final @NotNull List<String> accum;
        public final int cursor;

        Agglomerate(boolean negate, int negateIndex, int mainIndex, @NotNull List<String> accum, int cursor) {
            this.negate = negate;
            this.negateIndex = negateIndex;
            this.mainIndex = mainIndex;
            this.accum = accum;
            this.cursor = cursor;
        }

        public abstract void execute(@NotNull NounWorld world, @NotNull GraphSpace gspace);
        public abstract void execute(@NotNull NounWorld world, @NotNull GraphSpace gspace, @NotNull String gram, final boolean negateWord, final boolean postUpdate);
    }

    static class SimpleAgglomerate extends Agglomerate {
        public SimpleAgglomerate(boolean negate, int negateIndex, int mainIndex, @NotNull List<String> accum, int cursor) {
            super(negate, negateIndex, mainIndex, accum, cursor);
        }

        @Override
        public void execute(@NotNull NounWorld world, @NotNull GraphSpace gspace) {
            if (negate) {
                if (negateIndex == -1) {
                    // was negated before call
                    accum.forEach(s -> gspace.setLink(mainIndex, s, -1));
                } else {
                    // was negated in call
                    accum.stream().limit(negateIndex).forEach(s -> gspace.setLink(mainIndex, s, 1));
                    accum.stream().skip(negateIndex).forEach(s -> gspace.setLink(mainIndex, s, -1));
                }
            } else {
                // is not negated
                accum.forEach(s -> gspace.setLink(mainIndex, s, 1));
            }
        }

        @Override
        public void execute(@NotNull NounWorld world, @NotNull GraphSpace gspace, @NotNull String gram, final boolean negateWord, final boolean postUpdate) {
            if (negate) {
                if (negateIndex == -1) {
                    // was negated before call
                    accum.forEach(s -> gspace.setLink(mainIndex, s, -1));
                    accum.forEach(s -> gspace.setLink(gram, s, 1));
                    if (!postUpdate) gspace.setLink(gram, mainIndex, negateWord ? -1 : 1);
                } else {
                    // was negated in call
                    accum.stream().limit(negateIndex).forEach(s -> gspace.setLink(mainIndex, s, 1));
                    accum.stream().skip(negateIndex).forEach(s -> gspace.setLink(mainIndex, s, -1));
                    // TODO: found if this case if even possible
                }
            } else {
                // is not negated
                accum.forEach(s -> gspace.setLink(mainIndex, s, 1));
                accum.forEach(s -> gspace.setLink(gram, s, 1));
                gspace.setLink(gram, mainIndex, negateWord ? -1 : 1);
            }
        }

        @Override
        public String toString() {
            return "SimpleAgglomerate{" +
                    "negate=" + negate +
                    ", negateIndex=" + negateIndex +
                    ", mainIndex=" + mainIndex +
                    ", accum=" + accum +
                    ", cursor=" + cursor +
                    '}';
        }
    }

    static class ComplexAgglomerate extends Agglomerate {
        public ComplexAgglomerate(boolean negate, int negateIndex, int mainIndex, @NotNull List<String> accum, int cursor) {
            super(negate, negateIndex, mainIndex, accum, cursor);
        }

        @Override
        public void execute(@NotNull NounWorld world, @NotNull GraphSpace gspace) {
            final String agglomerate;
            if (negate) {
                if (negateIndex == -1) {
                    // was negated before call
                    agglomerate = String.join(" and ", accum);
                    world.addWordToGraphSpace(agglomerate);
                    world.addWordToVocab(agglomerate);
                    gspace.setLink(mainIndex, agglomerate, -1);
                } else {
                    // was negated in call
                    agglomerate = accum.stream().skip(negateIndex).collect(Collectors.joining(" and "));
                    world.addWordToGraphSpace(agglomerate);
                    world.addWordToVocab(agglomerate);
                    gspace.setLink(mainIndex, agglomerate, -1);
                    accum.stream().limit(negateIndex).forEach(s -> gspace.setLink(mainIndex, s, 1));
                }
            } else {
                // is not negated
                agglomerate = String.join(" and ", accum);
                world.addWordToGraphSpace(agglomerate);
                world.addWordToVocab(agglomerate);
                gspace.setLink(mainIndex, agglomerate, 1);
            }
        }

        @Override
        public void execute(@NotNull NounWorld world, @NotNull GraphSpace gspace, @NotNull String gram, final boolean negateWord, final boolean postUpdate) {
            final String agglomerate;
            if (negate) {
                if (negateIndex == -1) {
                    // was negated before call
                    agglomerate = String.join(" and ", accum);
                    world.addWordToGraphSpace(agglomerate);
                    world.addWordToVocab(agglomerate);
                    gspace.setLink(mainIndex, agglomerate, -1);

                    gspace.setLink(gram, agglomerate, 1);
                    if (!postUpdate) gspace.setLink(gram, mainIndex, -1);
                } else {
                    // was negated in call
                    agglomerate = accum.stream().skip(negateIndex).collect(Collectors.joining(" and "));
                    world.addWordToGraphSpace(agglomerate);
                    world.addWordToVocab(agglomerate);
                    gspace.setLink(mainIndex, agglomerate, -1);
                    accum.stream().limit(negateIndex).forEach(s -> gspace.setLink(mainIndex, s, 1));

                    accum.stream().limit(negateIndex).forEach(s -> gspace.setLink(gram, s, 1));
                    gspace.setLink(gram, agglomerate, -1);
                    gspace.setLink(gram, mainIndex, 1);
                }
            } else {
                // is not negated
                agglomerate = String.join(" and ", accum);
                world.addWordToGraphSpace(agglomerate);
                world.addWordToVocab(agglomerate);
                gspace.setLink(mainIndex, agglomerate, 1);

                gspace.setLink(gram, agglomerate, 1);
                gspace.setLink(gram, mainIndex, 1);
            }
        }
    }

    // Consequences of multiple adjective of the same category
    // eg. apple is red or green
    // 'red' and 'green' are both adjectives of the same category 'color'
    private Agglomerate agglomerateProcess(@NotNull List<CoreLabel> labels, final int cursor, final int sindex, final int mainIndex, boolean negate) {
        boolean adjective = true;
        boolean set = false;
        boolean isIntricate = false;
        int negateIndex = -1;
        int subcursor = cursor;

        final @NotNull List<String> accum = new ArrayList<>();
        CoreLabel label;

        for (; subcursor < labels.size(); subcursor++) {
            label = labels.get(subcursor);

            if ((subcursor != cursor) && (label.index() <= sindex)) {
                break;
            }

            if (Label.isAdjective(label)) {
                if (!adjective) { // expected a conjunction
                    break;
                }
                adjective = false;

                accum.add(label.word());

                addWordToVocab(label.word());
                addWordToVocab(label.lemma());
                addWordToGraphSpace(label.word());
                addWordToGraphSpace(label.lemma());

                index.add(label);
            } else if (Label.isConjunction(label)) {
                if (adjective) { // expected an adjective
                    break;
                }
                final String cc = label.lemma();
                if (cc.equalsIgnoreCase("and")) {
                    isIntricate = true;
                } else if (cc.equalsIgnoreCase("or")) {
                    isIntricate = false;
                } else {
                    break;
                }
                set = true;
                adjective = true;
            } else if (Label.isPunctuation(label)) {
                if (label.lemma().equalsIgnoreCase(",")) {
                    if (set) {
                        break;
                    }
                    adjective = true; // reset state
                }
            } else if (Label.isAdverb(label)) {
                if (set) {
                    break;
                }
                if (!label.lemma().equalsIgnoreCase("not")) {
                    break;
                }
                negate = true;
                negateIndex = accum.size();
            } else {
                break;
            }

        }

        if (isIntricate) {
            return new ComplexAgglomerate(negate, negateIndex, mainIndex, accum, subcursor - 1);
        }
        return new SimpleAgglomerate(negate, negateIndex, mainIndex, accum, subcursor - 1);
    }

    @Override
    public String toString() {
        return "NounWorld{" +
                "cursor=" + cursor +
                ", vocab=" + vocab +
                ", vspace=" + vspace +
                ", gspace=" + gspace +
                ", index=" + index +
                '}';
    }

    static class SubNounWorld implements World {
        public int cursor = 0;
        public final @NotNull VocabSpace vocab;
        private final @NotNull GramWorld world;
        private final @NotNull AdjectiveIndex index;
        public @NotNull List<List<String>> stackes = new ArrayList<>();
        public String gram = null;

        public boolean @NotNull [] negates;

        private SubNounWorld(boolean @NotNull [] negates, @NotNull VocabSpace vocab,  @NotNull GramWorld world, @NotNull AdjectiveIndex index) {
            this.vocab = vocab;
            this.world = world;
            this.index = index;
            stackes.add(new ArrayList<>());
            this.negates = negates;
        }

        public SubNounWorld(boolean @NotNull [] negates, @NotNull GramWorld world, @NotNull AdjectiveIndex index) {
            this.vocab = new VocabSpace();
            this.world = world;
            this.index = index;
            stackes.add(new ArrayList<>());
            this.negates = negates;
        }

        @Override
        public void process(@NotNull List<CoreLabel> labels, int cursor) {
            int subcursor = cursor;
            int sindex = labels.get(cursor).index();
            boolean negate = false;
            int subtract = vocab.size();

            SubNounWorld subworld;
            String lgram = null;
            CoreLabel label;

            for (; subcursor < labels.size(); subcursor++) {
                label = labels.get(subcursor);

                if ((subcursor != cursor) && (label.index() <= sindex)) {
                    break;
                }

                if (Label.isNoun(label)) {
                    world.reset();
                    world.process(labels, subcursor);

                    lgram = world.gram.stream().map(CoreLabel::lemma).collect(Collectors.joining(" "));
                    if (this.gram == null) {
                        this.gram = lgram;
                    }

                    final int gramIndex = vocab.getIndexOf(lgram);
                    if (gramIndex == -1) {
                        vocab.addWord(lgram);

                        negates = Arrays.copyOf(negates, negates.length + 1);
                        negates[negates.length - 1] = negate;
                    } else {
                        negates[gramIndex] = negate;
                    }
                    stackes.getLast().add(lgram);
                    subcursor += world.gram.size() - 1;
                } else if (Label.isAdverb(label)) {
                    negate = label.lemma().equalsIgnoreCase("not");
                } else if (Label.isAdjective(label)) {
                    index.add(label);
                    final int labelIndex = vocab.getIndexOf(label.word());
                    if (labelIndex == -1) {
                        vocab.addWord(label.word());

                        negates = Arrays.copyOf(negates, negates.length + 1);
                        negates[negates.length - 1] = negate;
                    } else {
                        negates[labelIndex - subtract] = negate;
                    }
                    stackes.getLast().add(label.word());
                } else if (Label.isConjunction(label)) {
                    if (label.lemma().equalsIgnoreCase("but")) {
                        // subworld because another statement is being made, could use a stack of int to store the vocab size to avoid stack overflow
                        subworld = new SubNounWorld(negates, vocab, world, index);
                        subworld.process(labels, subcursor + 1);

                        subcursor = subworld.cursor - 1;
                        negates = subworld.negates;

                        if (gram == null) {
                            // gram must be in the subworld then
                            negates[subworld.vocab.getIndexOf(subworld.gram)] = negate;
                            stackes.getLast().add(subworld.gram);
                        }

                        stackes.addAll(subworld.stackes);
                    } else if (label.lemma().equalsIgnoreCase("and")) {
                        // reset state
                        negate = false;
                        stackes.add(new ArrayList<>());
                    } else if (label.lemma().equalsIgnoreCase("nor")) {
                        negate = true;
                        for (int i = subtract; i < negates.length; i++) {
                            negates[i] = true;
                        }
                        stackes.add(new ArrayList<>());
                    } else if (label.lemma().equalsIgnoreCase("neither")) {
                        negate = true;
                        //stackes.add(new ArrayList<>());
                    }
                } else if (Label.isPrepositionOrSubordinatingConjunction(label)) {
                    if (label.lemma().equalsIgnoreCase("with")) {
                        break; // delegate work to upper world
                    }
                }
            }
            this.cursor = subcursor;
        }

        @Override
        public String toString() {
            return "SubNounWorld{" +
                    "cursor=" + cursor +
                    ", vocab=" + vocab +
                    ", world=" + world +
                    ", negates=" + Arrays.toString(negates) +
                    ", gram='" + gram + '\'' +
                    ", stackes=" + stackes +
                    '}';
        }
    }
}
