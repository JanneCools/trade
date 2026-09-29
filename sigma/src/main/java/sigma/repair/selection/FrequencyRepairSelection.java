package sigma.repair.selection;

import core.RepairException;
import core.dataset.DataObject;
import core.dataset.Dataset;
import core.util.ItemSelector;
import sigma.datastructures.fptree.AttributeValue;
import sigma.datastructures.fptree.FPTree;
import sigma.datastructures.fptree.FPTreeFactory;
import sigma.datastructures.fptree.Itemset;
import sigma.datastructures.fptree.ItemsetConvertor;
import sigma.datastructures.rules.SCCSigmaRuleset;

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
