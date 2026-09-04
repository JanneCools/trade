package be.ugent.ledc.sigma.sscgeneration;

import be.ugent.ledc.sigma.sscgeneration.implication.SigmaRuleImplicationFactory;
import be.ugent.ledc.sigma.sscgeneration.implication.StandardImplicationFactory;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRulesetOperations;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The FCF generator is an SCC generator that adopts the FCF algorithm
 * to build a set that is set-cover correctable.
 * @author abronsel
 */
public final class FCFGenerator implements SCCGenerator<SigmaRule, SigmaRuleset>
{
    public static int VERBOSITY = 4;
    
    /**
     * The implication factory used during SCC generation. The factory decides
     * in each node of the FCF structure, which implication algorithm is used.
     */
    private final SigmaRuleImplicationFactory implicationFactory;
    
    /**
     * A comparator to sort attributes prior to building the FCF structure
     */
    private final Comparator<String> attributeComparator;
    
    /**
     * Forces FCF to use root pruning. This is an optimization strategy that
     * removes rules from the root if they are generated somewhere else in the tree
     */
    private final boolean useRootPruning;

    public FCFGenerator(SigmaRuleImplicationFactory implicationFactory, Comparator<String> attributeComparator, boolean useRootPruning)
    {
        this.implicationFactory = implicationFactory;
        this.attributeComparator = attributeComparator;
        this.useRootPruning = useRootPruning;
    }
    
    public FCFGenerator(Comparator<String> attributeComparator)
    {
        this(new StandardImplicationFactory(), attributeComparator, false);
    }
    
    public FCFGenerator()
    {
        this(Comparator.naturalOrder());
    }

    @Override
    public Set<SigmaRule> generateSCCSet(SigmaRuleset ruleset)
    {
        if(VERBOSITY >= 1)
        {
            System.out.println("Computing rule partition...");
        }
     
        //We compute sufficient sets for each independent class of rules
        Set<SigmaRuleset> partition = SigmaRulesetOperations.partition(ruleset);
        
        if(VERBOSITY >= 1)
        {
            System.out.println("#partitions: " + partition.size());
            System.out.println();
        }
        
        //For each partition class, create a sufficient set
        Set<SigmaRule> sufficientRules = new HashSet<>();
        
        for(SigmaRuleset partitionClass: partition)
        {
            Set<SigmaRule> next = partitionClass.getRules().size() <= 1
                ? partitionClass.getRules()
                : startFCF(partitionClass);
            
            sufficientRules.addAll(next);
        }

        return sufficientRules;

    }

    private Set<SigmaRule> startFCF(SigmaRuleset ruleset)
    {
        //Get rules
        Set<SigmaRule> rules = ruleset.getRules();
        
        //Clean
        clean(rules);
        
        //Collect generators and sort them with the provided comparator
        List<String> generators = rules
            .stream()
            .flatMap(rule -> rule.getInvolvedAttributes().stream())
            .distinct()
            .sorted(attributeComparator)
            .collect(Collectors.toList());

        if(VERBOSITY >= 1)
        {
            System.out.println("Generators: " + generators);
            System.out.println("---------------------------------------");
        }
        
        //Generate implicit rules
        rules.addAll(generate(
            rules,
            rules,
            ruleset.getContractors(),
            generators,
            new HashSet<>()));
    
        clean(rules);
        
        //Clean
        return rules;
    }
    
