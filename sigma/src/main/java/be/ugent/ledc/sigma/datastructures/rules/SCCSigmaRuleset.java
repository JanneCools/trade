package be.ugent.ledc.sigma.datastructures.rules;

import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import be.ugent.ledc.sigma.sscgeneration.SCCGenerator;

public class SCCSigmaRuleset extends SigmaRuleset {

    private SCCSigmaRuleset(Map<String, SigmaContractor<?>> contractors, Set<SigmaRule> sigmaRules) {
        super(contractors, sigmaRules);
    }

    public static SCCSigmaRuleset create(SigmaRuleset ruleset, SCCGenerator<SigmaRule, SigmaRuleset> generator) {
        return new SCCSigmaRuleset(
                ruleset.getContractors(),
                generator.generateSCCSet(ruleset)
        );
    }

    public static SCCSigmaRuleset create(SigmaRuleset ruleset) {
        return new SCCSigmaRuleset(
                ruleset.getContractors(),
                ruleset.getRules());
    }

    @Override
    public SCCSigmaRuleset project(Set<String> attributes) {
        return new SCCSigmaRuleset(
                getContractors() //Project the contractors
                        .entrySet()
                        .stream()
                        .filter(e -> attributes.contains(e.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                getRules() //Project the rules
                        .stream()
                        .filter(rule -> attributes.containsAll(rule.getInvolvedAttributes()))
                        .collect(Collectors.toSet()));
    }
    
    public SCCSigmaRuleset merge(SCCSigmaRuleset other) {

        Map<String, SigmaContractor<?>> contractors =
                Stream.of(this.getContractors(), other.getContractors())
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));

        Set<SigmaRule> sigmaRules = new HashSet<>(this.getRules());
        sigmaRules.addAll(other.getRules());

        return new SCCSigmaRuleset(contractors, sigmaRules);

    }
}
