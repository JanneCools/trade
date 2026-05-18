package be.ugent.ledc.core.dataset;

import be.ugent.ledc.core.DataException;
import be.ugent.ledc.core.dataset.contractors.TypeContractorFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An implementation of a key-value type object that is backed by a HashMap.
 * The DataObject is intrinsically uncontracted and fetching data requires testing via
 * the designated testers in order to avoid DataExceptions to be thrown.
 * 
 * Note that not any object can be added as a value for a DataObject: only objects for which
 * a designated setters is defined can be added. Other object type will result in an exception to be thrown.
 * @author abronsel
 */
public class DataObject
{
    /**
     * The map that backs this data object a maps each attribute name to a value.
     */
    private final Map<String, Object> keyValueMap;
    
    /**
     * A flag that indicate if the names of the attributes are case sensitive.
     */
    private final boolean caseSensitiveNames;

    /**
     * An empty constructor that initiates an empty object.
     */
    public DataObject()
    {
        this(false);
    }
    
    /**
     * A constructor that initiates an empty object, but sets the flag for case-sensitivity of attribute names.
     * @param caseSensitiveNames The desired value for the case sensitive names flag.
     */
    public DataObject(boolean caseSensitiveNames)
    {
        this.keyValueMap = new TreeMap<>();
        this.caseSensitiveNames = caseSensitiveNames;
    }
    
    /**
     * A constructor that sets the flag for case-sensitivity of attribute names and immediately
     * sets some attribute values. 
     * @param keyValueMap The initial attributes and their values.
     * @param caseSensitiveNames The desired value for the case sensitive names flag.
     */
    public DataObject(Map<String, Object> keyValueMap, boolean caseSensitiveNames)
    {
        this.keyValueMap = new TreeMap<>();
        this.caseSensitiveNames = caseSensitiveNames;
        keyValueMap.entrySet().forEach(e -> set(e.getKey(), e.getValue()));
    }
    
    /**
     * A copy constructor that copies the attributes and values of the given object.
     * @param dataObject The object that is copied from.
     */
    public DataObject(DataObject dataObject)
    {
        this.keyValueMap = new TreeMap<>();
        
        dataObject
        .getAttributes()
        .stream()
        .forEach(a -> keyValueMap.put(a, dataObject.get(a)));
        
        this.caseSensitiveNames = dataObject.isCaseSensitiveNames();
    }

    /**
     * The getter for the case sensitive names flag.
     * @return True if this object has case sensitive attribute names.
     */
    public final boolean isCaseSensitiveNames()
    {
        return caseSensitiveNames;
    }
    
    /**
     * Gets the value
     * @param attribute The attribute for which get the value.
     * @return 
     */
    public Object get(String attribute)
    {
        return caseSensitiveNames ? this.keyValueMap.get(attribute) : this.keyValueMap.get(attribute.toLowerCase());
    }

    public String getString(String attribute) throws DataException
    {
        Object value = get(attribute);
        
        if(value == null)
            return null;
        if(value instanceof String string)
            return string;
        throw new DataException("Attribute '" + attribute + "' is not of type String.");
    }
    
    public Long getLong(String attribute) throws DataException
    {
        Object value = get(attribute);
        
        if(value == null)
            return null;
        if(value instanceof Long long1)
            return long1;
        throw new DataException("Attribute '" + attribute + "' is not of type Long.");
    }

    public Integer getInteger(String attribute) throws DataException
    {
        Object value = get(attribute);
        
        if(value == null)
            return null;
        if(value instanceof Integer integer)
            return integer;
        throw new DataException("Attribute '" + attribute + "' is not of type Integer.");
    }
    
    public Boolean getBoolean(String attribute) throws DataException
    {
        Object value = get(attribute);
        
        if(value == null)
            return null;
        if(value instanceof Boolean boolean1)
            return boolean1;
        throw new DataException("Attribute '" + attribute + "' is not of type Boolean.");
    }
    
    public Double getDouble(String attribute) throws DataException
    {
        Object value = get(attribute);
        
        if(value == null)
            return null;
        if(value instanceof Double double1)
            return double1;
        throw new DataException("Attribute '" + attribute + "' is not of type Double.");
    }
    
    public BigDecimal getBigDecimal(String attribute) throws DataException
    {
        Object value = get(attribute);
        
        if(value == null)
            return null;
        if(value instanceof BigDecimal bigDecimal)
            return bigDecimal;
        throw new DataException("Attribute '" + attribute + "' is not of type BigDecimal.");
    }
    
