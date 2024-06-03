package org.example.wag.world;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.example.wag.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class GlobalWorld implements World {
    private final @NotNull List<Subject> subjects = new ArrayList<>();
    private final @NotNull List<Relationship> relationships = new ArrayList<>();
    private final @NotNull List<Group> groups = new ArrayList<>();
    private final @NotNull VocabSpace vocab = new VocabSpace();
    private final @NotNull VectorSpace vspace = new VectorSpace(0);
    private final @NotNull GraphSpace gspace = new GraphSpace();
    private final @NotNull AdjectiveIndex index = new AdjectiveIndex();

    private void init(@NotNull List<CoreLabel> labels) {
        AtomicInteger d = new AtomicInteger();
        vocab.resize((int) labels.stream().filter(Label::isCommonNoun).count());
        labels.stream().filter(Label::isCommonNoun).forEach(label -> {
            String name = label.lemma();
            if (!contain(name)) {
                d.addAndGet(1);
                vocab.addWord(name);
                //hashes[d.get() - 1] = name.hashCode();
                this.vspace.set(name, VectorSpace.Vector.zeros(d.get()).set(d.get() - 1, 1).apply());
                this.gspace.emplace(name);
            }
        });

        this.vspace.resize(d.get()).equalize();
    }

    public boolean contain(@NotNull String word) {
        return vocab.contain(word);
    }

    public double[] getVector(@NotNull String word) {
        int index = vocab.getIndexOf(word);
        if (index != -1) {
            return vspace.get(index);
        }
        return null;
    }

    public int[] getGraph(@NotNull String word) {
        int index = vocab.getIndexOf(word);
        if (index != -1) {
            //return gspace.getWeights(index);
        }
        return null;
    }

    public void addWord(@NotNull String word) {
        if (!vocab.contain(word)) {
            vocab.addWord(word);
            int d = this.vocab.size();
            vspace.resize(d);
            vspace.equalize();

            //vspace.add(VectorSpace.Vector.zeros(d).set(d - 1, 1).apply());
            //gspace.emplace();
        }
    }

    @Override
    public void process(@NotNull List<CoreLabel> labels) {
        for (int cursor = 0; cursor < labels.size(); cursor++) {
            CoreLabel label = labels.get(cursor);
            if (Label.isProperNoun(label)) {
                SubjectWorld w = new SubjectWorld(subjects, vocab, vspace, gspace, index);
                w.process(labels, cursor);
                cursor = w.cursor;
            } else if (Label.isCommonNoun(label)) {
                NounWorld w = new NounWorld(vocab, vspace, gspace, index);
                w.process(labels, cursor);
                cursor = w.cursor;
            }
        }
    }

    @Override
    public String toString() {
        return "GlobalWorld{" +
                "vocab=" + vocab +
                ", subjects=" + subjects +
                ", relationships=" + relationships +
                ", vspace=" + vspace +
                ", gspace=" + gspace +
                ", index=" + index +
                '}';
    }

    public String displayVectorSpace() {
        return vspace.prettyString(vocab);
    }

    public String displayGraphSpace() {
        return gspace.prettyString(vocab);
    }

    public String displaySubjects() {
        return subjects.stream().map(s -> s.prettyString(vocab)).collect(Collectors.joining("\n"));
    }

    public static GlobalWorld generate(String path) throws IOException {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,pos,lemma");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        List<List<CoreLabel>> labels = Files.lines(Path.of(path))
                .map(pipeline::processToCoreDocument)
                .map(CoreDocument::tokens)
                .toList();

        final GlobalWorld w = new GlobalWorld();
        w.init(labels.stream().reduce(new ArrayList<>(), (a, b) -> {
            a.addAll(b);
            return a;
        }));
        labels.forEach(w::process);
        return w;
    }
}
