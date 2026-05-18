package be.ugent.ledc.sigma.repair;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.cost.CostModel;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A generic repair engine for a dataset where tuples must satisfy a set of SigmaRules.
 * 
 * Implementations of RepairEngine may enforce additional constraints (e.g., key constraints,
 * partial key constraints...) or may enforce restrictions on the cost models they allow
 * (e.g., constant cost models only).
 * @author abronsel
 * @param <M> The type of cost model used by this repair engine
 * @param <R> The type of ruleset that is accepted by this repair engine
 */
public abstract class RepairEngine<M extends CostModel<?>, R extends SigmaRuleset>
{
    /**
     * The set of rules that needs to be satisfied.
     */
    private final R rules;

    /**
     * The cost model for this repair engine that dictates the cost of repairs.
     */
    private final M costModel;

    /**
     * The strategy to repair null values in objects.
     */
    private final NullBehavior nullBehavior;

    public static int VERBOSITY = 0;

    public RepairEngine(R rules, M costModel, NullBehavior nullBehavior)
    {
        this.rules = rules;
        this.costModel = costModel;
        this.nullBehavior = nullBehavior;
    }

    public RepairEngine(R rules, M costModel)
    {
        this(rules, costModel, NullBehavior.CONSTRAINED_REPAIR);
    }

    public R getRules()
    {
        return rules;
    }

    public M getCostModel()
    {
        return costModel;
    }

    public NullBehavior getNullBehavior()
    {
        return nullBehavior;
    }

    /**
     * Verifies if this object needs repair on any of its attributes. This is
     * equivalent to needsRepair(o, o.getAttributes()).
     *
     * @param o
     * @return
     */
    public boolean needsRepair(DataObject o)
    {
        return needsRepair(o, o.getAttributes());
    }

    /**
     * A method that checks if the given object o needs to be repaired when considering
     * only the given attributes. The method will return true if either of the following
     * conditions are true:
     * <p>
     * 1. At least one rule of the projection over attributes fails
     * 2. At least one attribute in the projection contains a null value and meets
     * the requirements for repair set by the NullBehavior.
     *
     * @param o          The object for which this method verifies if repair is necessary or not.
     * @param attributes The attributes on which verification is done.
     * @return
     */
    public boolean needsRepair(DataObject o, Set<String> attributes)
    {
        //If not all rules are satisfied, the objects requires repair
        if (!getRules().project(attributes).isSatisfied(o))
            return true;

        //Else, verify the conditions for repair null values, if any
        return attributes
                .stream()
                .anyMatch(a -> o.get(a) == null && getNullBehavior().repairIfNull(rules, a));
    }

    /**
     * Repairs a given dataset by applying modification to DataObject in such
     * a way that after repairing, all SigmaRules (and any additional enforced
     * constraints) are satisfied.
     * <p>
     * Repairs aims to be minimal in terms of the given cost model, although
     * in general it is hard to ensure minimality. That is, an implementation
     * may use approximate algorithms to offer a better computational complexity.
     * The guarantee on minimality thus depends on the implementation of the
     * repair engine.
     *
     * @param dataset
     * @return
     * @throws RepairException
     */
    public Dataset repair(Dataset dataset) throws RepairException
    {
        applyRuleContracts(dataset);
        return buildRepair(dataset);
    }
    
    public abstract Dataset buildRepair(Dataset dataset) throws RepairException;
    
    public void applyRuleContracts(Dataset d)
    {
        Set<String> attributesInRules = this.rules
            .stream()
            .flatMap(rule -> rule.getInvolvedAttributes().stream())
            .collect(Collectors.toSet());
        
        
        for(String a: attributesInRules)
        {
            SigmaContractor ctr = this.rules.getContractors().get(a);

            for(DataObject o: d)
            {
                o.set(a, ctr.get(o.get(a)));
            }
        }
    }
}
