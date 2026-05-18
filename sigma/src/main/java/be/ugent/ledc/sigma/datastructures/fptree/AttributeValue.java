package be.ugent.ledc.sigma.datastructures.fptree;

import be.ugent.ledc.core.datastructures.Pair;

public class AttributeValue<V> extends Pair<String, V>
{
    public AttributeValue(String first, V second)
    {
        super(first, second);
    }
    
    @Override
    public String toString()
    {
        return getFirst() + "=" + getSecond();
    }
    
    public String getAttribute()
    {
        return getFirst();
    }
    
    public V getValue()
    {
        return getSecond();
    }
}
