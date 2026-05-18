package be.ugent.ledc.core.binding.jdbc.schema.generator;

import be.ugent.ledc.core.binding.BindingException;
import be.ugent.ledc.core.binding.DataReadException;
import be.ugent.ledc.core.binding.jdbc.DBMS;
import be.ugent.ledc.core.binding.jdbc.JDBCBinder;
import be.ugent.ledc.core.binding.jdbc.JDBCDataReader;
import be.ugent.ledc.core.binding.jdbc.RelationalDB;
import be.ugent.ledc.core.binding.jdbc.agents.SchemaAgent;
import be.ugent.ledc.core.binding.jdbc.schema.CheckConstraint;
import be.ugent.ledc.core.binding.jdbc.schema.Reference;
import be.ugent.ledc.core.binding.jdbc.schema.DatabaseSchema;
import be.ugent.ledc.core.binding.jdbc.schema.SchemaGeneralizer;
import be.ugent.ledc.core.binding.jdbc.schema.TableSchema;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Schematizer connects to a database instance and extracts a DatabaseSchema from it, representing the database structure and constraints.
 * @author antoon
 */
public class Schematizer
{
    /**
     * List of filters
     */
    protected ArrayList<TableSchemaFilter> listOfFilters;

    /**
     * A flag that indicates whether the Schematizer must look for missing references
     */
    protected boolean findMissingReferences;
    
    private JDBCDataReader dataReader;
    
    private boolean addRowCounts = false;
    
    public Schematizer() 
    {
        //Initialize list of filters
        this.listOfFilters = new ArrayList<>();

        //Initialize flag
        this.findMissingReferences = false;
    }

    public DatabaseSchema schematize(RelationalDB rdb) throws BindingException
    {        
        try
        {
            //Prepare queries
            String columnQuery      = rdb.getVendor().getAgent().getSchemaAgent().getColumnQuery(rdb);
            String primaryKeyQuery  = rdb.getVendor().getAgent().getSchemaAgent().getPrimaryKeyQuery(rdb);
            String foreignKeyQuery  = rdb.getVendor().getAgent().getSchemaAgent().getForeignKeyQuery(rdb);
            String sequenceQuery    = rdb.getVendor().getAgent().getSchemaAgent().getSequenceQuery(rdb);
            String checkQuery       = rdb.getVendor().getAgent().getSchemaAgent().getCheckConstraintQuery(rdb);
            String uniqueQuery      = rdb.getVendor().getAgent().getSchemaAgent().getUniqueConstraintQuery(rdb);

            JDBCBinder binder = new JDBCBinder(rdb);
            
            dataReader = new JDBCDataReader(binder);

            //Make schema
            DatabaseSchema schema = makeSchema(columnQuery, rdb.getSchemaName(), rdb.getVendor());

            //Add primary keys
            schema = addPrimaryKeys(primaryKeyQuery, schema);

            //Add foreign keys
            schema = addForeignKeys(foreignKeyQuery, schema);

            //Add check constraints
            schema = addCheckConstraints(checkQuery, schema, rdb.getVendor());
            
            //Add unique constraints
            schema = addUniqueConstraints(uniqueQuery, schema);

            //Add sequences if applicable
            if(sequenceQuery != null)
            {
                schema = addSequences(sequenceQuery, schema);
            }
            
            //Add row counts
            if(addRowCounts)
            {
                schema = addRowCounts(schema, binder);
            }
            
            //Apply filters
            if(listOfFilters != null && schema != null)
            {
                //Get list of table schemas
                List<TableSchema> listOfTableSchemas = schema.getTableSchemas();

                for (TableSchema tableSchema : listOfTableSchemas)
                {
                    for (TableSchemaFilter filter : listOfFilters)
                    {
                        if(!filter.accept(tableSchema))
                        {
                            //Remove from DatabaseSchema
                            schema.removeTableSchema(tableSchema);

                            //Break out
                            break;
                        }
                    }
                }
            }

            //Check if missing references must be detected
            if(findMissingReferences)
            {
                //Make reference finder
                ReferenceFinder referenceFinder = new ReluctantReferenceFinder();

                //Make a list of missing references
                Map<String, List<Reference>> missingReferences = referenceFinder.findMissingReferences(schema);

                //Iterate
                for(String tableSchemaName: schema.keySet())
                {
                    //Check
                    if(missingReferences.containsKey(tableSchemaName))
                    {
                        //Get missing references
                        List<Reference> references = missingReferences.get(tableSchemaName);

                        for (Reference reference : references)
                        {
                            //Add reference
                            schema.get(tableSchemaName).getReferences().add(reference);
                        }
                    }
                }
            }
            //Close
            dataReader.closeConnection();
                       
            //Return
            return SchemaGeneralizer.generalize(schema, rdb.getVendor());
        }
        catch(SQLException | DataReadException ex)
        {
            throw new SchematizeException(ex);
        }
    }

