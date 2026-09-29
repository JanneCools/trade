package sigma.datastructures.fptree;

import core.datastructures.Pair;

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
