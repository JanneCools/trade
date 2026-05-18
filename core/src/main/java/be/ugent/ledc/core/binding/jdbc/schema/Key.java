package be.ugent.ledc.core.binding.jdbc.schema;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * A Key is the internal representation of a primary key and is modeled as a set of attribute names.
 * @author antoon
 */
public class Key extends HashSet<String>
{

    public Key(){}
    
    public Key(String[] array)
    {
        super();
        addAll(Arrays.asList(array));
    }

    public Key(Collection<? extends String> c)
    {
        super(c);
    }

    public Key(int initialCapacity, float loadFactor)
    {
        super(initialCapacity, loadFactor);
    }

    public Key(int initialCapacity)
    {
        super(initialCapacity);
    }
    
}