    protected DatabaseSchema makeSchema(String query, String schemaName, DBMS vendor) throws DataReadException
    {        
        Dataset fetchedData = dataReader.readData(query);
        
        //List of tableSchemas
        List<TableSchema> listOfTableSchemas = new ArrayList<>();

        String currentTable = "";

        for(DataObject o: fetchedData.getDataObjects())
        {
            String tableName            = (String)o.get(SchemaAgent.TABLE_NAME);
            String tableType            = (String)o.get(SchemaAgent.TABLE_TYPE);
            String columnName           = (String)o.get(SchemaAgent.COLUMN_NAME);
            String datatype             = (String)o.get(SchemaAgent.DATA_TYPE);
            Object datatypeLength       = o.get(SchemaAgent.DATA_TYPE_LENGTH) ;
            Object datatypeDisplayLength= o.get(SchemaAgent.DATA_TYPE_DISPLAY_LENGTH) ;
            Object datatypePrecision    = o.get(SchemaAgent.DATA_TYPE_PRECISION);
            Object datatypeScale        = o.get(SchemaAgent.DATA_TYPE_SCALE);
            Object datatypeRadix        = o.get(SchemaAgent.DATA_TYPE_PRECISION_RADIX);
            String isNullable           = (String)o.get(SchemaAgent.IS_NULLABLE);
            String isAutoIncrement      = (String)o.get(SchemaAgent.IS_AUTO_INCREMENT);
            Object defaultValue         = o.get(SchemaAgent.DEFAULT_VALUE);
            
            //System.out.println(tableName + " " + tableType + " " + columnName + " " + datatype + " " + isNullable + " " + defaultValue );

            if(!tableName.equals(currentTable))
            {
                //New TableSchema required
                TableSchema tableSchema = new TableSchema();
                tableSchema.setName(tableName);
                tableSchema.setType(tableType);
                listOfTableSchemas.add(tableSchema);
            }

            //Get the current tableSchema
            TableSchema current = listOfTableSchemas.get(listOfTableSchemas.size()-1);

            //Add the datatype
            current.setAttributeProperty(columnName, TableSchema.PROP_DATATYPE, datatype);
            
            //Add the datatype specs
            current.setAttributeProperty(columnName, TableSchema.PROP_DATATYPE_LENGTH, datatypeLength);
            current.setAttributeProperty(columnName, TableSchema.PROP_DATATYPE_DISPLAY_LENGTH, datatypeDisplayLength);
            current.setAttributeProperty(columnName, TableSchema.PROP_DATATYPE_PRECISION, datatypePrecision);
            current.setAttributeProperty(columnName, TableSchema.PROP_DATATYPE_SCALE, datatypeScale);
            current.setAttributeProperty(columnName, TableSchema.PROP_DATATYPE_RADIX, datatypeRadix);
            
            //Add the default value
            current.setAttributeProperty(columnName, TableSchema.PROP_DEFAULT_VALUE, defaultValue);

            //By default: mark as non-key attribute and non indexed
            current.setAttributeProperty(columnName, TableSchema.PROP_IS_KEY_ATTRIBUTE, false);
            current.setAttributeProperty(columnName, TableSchema.PROP_IS_INDEXED, false);

            //Check if nullable
            current.setAttributeProperty(columnName, TableSchema.PROP_IS_NULLABLE, isNullable.equals("YES"));
            
            //Check if auto increment
            current.setAttributeProperty(columnName, TableSchema.PROP_IS_AUTOINCREMENT, isAutoIncrement.equals("YES"));
            
            //Adjust current table
            currentTable = tableName;
        }

        //Make the schema
        DatabaseSchema schema = new DatabaseSchema(schemaName);

        for (TableSchema tableSchema : listOfTableSchemas)
        {
            schema.put(tableSchema.getName(), tableSchema);
        }

        //Return schema
        return schema;
    }

