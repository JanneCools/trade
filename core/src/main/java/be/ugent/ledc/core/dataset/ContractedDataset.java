package be.ugent.ledc.core.dataset;

import be.ugent.ledc.core.dataset.Contract.ContractBuilder;
import be.ugent.ledc.core.dataset.contractors.TypeContractor;
import be.ugent.ledc.core.dataset.contractors.TypeContractorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An extension of a SimpleDataset where attributes are subjected to certain contracts
 * they must obey. This provides a very generic means to enforces constraints on the
 * data in the dataset.
 * @author abronsel
 */
public class ContractedDataset extends SimpleDataset
{
    private final Contract contract;
    
    public ContractedDataset(Contract contract)
    {
        super();
        this.contract = contract;
    }
    
    protected ContractedDataset(Map<String, TypeContractor<?>> contractMap)
    {
        super();
        Contract.ContractBuilder builder = new Contract.ContractBuilder();
        for(String a: contractMap.keySet())
        {
            builder.addContractor(a, contractMap.get(a));
        }
        this.contract = builder.build();
    }

    @Override
    /**
     * The addDataObject now verifies if the new object meets all contracts before
     * it is added to the dataset.
     */
    public boolean addDataObject(DataObject dataObject)
    {
        if(contract.verify(dataObject))
            return super.addDataObject(dataObject);

        return false;
    }
    
    @Override
    public void replaceDataObject(DataObject oldDataObject, DataObject newDataObject)
    {
        if(oldDataObject == null)
            addDataObject(newDataObject);
        
        if(!contract.verify(newDataObject))
            return;
        
        if(getDataObjects().contains(oldDataObject))
        {
            while(getDataObjects().indexOf(oldDataObject) != -1)
            {
                int index = getDataObjects().indexOf(oldDataObject);
                getDataObjects().add(index, newDataObject);
                getDataObjects().remove(index+1);
            }
        }
    }

    /**
     * Returns the data held by this dataset as an immutable list in order to prevent changes
     * to happen outside the scope of contract checking
     * @return 
     */
    @Override
    public List<DataObject> getDataObjects()
    {
        return Collections.unmodifiableList(super.getDataObjects());
    }
    
    /**
     * Returns a SimpleDataset that is a copy of the data held by the current
     * ContractedDataset.
     * @return 
     */    
    public SimpleDataset getAsSimpleDataset()
    {
        return new SimpleDataset(
            getDataObjects()
            .stream()
            .map(DataObject::new)
            .collect(Collectors.toList()));
    }

