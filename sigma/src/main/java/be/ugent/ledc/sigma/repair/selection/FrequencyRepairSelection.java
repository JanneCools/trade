package be.ugent.ledc.sigma.repair.selection;

import be.ugent.ledc.core.RepairException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.util.ItemSelector;
import be.ugent.ledc.sigma.datastructures.fptree.AttributeValue;
import be.ugent.ledc.sigma.datastructures.fptree.FPTree;
import be.ugent.ledc.sigma.datastructures.fptree.FPTreeFactory;
import be.ugent.ledc.sigma.datastructures.fptree.Itemset;
import be.ugent.ledc.sigma.datastructures.fptree.ItemsetConvertor;
import be.ugent.ledc.sigma.datastructures.rules.SCCSigmaRuleset;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FrequencyRepairSelection implements RepairSelection
{
    private final FPTree<AttributeValue<?>> fpTree;
    
    public FrequencyRepairSelection(Dataset cleanData, SCCSigmaRuleset rules) throws RepairException {

        if(cleanData.getDataObjects().stream().anyMatch(o -> rules.getRules().stream().anyMatch(r -> !r.isSatisfied(o))))
        {
            throw new RepairException("The given clean do not satisfy all rules. Cannot compute repairs.");
        }

        //Construct a Frequent Pattern tree
        fpTree = FPTreeFactory.makeTree(ItemsetConvertor.convert(cleanData, true), 1);
    }

    @Override
    public DataObject selectRepair(Set<DataObject> minimalRepairs)
    {
        Map<DataObject, Integer> countMap = new HashMap<>();
        
        for(DataObject repair: minimalRepairs)
        {
            Itemset<AttributeValue<?>> itemset = ItemsetConvertor.convert(repair, true);
            
            countMap.put(repair, fpTree.support(itemset));
        }
        
        if(countMap.isEmpty())
            minimalRepairs.stream().collect(Collectors.toMap(o -> o, o -> 1));
        
        return ItemSelector.selectItemByCount(countMap);
    }
    
}
