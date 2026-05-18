package be.ugent.ledc.core.binding.jdbc.schema;

import java.util.HashMap;

/**
 * A class that models a reference from one TableSchema to another. It basically maps attributes from the key of one TableSchema to attributes of another TableSchema.
 * @author antoon
 */
public class Reference extends HashMap<String,String>
{
    /**
     * The TableSchema that is being referenced.
     */
    private TableSchema referenced;

    /**
     * A flag that indicates whether this reference is observed, or added by a ReferenceFinder.
     */
    private boolean observed;
    
    /**
     * The strategy for maintenance of referential integrity after UPDATE statement
     */
    private String onUpdate;
    
    /**
     * The strategy for maintenance of referential integrity after DELETE statement
     */
    private String onDelete;
    
    /**
     * Name of the constraint that defines the foreign key
     */
    private String name;

    //Constant values for onUpdate and onDelete
    public static final String CASCADE = "CASCADE";
    public static final String NO_ACTION = "NO ACTION";
    public static final String SET_NULL = "SET NULL";
    
    /**
     * Empty constructor
     * @param referenced
     */
    public Reference(TableSchema referenced)
    {
        //By default references are assumed to be observed.
        this.observed = true;
        this.onDelete = CASCADE;
        this.onUpdate = CASCADE;
        this.referenced = referenced;
    }
    
    /**
     * Empty constructor
     * @param referenced
     * @param localAttribute
     * @param remoteAttribute
     */
    public Reference(TableSchema referenced, String localAttribute, String remoteAttribute)
    {
        //By default references are assumed to be observed.
        this.observed = true;
        this.onDelete = CASCADE;
        this.onUpdate = CASCADE;
        this.referenced = referenced;
        put(localAttribute, remoteAttribute);
    }
    
    public Reference()
    {
        this.observed = true;
        this.onDelete = CASCADE;
        this.onUpdate = CASCADE;
    }

    public TableSchema getReferenced()
    {
        return referenced;
    }

    public void setReferenced(TableSchema referenced)
    {
        this.referenced = referenced;
    }
    
    

    /**
     * Checks the validity of the reference, i.e. whether the referenced SourceSignature is correctly referenced by its key
     * @return Returns true if the reference mapping contains valid names.
     */
    public boolean isValid()
    {
        if(keySet().isEmpty() || values().isEmpty())
        {
            return false;
        }
        if(!values().containsAll(referenced.getPrimaryKey()))
        {
            return false;
        }
        return true;
    }

    public boolean isObserved()
    {
        return observed;
    }

    public void setObserved(boolean isObserved)
    {
        this.observed = isObserved;
    }

    public String getOnUpdate()
    {
        return onUpdate;
    }

    public void setOnUpdate(String onUpdate)
    {
        this.onUpdate = onUpdate;
    }

    public String getOnDelete()
    {
        return onDelete;
    }

    public void setOnDelete(String onDelete)
    {
        this.onDelete = onDelete;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
