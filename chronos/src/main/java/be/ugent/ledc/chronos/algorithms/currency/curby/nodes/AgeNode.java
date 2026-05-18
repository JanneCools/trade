package be.ugent.ledc.chronos.algorithms.currency.curby.nodes;

import be.ugent.ledc.chronos.algorithms.currency.curby.distribution.ProbabilityTable;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.operators.UnitScore;
import java.util.TreeMap;

public abstract class AgeNode<T> extends Node<Integer>
{    
    private T previousValue = null;
    
    private Integer age = null;
    
    private TreeMap<Integer, UnitScore> ageMap;
    
    public AgeNode(String attribute)
    {
        super(attribute);
        this.ageMap = new TreeMap<>();
    }

    public UnitScore currency()
    {
        return belief().probability(age);
    }
    
    @Override
    public ProbabilityTable<Integer> belief()
    {
        return new ProbabilityTable<>(ageMap);
    }
    
    @Override
    public void updateState(DataObject current)
    {
        if(current == null)
        {
            age = age == null ? 0 : age + 1;
            return;
        }
        
        Object cValue = current.get(getAttribute());
        
        //Check if a change has occurred
        if(cValue != null && !cValue.equals(getPreviousValue()))
        {
            setAge(0);
            setPreviousValue((T)cValue);
        }
        else
        {
            age = age == null ? 0 : age + 1;
        }
    }

    protected T getPreviousValue()
    {
        return previousValue;
    }

    protected void setPreviousValue(T previousValue)
    {
        this.previousValue = previousValue;
    }

    protected Integer getAge()
    {
        return age;
    }
    
    protected void setAge(Integer age)
    {
        this.age = age;
    }

    /**
     * The shift operation models the advancement of the time with one unit.
     * It thus takes the previous age distribution and shifts all points
     * with one unit to the right, leaving the probability of age=0 unspecified.
     * @return 
     */
    protected TreeMap<Integer, UnitScore> shift()
    {
        TreeMap<Integer, UnitScore> shiftedMap = new TreeMap<>();
        
        for(Integer a: ageMap.keySet())
        {
            shiftedMap.put(a+1, getAgeMap().get(a));
        }
        
        return shiftedMap;
    }

    protected TreeMap<Integer, UnitScore> getAgeMap()
    {
        return ageMap;
    }

    @Override
    public void clear()
    {
        this.ageMap = new TreeMap<>();
        this.previousValue = null;
        this.age = null;
    }
}
