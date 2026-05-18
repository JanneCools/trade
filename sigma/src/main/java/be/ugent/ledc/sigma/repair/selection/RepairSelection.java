package be.ugent.ledc.sigma.repair.selection;

import be.ugent.ledc.core.dataset.DataObject;
import java.util.Set;

public interface RepairSelection
{   
    /**
     * Select the best repair from a set of given repairs. Each repair is a set of attributes that will change.
     * @param minimalRepairs
     * @return 
     */
    DataObject selectRepair(Set<DataObject> minimalRepairs);
}
