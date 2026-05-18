package be.ugent.ledc.sigma.repair;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import be.ugent.ledc.sigma.datastructures.rules.SCCSigmaRuleset;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;
import be.ugent.ledc.sigma.repair.cost.models.ConstantCostModel;
import be.ugent.ledc.sigma.repair.selection.FrequencyValueSelector;
import be.ugent.ledc.sigma.repair.selection.ValueSelector;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SequentialRepair is a repair engine that works with a constant cost model.
 * This means that the cost of making a change to an attribute is fixed and independent
 * of the precise change that is made.
 * 
 * The repair strategy is the same as the one pointed out by Fellegi and Holt
 * in their paper. That means the repair engine has the following strategy:
 * 
 * 1. from all minimal set covers, one cover is chosen at random
 * 2. attributes are repaired one at a time by making a choice of the permitted
 *    values an attribute can take.
 * 
 * Note that in step 2, the permitted values are determined by (i) those attributes
 * that are not repaired and (ii) those attributes that already have been repaired.
 * Because of (ii), the order in which attributes are repaired plays a role in how
 * the repaired object will look like. There is however no guarantee on this order:
 * attributes are repaired in a random order.
 * 
 * Also note that in their original paper, Fellegi and Holt made no specifications
 * on how to choose a value among the available alternatives.
 * Here, we leave the exact strategy open as well.
 * The default strategy, is based on frequency.
 * That is, from the available clean data,
 * we measure the frequency of each of the permitted values for an attribute.
 * Next, we sample one value from the permitted ones, where the probability of selecting
 * a value is proportionate to its observed frequency.
 *  
 * @author abronsel
 */
public class SequentialRepair extends ConstantCostRepair
{
    private final Map<String, ValueSelector> valueSelectors;

    public SequentialRepair(Map<String, ValueSelector> valueSelectors, SCCSigmaRuleset rules, ConstantCostModel costModel, NullBehavior nullBehavior)
    {
        super(rules, costModel, nullBehavior);
        this.valueSelectors = valueSelectors;
    }

    public SequentialRepair(Map<String, ValueSelector> valueSelectors, SCCSigmaRuleset rules, ConstantCostModel costModel)
    {
        super(rules, costModel);
        this.valueSelectors = valueSelectors;
    }
    
    public SequentialRepair(SCCSigmaRuleset rules, ConstantCostModel costModel, NullBehavior nullBehavior, Dataset cleanData) throws RepairException
    {
        super(rules, costModel, nullBehavior);
        this.valueSelectors = new HashMap<>();

        costModel
                .getAttributes()
                .forEach(a -> {
                    try {
                        valueSelectors.put(a, new FrequencyValueSelector<>(cleanData, rules));
                    } catch (RepairException e) {
                        throw new RuntimeException(e);
                    }
                });

    }
        
    public SequentialRepair(SCCSigmaRuleset rules, ConstantCostModel costModel, Dataset cleanData) throws RepairException
    {
        super(rules, costModel);
        this.valueSelectors = new HashMap<>();

        costModel
            .getAttributes()
            .forEach(a -> {
                try {
                    valueSelectors.put(a, new FrequencyValueSelector<>(cleanData, rules));
                } catch (RepairException e) {
                    throw new RuntimeException(e);
                }
            });
    }
    
