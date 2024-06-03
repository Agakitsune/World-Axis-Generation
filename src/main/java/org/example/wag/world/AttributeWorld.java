package org.example.wag.world;

import edu.stanford.nlp.ling.CoreLabel;
import org.example.wag.Attribute;
import org.example.wag.Label;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AttributeWorld implements World {
    private final @NotNull GramWorld world;
    public final @NotNull List<Attribute> attributes = new ArrayList<>();
    public int cursor = 0;

    public AttributeWorld(@NotNull GramWorld world) {
        this.world = world;
    }

    @Override
    public void process(@NotNull List<CoreLabel> labels, int cursor) {
        int subcursor = cursor;
        int sindex = labels.get(cursor).index();
        boolean resetAttribute = false;
        boolean negate = false;

        String attribute = null;
        String gram;
        List<String> amplifiers = new ArrayList<>();
        List<String> description = new ArrayList<>();
        CoreLabel label;

        for (; subcursor < labels.size(); subcursor++) {
            label = labels.get(subcursor);

            System.out.println(label + " " + label.tag());
            if ((subcursor != cursor) && (label.index() <= sindex)) {
                break;
            }

            if (Label.isNoun(label)) {
                world.reset();
                world.process(labels, subcursor);

                gram = world.gram.stream().map(CoreLabel::lemma).collect(Collectors.joining(" "));
                if (attribute == null || resetAttribute) {
                    attribute = gram;
                    resetAttribute = false;
                } else {
                    description.add(gram);
                }
                subcursor += world.gram.size() - 1;
            } else if (Label.isPrepositionOrSubordinatingConjunction(label)) {
                String prep = label.lemma();
                if (prep.equalsIgnoreCase("of")) {
                    resetAttribute = true;
                    amplifiers.add(attribute);
                    // next noun is the attribute, all nouns before that is there to describe the attribute
                    // eg. a lot of experience
                    // 'lot' will be the first attribute detected, but since there is 'of' just after, it will be downgraded in favor of 'experience'
                } else if (prep.equalsIgnoreCase("with")) {
                    // this preposition doesn't introduce another attribute, it is similar to 'in' in this case
                    // eg. a lof of experience with video games
                    // in this case 'video games' still bring information about the 'experience' attribute, just like 'in'
                } else if (prep.equalsIgnoreCase("in")) {
                    // bring information on the attribute
                    // eg. a lof of experience in video games
                    // 'video games' bring information about the 'experience' attribute
                }
            } else if (Label.isConjunction(label)) {
                String cc = label.lemma();
                if (cc.equalsIgnoreCase("and")) {
                    if (attribute == null) {
                        // TODO: find what to do here, with a syntax correct input, this should never happen
                        break;
                    }
                    attributes.add(new Attribute(attribute).addAmplifier(amplifiers).addDescription(description).setNegative(negate));
                    amplifiers.clear();
                    description.clear();
                    negate = false;
                    attribute = null;
                    // new attribute will be introduced, push actual attribute to keep track
                    // eg. a lof of experience and knowledge
                    // 'experience' is an attribute, 'knowledge' is another attribute
                    // TODO: detect when the attribute is "related" to the previous one
                    //  eg. a lof of experience and knowledge
                    //  'lot' affects both 'experience' and 'knowledge'
                    //  eg. a lot of experience and a physique
                    //  must detect if there is a determinant or something else before the second attribute
                    // TODO: store "related" attribute to graph correctly
                    //  eg. a lof of experience and knowledge in video games
                    //  'video games' bring information to 'experience' and 'knowledge', so they must be connected in the graph space
                }
            } else if (Label.isAdverb(label)) {
                negate = label.lemma().equalsIgnoreCase("not");
            } else if (Label.isAdjective(label)) {
                // TODO: take adjective into account
            }
        }
        attributes.add(new Attribute(attribute).addAmplifier(amplifiers).addDescription(description).setNegative(negate));

        this.cursor = subcursor;
    }

    @Override
    public String toString() {
        return "AttributeWorld{" +
                "world=" + world +
                ", attributes=" + attributes +
                ", cursor=" + cursor +
                '}';
    }
}
