package be.ugent.ledc.sigma.repair;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.cost.CostModel;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.dataset.SimpleDataset;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;

/**
 * A RepairEngine that works on an object-per-object basis. For these types of repair engines,
 * the strategy is basically to check, for each DataObject, it it needs repair, accounting for
 * the behaviour towards null values. 
 * @param <M> The type of cost model used by this repair engine
 */
public abstract class ObjectLevelRepairEngine<M extends CostModel<?>, R extends SigmaRuleset> extends RepairEngine<M,R>
{
    public ObjectLevelRepairEngine(R rules, M costModel, NullBehavior nullBehavior)
    {
        super(rules, costModel, nullBehavior);
    }

    public ObjectLevelRepairEngine(R rules, M costModel)
    {
        super(rules, costModel);
    }

    @Override
    public Dataset buildRepair(Dataset dataset) throws RepairException
    {
        Dataset repairedDataset = new SimpleDataset();

        for(DataObject object: dataset.getDataObjects())
        {
            if(needsRepair(object))
                repairedDataset.addDataObject(repair(object));
            else
                repairedDataset.addDataObject(object);
        }

        return repairedDataset;
    }

    public abstract DataObject repair(DataObject dirty) throws RepairException;
}
