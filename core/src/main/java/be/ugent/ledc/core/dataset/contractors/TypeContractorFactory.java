package be.ugent.ledc.core.dataset.contractors;

import be.ugent.ledc.core.binding.jdbc.agents.PersistentDatatype;
import be.ugent.ledc.core.dataset.DataObject;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.List;
import java.util.stream.Stream;

public class TypeContractorFactory
{    
    public static final TypeContractor<String> STRING = new TypeContractor<String>
    (o -> o == null || o instanceof String)
    {
        @Override
        public String getFromDataObject(DataObject o, String attribute)
        {
            return o.getString(attribute);
        }

        @Override
        public String name()
        {
            return "string";
        }

        @Override
        public String toPersistentDatatype(boolean verbose)
        {
            return PersistentDatatype.TEXT;
        }
    };
    
    public static final TypeContractor<Integer> INTEGER = new TypeContractor<Integer>
    (t -> t == null || t instanceof Integer)
    {
        @Override
        public Integer getFromDataObject(DataObject o, String attribute)
        {
            return o.getInteger(attribute);
        }

        @Override
        public String name()
        {
            return "integer";
        }
        
        @Override
        public String toPersistentDatatype(boolean verbose)
        {
            return PersistentDatatype.INT;
        }
    };
    
    public static final TypeContractor<Long> LONG = new TypeContractor<Long>
    (t -> t == null || t instanceof Long)
    {
        @Override
        public Long getFromDataObject(DataObject o, String attribute)
        {
            return o.getLong(attribute);
        }

        @Override
        public String name()
        {
            return "long";
        }
        
        @Override
        public String toPersistentDatatype(boolean verbose)
        {
            return PersistentDatatype.BIGINT;
        }
    };
    
    public static final TypeContractor<Boolean> BOOLEAN = new TypeContractor<Boolean>
    (t -> t == null || t instanceof Boolean)
    {
        @Override
        public Boolean getFromDataObject(DataObject o, String attribute)
        {
            return o.getBoolean(attribute);
        }

        @Override
        public String name()
        {
            return "boolean";
        }
        
        @Override
        public String toPersistentDatatype(boolean verbose)
        {
            return PersistentDatatype.BOOLEAN;
        }
    };
    
    public static final TypeContractor<BigDecimal> BIGDECIMAL = new TypeContractor<BigDecimal>
    (t -> t == null || t instanceof BigDecimal)
    {
        @Override
        public BigDecimal getFromDataObject(DataObject o, String attribute)
        {
            return o.getBigDecimal(attribute);
        }
        
        @Override
        public String name()
        {
            return "bigdecimal";
        }
        
        @Override
        public String toPersistentDatatype(boolean verbose)
        {
            return verbose
                ? PersistentDatatype.NUMERIC + "(30,10)"
                : PersistentDatatype.NUMERIC;
        }
    };
    
    public static final TypeContractor<Double> DOUBLE = new TypeContractor<Double>
    (t -> t == null || t instanceof Double)
    {
        @Override
        public Double getFromDataObject(DataObject o, String attribute)
        {
            return o.getDouble(attribute);
        }

        @Override
        public String name()
        {
            return "double";
        }
        
        @Override
        public String toPersistentDatatype(boolean verbose)
        {
            return PersistentDatatype.BIGFLOAT;
        }
    };
    
    public static final TypeContractor<Float> FLOAT = new TypeContractor<Float>
    (t -> t == null || t instanceof Float)
    {
        @Override
        public Float getFromDataObject(DataObject o, String attribute)
        {
            return o.getFloat(attribute);
        }

        @Override
        public String name()
        {
            return "float";
        }
        
        @Override
        public String toPersistentDatatype(boolean verbose)
        {
            return PersistentDatatype.FLOAT;
        }
    };
    
    public static final TypeContractor<LocalDate> DATE = new TypeContractor<LocalDate>
    (t -> t == null || t instanceof LocalDate)
    {
        @Override
        public LocalDate getFromDataObject(DataObject o, String attribute)
        {
            return o.getDate(attribute);
        }

        @Override
        public String name()
        {
            return "date";
        }
        
        @Override
        public String toPersistentDatatype(boolean verbose)
        {
            return PersistentDatatype.DATE;
        }
    };
    