    public Float getFloat(String attribute) throws DataException
    {
        Object value = get(attribute);
        
        if(value == null)
            return null;
        if(value instanceof Float float1)
            return float1;
        throw new DataException("Attribute '" + attribute + "' is not of type Float.");
    }
    
    public LocalDate getDate(String attribute) throws DataException
    {
        Object value = get(attribute);
        
        if(value == null)
            return null;
        if(value instanceof LocalDate localDate)
            return localDate;
        throw new DataException("Attribute '" + attribute + "' is not of type LocalDate.");
    }
    
    public LocalTime getTime(String attribute) throws DataException
    {
        Object value = get(attribute);
        
        if(value == null)
            return null;
        if(value instanceof LocalTime localTime)
            return localTime;
        throw new DataException("Attribute '" + attribute + "' is not of type LocalTime.");
    }
    
    public LocalDateTime getDateTime(String attribute) throws DataException
    {
        Object value = get(attribute);
        
        if(value == null)
            return null;
        if(value instanceof LocalDateTime localDateTime)
            return localDateTime;
        throw new DataException("Attribute '" + attribute + "' is not of type LocalDateTime.");
    }
    
    public Duration getDuration(String attribute) throws DataException
    {
        Object value = get(attribute);
        
        if(value == null)
            return null;
        if(value instanceof Duration duration)
            return duration;
        throw new DataException("Attribute '" + attribute + "' is not of type Duration.");
    }
    
    public Period getPeriod(String attribute) throws DataException
    {
        Object value = get(attribute);
        
        if(value == null)
            return null;
        if(value instanceof Period period)
            return period;
        throw new DataException("Attribute '" + attribute + "' is not of type Period.");
    }
    
    public Instant getInstant(String attribute) throws DataException
    {
        Object value = get(attribute);
        
        if(value == null)
            return null;
        if(value instanceof Instant instant)
            return instant;
        throw new DataException("Attribute '" + attribute + "' is not of type Instant.");
    }
    
    public List getList(String attribute) throws DataException
    {
        Object value = get(attribute);
        
        if(value == null)
            return null;
        if(value instanceof List list)
            return list;
        throw new DataException("Attribute '" + attribute + "' is not of type List.");
    }
    
    public DataObject getDataObject(String attribute) throws DataException
    {
        Object value = get(attribute);
        
        if(value == null)
            return null;
        if(value instanceof DataObject dataObject)
            return dataObject;
        throw new DataException("Attribute '" + attribute + "' is not of type DataObject.");
    }
    
    public DataObject drop(String attribute)
    {
        if(caseSensitiveNames)
            this.keyValueMap.remove(attribute);
        else
            this.keyValueMap.remove(attribute.toLowerCase());
        
        return this;
    }
    
    public DataObject set(String attribute, Object o) throws DataException
    {
        if (TypeContractorFactory.getTypedContractors().anyMatch(contractor -> contractor.test(o)))
        {
            this.keyValueMap.put(caseSensitiveNames?attribute:attribute.toLowerCase(), o);
        }
        else
        {
            throw new DataException("The value for attribute '" + attribute + "' is outside the range of permitted types.");
        }
        
        return this;
    }
    
    public DataObject extend(String attribute, Function<DataObject, Object> derivator) throws DataException
    {
        //Compute
        Object o = derivator.apply(this);
        
        //set
        set(attribute, o);
        
        //return
        return this;
    }
    
    public DataObject setString(String attribute, String stringValue)
    {
        this.keyValueMap.put(caseSensitiveNames?attribute:attribute.toLowerCase(), stringValue);
        return this;
    }
    
    public DataObject setLong(String attribute, Long longValue)
    {
        this.keyValueMap.put(caseSensitiveNames?attribute:attribute.toLowerCase(), longValue);
        return this;
    }
    
    public DataObject setInteger(String attribute, Integer integerValue)
    {
        this.keyValueMap.put(caseSensitiveNames?attribute:attribute.toLowerCase(), integerValue);
        return this;
    }
    
    public DataObject setBoolean(String attribute, Boolean booleanValue)
    {
        this.keyValueMap.put(caseSensitiveNames?attribute:attribute.toLowerCase(), booleanValue);
        return this;
    }
    
