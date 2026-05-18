package be.ugent.ledc.core.binding.jdbc.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A DatabaseSchema models a collection of TableSchemas and keeps track of the References between TableSchemas.
 * @author antoon
 */
public class DatabaseSchema extends HashMap<String, TableSchema>
{
    /**
     * The name of the schema
     */
    private String name;
    
    /**
     * List of sequences
     */
    private ArrayList<String> sequences = new ArrayList<>();
    
    /**
     * Constructor with the name of the schema.
     * @param name
     */
    public DatabaseSchema(String name)
    {
        super();
        this.name = name;
    }

    /**
     * Empty constructor.
     */
    public DatabaseSchema()
    {
        super();
        this.name = "default";
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    /**
     * Gets a list of all TableSchemas in the {@link DatabaseSchema}.
     * @return {@link List} of TableSchemas
     */
    public List<TableSchema> getTableSchemas()
    {
        List<TableSchema> listOfTableSchemas = new ArrayList<>();
        listOfTableSchemas.addAll(values());
        return listOfTableSchemas;
    }
    
    public List<TableSchema> getUnreferencedTableSchemas(){
	/*
	 * Start from all table schemas
	 */
	List<TableSchema> tableSchemas = new ArrayList<>(values());
	for (TableSchema tableSchema : values()) {
	    /*
	     * For each table schema, look which are referenced and remove those from the list
	     */
	    tableSchemas.removeAll(getReferencingTableSchemas(tableSchema));
	}
	/*
	 * What remains are all unreferenced table schemas
	 */
	return tableSchemas;
    }

    /**
     * Gets a list of all TableSchemas in the DatabaseSchema that are referenced by the given SourceTableSchema.
     * @param tableSchema
     * @return {@link List} of TableSchemas that reference to the given {@link TableSchema}.
     */
    public List<TableSchema> getReferencingTableSchemas(TableSchema tableSchema)
    {
        //Prepare a list for tableSchemas referencing to the given one
        List<TableSchema> listOfReferencingTableSchemas = new ArrayList<>();

        //Get a list of all tableSchemas
        List<TableSchema> listOfTableSchemas = getTableSchemas();
        
        //Iterate over all tableSchemas
        for(int i=0; i<listOfTableSchemas.size(); i++)
        {
            //Get the next tableSchema
            TableSchema sign = listOfTableSchemas.get(i);

            //Check if this tableSchema references to the given one
            if(sign.referencesTo(tableSchema))
            {
                listOfReferencingTableSchemas.add(sign);
            }
        }

        return listOfReferencingTableSchemas;
    }

    /**
     * Gets a list of all TableSchemas in the {@link DatabaseSchema} that are referenced by the given tableSchema name.
     * @param tableSchemaName
     * @return {@link List} of TableSchemas that reference to the given tableSchema name.
     */
    public List<TableSchema> getReferencingTableSchemas(String tableSchemaName)
    {
        return getReferencingTableSchemas(get(tableSchemaName));
    }

    public boolean removeTableSchema(TableSchema tableSchema)
    {
        List<TableSchema> listOfTableSchemas = getTableSchemas();

        //Check other tableSchemas
        for(int k=0; k<listOfTableSchemas.size(); k++)
        {
            listOfTableSchemas.get(k).removeReferenceTo(tableSchema);
        }
       
        if(containsKey(tableSchema.getName()))
        {
            //Remove tableSchema
            remove(tableSchema.getName());
            
            return true;
        }
        else
        {
            return false;
        }
    }
    
    public void addTableSchema(TableSchema tableSchema)
    {
        //System.out.println(tableSchema.getName());
        put(tableSchema.getName(), tableSchema);
    }

        
    public ArrayList<String> getSequences()
    {
        return sequences;
    }

    public void setSequences(ArrayList<String> sequences)
    {
        this.sequences = sequences;
    }
    
    @Override
    public String toString()
    {
        return name;
    }
}