    public SequentialRepair(SCCSigmaRuleset rules, Dataset cleanData) throws RepairException
    {
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
     * @param minimalCovers All minimal covers to select from.
     * @param dirty The object that is repaired.
     * @return A randomly selected minimal cover.
     */
    @Override
    public Set<String> selectSolution(Set<Set<String>> minimalCovers, DataObject dirty)
    {
        return new ArrayList<>(minimalCovers)
            .get(new Random().nextInt(minimalCovers.size()));
    }
    
    @Override
    public DataObject repair(DataObject o, Set<String> solution) throws RepairException
    {
         //Construct the set of attributs that will NOT be imputed
        Set<String> attributesToKeep = new HashSet<>(o.getAttributes());
        attributesToKeep.removeAll(solution);
        
        //Construct a repair by projecting o over the attributes to keep
        DataObject repair = o.project(attributesToKeep);
        
        //Deep copy the solution
        Set<String> toTreat = new HashSet<>(solution);
        
//        System.out.println("Dirty object: " + o);
        
        while(!toTreat.isEmpty())
        {
            //Select an attribute to treat
            String attributeToTreat = select(toTreat, repair);

            if(attributeToTreat == null)
            {
                toTreat.stream().forEach(a -> repair.set(a, null));
                break;
            }
            
            //Get selected rules
            SigmaRuleset selectedRules = selectRules(attributeToTreat, toTreat, repair);

//            System.out.println("Attribute to treat: " + attributeToTreat);
//            System.out.println("Repair so far: " + repair);
//            System.out.println("Selected rules: ");
//            selectedRules.getRules().stream().map(SigmaRule::toString).forEach(System.out::println);

            //Get permitted values for attribute to treat
            ValueIterator<?> permittedValues = RepairOperations.getPermittedValues(
                attributeToTreat,
                (Comparable)o.get(attributeToTreat),
                selectedRules,
                repair
            );

            if(permittedValues.isEmpty())
            {
                throw new RepairException("Something went wrong in the sequential imputation algorithm. The set of permitted values for '" + attributeToTreat + "' is empty...");
            }

            //Select a value
            Object repairValue = valueSelectors
                .get(attributeToTreat)
                .selectValue(
                    attributeToTreat,
                    o,
                    permittedValues,
                    repair);

            if(VERBOSITY > 1)
                System.out.println("\tChanging '" + attributeToTreat + "' from "
                    + o.get(attributeToTreat)
                    + " to "
                    + repairValue);

            //Update the object
            repair.set(attributeToTreat, repairValue);

            //Remove the treated attribute from the treat list
            toTreat.remove(attributeToTreat);
        }
        
        return repair;
    }
    
    protected String select(Set<String> toTreat, DataObject repair) throws RepairException
    {
        for(String attribute: toTreat)
        {
            if(!selectRules(attribute, toTreat, repair).getRules().isEmpty())
            {
                return attribute;
            }
        }
        
        return null;
        //throw new RepairException("Could not find an attribute that is involved in a rule without any of the other attributes under treatment.");
    }
    
    protected SigmaRuleset selectRules(String attribute, Set<String> toTreat, DataObject repair)
    {
        //We select rules satisfying 3 conditions
        Set<SigmaRule> ruleSelection = getRules()
            .getRules()
            .stream()
            .filter(rule -> rule.involves(attribute)) //1. Rule must involve attribute
            .filter(rule -> toTreat //2. Rule must not involve other attributes to treat
                .stream()
                .allMatch(otherAttribute ->
                    otherAttribute.equals(attribute) ||
                    !rule.involves(otherAttribute)
                ))
            //3. Rules CAN fail after we assign a value for toTreat
            .filter(rule -> rule
                .fix(repair) //If we fix the values of clean object so far...
                .getAtoms()
                .stream()
                .noneMatch(atom -> atom.equals(AbstractAtom.ALWAYS_FALSE))) // ...no atom should be always false
            //In the above, if some atom would be always false, 
            //that rule will always be satisfied regardless of the value
            //we assign to 'attribute'
            .collect(Collectors.toSet());
        
        //We wrap the selected rules in a sigma ruleset and project on the contractors
        return new SigmaRuleset(
        //We project the conctractors: keep only attribute + fixed attributes
        getRules()
            .getContractors()
            .entrySet()
            .stream()
            //.filter(e -> e.getKey().equals(attribute) || !toTreat.contains(e.getKey()))
            .filter(e -> e.getKey().equals(attribute))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
            ruleSelection
        );
    }
}
