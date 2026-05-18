package be.ugent.ledc.core.binding.jdbc.agents;

import be.ugent.ledc.core.binding.jdbc.schema.TableSchema;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.Dataset;
import be.ugent.ledc.core.dataset.FixedTypeDataset;
import be.ugent.ledc.core.dataset.SimpleDataset;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.Collectors;

public class PersistentDatatype
{
    //GENERIC CHARACTER DATATYPE NAMES
    public static final String VARCHAR          = "varchar";
    public static final String CHAR             = "char";
    public static final String TEXT             = "text";
    public static final String UNICODE_VARCHAR  = "nvarchar";
    public static final String UNICODE_CHAR     = "nchar";
    public static final String UNICODE_TEXT     = "ntext";
    
    //GENERIC NUMERIC DATATYPE NAMES
    public static final String BOOLEAN      = "boolean";
    public static final String TINYINT      = "tinyint";
    public static final String SMALLINT     = "smallint";
    public static final String INT          = "int";
    public static final String BIGINT       = "bigint";
    public static final String FLOAT        = "float";    // FLOATING-POINT DATA STORAGE (4 BYTES)
    public static final String BIGFLOAT     = "bigfloat"; // FLOATING-POINT DATA STORAGE (8 BYTES)
    public static final String DECIMAL      = "decimal";  // FIXED-POINT DATA STORAGE
    public static final String NUMERIC      = "numeric";  // FIXED-POINT DATA STORAGE
    public static final String MONEY        = "money";
    
    //GENERIC DATE/TIME DATATYPE NAMES
    public static final String YEAR                 = "year";
    public static final String TIME                 = "time";
    public static final String DATE                 = "date";
    public static final String TIMESTAMP            = "timestamp";
    public static final String TIMESTAMP_TIMEZONE   = "timestamp_with_timezone";
    
    //BINARE LARGE OBJECT
    public static final String BLOB     = "blob";
    
    //ARRAYS
    public static final String TEXT_ARRAY           = "text[]";
    public static final String INT_ARRAY            = "int[]";
    public static final String BIGINT_ARRAY         = "bigint[]";
    
    public static Class toClass(String persistentDatatype)
    {
        switch(persistentDatatype)
        {
            case VARCHAR:
            case CHAR:
            case TEXT:
            case UNICODE_VARCHAR:
            case UNICODE_CHAR:
            case UNICODE_TEXT:
                return String.class;
            case BOOLEAN:
                return Boolean.class;
            case TINYINT:
                return Byte.class;
            case SMALLINT:
                return Short.class;
            case INT:
                return Integer.class;
            case BIGINT:
                return Long.class;
            case FLOAT:
                return Float.class;
            case BIGFLOAT:
                return Double.class;
            case DECIMAL:
            case NUMERIC:
                return BigDecimal.class;
            case MONEY:
                return Double.class;
            case YEAR:
                return String.class;
            case TIME:
                return LocalTime.class;
            case DATE:
                return LocalDate.class;
            case TIMESTAMP:
                return LocalDateTime.class;
            case TIMESTAMP_TIMEZONE:
                return LocalDateTime.class;
            case BLOB:
                return Object.class;
            default:
                return Object.class;
        }
    }
    
    public static String toPersistentDatatype(int sqlType)
    {
        switch(sqlType)
        {
            case java.sql.Types.BIGINT:
                return BIGINT;
            case java.sql.Types.BIT:
                return BOOLEAN;
            case java.sql.Types.BOOLEAN:
                return BOOLEAN;
            case java.sql.Types.CHAR:
                return CHAR;
            case java.sql.Types.DATE:
                return DATE;
            case java.sql.Types.DECIMAL:
                return DECIMAL;
            case java.sql.Types.DOUBLE:
                return BIGFLOAT;
            case java.sql.Types.FLOAT:
                return FLOAT;
            case java.sql.Types.INTEGER:
                return INT;
            case java.sql.Types.JAVA_OBJECT:
                return BLOB;
            case java.sql.Types.LONGNVARCHAR:
                return UNICODE_TEXT;
            case java.sql.Types.LONGVARBINARY:
                return TEXT;
            case java.sql.Types.LONGVARCHAR:
                return TEXT;
            case java.sql.Types.NCHAR:
                return UNICODE_CHAR;
            case java.sql.Types.NVARCHAR:
                return UNICODE_VARCHAR;
            case java.sql.Types.VARCHAR:
                return VARCHAR;
            case java.sql.Types.NUMERIC:
                return NUMERIC;
            case java.sql.Types.TINYINT:
                return TINYINT;
            case java.sql.Types.SMALLINT:
                return SMALLINT;
            default:
                return TEXT;
        }
    }
    
    public static Object parse(String stringValue, String persistentDatatype)
    {
        if(stringValue == null || stringValue.isEmpty())
            return null;
        
        switch(persistentDatatype)
        {
            case VARCHAR:
            case CHAR:
            case TEXT:
            case UNICODE_VARCHAR:
            case UNICODE_CHAR:
            case UNICODE_TEXT:
                return stringValue;
            case BOOLEAN:
                return Boolean.valueOf(stringValue);
            case TINYINT:
                return Byte.valueOf(stringValue);
            case SMALLINT:
                return Short.valueOf(stringValue);
            case INT:
                return Integer.valueOf(stringValue);
            case BIGINT:
                return Long.valueOf(stringValue);
            case FLOAT:
                return Float.valueOf(stringValue);
            case BIGFLOAT:
                return Double.valueOf(stringValue);
            case DECIMAL:
            case NUMERIC:
                return new BigDecimal(stringValue);
            case MONEY:
                return Double.valueOf(stringValue);
            case YEAR:
                return stringValue;
            case TIME:
                return LocalTime.parse(stringValue);
            case DATE:
                return LocalDate.parse(stringValue);
            case TIMESTAMP:
                return LocalDateTime.parse(stringValue);
            case TIMESTAMP_TIMEZONE:
                return LocalDateTime.parse(stringValue);

        }
        
        return null;
        
    }
    
    public static Dataset convertTextData(FixedTypeDataset<String> stringData, TableSchema tableSchema)
    {
        return new SimpleDataset(
            stringData
            .getDataObjects()
            .stream()
            .map(o -> convertStringObject(o, tableSchema))
            .collect(Collectors.toList())
        );
    }
    
    private static DataObject convertStringObject(DataObject o, TableSchema tableSchema)
    {
        return new DataObject(
            o.getAttributes()
            .stream()
            .filter(a-> o.get(a) != null)
            .collect(Collectors.toMap(
                a -> a,
                a -> tableSchema.getAttributeNames().contains(a)
                    ? PersistentDatatype
                        .parse(
                            o.getString(a),
                            (String)tableSchema
                                .getAttributeProperty(a, TableSchema.PROP_DATATYPE))
                    : o.get(a)
                )),
            o.isCaseSensitiveNames());
    }
}