    public Contract getContract()
    {
        return contract;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContractedDataset that = (ContractedDataset) o;
        return Objects.equals(contract, that.contract);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contract);
    }
 
    @Override
    public ContractedDataset unionAll(Dataset otherDataset)
    {
        ContractedDataset result = new ContractedDataset(contract);
        
        Stream.concat(getDataObjects().stream(), otherDataset.getDataObjects().stream())
            .forEach(result::addDataObject);
        return result;
    }

    @Override
    public String toString() {
        return "ContractedDataset{" +
                "contract=" + contract +
                "} " + super.toString();
    }
    
    /**
     * Returns the natural join of the current ContractedDataset with the other dataset. 
     * This is a ContractedDataset containing combinations of DataObjects where common attributes
     * have the same value.
     * @param otherDataset The other dataset to which this Dataset is joined naturally.
     * @return The natural join of this ContractedDataset with the otherDataset.
     */
    public ContractedDataset naturalJoin(ContractedDataset otherDataset)
    {
        ContractedDataset naturalJoin = new ContractedDataset(
            Contract.merge(
                contract,
                otherDataset.getContract()
            )
        );
        
        //If no attributes are in common, the natural join equals the cross product
        if(getContract().getAttributes().stream().noneMatch(a -> otherDataset.getContract().getAttributes().contains(a)))
        {
            crossProduct(getDataObjects(), otherDataset.getDataObjects())
            .stream()
            .forEach(o -> naturalJoin.addDataObject(o));
        }
        //Compute the natural join via sort-merge join
        else
        {
            //First get common attributes
            List<String> commonAttributes = getContract()
                .getAttributes()
                .stream()
                .filter(a -> otherDataset.getContract().getAttributes().contains(a))
                .distinct()
                .collect(Collectors.toList());
            
            //Compose a projection comparator
            Comparator<DataObject> projComparator = DataObject.getProjectionComparator(commonAttributes);
            
            //Sort left objects
            List<DataObject> leftSorted = getDataObjects()
                .stream()
                .sorted(projComparator)
                .collect(Collectors.toList());
            
            //Sort right objects
            List<DataObject> rightSorted = otherDataset
                .getDataObjects()
                .stream()
                .sorted(projComparator)
                .collect(Collectors.toList());
            
            //Perform the merge
            int leftIndex = 0;
            int rightIndex = 0;
            
            while(leftIndex < leftSorted.size() && rightIndex < rightSorted.size())
            {
                DataObject l = leftSorted.get(leftIndex).project(commonAttributes);
                DataObject r = rightSorted.get(rightIndex).project(commonAttributes);
            
                if(l.equals(r))
                {
                    List<DataObject> leftPart = sublist(leftSorted, leftIndex, commonAttributes);
                    List<DataObject> rightPart = sublist(rightSorted, rightIndex, commonAttributes);
                    
                    //Combine objects with equal value for projected attributes
                    crossProduct(leftPart, rightPart)
                    .stream()
                    .forEach(o -> naturalJoin.addDataObject(o));
                    
                    leftIndex += leftPart.size();
                    rightIndex+= rightPart.size();
                }
                else if(projComparator.compare(l, r) < 0)
                    leftIndex++; //Advance left
                else
                    rightIndex++; //Advance right

            }
        }
        
        return naturalJoin;
    }
    
    private List<DataObject> sublist(List<DataObject> sortedObjects, int index, List<String> commonAttributes)
    {
        List<DataObject> sublist = new ArrayList<>();
        
        for(int i=index; i<sortedObjects.size();i++)
        {
            if(sortedObjects.get(index).project(commonAttributes).equals(sortedObjects.get(i).project(commonAttributes)))
                sublist.add(sortedObjects.get(i));
            else
                break;
        }
        
        return sublist;
    }
    
    private List<DataObject> crossProduct(List<DataObject> left, List<DataObject> right)
    {
        return left
        .stream()
        .flatMap(leftObject -> right
            .stream()
            .map(rightObject -> leftObject.concat(rightObject))
        )
        .collect(Collectors.toList());
    }
      
    public <C extends Comparable<? super C>> SimpleDataset groupByAndAggregate(Set<String> groupingAttributes, AggregationClause<C> aggregationClause)
    {
        SimpleDataset resultDataset = new SimpleDataset();
        
        Map<DataObject, List<DataObject>> groupedObjects = getDataObjects()
            .stream()
            .collect(Collectors.groupingBy(o -> o.project(groupingAttributes)));
        
        for(DataObject groupKey: groupedObjects.keySet())
        {
            List<C> valuesToAggregate = groupedObjects.get(groupKey)
                .stream()
                .map(o -> (C)o.get(aggregationClause.getInputAttribute()))
                .collect(Collectors.toList());
            
            DataObject newObject = new DataObject(groupKey)
                .set(aggregationClause.getOutputAttribute(),
                    aggregationClause.getAggregator().aggregate(valuesToAggregate));
            
            resultDataset.addDataObject(newObject);
        }
        
        return resultDataset;
    }
    
    //
    // Conversion of datatypes
    //
    
    private <C extends TypeContractor<?>> ContractedDataset initForConversion(String attribute, C contractor)
    {
        ContractBuilder builder = new Contract.ContractBuilder();
        
        //Maintain contract for other attributes
        getContract()
            .getAttributes()
            .stream()
            .filter(a -> !a.equals(attribute))
            .forEach(a -> builder
                .addContractor(a, getContract().getAttributeContract(a)));
        
        builder.addContractor(
            attribute,
            contractor);
        
        return new ContractedDataset(builder.build());
    }
    
    /**
     * Returns a new instance of a ContractedDataset where the old contract is updated
     * to deal with the new datatype (String) of the given attribute.
     * @param attribute
     * @return 
     */
    @Override
    public ContractedDataset asString(String attribute)
    {
        return asString(attribute, TypeContractorFactory.STRING);
    }
    
    /**
     * Returns a new instance of a ContractedDataset where the old contract is updated
     * to deal with the new datatype (String) of the given attribute and in addition a specific contractor is used.
     * @param <C>
     * @param attribute
     * @param contractor
     * @return 
     */
    public <C extends TypeContractor<String>> ContractedDataset asString(String attribute, C contractor)
    {
        ContractedDataset updatedDataset = initForConversion(attribute, contractor);

        for (DataObject o: this) {
            DataObject updatedO = new DataObject(o);
            updatedO.asString(attribute);
            updatedO.setString(attribute, contractor.get(updatedO.getString(attribute)));
            updatedDataset.addDataObject(updatedO);
        }

        return updatedDataset;
    }
    
    /**Returns a new instance of a ContractedDataset where the old contract is updated
     * by an explictly given contractor C to deal with the new datatype (String) of the given set of attributes.
     * @param <C>
     * @param attributes
     * @param contractor
     * @return 
     */
    public <C extends TypeContractor<String>> ContractedDataset asString(Set<String> attributes, C contractor)
    {
        ContractedDataset updatedDataset = null;
        
        for(String a: attributes)
        {
            updatedDataset = asString(a, contractor);
        }

        return updatedDataset;
    }
    
    /**
     * Returns a new instance of a ContractedDataset where the old contract is updated
     * to deal with the new datatype (Integer) of the given attribute.
     * The new contractor is defaulted to TypeContractorFactory.INTEGER.
     * @param attribute
     * @return 
     */
    @Override
    public ContractedDataset asInteger(String attribute)
    {
        return asInteger(attribute, TypeContractorFactory.INTEGER);
    }
    
    /**
     * Returns a new instance of a ContractedDataset where the old contract is updated
     * by an explictly given contractor C to deal with the new datatype (Integer) of the given attribute.
     * @param <C> A type of contractor for Integers
     * @param attribute
     * @param contractor The new contractor for the attribute
     * @return 
     */
    public <C extends TypeContractor<Integer>> ContractedDataset asInteger(String attribute, C contractor)
    {
        ContractedDataset updatedDataset = initForConversion(attribute, contractor);

        for (DataObject o: this) {
            DataObject updatedO = new DataObject(o);
            updatedO.asInteger(attribute);
            updatedO.setInteger(attribute, contractor.get(updatedO.getInteger(attribute)));
            updatedDataset.addDataObject(updatedO);
        }

        return updatedDataset;
    }
    
    /** Returns a new instance of a ContractedDataset where the old contract is updated
     * by an explictly given contractor C to deal with the new datatype (Integer) of the given set of attributes.
     * @param <C>
     * @param attributes
     * @param contractor
     * @return 
     */
    public <C extends TypeContractor<Integer>> ContractedDataset asInteger(Set<String> attributes, C contractor)
    {
        ContractedDataset updatedDataset = null;
        
        for(String a: attributes)
        {
            if(updatedDataset == null)
                updatedDataset = asInteger(a, contractor);
            else
                updatedDataset = updatedDataset.asInteger(a, contractor);
        }

        return updatedDataset;
    }
    
    /**
     * Returns a new instance of a ContractedDataset where the old contract is updated
     * to deal with the new datatype (Long) of the given attribute.
     * The new contractor is defaulted to TypeContractorFactory.LONG.
     * @param attribute
     * @return 
     */
    @Override
    public ContractedDataset asLong(String attribute)
    {               
        return asLong(attribute, TypeContractorFactory.LONG);
    }
    
    /**
     * Returns a new instance of a ContractedDataset where the old contract is updated
     * by an explictly given contractor C to deal with the new datatype (Long) of the given attribute.
     * @param <C> A type of contractor for Longs
     * @param attribute
     * @param contractor The new contractor for the attribute
     * @return 
     */
    public <C extends TypeContractor<Long>> ContractedDataset asLong(String attribute, C contractor)
    {
        ContractedDataset updatedDataset = initForConversion(attribute, contractor);

        for (DataObject o: this)
        {
            DataObject updatedO = new DataObject(o);
            updatedO.asLong(attribute);
            updatedO.setLong(attribute, contractor.get(updatedO.getLong(attribute)));
            updatedDataset.addDataObject(updatedO);
        }

        return updatedDataset;
    }
    
    /** Returns a new instance of a ContractedDataset where the old contract is updated
     * by an explictly given contractor C to deal with the new datatype (Long) of the given set of attributes.
     * @param <C>
     * @param attributes
     * @param contractor
     * @return 
     */
    public <C extends TypeContractor<Long>> ContractedDataset asLong(Set<String> attributes, C contractor)
    {
        ContractedDataset updatedDataset = null;
        
        for(String a: attributes)
        {
            if(updatedDataset == null)
                updatedDataset = asLong(a, contractor);
            else
                updatedDataset = updatedDataset.asLong(a, contractor);
        }

        return updatedDataset;
    }
    
    /**
     * Returns a new instance of a ContractedDataset where the old contract is updated
     * to deal with the new datatype (Boolean) of the given attribute.
     * The new contractor is defaulted to TypeContractorFactory.BOOLEAN.
     * @param attribute
     * @return 
     */
    @Override
    public ContractedDataset asBoolean(String attribute)
    {               
        return asBoolean(attribute, TypeContractorFactory.BOOLEAN);
    }
    
    /**
     * Returns a new instance of a ContractedDataset where the old contract is updated
     * by an explictly given contractor C to deal with the new datatype (Boolean) of the given attribute.
     * @param <C> A type of contractor for Booleans
     * @param attribute
     * @param contractor The new contractor for the attribute
     * @return 
     */
    public <C extends TypeContractor<Boolean>> ContractedDataset asBoolean(String attribute, C contractor)
    {
        ContractedDataset updatedDataset = initForConversion(attribute, contractor);

        for (DataObject o: this) {
            DataObject updatedO = new DataObject(o);
            updatedO.asBoolean(attribute);
            updatedO.setBoolean(attribute, contractor.get(updatedO.getBoolean(attribute)));
            updatedDataset.addDataObject(updatedO);
        }

        return updatedDataset;
    }

    /** Returns a new instance of a ContractedDataset where the old contract is updated
     * by an explictly given contractor C to deal with the new datatype (Boolean) of the given set of attributes.
     * @param <C>
     * @param attributes
     * @param contractor
     * @return 
     */
    public <C extends TypeContractor<Boolean>> ContractedDataset asBoolean(Set<String> attributes, C contractor)
    {
        ContractedDataset updatedDataset = null;
        
        for(String a: attributes)
        {
            if(updatedDataset == null)
                updatedDataset = asBoolean(a, contractor);
            else
                updatedDataset = updatedDataset.asBoolean(a, contractor);
        }

        return updatedDataset;
    }
    
    /**
     * Returns a new instance of a ContractedDataset where the old contract is updated
     * to deal with the new datatype (Double) of the given attribute.
     * @param attribute
     * @return 
     */
    @Override
    public ContractedDataset asDouble(String attribute)
    {          
        return asDouble(attribute, TypeContractorFactory.DOUBLE);
    }
    
    public <C extends TypeContractor<Double>> ContractedDataset asDouble(String attribute, C contractor)
    {        
        ContractedDataset updatedDataset = initForConversion(attribute, contractor);

        for (DataObject o: this)
        {
            DataObject updatedO = new DataObject(o);
            updatedO.asDouble(attribute);
            updatedO.setDouble(attribute, contractor.get(updatedO.getDouble(attribute)));
            updatedDataset.addDataObject(updatedO);
        }

        return updatedDataset;
    }
    
    /** Returns a new instance of a ContractedDataset where the old contract is updated
     * by an explictly given contractor C to deal with the new datatype (Double) of the given set of attributes.
     * @param <C>
     * @param attributes
     * @param contractor
     * @return 
     */
    public <C extends TypeContractor<Double>> ContractedDataset asDouble(Set<String> attributes, C contractor)
    {
        ContractedDataset updatedDataset = null;
        
        for(String a: attributes)
        {
            if(updatedDataset == null)
                updatedDataset = asDouble(a, contractor);
            else
                updatedDataset = updatedDataset.asDouble(a, contractor);
        }

        return updatedDataset;
    }
    
    /**
     * Returns a new instance of a ContractedDataset where the old contract is updated
     * to deal with the new datatype (Float) of the given attribute.
     * @param attribute
     * @return 
     */
    @Override
    public ContractedDataset asFloat(String attribute)
    {
        return asFloat(attribute, TypeContractorFactory.FLOAT);
    }
            
    public <C extends TypeContractor<Float>> ContractedDataset asFloat(String attribute, C contractor)
    {
        ContractedDataset updatedDataset = initForConversion(attribute, contractor);

        for (DataObject o: this) {
            DataObject updatedO = new DataObject(o);
            updatedO.asFloat(attribute);
            updatedO.setFloat(attribute, contractor.get(updatedO.getFloat(attribute)));
            updatedDataset.addDataObject(updatedO);
        }

        return updatedDataset;
    }
    
    /** Returns a new instance of a ContractedDataset where the old contract is updated
     * by an explictly given contractor C to deal with the new datatype (Float) of the given set of attributes.
     * @param <C>
     * @param attributes
     * @param contractor
     * @return 
     */
    public <C extends TypeContractor<Float>> ContractedDataset asFloat(Set<String> attributes, C contractor)
    {
        ContractedDataset updatedDataset = null;
        
        for(String a: attributes)
        {
            if(updatedDataset == null)
                updatedDataset = asFloat(a, contractor);
            else
                updatedDataset = updatedDataset.asFloat(a, contractor);
        }

        return updatedDataset;
    }
    
    /**
     * Returns a new instance of a ContractedDataset where the old contract is updated
     * to deal with the new datatype (BigDecimal) of the given attribute.
     * @param attribute
     * @return 
     */
    @Override
    public ContractedDataset asBigDecimal(String attribute)
    {
        return asBigDecimal(attribute, TypeContractorFactory.BIGDECIMAL);
    }
            
    public <C extends TypeContractor<BigDecimal>> ContractedDataset asBigDecimal(String attribute, C contractor)
    {
        ContractedDataset updatedDataset = initForConversion(attribute, contractor);

        for (DataObject o: this) {
            DataObject updatedO = new DataObject(o);
            updatedO.asBigDecimal(attribute);
            updatedO.setBigDecimal(attribute, contractor.get(updatedO.getBigDecimal(attribute)));
            updatedDataset.addDataObject(updatedO);
        }

        return updatedDataset;
    }
    
    /** Returns a new instance of a ContractedDataset where the old contract is updated
     * by an explictly given contractor C to deal with the new datatype (BigDecimal) of the given set of attributes.
     * @param <C>
     * @param attributes
     * @param contractor
     * @return 
     */
    public <C extends TypeContractor<BigDecimal>> ContractedDataset asBigDecimal(Set<String> attributes, C contractor)
    {
        ContractedDataset updatedDataset = null;
        
        for(String a: attributes)
        {
            if(updatedDataset == null)
                updatedDataset = asBigDecimal(a, contractor);
            else
                updatedDataset = updatedDataset.asBigDecimal(a, contractor);
        }

        return updatedDataset;
    }
    
    /**
     * Returns a new instance of a ContractedDataset where the old contract is updated
     * to deal with the new datatype (LocalDate) of the given attribute.
     * @param attribute
     * @return 
     */
    @Override
    public ContractedDataset asDate(String attribute)
    {
        return asDate(attribute, TypeContractorFactory.DATE);
    }
    
    @Override
    public ContractedDataset asDate(String attribute, String pattern)
    {
        return asDate(attribute, TypeContractorFactory.DATE, pattern);
    }
            
    public <C extends TypeContractor<LocalDate>> ContractedDataset asDate(String attribute, C contractor)
    {
        ContractedDataset updatedDataset = initForConversion(attribute, contractor);

        for (DataObject o: this) {
            DataObject updatedO = new DataObject(o);
            updatedO.asDate(attribute);
            updatedO.setDate(attribute, contractor.get(updatedO.getDate(attribute)));
            updatedDataset.addDataObject(updatedO);
        }

        return updatedDataset;
    }
    
    /** Returns a new instance of a ContractedDataset where the old contract is updated
     * by an explictly given contractor C to deal with the new datatype (LocalDate) of the given set of attributes.
     * @param <C>
     * @param attributes
     * @param contractor
     * @return 
     */
    public <C extends TypeContractor<LocalDate>> ContractedDataset asDate(Set<String> attributes, C contractor)
    {
        ContractedDataset updatedDataset = null;
        
        for(String a: attributes)
        {
            if(updatedDataset == null)
                updatedDataset = asDate(a, contractor);
            else
                updatedDataset = updatedDataset.asDate(a, contractor);
        }

        return updatedDataset;
    }
    
    public <C extends TypeContractor<LocalDate>> ContractedDataset asDate(String attribute, C contractor, String pattern)
    {
        ContractedDataset updatedDataset = initForConversion(attribute, contractor);

        for (DataObject o: this) {
            DataObject updatedO = o.asDate(attribute, pattern);
            updatedO.setDate(attribute, contractor.get(updatedO.getDate(attribute)));
            updatedDataset.addDataObject(updatedO);
        }

        return updatedDataset;
    }
    
    /** Returns a new instance of a ContractedDataset where the old contract is updated
     * by an explictly given contractor C to deal with the new datatype (LocalDate) of the given set of attributes.
     * @param <C>
     * @param attributes
     * @param contractor
     * @param pattern
     * @return 
     */
    public <C extends TypeContractor<LocalDate>> ContractedDataset asDate(Set<String> attributes, C contractor, String pattern)
    {
        ContractedDataset updatedDataset = null;
        
        for(String a: attributes)
        {
            if(updatedDataset == null)
                updatedDataset = asDate(a, contractor, pattern);
            else
                updatedDataset = updatedDataset.asDate(a, contractor, pattern);
        }

        return updatedDataset;
    }
    
    /**
     * Returns a new instance of a ContractedDataset where the old contract is updated
     * to deal with the new datatype (LocalTime) of the given attribute.
     * @param attribute
     * @return 
     */
    @Override
    public ContractedDataset asTime(String attribute)
    {
        return asTime(attribute, TypeContractorFactory.TIME);
    }
            
    public <C extends TypeContractor<LocalTime>> ContractedDataset asTime(String attribute, C contractor)
    {
        ContractedDataset updatedDataset = initForConversion(attribute, contractor);

        for (DataObject o: this) {
            DataObject updatedO = new DataObject(o);
            updatedO.asTime(attribute);
            updatedO.setTime(attribute, contractor.get(updatedO.getTime(attribute)));
            updatedDataset.addDataObject(updatedO);
        }

        return updatedDataset;
    }

    /** Returns a new instance of a ContractedDataset where the old contract is updated
     * by an explictly given contractor C to deal with the new datatype (LocalTime) of the given set of attributes.
     * @param <C>
     * @param attributes
     * @param contractor
     * @return 
     */
    public <C extends TypeContractor<LocalTime>> ContractedDataset asTime(Set<String> attributes, C contractor)
    {
        ContractedDataset updatedDataset = null;
        
        for(String a: attributes)
        {
            if(updatedDataset == null)
                updatedDataset = asTime(a, contractor);
            else
                updatedDataset = updatedDataset.asTime(a, contractor);
        }

        return updatedDataset;
    }
    
    /**
     * Returns a new instance of a ContractedDataset where the old contract is updated
     * to deal with the new datatype (LocalDateTime) of the given attribute.
     * @param attribute
     * @return 
     */
    @Override
    public ContractedDataset asDateTime(String attribute)
    {
        return asDateTime(attribute, TypeContractorFactory.DATETIME);
    }
            
    public <C extends TypeContractor<LocalDateTime>> ContractedDataset asDateTime(String attribute, C contractor)
    {
        ContractedDataset updatedDataset = initForConversion(attribute, contractor);

        for (DataObject o: this) {
            DataObject updatedO = new DataObject(o);
            updatedO.asDateTime(attribute);
            updatedO.setDateTime(attribute, contractor.get(updatedO.getDateTime(attribute)));
            updatedDataset.addDataObject(updatedO);
        }

        return updatedDataset;
    }
    
    /** Returns a new instance of a ContractedDataset where the old contract is updated
     * by an explictly given contractor C to deal with the new datatype (LocalDateTime) of the given set of attributes.
     * @param <C>
     * @param attributes
     * @param contractor
     * @return 
     */
    public <C extends TypeContractor<LocalDateTime>> ContractedDataset asDateTime(Set<String> attributes, C contractor)
    {
        ContractedDataset updatedDataset = null;
        
        for(String a: attributes)
        {
            if(updatedDataset == null)
                updatedDataset = asDateTime(a, contractor);
            else
                updatedDataset = updatedDataset.asDateTime(a, contractor);
        }

        return updatedDataset;
    }
    
    /**
     * Returns a new instance of a ContractedDataset where the old contract is updated
     * to deal with the new datatype (LocalDateTime) of the given attribute.
     * @param attribute
     * @param pattern The datetime pattern used for conversion.
     * @return 
     */
    @Override
    public ContractedDataset asDateTime(String attribute, String pattern)
    {
        return asDateTime(attribute, TypeContractorFactory.DATETIME, pattern);
    }
            
    public <C extends TypeContractor<LocalDateTime>> ContractedDataset asDateTime(String attribute, C contractor, String pattern)
    {
        ContractedDataset updatedDataset = initForConversion(attribute, contractor);

        for (DataObject o: this) {
            DataObject updatedO = new DataObject(o);
            updatedO.asDateTime(attribute, pattern);
            updatedO.setDateTime(attribute, contractor.get(updatedO.getDateTime(attribute)));
            updatedDataset.addDataObject(updatedO);
        }

        return updatedDataset;
    }
    
    /** Returns a new instance of a ContractedDataset where the old contract is updated
     * by an explictly given contractor C to deal with the new datatype (LocalDateTime) of the given set of attributes.
     * @param <C>
     * @param attributes
     * @param contractor
     * @param pattern
     * @return 
     */
    public <C extends TypeContractor<LocalDateTime>> ContractedDataset asDateTime(Set<String> attributes, C contractor, String pattern)
    {
        ContractedDataset updatedDataset = null;
        
        for(String a: attributes)
        {
            if(updatedDataset == null)
                updatedDataset = asDateTime(a, contractor, pattern);
            else
                updatedDataset = updatedDataset.asDateTime(a, contractor, pattern);
        }

        return updatedDataset;
    }
    
    public static ContractedDataset create(Dataset dataset, Contract contract)
    {
        ContractedDataset cd = new ContractedDataset(contract);
        
        dataset
            .stream()
            .forEach(cd::addDataObject);
        
        return cd;
    }
}