    private <T extends Comparable<? super T>> Set<SigmaRule> generate(Set<SigmaRule> allMaxRules, Set<SigmaRule> currentBranchRules, Map<String, SigmaContractor<?>> sigmaContractors, List<String> remainingAttributes, Set<String> nonInvolvedAttributes)
    {
        //Iterator over remaining attributes
        Iterator<String> attributeIterator = remainingAttributes.iterator();
      
        // Loop over all remaining child attributes
        while (attributeIterator.hasNext()) {

            // Select generator
            String generator = attributeIterator.next();
            attributeIterator.remove();
            
            // Select all rules in the current branch with:
            // - the generator involved 
            // - the nonInvolvedAttributes not involved
            
            Set<SigmaRule> candidateContributors = currentBranchRules
                .stream()
                .filter(r -> r.involves(generator))
                .filter(r -> nonInvolvedAttributes.stream().noneMatch(r::involves))
                .collect(Collectors.toSet());

            if(VERBOSITY >= 2)
            {
                System.out.println("Generator: " + generator);
                System.out.println("Non-involved: " + nonInvolvedAttributes);
            }
            
            if (candidateContributors.size() > 1)
            {

                if(VERBOSITY >= 4)
                {
                    System.out.println("\n\tContributors: ");
                    candidateContributors.forEach(cc -> System.out.println("\t- " + cc));
                    System.out.println();
                }

                // Request a rule implicator at the factory and use it to imply rules
                Set<SigmaRule> newRules = implicationFactory.create(
                    generator,
                        (SigmaContractor<T>) sigmaContractors.get(generator),
                    candidateContributors
                ).generate(
                    generator,
                        (SigmaContractor<T>) sigmaContractors.get(generator),
                    candidateContributors
                );
                
                if(VERBOSITY >= 4)
                {
                    if(!newRules.isEmpty())
                    {
                        System.out.println("\tNew rules (before cleaning): ");
                        newRules.forEach(nr -> System.out.println("\t- " + nr));
                        System.out.println();
                    }
                }

                // Prepare for recursive call
                if (!newRules.isEmpty()) {

                    // Replace redundant edit rules in newRules by their dominant edit rules in allMaxRules
                    Map<SigmaRule, Set<SigmaRule>> newRulesDominanceMapping = mapOnDominant(newRules, allMaxRules);
                    newRules = newRules
                        .stream()
                        .flatMap(newRule -> newRulesDominanceMapping.get(newRule).stream())
                        .collect(Collectors.toSet());

                    // Replace redundant edit rules in allMaxRules by their dominant edit rules in newRules
                    Map<SigmaRule, Set<SigmaRule>> allMaxRulesDominanceMapping = mapOnDominant(allMaxRules, newRules);
                    allMaxRules = allMaxRules
                        .stream()
                        .flatMap(newRule -> allMaxRulesDominanceMapping.get(newRule).stream())
                        .collect(Collectors.toSet());

                    // Replace redundant edit rules in currentBranchRules by their dominant edit rules in newRules
                    Map<SigmaRule, Set<SigmaRule>> currentBranchRulesDominanceMapping = mapOnDominant(currentBranchRules, newRules);
                    currentBranchRules = currentBranchRules
                        .stream()
                        .flatMap(newRule -> currentBranchRulesDominanceMapping.get(newRule).stream())
                        .collect(Collectors.toSet());

                    // Add all generated essentially new rules to the set of allMaxRules
                    allMaxRules.addAll(newRules);

                    // Pass the currentBranchRules together with the generated essentially new edits (or their dominating rule) for the next recursive call
                    Set<SigmaRule> nextLevelRules = new HashSet<>(currentBranchRules);
                    nextLevelRules.addAll(newRules);

                    if(VERBOSITY >= 2)
                    {
                        System.out.println("\tNew rules (after cleaning): ");
                        newRules.forEach(nr -> System.out.println("\t- " + nr));
                        System.out.println("---------------------------------------");
                    }
                    
                    if (!remainingAttributes.isEmpty() && nextLevelRules.size() >= 2) {

                        // Add the current generator the set of attributes that are not supposed to enter during the next recursive calls
                        Set<String> newNonEnteringAttributes = new HashSet<>(nonInvolvedAttributes);
                        newNonEnteringAttributes.add(generator);

                        // Call method recursively
                        allMaxRules = generate(
                            allMaxRules,
                            nextLevelRules,
                            sigmaContractors,
                            new ArrayList<>(remainingAttributes),
                            newNonEnteringAttributes
                        );

                    }
                }
                else
                {
                    if(VERBOSITY >= 3) {
                        System.out.println("\tNo new rules (after cleaning)");
                        System.out.println("---------------------------------------");
                    }

                }
            } else {
                if(VERBOSITY >= 2)
                {
                    System.out.println("\n\tNo contributors");
                    System.out.println("---------------------------------------");
                }
            }
        }

        return allMaxRules;

    }

    /**
     * Removes a rule r if there is another to rule to which r is redundant
     * @param rules 
     */
    private void clean(Set<SigmaRule> rules)
    {
        rules.removeIf(r1 -> 
            rules
            .stream()
            .anyMatch(r2 -> !r1.equals(r2) && r1.isRedundantTo(r2))
        );
    }
    
    private Map<SigmaRule, Set<SigmaRule>> mapOnDominant(Set<SigmaRule> potentiallyRedundant, Set<SigmaRule> potentiallyDominant) {

        Map<SigmaRule,  Set<SigmaRule>> dominanceMapping = new HashMap<>();

        for (SigmaRule ruleA : potentiallyRedundant)
        {
            dominanceMapping.put(ruleA, new HashSet<>());

            for (SigmaRule ruleB : potentiallyDominant)
            {
                if (ruleA.isRedundantTo(ruleB))
                {
                    dominanceMapping.get(ruleA).add(ruleB);
                }
            }

            //Ensure the dominant rules are maximal
            dominanceMapping
                .get(ruleA)
                .removeIf(redRule -> dominanceMapping
                    .get(ruleA)
                    .stream()
                    .anyMatch(domRule -> !domRule.equals(redRule) && redRule.isRedundantTo(domRule))
                );
            
            if (dominanceMapping.get(ruleA).isEmpty())
            {
                dominanceMapping.get(ruleA).add(ruleA);
            }
        }

        return dominanceMapping;
    }
}
