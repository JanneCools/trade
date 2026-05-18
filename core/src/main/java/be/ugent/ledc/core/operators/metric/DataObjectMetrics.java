package be.ugent.ledc.core.operators.metric;

import be.ugent.ledc.core.dataset.DataObject;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public enum DataObjectMetrics implements Metric<Double, DataObject>
{
    EUCLIDEAN((o1,o2) -> Math.sqrt(Stream.concat(
            o1.getAttributes().stream(),
            o2.getAttributes().stream())
        .distinct()
        .filter(a -> o1.get(a) != null && o2.get(a) != null)
        .mapToDouble(a -> Math.pow(new DataObject(o1)
                .asDouble(a)
                .getDouble(a)
                -
                new DataObject(o2)
                .asDouble(a)
                .getDouble(a),
            2.0)
        )
        .sum())  
    ),
    
    FAST_EUCLIDEAN((o1,o2) -> 
        Math.sqrt(o1
            .getAttributes()
            .stream()
            .filter(a -> o1.get(a) != null && o2.get(a) != null)
            .mapToDouble(a -> (o1.getDouble(a) - o2.getDouble(a)) * (o1.getDouble(a) - o2.getDouble(a)))
            .sum())  
    ),
    
    MANHATTAN((o1,o2) -> Stream.concat(
            o1.getAttributes().stream(),
            o2.getAttributes().stream())
        .distinct()
        .filter(a -> o1.get(a) != null && o2.get(a) != null)
        .mapToDouble(a -> Math.abs(new DataObject(o1)
                .asDouble(a)
                .getDouble(a)
                -
                new DataObject(o2)
                .asDouble(a)
                .getDouble(a))
        )
        .sum()
    ),
    
    FAST_MANHATTAN((o1,o2) -> 
        o1
        .getAttributes()
        .stream()
        .filter(a -> o1.get(a) != null && o2.get(a) != null)
        .mapToDouble(a -> Math.abs(o1.getDouble(a) - o2.getDouble(a)))
        .sum()
    )
    ;

    private final BiFunction<DataObject, DataObject, Double> embeddedMetric;

    private DataObjectMetrics(BiFunction<DataObject, DataObject, Double> embeddedMetric)
    {
        this.embeddedMetric = embeddedMetric;
    }
    
    
        
    @Override
    public Double distance(DataObject o1, DataObject o2)
    {
        return embeddedMetric.apply(o1, o2);
    }
    
}