    public DataObject setDouble(String attribute, Double doubleValue)
    {
        this.keyValueMap.put(caseSensitiveNames?attribute:attribute.toLowerCase(), doubleValue);
        return this;
    }
    
    public DataObject setBigDecimal(String attribute, BigDecimal bigDecimalValue)
    {
        this.keyValueMap.put(caseSensitiveNames?attribute:attribute.toLowerCase(), bigDecimalValue);
        return this;
    }
    
    public DataObject setFloat(String attribute, Float floatValue)
    {
        this.keyValueMap.put(caseSensitiveNames?attribute:attribute.toLowerCase(), floatValue);
        return this;
    }
    
    public DataObject setDate(String attribute, LocalDate localDateValue)
    {
        this.keyValueMap.put(caseSensitiveNames?attribute:attribute.toLowerCase(), localDateValue);
        return this;
    }
    
    public DataObject setTime(String attribute, LocalTime localTimeValue)
    {
        this.keyValueMap.put(caseSensitiveNames?attribute:attribute.toLowerCase(), localTimeValue);
        return this;
    }
    
    public DataObject setDateTime(String attribute, LocalDateTime localDateTimeValue)
    {
        this.keyValueMap.put(caseSensitiveNames?attribute:attribute.toLowerCase(), localDateTimeValue);
        return this;
    }
    
    public DataObject setDuration(String attribute, Duration durationValue)
    {
        this.keyValueMap.put(caseSensitiveNames?attribute:attribute.toLowerCase(), durationValue);
        return this;
    }
    
    public DataObject setPeriod(String attribute, Period periodValue)
    {
        this
            .keyValueMap
            .put(
                caseSensitiveNames
                    ? attribute
                    : attribute.toLowerCase(),
                periodValue
            );
        return this;
    }
    
    public DataObject getInstant(String attribute, Instant instantValue)
    {
        this.keyValueMap.put(caseSensitiveNames?attribute:attribute.toLowerCase(), instantValue);
        return this;
    }
    
    public DataObject setList(String attribute, List listValue)
    {
        this.keyValueMap.put(caseSensitiveNames?attribute:attribute.toLowerCase(), listValue);
        return this;
    }
    
    public DataObject setDataObject(String attribute, DataObject dataObjectValue)
    {
        this.keyValueMap.put(caseSensitiveNames?attribute:attribute.toLowerCase(), dataObjectValue);
        return this;
    }
    
    public boolean isString(String attribute)
    {
        return get(attribute) instanceof String;
    }
    
    public boolean isLong(String attribute)
    {
        return get(attribute) instanceof Long;
    }
    
    public boolean isInteger(String attribute)
    {
        return get(attribute) instanceof Integer;
    }
    
    public boolean isBoolean(String attribute)
    {
        return get(attribute) instanceof Boolean;
    }
    
    public boolean isDouble(String attribute)
    {
        return get(attribute) instanceof Double;
    }
    
    public boolean isBigDecimal(String attribute)
    {
        return get(attribute) instanceof BigDecimal;
    }
    
    public boolean isFloat(String attribute)
    {
        return get(attribute) instanceof Float;
    }
    
    public boolean isDate(String attribute)
    {
        return get(attribute) instanceof LocalDate;
    }
    
    public boolean isTime(String attribute)
    {
        return get(attribute) instanceof LocalTime;
    }
    
    public boolean isDateTime(String attribute)
    {
        return get(attribute) instanceof LocalDateTime;
    }
    
    public boolean isDuration(String attribute)
    {
        return get(attribute) instanceof Duration;
    }
    
    public boolean isPeriod(String attribute)
    {
        return get(attribute) instanceof Period;
    }
    
    public boolean isInstant(String attribute)
    {
        return get(attribute) instanceof Instant;
    }
    
    public boolean isList(String attribute)
    {
        return get(attribute) instanceof List;
    }
    
    public boolean isDataObject(String attribute)
    {
        return get(attribute) instanceof DataObject;
    }
  
    public Set<String> getAttributes()
    {
        return this.keyValueMap.keySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataObject that = (DataObject) o;
        return Objects.equals(keyValueMap, that.keyValueMap);
    }

    @Override
    public int hashCode()
    {
        int hash = 0;
        int c=1;
        for(Entry<String, Object> e: keyValueMap.entrySet())
        {
            hash += e.hashCode() * (c++);
        }
        
        return hash;
        //return Objects.hash(keyValueMap);
    }