    protected DatabaseSchema addPrimaryKeys(String query, DatabaseSchema schema) throws DataReadException
    {       
        dataReader.readData(query)
            .getDataObjects()
            .stream()
            .forEach((o) ->
            {
                String tableName    = (String) o.get(SchemaAgent.TABLE_NAME);
                String columnName   = (String) o.get(SchemaAgent.COLUMN_NAME);

                //Mark column as key attribute
                schema.get(tableName).setAttributeProperty(columnName, TableSchema.PROP_IS_KEY_ATTRIBUTE, true);
            });

        //Return schema
        return schema;
    }

    protected DatabaseSchema addForeignKeys(String query, DatabaseSchema schema) throws DataReadException
    {       
        //Execute
        Dataset fetchedObjects = dataReader.readData(query);
        
        String previous = "";

        for(DataObject o: fetchedObjects.getDataObjects())
        {
            //Get data from resultSet
            String name             = (String)o.get(SchemaAgent.CONSTRAINT_NAME);
            String leftTableName    = (String)o.get(SchemaAgent.LEFT_TABLE_NAME);
            String leftColumnName   = (String)o.get(SchemaAgent.LEFT_COLUMN_NAME);
            String rightTableName   = (String)o.get(SchemaAgent.RIGHT_TABLE_NAME);
            String rightColumnName  = (String)o.get(SchemaAgent.RIGHT_COLUMN_NAME);
            
            List<Reference> listOfReferences = schema.get(leftTableName).getReferences();
       
            if(previous.equals(name) && !listOfReferences.isEmpty())
            {
                listOfReferences.get(listOfReferences.size() - 1).put(leftColumnName,rightColumnName);
            }
            else
            {
                if(schema.get(rightTableName) != null)
                {
                    //Make a new reference
                    Reference newRef = new Reference(schema.get(rightTableName));

                    //Add last column names
                    newRef.put(leftColumnName,rightColumnName);

                    //Add onUpdate and onDelete strategies
                    newRef.setOnUpdate((String) o.get(SchemaAgent.UPDATE_RULE));
                    newRef.setOnDelete((String) o.get(SchemaAgent.DELETE_RULE));

                    //Add constraint name
                    newRef.setName(name);

                    //Add new reference to list
                    listOfReferences.add(newRef);
                }
            }

            //Update list of reference
            schema.get(leftTableName).setReferences(listOfReferences);

            //update constraint name
            previous = name;
        }
        
        for(String tableSchemaName: schema.keySet())
        {
            //TableSchema
            TableSchema tableSchema = schema.get(tableSchemaName);
            
            //Count non-self references
            int nonSelfReferences = 0;
            
            //Count number of non self references
            for(Reference r: tableSchema.getReferences())
            {
                if(!r.getReferenced().equals(tableSchema))
                {
                    nonSelfReferences++;
                }
            }
            
            if(nonSelfReferences >= 2)
            {
                tableSchema.setLogicalType("Relationship");
            }
            else
            {
                tableSchema.setLogicalType("Entity");
            }
        }

        //Return
        return schema;
    }

    protected DatabaseSchema addCheckConstraints(String query, DatabaseSchema schema, DBMS vendor) throws DataReadException
    {
        if(query != null)
        {
            //Execute
            Dataset fetchedObjects = dataReader.readData(query);

            for(DataObject o: fetchedObjects.getDataObjects())
            {
                //Get data from resultSet
                String tableName= (String)o.get(SchemaAgent.TABLE_NAME);
                String check    = (String)o.get(SchemaAgent.CHECK_CONSTRAINT);

                schema
                    .get(tableName)
                    .getCheckConstraints()
                    .add(new CheckConstraint(
                        check,
                        schema.get(tableName).getAttributeNames(),
                        vendor));
            }
        }
        
        //Return
        return schema;
    }
    
