package be.ugent.ledc.sigma.repair;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.dataset.SimpleDataset;
import be.ugent.ledc.core.util.ListOperations;
import be.ugent.ledc.core.util.SetOperations;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.fundy.datastructures.PartialKey;
import be.ugent.ledc.sigma.datastructures.rules.SCCSigmaRuleset;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;
import be.ugent.ledc.sigma.repair.bounding.Bounding;
import be.ugent.ledc.sigma.repair.bounding.EnumeratedBounding;
import be.ugent.ledc.sigma.repair.cost.functions.IterableCostFunction;
import be.ugent.ledc.sigma.repair.cost.models.IterableCostModel;
import be.ugent.ledc.sigma.repair.cost.models.ParkerModel;
import be.ugent.ledc.sigma.repair.selection.RepairSelection;
import java.util.stream.Collectors;

/**
 * LoCo repair is an engine for repair of sigma rules where cost models are not constant.
 * That means: the cost for changing an attribute a, is not constant, but depends on the original value
 * and the value that we change it to. In this setting, the approach of Fellegi and Holt
 * can not be used directly because the best solution can be a superset of a minimal
 * (in terms of the subset-relation) cover. 
 * 
 * To deal with this problem, the LoCo engine enables a Lowest-Cost first iteration
 * to evaluate cheaper solutions first, even if such solutions might not satisfy
 * all rules. 
 * 
 * The LoCo engine can be a powerful tool, when used in the right setting. In the worst
 * case, it will need to perform an iteration over all value-combinations and when dealing
 * with non-nominal data, this will quickly become non-tractable. This worst case happens
 * when there is no (or very little) differentiation between values in terms of cost.
 * In that case: you should consider constant cost models and use the set cover approach.
 * 
 * There are however cases in which LoCo will find repairs very quickly, some of which are listed
 * below:
 * 
 * - When you suspect that certain specific types of errors (single digit error, transposition errors,
 * rounding errors...) are frequently occurring, then non-constant cost models can be used to iterate
 * first over those values that can explained due to these errors. 
 * 
 * - If there are clear preferences to specific values, then non-constant cost models can be used
 * to model such preferences. E.g., when dealing with food data like allergen information, suggesting
 * a change from 'does not contain' to 'contains' comes with much less risk than the other way arround.
 * 
 * - In the Parker engine, a (partial) key constraint is defined on top of the sigma rules. In that setting,
 * we can use LoCoRepair with an induced cost model to mimic a voting procedure.
 * 
 * @author abronsel
 */
public class LoCoRepair extends ObjectLevelRepairEngine<IterableCostModel, SCCSigmaRuleset>
{
    private final ParkerRepair<IterableCostFunction, ParkerModel<IterableCostFunction>> hiddenParkerRepair;
    
    public static final String LEDC_KEY = "#ledc_key#";
    
    public LoCoRepair(SCCSigmaRuleset rules, IterableCostModel costModel, RepairSelection repairSelection) throws RepairException
    {
        this(
            rules,
            costModel,
            repairSelection,
            new EnumeratedBounding(), //Default: enumerate bounding
            false //Default: no forced bounding
        );
    }
    
    public LoCoRepair(SCCSigmaRuleset rules, IterableCostModel costModel, RepairSelection repairSelection, Bounding<?, SigmaContractor<?>, ValueIterator<?>> bounding, boolean forceBounding) throws RepairException
    {
        super(rules, costModel);
        
        this.hiddenParkerRepair = new ParkerRepair<>(
            new ParkerModel<>(
                new PartialKey(
                    SetOperations.set(LEDC_KEY),
                    rules
                        .stream()
                        .flatMap(rule -> rule
                            .getInvolvedAttributes()
                            .stream())
                        .collect(Collectors.toSet())
                ),
                costModel.getCostFunctions(),
                rules,
                bounding,
                forceBounding,
                true)
            ,
            repairSelection
        );
    }
    
    @Override
    public DataObject repair(DataObject dirty) throws RepairException
    {
        //Add a ledc key value
        DataObject copy = new DataObject()
            .concat(dirty)
            .setInteger(LEDC_KEY, 1);
        
        Dataset repair = this
            .hiddenParkerRepair
            .repair(new SimpleDataset(ListOperations.list(copy)));
        
        if(repair.getSize() == 1)
            return repair
                .getDataObjects()
                .get(0)
                .inverseProject(LEDC_KEY);
                    
        throw new RepairException("Unexpected amount of repairs found for object " + dirty);
    }
}
