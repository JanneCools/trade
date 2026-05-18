package be.ugent.ledc.core.binding.jdbc;

import be.ugent.ledc.core.DataException;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.dataset.contractors.TypeContractor;
import be.ugent.ledc.core.dataset.contractors.TypeContractorFactory;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.stream.Stream;

public abstract class AbstractJDBCDataReader extends JDBCOperator
{
    /**
     * A flag that indicates strong binding is requested. When set, SQL types
     * must be mapped to known data types and mapping to objects is not allowed.
     * When set to false, vendor specific types will be mapped to Objects.
     */
    private final boolean strongBinding;
    
    public AbstractJDBCDataReader(JDBCBinder binder, boolean strongBinding)
    {
        super(binder);
        this.strongBinding = strongBinding;
    }

    public AbstractJDBCDataReader(JDBCBinder binder)
    {
        this(binder, false);
    }
    
    protected void addData(ResultSet resultSet, Dataset dataset) throws SQLException
    {       
        //Scroll over the resultset
        while (resultSet.next())
            dataset.addDataObject(createDataObject(resultSet));

        //Close result set
        resultSet.close();
    }
    
    protected DataObject createDataObject(ResultSet resultSet) throws SQLException
    {
        ResultSetMetaData metaData = resultSet.getMetaData();
        
        //Create a new DataObjects
        DataObject object = new DataObject();

        //Explore all record columns
        for (int i = 1; i <= metaData.getColumnCount(); i++)
        {
            //Get the column name
            String attributeName = metaData.getColumnLabel(i);

            //Get the value for this column
            Object rawValue = resultSet.getObject(attributeName);

            //Add value to complex object
            object.set(
                attributeName,
                convert(
                    rawValue,
                    metaData.getColumnType(i),
                    metaData.getColumnTypeName(i),
                    metaData.getColumnClassName(i)
                )
            );
        }
        
        return object;
    }
    
    private Object convert(Object rawValue, int columnType, String typeName, String className) throws DataException, SQLException
    {
        if(rawValue == null)
            return rawValue;
        
        switch(columnType)
        {
            case Types.CHAR, Types.NCHAR, Types.VARCHAR, Types.LONGNVARCHAR, Types.LONGVARCHAR, Types.NVARCHAR ->
            {
                return (String) rawValue;
            }
            case Types.BIGINT ->
            {
                return (Long) rawValue;
            }
            case Types.INTEGER ->
            {
                return (Integer) rawValue;
            }
            case Types.BOOLEAN, Types.BIT ->
            {
                return (Boolean) rawValue;
            }
            case Types.DOUBLE ->
            {
                return (Double) rawValue;
            }
            case Types.NUMERIC, Types.DECIMAL ->
            {
                return (BigDecimal) rawValue;
            }
            case Types.FLOAT ->
            {
                return (Float) rawValue;
            }
            case Types.REAL ->
            {
                return (Float) rawValue;
            }
            case Types.DATE ->
            {
                return ((java.sql.Date) rawValue).toLocalDate();
            }
            case Types.TIME ->
            {
                return ((java.sql.Time) rawValue).toLocalTime();
            }
            case Types.ARRAY ->   
            {
                java.sql.Array a = (java.sql.Array) rawValue;
                return Stream
                        .of((Object[]) a.getArray())
                        .toList();
            }
            case Types.TIMESTAMP ->
            {
                return ((java.sql.Timestamp) rawValue).toLocalDateTime();
            }
        }
        
        
        if(!strongBinding)
        {
            return rawValue;
        }

        throw new DataException("Could not convert value to a known type for DataObject");
    }
    
    protected TypeContractor toTypeContractor(int columnType, String typeName, String className)
    {
        switch(columnType)
        {
            case Types.CHAR, Types.NCHAR, Types.VARCHAR, Types.LONGNVARCHAR, Types.LONGVARCHAR, Types.NVARCHAR ->
            {
                return TypeContractorFactory.STRING;
            }
            case Types.BIGINT ->
            {
                return TypeContractorFactory.LONG;
            }
            case Types.INTEGER ->
            {
                return TypeContractorFactory.INTEGER;
            }
            case Types.BOOLEAN, Types.BIT ->
            {
                return TypeContractorFactory.BOOLEAN;
            }
            case Types.DOUBLE ->
            {
                return TypeContractorFactory.DOUBLE;
            }
            case Types.DECIMAL, Types.NUMERIC ->
            {
                return TypeContractorFactory.BIGDECIMAL;
            }
            case Types.FLOAT, Types.REAL ->
            {
                return TypeContractorFactory.FLOAT;
            }
            case Types.DATE ->
            {
                return TypeContractorFactory.DATE;
            }
            case Types.TIME ->
            {
                return TypeContractorFactory.TIME;
            }
            case Types.TIMESTAMP -> 
            {
                return TypeContractorFactory.DATETIME;
            }
            case Types.ARRAY ->
            {
                return TypeContractorFactory.LIST;
            }
        }
       
        //If loose binding is allowed, map unknown types to objects.
        if(!strongBinding)
        {
            return TypeContractorFactory.OBJECT;
        }

        //Else throw an exception
        throw new DataException("Could not convert value with column type "
            + columnType
            + " to a known type for DataObject");
    }
}
