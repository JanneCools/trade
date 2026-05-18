package be.ugent.ledc.core.binding.jdbc.agents.postgres;

import be.ugent.ledc.core.binding.jdbc.agents.PersistentDatatype;
import be.ugent.ledc.core.binding.jdbc.agents.MigrationAgent;
import be.ugent.ledc.core.binding.jdbc.schema.Reference;
import be.ugent.ledc.core.binding.jdbc.schema.TableSchema;

/**
 * MigrationAgent Implementation for PostGreSQL
 */
public class PostgresMigrationAgent implements MigrationAgent
{

    @Override
    public void generalizeSignature(TableSchema signature)
    {
        for (String attributeName : signature.keySet())
        {
            //Initialize
            String generalDatatype;

            if (signature.getAttributeProperty(attributeName, TableSchema.PROP_DATATYPE) == null)
            {
                generalDatatype = PersistentDatatype.VARCHAR;
                signature.setAttributeProperty(attributeName, TableSchema.PROP_DATATYPE_LENGTH, 50);
            } else
            {
                //Get datatype
                String datatype = ((String) signature.getAttributeProperty(attributeName, TableSchema.PROP_DATATYPE)).toLowerCase();

                if ("serial".equals(datatype) || "bigserial".equals(datatype))
                {
                    signature.setAttributeProperty(attributeName, TableSchema.PROP_IS_AUTOINCREMENT, Boolean.TRUE);
                }

                switch (datatype)
                {
                    //Name-diverting textual data types
                    case "character varying":
                        generalDatatype = PersistentDatatype.VARCHAR;
                        break;
                    case "character":
                        generalDatatype = PersistentDatatype.CHAR;
                        break;

                    //Name-diverting numeric data types
                    case "integer":
                        generalDatatype = PersistentDatatype.INT;
                        break;
                    case "numeric":
                        generalDatatype = PersistentDatatype.DECIMAL;
                        break;
                    case "real":
                        generalDatatype = PersistentDatatype.FLOAT;
                        break;
                    case "double precision":
                        generalDatatype = PersistentDatatype.BIGFLOAT;
                        break;
                    case "serial":
                        generalDatatype = PersistentDatatype.INT;
                        break;
                    case "bigserial":
                        generalDatatype = PersistentDatatype.BIGINT;
                        break;

                    //Name-diverting date/time data types  
                    case "timestamp without time zone":
                        generalDatatype = PersistentDatatype.TIMESTAMP;
                        break;
                    case "timestamp with time zone":
                        generalDatatype = PersistentDatatype.TIMESTAMP_TIMEZONE;
                        break;
                    case "time with time zone":
                        generalDatatype = PersistentDatatype.TIME;
                        break;
                    case "time without time zone":
                        generalDatatype = PersistentDatatype.TIME;
                        break;

                    default:
                        generalDatatype = datatype;
                }
            }
            //Set general datatype
            signature.setAttributeProperty(attributeName, TableSchema.PROP_DATATYPE, generalDatatype);
        }
    }

