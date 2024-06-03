package org.example.wag;

import edu.stanford.nlp.ling.CoreLabel;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Label {
    public static boolean isNoun(@NotNull CoreLabel label) {
        return label.tag().startsWith("NN");
    }
    public static boolean isCommonNoun(@NotNull CoreLabel label) {
        return label.tag().startsWith("NN") && !label.tag().startsWith("NNP");
    }
    public static boolean isProperNoun(@NotNull CoreLabel label) {
        return label.tag().startsWith("NNP");
    }
    public static boolean isVerb(@NotNull CoreLabel label) {
        return label.tag().startsWith("VB");
    }
    public static boolean isAdjective(@NotNull CoreLabel label) {
        return label.tag().startsWith("JJ");
    }
    public static boolean isAdverb(@NotNull CoreLabel label) {
        return label.tag().startsWith("RB");
    }
    public static boolean isPronoun(@NotNull CoreLabel label) {
        return label.tag().startsWith("PRP");
    }
    public static boolean isPossessivePronoun(@NotNull CoreLabel label) {
        return label.tag().equals("PRP$");
    }
    public static boolean isPrepositionOrSubordinatingConjunction(@NotNull CoreLabel label) {
        return label.tag().startsWith("IN");
    }
    public static boolean isDeterminer(@NotNull CoreLabel label) {
        return label.tag().equals("DT");
    }
    public static boolean isConjunction(@NotNull CoreLabel label) {
        return label.tag().equals("CC");
    }
    public static boolean isModalAuxiliary(@NotNull CoreLabel label) {
        return label.tag().equals("MD");
    }
    public static boolean isTo(@NotNull CoreLabel label) {
        return label.tag().equals("TO");
    }
    public static boolean isPunctuation(@NotNull CoreLabel label) {
        return label.tag().equals(".");
    }
    public static boolean isWhPronoun(@NotNull CoreLabel label) {
        return label.tag().equals("WP") || label.tag().equals("WP$");
    }
    public static boolean isModal(@NotNull CoreLabel label) {
        return label.tag().equals("MD");
    }

    public static boolean isNobiliaryParticle(@NotNull CoreLabel label) {
        final byte[][] particle = {
                "of".getBytes(),
                "von".getBytes(),
                "de".getBytes(),
                "la".getBytes()
        };

        if (!isProperNoun(label)) {
            return false;
        }
        return Arrays.stream(particle).anyMatch(p -> Arrays.equals(label.word().toLowerCase().getBytes(), p));
    }

    public static boolean isRelationship(@NotNull CoreLabel label) {
        final byte[][] list = {
            "friendship".getBytes(),
            "friend".getBytes(),
            "partnership".getBytes(),
            "partner".getBytes(),
            "connection".getBytes(),
            "bond".getBytes(),
            "alliance".getBytes(),
            "ally".getBytes(),
            "affiliation".getBytes(),
            "associate".getBytes(),
            "association".getBytes(),
            "collaboration".getBytes(),
            "collaborator".getBytes(),
            "union".getBytes(),
            "interaction".getBytes(),
            "link".getBytes(),
            "engagement".getBytes(),
            "acquaintance".getBytes(),
            "companionship".getBytes(),
            "companion".getBytes(),
            "kinship".getBytes(),
            "kin".getBytes(),
            "amity".getBytes(),
            "commitment".getBytes(),
            "camaraderie".getBytes(),
            "colleague".getBytes(),
            "coalition".getBytes(),
            "symbiosis".getBytes(),
            "symbiote".getBytes(),
            "mentorship".getBytes(),
            "mentor".getBytes(),
            "mentee".getBytes(),
            "team".getBytes(),
            "teammate".getBytes(),
            "sibling".getBytes(),
            "marriage".getBytes(),
            "spouse".getBytes(),
            "mate".getBytes(),
            "peer".getBytes(),
            "confidant".getBytes(),
            "confidante".getBytes(),
            "neighbor".getBytes(),
            "roommate".getBytes(),
            "housemate".getBytes(),
            "classmate".getBytes(),
            "schoolmate".getBytes(),
            "playmate".getBytes(),
            "penpal".getBytes(),
            "correspondent".getBytes(),
            "adversary".getBytes(),
            "rival".getBytes(),
            "customer".getBytes(),
            "client".getBytes(),
            "patron".getBytes(),
            "benefactor".getBytes(),
            "recipient".getBytes(),
            "follower".getBytes(),
            "subscriber".getBytes(),
            "supporter".getBytes(),
            "advocate".getBytes(),
            "protector".getBytes(),
            "guardian".getBytes(),
            "parent".getBytes(),
            "child".getBytes(),
            "sibling".getBytes(),
            "cohort".getBytes(),
            "partner-in-crime".getBytes(),
            "soulmate".getBytes(),
            "confederate".getBytes(),
            "network".getBytes()
        };

        return Arrays.stream(list).anyMatch(r -> label.word().equalsIgnoreCase(new String(r)));
    }
}
