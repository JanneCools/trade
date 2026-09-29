package sigma.datastructures.fptree;

import core.dataset.ContractedDataset;
import core.dataset.DataObject;
import core.dataset.Dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ItemsetConvertor
{
    public static List<Itemset<AttributeValue<?>>> convert(Dataset dataset, boolean ignoreNulls) {

        List<Itemset<AttributeValue<?>>> converted = new ArrayList<>();
        dataset.getDataObjects().forEach((c) -> converted.add(convert(c, ignoreNulls)));
        return converted;
    }
    
    public static Itemset<AttributeValue<?>> convert(DataObject o, boolean ignoreNulls)
    {
        Itemset<AttributeValue<?>> itemset = new Itemset<>();

        for (String attribute : o.getAttributes())
        {
            if(o.get(attribute) != null || !ignoreNulls)
            {
                itemset.add(new AttributeValue<>(attribute, o.get(attribute)));
            }
        }

        return itemset;
    }
    
    public static List<Itemset<AttributeValue<String>>> convertAsStrings(ContractedDataset dataset, boolean ignoreNulls, Set<String> attributes) {

        List<Itemset<AttributeValue<String>>> converted = new ArrayList<>();
        for(DataObject o: dataset.getDataObjects())
        {
            converted.add(
                convertAsStrings(
                    o.project(attributes),
                    ignoreNulls
                )
            );
        }
        
        return converted;
    }
    
    public static Itemset<AttributeValue<String>> convertAsStrings(DataObject o, boolean ignoreNulls)
    {
        Itemset<AttributeValue<String>> itemset = new Itemset<>();

        for (String attribute : o.getAttributes())
        {
            DataObject copy = new DataObject().concat(o);
            
            if(copy.get(attribute) != null || !ignoreNulls)
            {
                itemset.add(new AttributeValue<>(
                    attribute,
                    copy.asString(attribute).getString(attribute)
                ));
            }
        }

        return itemset;
    }
}
