package be.ugent.ledc.sigma.sscgeneration.implication;

import be.ugent.ledc.core.util.SetOperations;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.atoms.AtomOperations;
import be.ugent.ledc.sigma.datastructures.atoms.SetAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetNominalAtom;
import be.ugent.ledc.sigma.datastructures.atoms.SetOrdinalAtom;
import be.ugent.ledc.sigma.datastructures.contracts.NominalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractorFactory;
import be.ugent.ledc.sigma.datastructures.operators.EqualityOperator;
import be.ugent.ledc.sigma.datastructures.operators.SetOperator;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleException;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRulesetOperations;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A RuleImplicator that is an optimized implication algorithm in which all attributes
 * are string-valued.
 * 
 * In many cases, the ConstantImplicator is the better choice, but experiments
 * showed a few cases in which this algorithm performed slightly better than
 * the generic sort-based algorithm used in ConstantImplicator.
 * @author abronsel
 */
public class SimpleImplicator extends SigmaRuleImplicator<String>
{
    @Override
    public Set<SigmaRule> generate(String generator, SigmaContractor<String> generatorContractor, Set<SigmaRule> contributors)
    {
        Set<SigmaRule> univariateRules = new HashSet<>();
        Set<SigmaRule> multivariateRules = new HashSet<>();
        
        for(SigmaRule rule: contributors)
        {
            if(rule.getInvolvedAttributes().size() == 1)
            {
                if(!rule.involves(generator))
                {
                    throw new SigmaRuleException("Unexpected univariate rule: "
                        + rule
                        + "\nCause: generator not involved.");
                }
                univariateRules.add(rule);
            }
            else
                multivariateRules.add(rule);   
        }
        
        //Use the univariate rules to form a domain
        Set<String> generatorDomainValues = SigmaRulesetOperations.convertUnivariateRulesToSet(univariateRules, generator);
           
        Set<SigmaRule> newRules = new HashSet<>();

        // If not all generator values appear in the set of contributors, return an empty set
        if(!generatorDomainValues
            .stream()
            .allMatch(value -> multivariateRules
                .stream()
                .anyMatch(cc -> buildValues(cc, generator).contains(value)))
        )
        {
            return newRules;
        }
        
        Set<SigmaRule> toRemove = new HashSet<>();
        
        for(SigmaRule contributor: multivariateRules)
        {
            if(buildValues(contributor, generator).equals(generatorDomainValues))
            {
                newRules.add(new SigmaRule(
                    contributor
                        .getAtoms()
                        .stream()
                        .filter(atom -> !atom.involves(generator))
                        .collect(Collectors.toSet())
                ));
                
                toRemove.add(contributor);
            }
        }
        multivariateRules.removeAll(toRemove);
        
        Set<List<Integer>> combinationsToSkip = new HashSet<>();

        // Sort generator values according to number of appearances in candidatecontributors
        Map<Object, Long> mappedGeneratorValuesOnCount = multivariateRules
            .stream()
            .map(cc -> buildValues(cc, generator))
            .flatMap(Set::stream)
            .toList()
            .stream()
            .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        
        List<Object> sortedGeneratorValues = mappedGeneratorValuesOnCount
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .toList();

        List<SigmaRule> candidateContributorsList = new ArrayList<>(multivariateRules);

        // Get the indices of the edit rules in contributorList containing
        //the first generator value
        Set<List<Integer>> currentLevelNodes = multivariateRules
            .stream()
            .filter(cc -> buildValues(cc, generator).contains(sortedGeneratorValues.get(0)))
            .map(cc -> Collections.singletonList(candidateContributorsList.indexOf(cc)))
            .collect(Collectors.toSet());
        
        Map<List<Integer>, ImpliedRule> mappedCurrentLevelNodes = currentLevelNodes
            .stream()
            .collect(Collectors.toMap(
                cln -> cln,
                cln -> new ImpliedRule(
                        candidateContributorsList.get(cln.get(0)),
                        ImpliedRule.IMPLIED)));

        for (int i = 1; i < sortedGeneratorValues.size(); i++)
        {

            Object generatorValue = sortedGeneratorValues.get(i);
            
            mappedCurrentLevelNodes = constructCurrentLevelNodes(
                mappedCurrentLevelNodes,
                combinationsToSkip,
                candidateContributorsList,
                generator,
                generatorDomainValues,
                generatorValue);

            Map<List<Integer>, ImpliedRule> mappedNewRules = mappedCurrentLevelNodes
                .entrySet()
                .stream()
                .filter(r -> r.getValue().isNew())
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue)
                );
            
