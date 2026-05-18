package be.ugent.ledc.sigma.repair.cost.models;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.cost.CostModel;
import be.ugent.ledc.sigma.repair.cost.functions.IterableCostFunction;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.contracts.ForwardNavigator;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;
import be.ugent.ledc.sigma.repair.RepairOperations;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class IterableCostModel extends CostModel<IterableCostFunction>
{
    public IterableCostModel(Map<String, IterableCostFunction> costFunctions)
    {
        super(costFunctions);
    }

    public IterableCostModel() {}

    public Iterator<DataObject> buildLocoIterator(DataObject dirty, Set<String> cover, Set<String> conditionals, SigmaRuleset rules, boolean shrink) throws RepairException
    {    
        if(cover.isEmpty())
        {
            return new LocoIterator(
                dirty,
                new HashMap<>(),
                new HashSet<>(),
                conditionals);
        }
        
        //In other cases, we find permitted values for each attribute in the cover
        Map<String, ValueIterator<?>> permittedValues = RepairOperations.getPermittedValues
        (
            dirty,
            cover,
            conditionals,
            rules
        );
        
        if(shrink)
        {
//            permittedValues = RepairOperations.shrink(permittedValues, rules);
        }

        //Sanity check on cover attributes and conditional attributes
        if(conditionals.stream().anyMatch(cover::contains))
            throw new RepairException("""
                Could not build a lowest cost iterator: the conditional attributes cannot overlap with the set cover.
                Set cover: """
                + cover
                + "\nConditionals: "
                + conditionals);

        //We return a lowest-cost first iterator
        return new LocoIterator(dirty, permittedValues, cover, conditionals);
    }

    private class LocoIterator implements Iterator<DataObject>
    {        
        private final TreeSet<DataObject> sortedObjects;

        private final Set<DataObject> history = new HashSet<>();
        
        private final Map<String, ForwardNavigator<Object>> navigators;

        public LocoIterator(DataObject dirty, Map<String, ValueIterator<?>> permittedValues, Set<String> cover, Set<String> conditionals) throws RepairException
        {
            //If cover is empty or some attribute cover has no cost function
            //we cannot iterate over solutions and init an empty iterator
            if(cover.isEmpty() || cover.stream().anyMatch(a -> getCostFunction(a) == null))
            {
                sortedObjects = new TreeSet<>();
                navigators = new HashMap<>();
                return;
            }
            
            DataObject dirtyProj = dirty.project(cover);
            this.sortedObjects = new TreeSet<>(
                Comparator.<DataObject, Integer>comparing((DataObject o1) -> cost(dirtyProj, o1))
                    .thenComparing(DataObject.getProjectionComparator(new ArrayList<>(cover)))
            );
            this.navigators = new HashMap<>();

            //Compose the object with lowest cost in change
            DataObject initial = new DataObject(dirty.isCaseSensitiveNames());
            
            boolean emptyNavigators = false;
            
            for(String attribute: cover)
            {
                //Get a forward navigator that accounts for the permitted values
                ForwardNavigator navigator = getCostFunction(attribute)
                    .getRepairNavigator(
                        dirty.get(attribute),
                        permittedValues.get(attribute),
                        dirty
                    );
                this.navigators.put(attribute, navigator);
                initial.set(attribute, navigator.first());
                if(navigator.first() == null)
                {
                    emptyNavigators = true;
                }
            }
            
            if(!emptyNavigators)
                sortedObjects.add(initial);
        }
        
        @Override
        public boolean hasNext()
        {
            return !sortedObjects.isEmpty();
        }

        @Override
        public DataObject next()
        {
            //Get the object with lowest cost
            DataObject nextObject = sortedObjects.pollFirst();

            for (String attribute : nextObject.getAttributes())
            {
                //Get the navigator
                ForwardNavigator<Object> navigator = navigators.get(attribute);
                
                //Get the current value
                Object current = nextObject.get(attribute);
                
                //Can we assign a next value for this attribute?
                if (navigator.hasNext(current))
                {
                    //If yes, then copy the object we are about the emit
                    DataObject copy = new DataObject(nextObject);

                    //Assign the next cost for this attribute
                    copy.set(attribute, navigator.next(current));

                    if (!history.contains(copy))
                    {
                        sortedObjects.add(copy);
                    }
                }
            }

            //Add the next to the history
            history.add(nextObject);

            //Return the next object
            return nextObject;
        }
    }
}
