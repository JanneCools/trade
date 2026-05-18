package be.ugent.ledc.core.dataset;

import be.ugent.ledc.core.DataException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A straightforward implementation of a Dataset backed by an ArrayList. This implementation
 * does not use Contracts and therefore offers no guarantees of the objects it contains.
 * Using this implementation requires your own checking of the objects contained in the Dataset.
 * 
 * @author abronsel
 */
public class SimpleDataset implements Dataset
{
    private final List<DataObject> dataObjects;

    public SimpleDataset(List<DataObject> data)
    {
        this.dataObjects = data.stream().map(o -> new DataObject(o)).collect(Collectors.toList());
    }
    
    public SimpleDataset()
    {
        this(new ArrayList<>());
    }
    
    @Override
    public boolean addDataObject(DataObject dataObject)
    {
        return this.dataObjects.add(dataObject);
    }
    
    @Override
    public boolean removeDataObject(DataObject dataObject)
    {
        return this.dataObjects.remove(dataObject);
    }

    @Override
    public void replaceDataObject(DataObject oldDataObject, DataObject newDataObject)
    {
        if(oldDataObject == null)
        {
            addDataObject(newDataObject);
            return;
        }
        
        if(oldDataObject.equals(newDataObject))
            return;
        
        if(this.dataObjects.contains(oldDataObject))
        {
            while(this.dataObjects.indexOf(oldDataObject) != -1)
            {
                int index = this.dataObjects.indexOf(oldDataObject);
                this.dataObjects.add(index, newDataObject);
                this.dataObjects.remove(index+1);
            }
        }
    }

    @Override
    public List<DataObject> getDataObjects()
    {
        return dataObjects;
    }

    @Override
    public int getSize()
    {
        return dataObjects.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleDataset that = (SimpleDataset) o;
        return Objects.equals(dataObjects, that.dataObjects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataObjects);
    }

    @Override
    public String toString() {
        return "SimpleDataset{" +
                "data=" + dataObjects +
                '}';
    }

    @Override
    public Dataset unionAll(Dataset otherDataset)
    {
        return new SimpleDataset(
            Stream.concat(
                dataObjects.stream(),
                otherDataset.getDataObjects().stream()
            ).collect(Collectors.toList())
        );
    }

    @Override
    public Dataset union(Dataset otherDataset)
    {
        return unionAll(otherDataset).distinct();
    }

    @Override
    public Dataset intersect(Dataset otherDataset)
    {
        return new SimpleDataset(
            getDataObjects()
                .stream()
                .filter(otherDataset.getDataObjects()::contains)
                .collect(Collectors.toList()));
    }

    @Override
    public Dataset project(String... attributes)
    {
        return new SimpleDataset(dataObjects
            .stream()
            .map(o -> o.project(attributes))
            .collect(Collectors.toList()));
    }
    
    @Override
    public Dataset inverseProject(String... attributes)
    {
        return new SimpleDataset(dataObjects
            .stream()
            .map(o -> o.inverseProject(attributes))
            .collect(Collectors.toList()));
    }

    @Override
    public Dataset select(Predicate<DataObject> selector)
    {
        return new SimpleDataset(dataObjects
            .stream()
            .filter(selector)
            .collect(Collectors.toList()));
    }

    @Override
    public Dataset extend(String attribute, Function<DataObject, Object> derivator) throws DataException
    {
        return new SimpleDataset(dataObjects
            .stream()
            .map(o -> o.extend(attribute, derivator))
            .collect(Collectors.toList()));
    }
    
    @Override
    public Dataset head(int headCount)
    {
        return headCount < 0
            ?  new SimpleDataset()
            : new SimpleDataset(
                dataObjects
                    .subList(
                        0,
                        Math.min(headCount, dataObjects.size())
                    )
                );
    }

    @Override
    public Dataset tail(int tailCount)
    {
        return tailCount < 0
            ?  new SimpleDataset()
            : new SimpleDataset(
                dataObjects
                    .subList(
                        Math.max(dataObjects.size() - tailCount, 0),
                        dataObjects.size()
                    )
                );
    }

    @Override
    public Dataset sort(Comparator<DataObject> comparator)
    {
        return new SimpleDataset(getDataObjects()
            .stream()
            .sorted(comparator)
            .collect(Collectors.toList())
        );
    }
    
    @Override
    public Dataset distinct()
    {
        return new SimpleDataset(getDataObjects()
            .stream()
            .distinct()
            .collect(Collectors.toList())
        );
    }
    
    //
    // Conversion of datatypes
    //
    
    @Override
    public Dataset asString(String attribute)
    {
        dataObjects.stream().forEach(o->o.asString(attribute));
        return this;
    }
    
    @Override
    public Dataset asInteger(String attribute)
    {
        dataObjects.stream().forEach(o -> o.asInteger(attribute));
        return this;
    }
    
    @Override
    public Dataset asLong(String attribute)
    {
        dataObjects.stream().forEach(o -> o.asLong(attribute));
        return this;
    }
    
    @Override
    public Dataset asBoolean(String attribute)
    {
        dataObjects.stream().forEach(o -> o.asBoolean(attribute));
        return this;
    }

    @Override
    public Dataset asDouble(String attribute)
    {
        dataObjects.stream().forEach(o -> o.asDouble(attribute));
        return this;
    }
    
    @Override
    public Dataset asFloat(String attribute)
    {
        dataObjects.stream().forEach(o -> o.asFloat(attribute));
        return this;
    }
    
    @Override
    public Dataset asBigDecimal(String attribute)
    {
        dataObjects.stream().forEach(o -> o.asBigDecimal(attribute));
        return this;
    }
    
    @Override
    public Dataset asDate(String attribute)
    {
        dataObjects.stream().forEach(o -> o.asDate(attribute));
        return this;
    }
    
    @Override
    public Dataset asDate(String attribute, String pattern)
    {
        dataObjects.stream().forEach(o -> o.asDate(attribute, pattern));
        return this;
    }
    
    @Override
    public Dataset asTime(String attribute)
    {
        dataObjects.stream().forEach(o -> o.asTime(attribute));
        return this;
    }
    
    @Override
    public Dataset asDateTime(String attribute)
    {
        dataObjects.stream().forEach(o -> o.asDateTime(attribute));
        return this;
    }
    
    @Override
    public Dataset asDateTime(String attribute, String pattern) throws DataException
    {
        dataObjects.stream().forEach(o -> o.asDateTime(attribute, pattern));
        return this;
    }

    @Override
    public Iterator<DataObject> iterator()
    {
        return dataObjects.iterator();
    }
    
    @Override
    public Stream<DataObject> stream()
    {
        return dataObjects.stream();
    }

    @Override
    public Dataset groupByAndCount(String countName, String... attributes)
    {
        return new SimpleDataset(
            stream()
            .collect(Collectors.groupingBy(o -> o.project(attributes)))
            .entrySet()
            .stream()
            .map(e -> new DataObject()
                .concat(e.getKey())
                .setInteger(countName, e.getValue().size())
            )
            .collect(Collectors.toList())
        );
    }
}