    public static final TypeContractor<LocalTime> TIME = new TypeContractor<LocalTime>
    (t -> t == null || t instanceof LocalTime)
    {
        @Override
        public LocalTime getFromDataObject(DataObject o, String attribute)
        {
            return o.getTime(attribute);
        }

        @Override
        public String name()
        {
            return "time";
        }
        
        @Override
        public String toPersistentDatatype(boolean verbose)
        {
            return PersistentDatatype.TIME;
        }
    };
    
    public static final TypeContractor<LocalDateTime> DATETIME = new TypeContractor<LocalDateTime>
    (t -> t == null || t instanceof LocalDateTime)
    {
        @Override
        public LocalDateTime getFromDataObject(DataObject o, String attribute)
        {
            return o.getDateTime(attribute);
        }

        @Override
        public String name()
        {
            return "datetime";
        }
        
        @Override
        public String toPersistentDatatype(boolean verbose)
        {
            return PersistentDatatype.TIMESTAMP;
        }
    };
    
    public static final TypeContractor<Duration> DURATION = new TypeContractor<Duration>
    (t -> t == null || t instanceof Duration)
    {
        @Override
        public Duration getFromDataObject(DataObject o, String attribute)
        {
            return o.getDuration(attribute);
        }

        @Override
        public String name()
        {
            return "duration";
        }
        
        @Override
        public String toPersistentDatatype(boolean verbose)
        {
            return null;
        }        
    };
    
    public static final TypeContractor<Period> PERIOD = new TypeContractor<Period>
    (t -> t == null || t instanceof Period)
    {
        @Override
        public Period getFromDataObject(DataObject o, String attribute)
        {
            return o.getPeriod(attribute);
        }

        @Override
        public String name()
        {
            return "period";
        }
        
        @Override
        public String toPersistentDatatype(boolean verbose)
        {
            return null;
        }        
    };
    
    public static final TypeContractor<Instant> INSTANT = new TypeContractor<Instant>
    (t -> t == null || t instanceof Instant)
    {
        @Override
        public Instant getFromDataObject(DataObject o, String attribute)
        {
            return o.getInstant(attribute);
        }

        @Override
        public String name()
        {
            return "instant";
        }
        
        @Override
        public String toPersistentDatatype(boolean verbose)
        {
            return null;
        }
    };
    
    public static final TypeContractor<List> LIST = new TypeContractor<List>
    (t -> t == null || t instanceof List)
    {
        @Override
        public List getFromDataObject(DataObject o, String attribute)
        {
            return o.getList(attribute);
        }

        @Override
        public String name()
        {
            return "list";
        }
        
        @Override
        public String toPersistentDatatype(boolean verbose)
        {
            return null;
        }
    };
    
    public static final TypeContractor<DataObject> DATA_OBJECT = new TypeContractor<DataObject>
    (t -> t == null || t instanceof DataObject)
    {
        @Override
        public DataObject getFromDataObject(DataObject o, String attribute)
        {
            return o.getDataObject(attribute);
        }

        @Override
        public String name()
        {
            return "dataobject";
        }
        
        @Override
        public String toPersistentDatatype(boolean verbose)
        {
            return null;
        }
    };
    
    public static final TypeContractor<Object> OBJECT = new TypeContractor<Object>
    (BasicContractors.UNBOUND)
    {
        @Override
        public Object getFromDataObject(DataObject o, String attribute)
        {
            return o.get(attribute);
        }

        @Override
        public String name()
        {
            return "object";
        }
        
        @Override
        public String toPersistentDatatype(boolean verbose)
        {
            return null;
        }
    };
    
    public static final Stream<TypeContractor<?>> getTypedContractors()
    {
        return Stream.of
        (STRING,
        INTEGER,
        LONG,
        BOOLEAN,
        DOUBLE,
        FLOAT,
        BIGDECIMAL,
        DATE,
        TIME,
        DATETIME,
        DURATION,
        PERIOD,
        INSTANT,
        LIST,
        DATA_OBJECT,
        OBJECT        
        );
    }
}
