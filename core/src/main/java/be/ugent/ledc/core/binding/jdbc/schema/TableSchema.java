package be.ugent.ledc.core.binding.jdbc.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TableSchema extends HashMap<String, Map<String,Object>>
{
    public static final String PROP_IS_KEY_ATTRIBUTE = "is_key_attribute";
    public static final String PROP_DATATYPE = "datatype";
    public static final String PROP_DATATYPE_LENGTH = "datatype_length";
    public static final String PROP_DATATYPE_DISPLAY_LENGTH = "datatype_display_length";
    public static final String PROP_DATATYPE_PRECISION = "datatype_precision";
    public static final String PROP_DATATYPE_SCALE = "datatype_scale";
    public static final String PROP_DATATYPE_RADIX = "datatype_radix";
    public static final String PROP_IS_INDEXED = "is_indexed";
    public static final String PROP_IS_NULLABLE = "is_nullable";
    public static final String PROP_IS_CHECKED = "is_checked";
    public static final String PROP_IS_UNIQUE = "is_unique";
    public static final String PROP_IS_AUTOINCREMENT = "is_autoincrement";
    public static final String PROP_COMMENT = "comment";
    public static final String PROP_DEFAULT_VALUE = "default_value";
    
    /**
     * Row count
     */
    private long rowCount;

    /**
     * The name of the TableSchema.
     */
    private String name;

    /**
     * A list of References
     */
    private List<Reference> references;
    
    /**
     * A list of check constraints
     */
    private final List<CheckConstraint> checkConstraints;
    
    /**
     * A list of unique constraints
     */
    private final Map<String,Set<String>> uniqueConstraints;
    
    /**
     * Type of the tableSchema.
     */
    private String type;
    
    private String logicalType;

    /**
     * Empty constructor.
     */
    public TableSchema()
    {
        references = new ArrayList<>();
        checkConstraints = new ArrayList<>();
        uniqueConstraints = new HashMap<>();
    }

    /**
     * Gets the name of the TableSchema
     * @return Name of the {@link TableSchema}
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the name of the TableSchema
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Gets the {@link Key} of the {@link TableSchema}
     * @return {@link Key} of the {@link TableSchema}
     */
    public Key getPrimaryKey()
    {
        //Construct a key
        Key primaryKey = new Key();

        //Iterate
        for(String attributeName: keySet())
        {
            if(isKeyAttribute(attributeName))
            {
                primaryKey.add(attributeName);
            }
        }

        return primaryKey;
    }

    /**
     * Sets the {@link Key} of the {@link TableSchema}
     */
    public void setPrimaryKey(Key primaryKey)
    {
        //Get iterator
        Iterator<String> attributeIterator = keySet().iterator();

        //Iterate
        while(attributeIterator.hasNext())
        {
            //Fetch
            String attributeName = attributeIterator.next();

            //Check if key attribute
            boolean isKeyAttribute = primaryKey.contains(attributeName);

            //Indicate whether this attribute is a key attribute
            get(attributeName).put(PROP_IS_KEY_ATTRIBUTE, isKeyAttribute);
        }
    }

    /**
     * Adds an attributeName and the string representation of the datatype as described in the metadata of the {@link Schema}.
     */
    public void setAttributeProperty(String attributeName, String propertyName, Object propertyValue)
    {
        //Get properties
        Map<String,Object> props = get(attributeName);

        if(props == null)
        {
            props = new HashMap<>();
        }

        //Add property
        if(propertyValue == null)
        {
            props.put(propertyName, null);
        }
        else
        {
            props.put(propertyName, propertyValue);
        }

        put(attributeName, props);
    }

    public Object getAttributeProperty(String attributeName, String propertyName)
    {
        //Get properties
        Map<String,Object> props = get(attributeName);

        //Check if not null
        if(props == null)
        {
            return null;
        }

        //Return value
        return props.get(propertyName);
    }

    /**
     * Gets the set of attribute names.
     * @return {@link Set} of attribute names.
     */
    public Set<String> getAttributeNames()
    {
        return keySet();
    }

    /**
     * Gets the list of References.
     * @return {@link List} of References.
     */
    public List<Reference> getReferences()
    {
        return references;
    }

    /**
     * Sets the list of References.
     */
    public void setReferences(List<Reference> references)
    {
        this.references = references;
    }

    /**
     * Gets the set of referencing attributes.
     * @return {@link Set} of reference attributes.
     */
    public Set<String> getReferencingAttributes()
    {
        Set<String> referencingAttributes = new HashSet<>();

        for (int i = 0; i < references.size(); i++)
        {
            referencingAttributes.addAll(references.get(i).keySet());
        }

        return referencingAttributes;
    }

    /**
     * Returns true if the attribute is part of the {@link Key}
     * @return Returns true if the attribute is part of the {@link Key}
     */
    public boolean isKeyAttribute(String attributeName)
    {
        Map<String,Object> properties = get(attributeName);

        if(!properties.containsKey(PROP_IS_KEY_ATTRIBUTE))
        {
            return false;
        }
        
        return (Boolean)properties.get(PROP_IS_KEY_ATTRIBUTE);
    }

    /**
     * Returns true if the attribute is a referencing attribute.
     * @return Returns true if the attribute is a referencing attribute.
     */
    public boolean isReferencingAttribute(String attributeName)
    {
        if(getReferencingAttributes().contains(attributeName))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Gets the {@link TableSchema} referenced by this attribute.
     * @return {@link TableSchema} referenced by attribute name.
     */
    public TableSchema getReferencedSignature(String attributeName)
    {
        for(int i=0; i<references.size(); i++)
        {
            if(references.get(i).containsKey(attributeName))
            {
                return references.get(i).getReferenced();
            }
        }

        //Attribute name not found
        return null;
    }

    /**
     * Returns true if the given {@link TableSchema} is referenced.
     * @return Returns true if the given {@link TableSchema} is referenced.
     */
    public boolean referencesTo(TableSchema tableSchema)
    {
        //Search in references
        for(int i=0; i<references.size(); i++)
        {
            //Get the next reference
            Reference reference = references.get(i);

            if(reference.getReferenced().equals(tableSchema))
            {
                return true;
            }
        }

        return false;
    }

    public boolean removeReferenceTo(TableSchema tableSchema)
    {
        //Search in references
        for(int i=0; i<references.size(); i++)
        {
            //Get the next reference
            Reference reference = references.get(i);

            if(reference.getReferenced().equals(tableSchema))
            {
                //Remove reference
                references.remove(i);

                //Return true
                return true;
            }
        }

        //tableSchema was not referenced: return false;
        return false;
    }

    /**
     * Gets the attribute that is linked to the referencedAttribute in the referenced TableSchema
     * @return Attribute name linked to the given attribute in the given {@link TableSchema}
     */
    public String getReferencingAttribute(TableSchema tableSchema, String referencedAttribute)
    {
        for(int i=0; i<references.size(); i++)
        {
            //Get next reference
            Reference reference = references.get(i);

            if(reference.getReferenced().equals(tableSchema))
            {
                Iterator<String> rIterator = reference.keySet().iterator();

                while(rIterator.hasNext())
                {
                    String rAttribute = rIterator.next();
                    if(reference.get(rAttribute).equals(referencedAttribute))
                    {
                        return rAttribute;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets all attributes that are linked to the referencedAttribute in the referenced {@link TableSchema}
     * @param tableSchema
     * @param referencedAttribute
     * @return {@link Set} of attributes linked to the given attribute
     */
    public Set<String> getReferencingAttributes(TableSchema tableSchema, String referencedAttribute)
    {
        Set<String> referencingAttributes = new HashSet<>();

        for(int i=0; i<references.size(); i++)
        {
            //Get next reference
            Reference reference = references.get(i);

            if(reference.getReferenced().equals(tableSchema))
            {
                Iterator<String> rIterator = reference.keySet().iterator();

                while(rIterator.hasNext())
                {
                    String rAttribute = rIterator.next();

                    if(reference.get(rAttribute).equals(referencedAttribute))
                    {
                        referencingAttributes.add(rAttribute);
                    }
                }
            }
        }
        return referencingAttributes;
    }

    public List<CheckConstraint> getCheckConstraints()
    {
        return checkConstraints;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o==null) {
            return false;
        }
        if(o instanceof TableSchema)
        {
            //Cast
            TableSchema sign = (TableSchema)o;

            //Return
            return super.equals(sign) && name.equals(sign.getName());
        }
        else
        {
            return false;
        }
    }

    public long getRowCount()
    {
        return rowCount;
    }

    public void setRowCount(long rowCount)
    {
        this.rowCount = rowCount;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public Map<String, Set<String>> getUniqueConstraints()
    {
        return uniqueConstraints;
    }

    public String getLogicalType()
    {
        return logicalType;
    }

    public void setLogicalType(String logicalType)
    {
        this.logicalType = logicalType;
    }
    
    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 79 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