    @Override
    public String toString()
    {
        return keyValueMap
            .entrySet()
            .stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(",", "{", "}"));
    }
    
    //
    // Conversion of datatypes
    //
    
    public DataObject asString(String attribute)
    {
        if(get(attribute) != null)
            setString(attribute, get(attribute).toString());
        
        return this;
    }
    
    public DataObject asString(Set<String> attributes) throws DataException
    {
        attributes
            .stream()
            .forEach(a -> asString(a));
        return this;
    }
    
    public DataObject allAsString() throws DataException
    {
        return asString(getAttributes());
    }
    
    public DataObject asInteger(String attribute) throws DataException
    {
        if(get(attribute) == null || isInteger(attribute))
            return this;
        
        Integer value = null;
        
        if(isString(attribute))
            value = Integer.valueOf(getString(attribute));
        else if(isLong(attribute))
            value = getLong(attribute).intValue();
        else if(isBigDecimal(attribute))
            value = getBigDecimal(attribute).setScale(0, RoundingMode.HALF_UP).intValue();
        else if(isDouble(attribute))
            value = (int) Math.round(getDouble(attribute));
        else if(isFloat(attribute))
            value = Math.round(getFloat(attribute));
        else if(isBoolean(attribute))
            value = getBoolean(attribute) ? 1 : 0;
        else 
            throw new DataException("Could not convert attribute '" + attribute + "' to an Integer value.");

        setInteger(attribute, value);
        return this;
    }
    
    public DataObject asInteger(Set<String> attributes) throws DataException
    {
        attributes
            .stream()
            .forEach(a -> asInteger(a));
        return this;
    }
    
    public DataObject allAsInteger() throws DataException
    {
        return asInteger(getAttributes());
    }
    
    public DataObject asLong(String attribute) throws DataException
    {
        if(get(attribute) == null || isLong(attribute))
            return this;
        
        Long value = null;
        
        if(isString(attribute))
            value = Long.valueOf(getString(attribute));
        else if(isInteger(attribute))
            value = getInteger(attribute).longValue();
        else if(isBigDecimal(attribute))
            value = getBigDecimal(attribute).setScale(0, RoundingMode.HALF_UP).longValue();
        else if(isDouble(attribute))
            value = Math.round(getDouble(attribute));
        else if(isFloat(attribute))
            value = (long)Math.round(getFloat(attribute));
        else if(isBoolean(attribute))
            value = getBoolean(attribute) ? 1l : 0l;
        else 
            throw new DataException("Could not convert attribute '" + attribute + "' to a Long value.");

        setLong(attribute, value);
        return this;
    }
    
    public DataObject asLong(Set<String> attributes) throws DataException
    {
        attributes
            .stream()
            .forEach(a -> asLong(a));
        return this;
    }
    
    public DataObject allAsLong() throws DataException
    {
        return asLong(getAttributes());
    }
    
    public DataObject asBoolean(String attribute) throws DataException
    {
        if(get(attribute) == null || isBoolean(attribute))
            return this;
        
        Boolean value = null;
        
        if(isString(attribute))
            value = Boolean.valueOf(getString(attribute));
        else if(isInteger(attribute))
            value = getInteger(attribute) == 0;
        else if(isLong(attribute))
            value = getLong(attribute) == 0l;
        else if(isBigDecimal(attribute))
            value = getBigDecimal(attribute).equals(BigDecimal.ZERO);
        else if(isDouble(attribute))
            value = Math.abs(getDouble(attribute)) < 0.0;
        else if(isFloat(attribute))
            value = Math.abs(getFloat(attribute)) < 0f;
        else 
            throw new DataException("Could not convert attribute '" + attribute + "' to a Boolean value.");

        setBoolean(attribute, value);
        return this;
    }

    public DataObject asBoolean(Set<String> attributes) throws DataException
    {
        attributes
            .stream()
            .forEach(a -> asBoolean(a));
        return this;
    }
    
    public DataObject allAsBoolean() throws DataException
    {
        return asBoolean(getAttributes());
    }
    
    public DataObject asDouble(String attribute) throws DataException
    {
        if(get(attribute) == null || isDouble(attribute))
            return this;
        
        Double value = null;
        
        if(isString(attribute))
            value = Double.valueOf(getString(attribute));
        else if(isInteger(attribute))
            value = getInteger(attribute).doubleValue();
        else if(isLong(attribute))
            value = getLong(attribute).doubleValue();
        else if(isBigDecimal(attribute))
            value = getBigDecimal(attribute).doubleValue();
        else if(isFloat(attribute))
            value = getFloat(attribute).doubleValue();
        else if(isBoolean(attribute))
            value = getBoolean(attribute) ? 1.0 : 0.0;
        else 
            throw new DataException("Could not convert attribute '" + attribute + "' to a Double value.");

        setDouble(attribute, value);
        return this;
    }
    
    public DataObject asDouble(Set<String> attributes) throws DataException
    {
        attributes
            .stream()
            .forEach(a -> asDouble(a));
        return this;
    }
    
    public DataObject allAsDouble() throws DataException
    {
        return asDouble(getAttributes());
    }
    
    public DataObject asFloat(String attribute) throws DataException
    {
        if(get(attribute) == null || isFloat(attribute))
            return this;
        
        Float value = null;
        
        if(isString(attribute))
            value = Float.valueOf(getString(attribute));
        else if(isInteger(attribute))
            value = getInteger(attribute).floatValue();
        else if(isLong(attribute))
            value = getLong(attribute).floatValue();
        else if(isBigDecimal(attribute))
            value = getBigDecimal(attribute).floatValue();
        else if(isDouble(attribute))
            value = getDouble(attribute).floatValue();
        else if(isBoolean(attribute))
            value = getBoolean(attribute) ? 1f : 0f;
        else 
            throw new DataException("Could not convert attribute '" + attribute + "' to a Float value.");

        setFloat(attribute, value);
        return this;
    }
    
    public DataObject asFloat(Set<String> attributes) throws DataException
    {
        attributes
            .stream()
            .forEach(a -> asFloat(a));
        return this;
    }
    
    public DataObject allAsFloat() throws DataException
    {
        return asFloat(getAttributes());
    }
    
    public DataObject asBigDecimal(String attribute) throws DataException
    {
        if(get(attribute) == null || isBigDecimal(attribute))
            return this;
        
        BigDecimal value = null;

        if(isString(attribute))
            value = new BigDecimal(getString(attribute));
        else if(isInteger(attribute))
            value = new BigDecimal(getInteger(attribute));
        else if(isLong(attribute))
            value = new BigDecimal(getLong(attribute));
        else if(isDouble(attribute))
            value = BigDecimal.valueOf(getDouble(attribute));
        else if(isFloat(attribute))
            value = BigDecimal.valueOf(getFloat(attribute));
        else if(isBoolean(attribute))
            value = getBoolean(attribute) ? BigDecimal.ONE : BigDecimal.ZERO;
        else 
            throw new DataException("Could not convert attribute '" + attribute + "' to a BigDecimal value.");

        setBigDecimal(attribute, value);
        return this;
    }
    
    public DataObject asBigDecimal(Set<String> attributes) throws DataException
    {
        attributes
            .stream()
            .forEach(a -> asBigDecimal(a));
        return this;
    }
    
    public DataObject allAsBigDecimal() throws DataException
    {
        return asBigDecimal(getAttributes());
    }
    
    public DataObject asDate(String attribute) throws DataException
    {
        if(get(attribute) == null || isDate(attribute))
            return this;
        
        LocalDate value = null;

        if(isString(attribute))
            value = LocalDate.parse(getString(attribute));
        else if(isDateTime(attribute))
            value = getDateTime(attribute).toLocalDate();
        else 
            throw new DataException("Could not convert attribute '" + attribute + "' to a Date value.");

        setDate(attribute, value);
        return this;
    }
    
    public DataObject asDate(Set<String> attributes) throws DataException
    {
        attributes
            .stream()
            .forEach(a -> asDate(a));
        return this;
    }
    
    public DataObject allAsDate() throws DataException
    {
        return asDate(getAttributes());
    }
    
    public DataObject asDate(String attribute, String pattern) throws DataException
    {
        if(get(attribute) == null || isDate(attribute))
            return this;
        
        LocalDate value = null;
        
        if(isString(attribute))
            value = LocalDate.parse(getString(attribute), DateTimeFormatter.ofPattern(pattern));
        else if(isDateTime(attribute))
            value = getDateTime(attribute).toLocalDate();
        else 
            throw new DataException("Could not convert attribute '" + attribute + "' to a Date value.");

        setDate(attribute, value);
        return this;
    }
    
    public DataObject asDate(Set<String> attributes, String pattern) throws DataException
    {
        attributes
            .stream()
            .forEach(a -> asDate(a, pattern));
        return this;
    }
    
    public DataObject allAsDate(String pattern) throws DataException
    {
        return asDate(getAttributes(), pattern);
    }
    
    public DataObject asTime(String attribute) throws DataException
    {
        if(get(attribute) == null || isTime(attribute))
            return this;
        
        LocalTime value = null;

        if(isString(attribute))
            value = LocalTime.parse(getString(attribute));
        else if(isDateTime(attribute))
            value = getDateTime(attribute).toLocalTime();
        else 
            throw new DataException("Could not convert attribute '" + attribute + "' to a Time value.");

        setTime(attribute, value);
        return this;
    }
    
    public DataObject asTime(Set<String> attributes) throws DataException
    {
        attributes
            .stream()
            .forEach(a -> asTime(a));
        return this;
    }
    
    public DataObject allAsTime() throws DataException
    {
        return asTime(getAttributes());
    }
    
    public DataObject asDateTime(String attribute) throws DataException
    {
        if(get(attribute) == null || isDateTime(attribute))
            return this;
        
        LocalDateTime value = null;

        if(isString(attribute))
            try
            {
                value = LocalDateTime.parse(getString(attribute));
            }
            catch(DateTimeParseException ex)
            {
                throw new DataException(ex);
            }
        else if(isDate(attribute))
            value = getDate(attribute).atTime(0, 0);
        else 
            throw new DataException("Could not convert attribute '" + attribute + "' to a DateTime value.");

        setDateTime(attribute, value);
        return this;
    }
    
    public DataObject asDateTime(Set<String> attributes) throws DataException
    {
        attributes
            .stream()
            .forEach(a -> asDateTime(a));
        return this;
    }
    
    public DataObject allAsDateTime() throws DataException
    {
        return asDateTime(getAttributes());
    }
    
    public DataObject asDateTime(String attribute, String pattern) throws DataException
    {
        if(get(attribute) == null || isDateTime(attribute))
            return this;
        LocalDateTime value = null;

        if(isString(attribute))
            value = LocalDateTime.parse(getString(attribute), DateTimeFormatter.ofPattern(pattern));
        else 
            throw new DataException("Could not convert attribute '" + attribute + "' to a DateTime value.");

        setDateTime(attribute, value);
        return this;
    }
    
    public DataObject asDateTime(Set<String> attributes, String pattern) throws DataException
    {
        attributes
            .stream()
            .forEach(a -> asDateTime(a, pattern));
        return this;
    }
    
    public DataObject allAsDateTime(String pattern) throws DataException
    {
        return asDateTime(pattern);
    }
    
    //
    // Operators
    //
    
    /**
     * Returns a projection of this object over the given attributes.
     * @param attributes
     * @return 
     */
    public DataObject project(Collection<String> attributes)
    {
        DataObject projection = new DataObject(isCaseSensitiveNames());
        
        for(String a: attributes)
            projection.set(a, get(a));
        
        return projection;
    }
    
    public DataObject project(String ... attributes)
    {
        return project(Stream.of(attributes).collect(Collectors.toSet()));
    }
    
    /**
     * Returns a projection of this object that does not contain the given attributes.
     * @param attributes
     * @return 
     */
    public DataObject inverseProject(Set<String> attributes)
    {
        return project(
            getAttributes()
            .stream()
            .filter(a -> !attributes.contains(a))
            .collect(Collectors.toSet())
        );
    }
    
    public DataObject inverseProject(String ... attributes)
    {
        return inverseProject(Stream.of(attributes).collect(Collectors.toSet()));
    }
    
    /**
     * Makes a concatenation of this object with the other object. This is done
     * by taking the current object, and then set all attributes of the other objects.
     * In particular, this means that, if the current and other object have overlap
     * in their attributes, the concatenation will take the attribute values of the
     * other object. In other words, the method 'concat' does not reflect a symmetric operation!
     * @param other
     * @return 
     */
    public DataObject concat(DataObject other)
    {
        DataObject conc = new DataObject(caseSensitiveNames & other.isCaseSensitiveNames());
        
        for(String attribute: getAttributes())
        {
            conc.set(attribute, get(attribute));
        }
        
        for(String otherAttribute: other.getAttributes())
        {
            conc.set(otherAttribute, other.get(otherAttribute));
        }
        
        return conc;
    }
    
    //
    // Some tests that can be useful in comparing objects with each other
    //
    public boolean isProjectionOf(DataObject other)
    {
        return equals(other.project(getAttributes()));
    }
    
    /**
     * Tests if this object subsumes another object. This is true if for each attribute
     * appearing in one of both objects, either the objects have the same value or
     * the other object has a null value.
     * 
     * Subsumption is a concept often used in fusion of datasets in order to remove
     * redundant objects.
     * 
     * Subsumption is not a symmetric operator.
     * @param other
     * @return 
     */
    public boolean subsumes(DataObject other)
    {
        return other == null || getAttributes()
            .stream()
            .allMatch(a -> other.get(a) == null || other.get(a).equals(get(a)));
    }
    
    /**
     * Tests if this object is subsumed by another object. This is true if for each attribute
     * appearing in one of both objects, either the objects have the same value or
     * the this object has a null value.
     * 
     * Subsumption is a concept often used in fusion of datasets in order to remove
     * redundant objects.
     * 
     * Subsumption is not a symmetric operator.
     * @param other
     * @return 
     */
    public boolean isSubsumedBy(DataObject other)
    {
        return other != null && other.subsumes(this);
    }
    
    /**
     * Tests if this object is complemented by another object. This is true if for each attribute
     * appearing in one of both objects, either the objects have the same value or
     * one of them has a null value.
     * 
     * Complementation is a concept often used in fusion of datasets in order to remove
     * redundant objects.
     * 
     * Complementation is a symmetric operator.
     * @param other
     * @return 
     */
    public boolean complements(DataObject other)
    {
        return other == null
            ||
            Stream.concat(
                getAttributes().stream(),
                other.getAttributes().stream()
            )
            .distinct()
            .allMatch(a -> get(a) == null || other.get(a) == null || get(a).equals(other.get(a)));
    }
    
    /**
     * Computes the set of attributes that have different values between two objects
     * @param other
     * @return 
     */
    public Set<String> diff(DataObject other)
    {
        return other == null
            ? getAttributes()
            : Stream.concat(
                getAttributes().stream(),
                other.getAttributes().stream()
            )
            .distinct()
            .filter(a -> (get(a) == null && other.get(a) != null) || (get(a) != null && !get(a).equals(other.get(a))))
            .collect(Collectors.toSet());
    }

    /**
     * Computes the set of attributes from given diffAttributes that have different values between two objects
     * @param other
     * @param diffAttributes
     * @return
     * @throws DataException
     */
    public Set<String> diff(DataObject other, Set<String> diffAttributes) throws DataException {

        if (!this.getAttributes().containsAll(diffAttributes)) {
            throw new DataException("Data object " + this + " does not contain all given diff attributes " + diffAttributes);
        }

        if (!other.getAttributes().containsAll(diffAttributes)) {
            throw new DataException("Data object " + other + " does not contain all given diff attributes " + diffAttributes);
        }

        return this.diff(other).stream().filter(diffAttributes::contains).collect(Collectors.toSet());

    }
    
    public DataObject rename(String oldAttributeName, String newAttributeName)
    {
        if(newAttributeName == null || newAttributeName.equals(oldAttributeName))
            return this;
        
        if(get(newAttributeName) != null)
            throw new DataException("Cannot rename attribute: attributename '" + newAttributeName + "' already exists");

        set(newAttributeName, get(oldAttributeName));
        drop(oldAttributeName);
        
        return this;
    }
    
    public static Comparator<DataObject> getProjectionComparator(String... attributes)
    {
        return getProjectionComparator(Stream.of(attributes).collect(Collectors.toList()));
    }
    
    public static Comparator<DataObject> getProjectionComparator(List<String> attributes)
    {
        List<Comparator<DataObject>> basicComparators = new ArrayList<>();
        
        for(String a: attributes)
        {
            basicComparators
                .add(
                    Comparator.nullsLast(
                        Comparator.comparing(
                            (o) -> (Comparable)o.get(a),
                            Comparator.nullsLast(Comparator.naturalOrder())
                        )
                    )
                );
        }
        
        //Return a chain of comparators or else, throw a DataException
        return basicComparators
            .stream()
            .reduce(Comparator::thenComparing)
            .orElseThrow(() -> new DataException("Could not compose a projection comparator for attributes " + attributes));
    }
}
