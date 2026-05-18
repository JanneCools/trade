package be.ugent.ledc.sigma.repair;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.rules.SCCSigmaRuleset;
import be.ugent.ledc.sigma.repair.cost.models.ConstantCostModel;
import be.ugent.ledc.sigma.repair.covers.MinimalCoverSearch;
import java.util.Set;

/**
 * In a ConstantRepairEngine, we assume all cost models are ConstantCostModels.
 * This means the cost of making a change to an attribute, is fixed and does
 * not depend on the changed values.
 * 
 * The main advantage is that finding solutions can be done via simple set covers
 * of failing rules of the sufficient sigma ruleset.
 * @author abronsel
 */
public abstract class ConstantCostRepair extends ObjectLevelRepairEngine<ConstantCostModel, SCCSigmaRuleset>
{
    private final MinimalCoverSearch setCoverSearch;

    public ConstantCostRepair(SCCSigmaRuleset rules, ConstantCostModel costModel, NullBehavior nullBehavior)
    {
        super(rules, costModel, nullBehavior);
        this.setCoverSearch = new MinimalCoverSearch(rules, costModel);
    }
    
    public ConstantCostRepair(SCCSigmaRuleset rules, ConstantCostModel costModel)
    {
        super(rules, costModel);
        this.setCoverSearch = new MinimalCoverSearch(rules, costModel);
    }
    
    @Override
    public DataObject repair(DataObject dirty) throws RepairException
    {
        if(VERBOSITY > 0)
            System.out.println("Repairing " + dirty);
        
        //Find minimal covers
        Set<Set<String>> minimalCovers = this.setCoverSearch.findMinimalCovers(dirty, getNullBehavior());
        
        if(minimalCovers.isEmpty())
            throw new RepairException("Found no covers for object " + dirty);
        
        //Select a cover
        Set<String> solution = selectSolution(minimalCovers, dirty);
        
        //Apply a repair technique by changing attributes in the solution
        return repair(dirty, solution);
    }

    public MinimalCoverSearch getSetCoverSearch() {
        return setCoverSearch;
    }

    public abstract Set<String> selectSolution(Set<Set<String>> minimalCovers, DataObject o);
    
    public abstract DataObject repair(DataObject o, Set<String> solution) throws RepairException;
}
