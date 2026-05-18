package be.ugent.ledc.sigma.repair.selection;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.util.ItemSelector;
import java.util.Set;

public class RandomRepairSelection implements RepairSelection
{
    @Override
    public DataObject selectRepair(Set<DataObject> minimalRepairs)
    {
        return minimalRepairs.isEmpty()
            ? new DataObject()
            //: minimalRepairs.stream().findAny().get();
            : ItemSelector.selectItemAtRandom(minimalRepairs);
    }   
}