    @Override
    public void specifySignature(TableSchema signature)
    {
        for (String attributeName : signature.keySet())
        {
            //Get datatype
            String datatype = (String) signature.getAttributeProperty(attributeName, TableSchema.PROP_DATATYPE);

            String specificDatatype = "";

            switch (datatype)
            {
                case PersistentDatatype.VARCHAR:
                    specificDatatype = "character varying";
                    break;
                case PersistentDatatype.CHAR:
                    specificDatatype = "character";
                    break;
                case PersistentDatatype.UNICODE_VARCHAR:
                    specificDatatype = "character varying";
                    break;
                case PersistentDatatype.UNICODE_CHAR:
                    specificDatatype = "character";
                    break;
                case PersistentDatatype.UNICODE_TEXT:
                    specificDatatype = "text";
                    break;
                case PersistentDatatype.TINYINT:
                    specificDatatype = "smallint";
                    break;
                case PersistentDatatype.INT:
                    specificDatatype = "integer";

                    break;
                case PersistentDatatype.BIGINT:
                    specificDatatype = "bigint";

                    break;
                case PersistentDatatype.FLOAT:
                    specificDatatype = "real";
                    break;
                case PersistentDatatype.BIGFLOAT:
                    specificDatatype = "double precision";
                    break;
                case PersistentDatatype.DECIMAL:
                    Number scaleAsNumber = (Number) signature.getAttributeProperty(attributeName, TableSchema.PROP_DATATYPE_SCALE);

                    if (scaleAsNumber != null)
                    {
                        int scale = scaleAsNumber.intValue();

                        if (scale == 0)
                        {
                            Number radixAsDouble = (Number) signature.getAttributeProperty(attributeName, TableSchema.PROP_DATATYPE_RADIX);
                            Number precisionAsDouble = (Number) signature.getAttributeProperty(attributeName, TableSchema.PROP_DATATYPE_PRECISION);

                            if (precisionAsDouble == null)
                            {
                                specificDatatype = "integer";
                            } else if (radixAsDouble != null)
                            {
                                int radix = radixAsDouble.intValue();
                                int precision = precisionAsDouble.intValue();

                                if (radix == 10)
                                {
                                    if (precision <= 10)
                                    {
                                        specificDatatype = "integer";
                                    } else
                                    {
                                        specificDatatype = "bigint";
                                    }
                                } else if (radix == 2)
                                {
                                    if (precision <= 32)
                                    {
                                        specificDatatype = "integer";
                                    } else
                                    {
                                        specificDatatype = "bigint";
                                    }
                                }
                            }
                        } else
                        {
                            specificDatatype = "decimal";
                        }
                    } else
                    {
                        specificDatatype = "decimal";
                    }

                    break;
                case PersistentDatatype.YEAR:
                    specificDatatype = "character varying";
                    signature.setAttributeProperty(attributeName, TableSchema.PROP_DATATYPE_LENGTH, 4);
                    break;
                case PersistentDatatype.TIME:
                    specificDatatype = "time with time zone";
                    break;
                case PersistentDatatype.DATE:
                    specificDatatype = "date";
                    break;
                case PersistentDatatype.TIMESTAMP:
                    specificDatatype = "timestamp without time zone";
                    break;
                case PersistentDatatype.TIMESTAMP_TIMEZONE:
                    specificDatatype = "timestamp with time zone";
                    break;
                case PersistentDatatype.BLOB:
                    specificDatatype = "bytea";
                    break;
                default:
                    specificDatatype = datatype;
                    break;
            }

            if (signature.getAttributeProperty(attributeName, TableSchema.PROP_IS_AUTOINCREMENT) != null && (Boolean) (signature.getAttributeProperty(attributeName, TableSchema.PROP_IS_AUTOINCREMENT)))
            {
                switch (specificDatatype)
                {
                    case "integer":
                        specificDatatype = "serial";
                        break;
                    case "bigint":
                        specificDatatype = "bigserial";
                        break;
                }
            }

            //Set general datatype
            signature.setAttributeProperty(attributeName, TableSchema.PROP_DATATYPE, specificDatatype);
        }
    }

    @Override
    public void specifyReference(Reference reference)
    {
        if (reference.getOnUpdate() != null && reference.getOnUpdate().equals("NO_ACTION"))
        {
            reference.setOnUpdate("NO ACTION");
        }
    }

    @Override
    public Class convertToClass(String datatype)
    {
        switch (datatype.toLowerCase())
        {
            case "character varying":
                return java.lang.String.class;
            case "text":
                return java.lang.String.class;
            case "bigint":
                return java.lang.Long.class;
            case "boolean":
                return java.lang.Boolean.class;
            case "integer":
                return java.lang.Integer.class;
            case "timestamp with time zone":
                return java.util.Calendar.class;
            case "timestamp without time zone":
                return java.util.Calendar.class;
            case "timestamp":
                return java.util.Calendar.class;
            case "date":
                return java.util.Calendar.class;
            case "double precision":
                return java.lang.Double.class;
            default:
                return Object.class;
        }
    }
}
