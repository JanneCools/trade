package be.ugent.ledc.sigma.repair;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import be.ugent.ledc.sigma.datastructures.rules.SCCSigmaRuleset;
import be.ugent.ledc.sigma.repair.cost.models.ConstantCostModel;
import be.ugent.ledc.sigma.repair.selection.DonorSelector;
import be.ugent.ledc.sigma.repair.selection.FrequencyDonorSelector;

import java.util.*;
import java.util.stream.Collectors;


public class JointRepair extends ConstantCostRepair {
    private final DonorSelector donorSelector;

    public JointRepair(DonorSelector donorSelector, SCCSigmaRuleset rules, ConstantCostModel costModel, NullBehavior nullBehavior) {
        super(rules, costModel, nullBehavior);
        this.donorSelector = donorSelector;
    }

    public JointRepair(DonorSelector donorSelector, SCCSigmaRuleset rules, ConstantCostModel costModel) {
        super(rules, costModel);
        this.donorSelector = donorSelector;
    }

    public JointRepair(SCCSigmaRuleset rules, ConstantCostModel costModel, NullBehavior nullBehavior, Dataset cleanData) throws RepairException {
        super(rules, costModel, nullBehavior);
        this.donorSelector = new FrequencyDonorSelector(cleanData, rules);
    }

    public JointRepair(SCCSigmaRuleset rules, ConstantCostModel costModel, Dataset cleanData) throws RepairException {
        super(rules, costModel);
        this.donorSelector = new FrequencyDonorSelector(cleanData, rules);
    }

    public JointRepair(SCCSigmaRuleset rules, Dataset cleanData) throws RepairException {
        this(rules,
            new ConstantCostModel(
                rules
                    .getRules()
                    .stream()
                    .flatMap(rule -> rule.getInvolvedAttributes().stream())
                    .collect(Collectors.toSet())),
            cleanData);
    }

    /**
     * As determined by Fellegi and Holt in their approach, a solution is selected
     * at random from the minimal cost covers.
     *
     * @param minimalCovers All minimal covers to select from.
     * @param dirty         The object that is repaired.
     * @return A randomly selected minimal cover.
     */
    @Override
    public Set<String> selectSolution(Set<Set<String>> minimalCovers, DataObject dirty) {
        return new ArrayList<>(minimalCovers)
                .get(new Random().nextInt(minimalCovers.size()));
    }

    @Override
    public DataObject repair(DataObject o, Set<String> solution) throws RepairException {

        //Construct the set of attributes to search for donor
        Set<String> nonSolutionAttributes = new HashSet<>(getRules().getContractors().keySet());
        nonSolutionAttributes.removeAll(solution);

        //Construct a repair by projecting o over non-solution attributes
        DataObject repair = o.inverseProject(solution);

        SigmaRuleset selectedRules = selectRules(repair);

        //For each attribute to keep, make a value iterator for values that are ok
        Map<String, Set<AbstractAtom<?, ?, ?>>> mapping = new HashMap<>();

        for (String attribute : nonSolutionAttributes) {
            mapping.put(attribute,
                    selectedRules
                            .getRules()
                            .stream()
                            .flatMap(rule -> rule.getAtoms().stream())
                            .filter(atom -> atom.getAttributes().anyMatch(a -> a.equals(attribute)))
                            .filter(atom -> atom.getAttributes().noneMatch(solution::contains))
                            .collect(Collectors.toSet()));
        }

        Set<SigmaRule> variableConditions = new HashSet<>();

        for (SigmaRule selectedRule : selectedRules) {

            if (!selectedRule.getVariableAtoms().isEmpty() && selectedRule.getVariableAtoms().stream().anyMatch(atom -> atom.getAttributes().anyMatch(solution::contains) && atom.getAttributes().anyMatch(nonSolutionAttributes::contains))) {
                variableConditions.add(selectedRule.fix(repair));
            }

        }

        //Select donor tuples
        DataObject donor = donorSelector.selectDonor(solution, o, mapping, variableConditions);

        if(VERBOSITY > 1)
        {
            System.out.println("Donor values: " + donor);
        }
        
        //Complete the repair
        for (String attribute : donor.getAttributes())
        {
            repair.set(attribute, donor.get(attribute));
        }

        //Return
        return repair;
    }

    private SigmaRuleset selectRules(DataObject repair) {
        //We select rules that can still fail after fixing for the values we keep
        Set<SigmaRule> ruleSelection = getRules()
                .getRules()
                .stream()
                .filter(rule -> rule
                        .fix(repair) //If we fix the values of clean object so far...
                        .getAtoms()
                        .stream()
                        .noneMatch(atom -> atom.equals(AbstractAtom.ALWAYS_FALSE)))
                .collect(Collectors.toSet());

        //We wrap the selected rules in a sigma ruleset and project on the contractors
        return new SigmaRuleset(getRules()
                .getContractors()
                .entrySet()
                .stream()
                .filter(e -> !repair.getAttributes().contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                ruleSelection);
    }
}
