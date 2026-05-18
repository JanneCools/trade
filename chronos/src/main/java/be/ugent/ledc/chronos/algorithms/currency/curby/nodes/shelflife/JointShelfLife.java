package be.ugent.ledc.chronos.algorithms.currency.curby.nodes.shelflife;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.operators.UnitScore;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The multi-variable equivalent of the shelf life, where age is determined by
 * any change to one of the given attributes.
 * @author abronsel
 * @param <T> 
 */
public class JointShelfLife<T> extends GeometricShelfLife<T>
{
    private final Set<String> attributes;
    
    private final Map<String, Object> previousValues;

    public JointShelfLife(Set<String> attributes, UnitScore p, String attribute)
    {
        super(attribute, p);
        this.attributes = attributes;
        this.previousValues = new HashMap<>();
        attributes
            .stream()
            .forEach(a -> previousValues.put(a,null));
    }

    @Override
    public String getType()
    {
        return "joint";
    }

    @Override
    public void updateState(DataObject current)
    {
        if(current == null)
        {
            setAge(getAge() == null ? 0 : getAge() + 1);
            return;
        }      
        
        //Check if a change has occurred
        if(attributes
            .stream()
            .anyMatch(a ->
                    current.get(a) != null
                && !current.get(a).equals(previousValues.get(a))))
        {
            setAge(0);
            
            //Update previous values
            attributes
                .stream()
                .filter(a->current.get(a) != null)
                .forEach(a -> previousValues.put(a, current.get(a)));
        }
        else
        {
            setAge(getAge() == null ? 0 : getAge() + 1);
        }
    }

    @Override
    public void clear()
    {
        super.clear();
        this.previousValues.clear();
        this.attributes
            .stream()
            .forEach(a -> previousValues.put(a,null));
    }
    
    

}