    protected DatabaseSchema addUniqueConstraints(String query, DatabaseSchema schema) throws DataReadException
    {
        if(query != null)
        {
            //Execute
            Dataset fetchedObjects = dataReader.readData(query);

            Map<String, Map<String,Set<String>>> tempMap = new HashMap<>();
            
            for(DataObject o: fetchedObjects.getDataObjects())
            {
                //Get data from resultSet
                String tableName        = (String)o.get(SchemaAgent.TABLE_NAME);
                String constraintName   = (String)o.get(SchemaAgent.CONSTRAINT_NAME);
                String columnName       = (String)o.get(SchemaAgent.COLUMN_NAME);

                //Check table
                if(!tempMap.containsKey(tableName))
                {
                    tempMap.put(tableName, new HashMap<>());
                }
                
                //Check constraint
                if(!tempMap.get(tableName).containsKey(constraintName))
                {
                    tempMap.get(tableName).put(constraintName, new HashSet<>());
                }
                
                //Add column name
                tempMap.get(tableName).get(constraintName).add(columnName);
            }
            
            for(String tableName: tempMap.keySet())
            {
                for(String constraintName: tempMap.get(tableName).keySet())
                {
                    //Get the group of column names that are unique
                    Set<String> columnNames = tempMap.get(tableName).get(constraintName);
                    
                    //Add to schema
                    schema.get(tableName).getUniqueConstraints().put(constraintName, columnNames);
                    
                    if(columnNames.size() == 1)
                    {
                        //Set the unique property for this attribute
                        for(String columnName: columnNames)
                        {
                            schema.get(tableName).setAttributeProperty(columnName, TableSchema.PROP_IS_UNIQUE, true);
                        }
                    }
                }
            }
        }
        
        //Return
        return schema;
    }
    
    protected DatabaseSchema addIndices(String query, DatabaseSchema schema) throws DataReadException
    {       
        //Execute
        Dataset dataset = dataReader.readData(query);

        dataset.getDataObjects().stream().forEach((o) ->
        {
            String tableName    = (String) o.get(SchemaAgent.TABLE_NAME);
            String columnName   = (String) o.get(SchemaAgent.COLUMN_NAME);

            //Mark column as key attribute
            schema.get(tableName).setAttributeProperty(columnName, TableSchema.PROP_IS_INDEXED, true);
        });

        //Return schema
        return schema;
    }
    
    protected DatabaseSchema addSequences(String query, DatabaseSchema schema) throws DataReadException
    {        
        //Execute
        Dataset dataset = dataReader.readData(query);

        dataset.getDataObjects().stream().map((o) -> (String)o.get(SchemaAgent.SEQUENCE_NAME)).forEach((sequenceName) ->
        {
            schema.getSequences().add(sequenceName);
        });
        
        //Return
        return schema;
    }

    protected DatabaseSchema addRowCounts(DatabaseSchema schema, JDBCBinder binder) throws DataReadException
    {
        for(String tableName: schema.keySet())
        {
            String query = "SELECT COUNT(*) AS row_count FROM " + binder.getRdb().getVendor().getAgent().escaping().apply(tableName);
                      
            Dataset dataset = dataReader.readData(query);
            
            dataset.getDataObjects().stream().map((o) -> (Number) o.get("row_count")).forEach((rowCountRaw) ->
            {
                schema.get(tableName).setRowCount(rowCountRaw.longValue());
            });
        }

        //Return schema
        return schema;
    }
    
    public ArrayList<TableSchemaFilter> getListOfFilters()
    {
        return listOfFilters;
    }

    public void setListOfFilters(ArrayList<TableSchemaFilter> listOfFilters)
    {
        this.listOfFilters = listOfFilters;
    }

    public void addFilter(TableSchemaFilter filter)
    {
        //Initialize if necessary
        if(listOfFilters == null)
        {
            //Initialize list of filters
            this.listOfFilters = new ArrayList<>();
        }

        //Add filter
        listOfFilters.add(filter);
    }

    public boolean isFindMissingReferences()
    {
        return findMissingReferences;
    }

    public void setFindMissingReferences(boolean findMissingReferences)
    {
        this.findMissingReferences = findMissingReferences;
    }

    public boolean isAddRowCounts()
    {
        return addRowCounts;
    }

    public void setAddRowCounts(boolean addRowCounts)
    {
        this.addRowCounts = addRowCounts;
    }
}
