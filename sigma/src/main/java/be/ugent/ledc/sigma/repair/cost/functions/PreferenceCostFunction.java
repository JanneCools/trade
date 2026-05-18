package be.ugent.ledc.sigma.repair.cost.functions;

import be.ugent.ledc.core.cost.CostFunction;
import be.ugent.ledc.core.dataset.DataObject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PreferenceCostFunction<T extends Comparable<? super T>> implements CostFunction<T>
{
    private final List<T> preferenceOrder;
    
    private final boolean weakBias;

    public PreferenceCostFunction(List<T> preferenceOrder, boolean weakBias)
    {
        this.preferenceOrder = preferenceOrder;
        this.weakBias = weakBias;
    }
    
    public PreferenceCostFunction(List<T> preferenceOrder)
    {
        this(preferenceOrder, true);
    }
    
    public PreferenceCostFunction(T... preferenceOrder)
    {
        this(Stream
            .of(preferenceOrder)
            .collect(Collectors.toList())
        );
    }
    
    @Override
    public int computeCost(T originalValue, T repairedValue, DataObject originalObject)
    {
        if(originalValue == null)
            return 1;
        
        if(repairedValue == null)
            return Integer.MAX_VALUE;
        
        int po = preference(originalValue);
        int pr = preference(repairedValue);
        
        return weakBias
            ? (pr <= po ? 1 : 1 + (pr - po))
            : pr + 1;
    }

    private int preference(T value)
    {
        int index = preferenceOrder.indexOf(value);
        
        return index == -1
            ? preferenceOrder.size()
            : index;
    }

    public List<T> getPreferenceOrder()
    {
        return preferenceOrder;
    }

    public boolean isWeakBias()
    {
        return weakBias;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.preferenceOrder);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final PreferenceCostFunction<?> other = (PreferenceCostFunction<?>) obj;
        return Objects.equals(this.preferenceOrder, other.preferenceOrder);
    }
    
}
