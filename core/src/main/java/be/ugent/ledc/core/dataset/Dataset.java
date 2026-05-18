package be.ugent.ledc.core.dataset;

import be.ugent.ledc.core.DataException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The interface Dataset provides the generic operations one typically wants to 
 * apply on a collection of DataObjects. In the most simple implementation, a dataset
 * is simply a list of DataObjects on which these operations are applicable.
 * There might however be more strict implementations that restrict the type of values
 * an attribute of a DataObject can take.
 * @author abronsel
 */
public interface Dataset extends Iterable<DataObject>
{
    /**
     * Adds a DataObject to the Dataset
     * @param dataObject The object that is added
     * @return True if the object is added successfully, false if not.
     */
    boolean addDataObject(DataObject dataObject);
      
    /**
     * Removes a DataObject to the Dataset
     * @param dataObject The object that is removed
     * @return True if the object is removed successfully, false if not.
     */
    boolean removeDataObject(DataObject dataObject);
    
    /**
     * Removes a DataObject to the Dataset
     * @param oldDataObject
     * @param newDataObject
     */
    void replaceDataObject(DataObject oldDataObject, DataObject newDataObject);
    
    /**
     * Get the DataObjects held by this Dataset as a list
     * @return A list of DataObject present in this Dataset.
     */
    List<DataObject> getDataObjects();
    
    /**
     * Gets the number of objects held by this dataset.
     * @return The number of objects present in this dataset.
     */
    public int getSize();
    
    /**
     * Returns a projection of the dataset over the given attributes.
     * @param attributes Attributes to which objects are projected
     * @return A projection of the dataset. This is a Dataset where objects contain
     * only the given attributes and no others. 
     */
    public Dataset project(String ... attributes);
    
    /**
     * Returns an inverse (i.e., negated) projection of the dataset over the given attributes.
     * @param attributes Attributes to which objects are inversely projected
     * @return A projection of the dataset. This is a Dataset where objects do not contain
     * any of the given attributes. All other attributes are preserved.
     */
    public Dataset inverseProject(String ... attributes);
    
    public default Dataset project(Collection<String> attributes)
    {
        return project(attributes.toArray(String[]::new));
    }
    
    public default Dataset inverseProject(Collection<String> attributes)
    {
        return inverseProject(attributes.toArray(String[]::new));
    }
    
    /**
     * Returns a Dataset where a selection has been applied. This selection is passed
     * as a predicate. Only objects for which this predicate yields true, are preserved.
     * @param selector A selector that indicates the conditions to which a DataObject must
     * adhere in order for it to be preserved in the result.
     * @return A selection of the original dataset. This is a Dataset where objects pass
     * the given predicate.
     */
    public Dataset select(Predicate<DataObject> selector);
    
    /**
     * Returns a Dataset where an additional attribute is added that is computed based on the state of each object.
     * This function is useful for making transformations or adding derived data. 
     * @param attribute The name of the new attribute
     * @param derivator A function that indicates how the new attribute must be derived based on an existing object.
     * @return A new dataset with an additional attribute added.
     */
    public Dataset extend(String attribute, Function<DataObject, Object> derivator) throws DataException;
    
    /**
     * Returns a Dataset where only the first objects are present. The amount of objects kept, 
     * is indicated by the headCount. If headCount exceeds the size of the dataset, all objects
     * are preserved.
     * @param headCount The number of objects that must be preserved.
     * @return A version of the dataset where only the first 'headCount' attributes are preserved.
     */
    public Dataset head(int headCount);
    
    /**
     * Returns a Dataset where only the last objects are present. The amount of objects kept, 
     * is indicated by the tailCount. If tailCount exceeds the size of the dataset, all objects
     * are preserved.
     * @param tailCount The number of objects that must be preserved, counting from the back of the dataset.
     * @return A version of the dataset where only the last 'tailCount' attributes are preserved.
     */
    public Dataset tail(int tailCount);
    
    /**
     * Adds the objects in the otherDataset to the current Dataset.
     * 
     * @param otherDataset The other dataset for which the objects must be appended to the 
     * current dataset.
     * @return 
     */
    public Dataset unionAll(Dataset otherDataset);
    
    public Dataset union(Dataset otherDataset);
    
    public Dataset intersect(Dataset otherDataset);
    
    public Dataset sort(Comparator<DataObject> comparator);
    
    public Dataset distinct();
    
    public Dataset groupByAndCount(String countName, String ... attributes);
    
    public default Dataset groupByAndCount(String countName, Collection<String> attributes)
    {
        return groupByAndCount(countName, attributes.toArray(String[]::new));
    }
    
    //
    // Conversion functions
    //

    public Dataset asString(String attribute);

    public Dataset asInteger(String attribute);

    public Dataset asLong(String attribute);

    public Dataset asBoolean(String attribute);

    public Dataset asDouble(String attribute);
    
    public Dataset asFloat(String attribute);
    
    public Dataset asBigDecimal(String attribute);
    
    public Dataset asDate(String attribute);
    
    public Dataset asDate(String attribute, String pattern);
    
    public Dataset asTime(String attribute);  
    
    public Dataset asDateTime(String attribute);
    
    public Dataset asDateTime(String attribute, String pattern);
    
    //
    // Convenience functions
    //
    public Stream<DataObject> stream();

}
