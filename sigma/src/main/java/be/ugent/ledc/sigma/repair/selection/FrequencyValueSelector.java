package be.ugent.ledc.sigma.repair.selection;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.util.ItemSelector;
import be.ugent.ledc.sigma.datastructures.fptree.AttributeValue;
import be.ugent.ledc.sigma.datastructures.fptree.FPTree;
import be.ugent.ledc.sigma.datastructures.fptree.FPTreeFactory;
import be.ugent.ledc.sigma.datastructures.fptree.ItemsetConvertor;
import be.ugent.ledc.sigma.datastructures.rules.SCCSigmaRuleset;
import be.ugent.ledc.sigma.datastructures.values.ValueIterator;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Value selection based on frequency.
 * @author abronsel
 */
public class FrequencyValueSelector<T> implements ValueSelector<T>
{
    private final FPTree<AttributeValue<?>> fpTree;
    
    public FrequencyValueSelector(Dataset cleanData, SCCSigmaRuleset rules) throws RepairException {

        if(cleanData.getDataObjects().stream().anyMatch(o -> rules.getRules().stream().anyMatch(r -> !r.isSatisfied(o))))
        {
            throw new RepairException("The given clean do not satisfy all rules. Cannot compute repairs.");
        }

        fpTree = FPTreeFactory.makeTree(ItemsetConvertor.convert(cleanData.project(rules.getContractors().keySet()), true), 1);
    }

    @Override
    public T selectValue(String attributeToTreat, DataObject dirtyOriginal, ValueIterator<T> permittedValues, DataObject repair)
    {
        Map<T, Integer> valueCounts = new HashMap<>();
        
        if(permittedValues.cardinality() <= 50)
        {
            for(T value: permittedValues)
            {
                valueCounts.put(value, fpTree.support(new AttributeValue<>(attributeToTreat, value)));
            }
        }
        else
        {
            valueCounts.putAll((Map<? extends T, ? extends Integer>) fpTree
                .getHeaderTable()
                .keySet()
                .stream()
                .filter(av -> av.getAttribute().equals(attributeToTreat))
                .filter(av -> permittedValues.inSelection((T) av.getValue()))
                .collect(Collectors.toMap(
                    av -> av.getValue(),
                    av -> fpTree.support(av)
                )));
        }

        if(valueCounts.values().stream().mapToInt(i->i).sum() == 0)
        {
            //Pick a value at random
            return permittedValues.iterator().next();
        }

        return ItemSelector.selectItemByCount(valueCounts);
    }
}
