package be.ugent.ledc.core.datastructures.kdtree;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.dataset.contractors.TypeContractor;
import be.ugent.ledc.core.dataset.contractors.TypeContractorFactory;
import be.ugent.ledc.core.datastructures.Interval;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class BoundingBox<N extends Number & Comparable<? super N>>
{
    private final Map<String, Interval<N>> intervalMap;

    public BoundingBox(Collection<String> attributes)
    {
        intervalMap = attributes
                .stream()
                .collect(Collectors.toMap(
                    a -> a,
                    a -> new Interval<>(null,null)));
    }

    private BoundingBox(Map<String, Interval<N>> intervalMap)
    {
        this.intervalMap = intervalMap;
    }
    
    public boolean contains(DataObject query, TypeContractor<N> contractor)
    {
        return intervalMap
            .entrySet()
            .stream()
            .allMatch(etr -> intervalMap.get(etr.getKey()) != null
                && intervalMap
                    .get(etr.getKey())     
                    .contains(contractor
                        .getFromDataObject(query, etr.getKey())));
    }
    
    public boolean overlapsWithSphere(DataObject center, Double radius, TypeContractor<N> contractor)
    {
        if(isEmpty())
            return false;
        
        BoundingBox extended = new BoundingBox<>(intervalMap
            .keySet()
            .stream()
            .collect(Collectors.toMap(
                a -> a,
                a -> extend(intervalMap.get(a), radius))));
        
        return extended
            .contains(
                center.allAsDouble(),
                TypeContractorFactory.DOUBLE
            );
            
    }
    
    public BoundingBox leftUpdate(String attribute, N value)
    {
        Map<String, Interval<N>> uMap = initUpdate();
        
        if(uMap.get(attribute) != null)
        {
            N left = uMap.get(attribute).getLeftBound();

            if(left != null && value.compareTo(left) < 0)
            {
                uMap.put(attribute, null);
            }
            else
            {
                Interval<N> current = uMap.get(attribute);
                
                uMap.put(
                    attribute,
                    new Interval<>(
                        current.getLeftBound(),
                        value,
                        current.isLeftOpen(),
                        true)
                );
            }
        }
        
        return new BoundingBox(uMap);
    }
    
    public BoundingBox rightUpdate(String attribute, N value)
    {
        Map<String, Interval<N>> uMap = initUpdate();
        
        if(uMap.get(attribute) != null)
        {
            N right = uMap.get(attribute).getRightBound();

            if(right != null && value.compareTo(right) > 0)
            {
                uMap.put(attribute, null);
            }
            else
            {
                Interval<N> current = uMap.get(attribute);
                
                uMap.put(
                    attribute,
                    new Interval<>(
                        value,
                        current.getRightBound(),
                        false,
                        current.isRightOpen())
                );
            }
        }
        
        return new BoundingBox(uMap);
    }
    
    public boolean isEmpty()
    {
        return this
            .intervalMap
            .keySet()
            .stream()
            .anyMatch(a -> intervalMap.get(a) == null);
    }
    
    private Map<String, Interval<N>> initUpdate()
    {
        Map<String, Interval<N>> uMap = new HashMap<>();
        uMap.putAll(intervalMap);
        return uMap;
    }

    private Interval<Double> extend(Interval<N> current, Double radius)
    {
        Double extendedLeftBound = current.getLeftBound() == null
            || Double.NEGATIVE_INFINITY - current.getLeftBound().doubleValue() < radius
                ? null
                : current.getLeftBound().doubleValue() - radius;
        
        Double extendedRightBound = current.getRightBound() == null
            || Double.POSITIVE_INFINITY - current.getRightBound().doubleValue() < radius
                ? null
                : current.getRightBound().doubleValue() + radius;
        
        return new Interval<>(
            extendedLeftBound,
            extendedRightBound,
            extendedLeftBound == null ? true: current.isLeftOpen(),
            extendedRightBound == null ? true: current.isRightOpen()
        );
    }
    
}