            Map<List<Integer>, ImpliedRule> mappedTautologies = mappedCurrentLevelNodes
                .entrySet()
                .stream()
                .filter(r -> r.getValue().isTautology())
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue)
                );

            combinationsToSkip.addAll(mappedNewRules.keySet());
            combinationsToSkip.addAll(mappedTautologies.keySet());

            newRules.addAll(mappedNewRules
                .values()
                .stream()
                .map(ImpliedRule::getRule)
                .toList());

            mappedCurrentLevelNodes = mappedCurrentLevelNodes
                .entrySet()
                .stream()
                .filter(e -> e.getValue().isImplied())
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue)
                );
        }

        return newRules;
    }

    protected Set<String> buildValues(SigmaRule rule, String attribute)
    {
        return AtomOperations.getValuesetFromNominalAtoms(
            rule
                .getAtoms()
                .stream()
                .filter(atom -> atom.involves(attribute))
                .collect(Collectors.toSet()),
            attribute
        );
    }
    
    protected SigmaRule imply(SigmaRule first, SigmaRule second, Set<?> generatorDomain, String generator)
    {
        // Empty Map of implied attribute conditions
        Set<AbstractAtom<?,?,?>> impliedAtoms = new HashSet<>();

        Set<String> attributes = new HashSet<>();
        attributes.addAll(first.getInvolvedAttributes());
        attributes.addAll(second.getInvolvedAttributes());

        // Iterate over all involving attributes
        for (String attribute : attributes) {

            // If the attribute equals the generator
            if (attribute.equals(generator))
            {
                // If one of the rules does not involve the attribute, the implied rule does not involve this attribute
                // Otherwise, take the union of the attribute conditions of both rules for the current attribute
                if (first.involves(attribute) && second.involves(attribute))
                {
                    Set<?> values = SetOperations.union(
                        buildValues(first, attribute),
                        buildValues(second, attribute));

                    if (!values.equals(generatorDomain))
                        impliedAtoms.add(
                            buildAtom(
                                attribute,
                                values,
                                first.getContractor(attribute)));
                }
            }
            else
            {
                // Take the intersection of the attribute conditions of both rules for the current attribute
                if (first.involves(attribute) && !second.involves(attribute))
                {
                    impliedAtoms.add(
                        buildAtom(
                            attribute,
                            buildValues(first, attribute),
                            first.getContractor(attribute)
                        )
                    );
                }
                else if (!first.involves(attribute) && second.involves(attribute))
                {
                    impliedAtoms.add(
                        buildAtom(
                            attribute,
                            buildValues(second, attribute),
                            second.getContractor(attribute)
                        )
                    );
                }
                else if (first.involves(attribute) && second.involves(attribute))
                {
                    impliedAtoms.add(buildAtom(attribute,
                            SetOperations.intersect(
                                buildValues(first, attribute),
                                buildValues(second,attribute)
                            ),
                            first.getContractor(attribute)
                        )
                    );
                }
            }
        }
        
        return  new SigmaRule(impliedAtoms);
    }
    
    private ImpliedRule implyWithType(SigmaRule first, SigmaRule second, Set<?> generatorDomain, String generator)
    {
        SigmaRule iRule = imply(first, second, generatorDomain, generator);
        return new ImpliedRule(iRule, getType(iRule, generator));
    }
    
    private Map<List<Integer>, ImpliedRule> constructCurrentLevelNodes(Map<List<Integer>, ImpliedRule> mappedPreviousLevelNodes, Set<List<Integer>> combinationsToSkip, List<SigmaRule> candidateContributorsList, String generator, Set<?> generatorDomain, Object generatorValue)
    {
        Map<List<Integer>, ImpliedRule> mappedCurrentLevelNodes = new HashMap<>();

        // Get the indices of the edit rules in candidateContributorList containing the generator value
        Set<Integer> currentLevelNodes = candidateContributorsList.stream()
            .filter(cc -> buildValues(cc, generator).contains(generatorValue))
            .map(candidateContributorsList::indexOf).collect(Collectors.toSet());

        // Add possible combinations to previous level nodes
        for (List<Integer> previousLevelNode : mappedPreviousLevelNodes.keySet())
        {
            SigmaRule previousLevelNodeRule = mappedPreviousLevelNodes
                .get(previousLevelNode)
                .getRule();

            // If previousLevelNodeRule already contains current generator value, add it to the current level node rules
            if (buildValues(previousLevelNodeRule, generator).contains(generatorValue))
            {
                mappedCurrentLevelNodes.put(
                    previousLevelNode,
                    mappedPreviousLevelNodes.get(previousLevelNode)
                );
            }

            for (int currentLevelNode : currentLevelNodes)
            {
                // Skip combination if current level node already exists in previous level node
                if (previousLevelNode.contains(currentLevelNode))
                    continue;

                SigmaRule currentLevelNodeRule = candidateContributorsList.get(currentLevelNode);

                List<Integer> currentLevelNodeIndices = new ArrayList<>(previousLevelNode);
                currentLevelNodeIndices.add(currentLevelNode);

                // Skip comination if current level node contains no new generator value compared to previous level node or vice versa
                if (buildValues(previousLevelNodeRule, generator).containsAll(buildValues(currentLevelNodeRule, generator))
                || buildValues(currentLevelNodeRule, generator).containsAll(buildValues(previousLevelNodeRule, generator)))
                {
                    combinationsToSkip.add(currentLevelNodeIndices);
                    continue;
                }

                if (combinationsToSkip.stream().noneMatch(currentLevelNodeIndices::containsAll)) {

                    // Imply rule
                    ImpliedRule impliedEditRule = implyWithType(
                        previousLevelNodeRule,
                        currentLevelNodeRule,
                        generatorDomain,
                        generator);
                    mappedCurrentLevelNodes.put(currentLevelNodeIndices, impliedEditRule);
                }

            }

        }

        return mappedCurrentLevelNodes;

    }   

    private SetAtom buildAtom(String attribute, Set<?> values, SigmaContractor c)
    {
        if(c instanceof NominalContractor)
            return new SetNominalAtom(
                (NominalContractor) c,
                attribute,
                SetOperator.IN,
                values);
        else if(c instanceof OrdinalContractor)
            return new SetOrdinalAtom(
                (OrdinalContractor) c,
                attribute,
                SetOperator.IN,
                values);
        else throw new SigmaRuleException("Unknown contractor type in simple implication.");
    }

    private String getType(SigmaRule impliedRule, String generator)
    {
        if (impliedRule
            .getInvolvedAttributes()
            .stream()
            .anyMatch(a -> buildValues(impliedRule, a).isEmpty()))
        {
            return ImpliedRule.TAUTOLOGY;
        }
        else if (!impliedRule.getInvolvedAttributes().contains(generator)) {
            return ImpliedRule.NEW;
        }
        else
        {
            return ImpliedRule.IMPLIED;
        }
    }

    @Override
    public boolean isApplicable(String generator, SigmaContractor<?> generatorContractor, Set<SigmaRule> contributors)
    {
        return
            contributors
                .stream()
                .anyMatch(rule ->
                    rule.involves(generator)
                &&  rule.getInvolvedAttributes().size() == 1
                &&  rule
                        .getAtoms()
                        .stream()
                        .anyMatch(atom ->
                            atom.getOperator().equals(SetOperator.NOTIN) ||
                            atom.getOperator().equals(EqualityOperator.NEQ))
                )
        &&
            contributors
            .stream()
            .allMatch(rule -> rule.getVariableAtoms().isEmpty() &&
                rule
                .getInvolvedAttributes()
                .stream()
                .allMatch(a -> rule
                    .getContractor(a)
                    .equals(SigmaContractorFactory.STRING)));
    }

    private static class ImpliedRule
    {
        private static final String IMPLIED = "implied";
        private static final String NEW = "new";
        private static final String TAUTOLOGY = "tautology";
        
        private final SigmaRule rule;
        private final String type;

        public ImpliedRule(SigmaRule rule, String type)
        {
            this.rule = rule;
            this.type = type;
        }

        public SigmaRule getRule()
        {
            return rule;
        }

        public String getType()
        {
            return type;
        }
        
        public boolean isNew()
        {
            return type.equals(NEW);
        }
        
        public boolean isTautology()
        {
            return type.equals(TAUTOLOGY);
        }
        
        public boolean isImplied()
        {
            return type.equals(IMPLIED);
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 73 * hash + Objects.hashCode(this.rule);
            hash = 73 * hash + Objects.hashCode(this.type);
            return hash;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            final ImpliedRule other = (ImpliedRule) obj;
            if (!Objects.equals(this.type, other.type))
            {
                return false;
            }
            if (!Objects.equals(this.rule, other.rule))
            {
                return false;
            }
            return true;
        }
    } 
}
