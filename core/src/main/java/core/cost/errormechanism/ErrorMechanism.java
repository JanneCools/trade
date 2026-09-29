package core.cost.errormechanism;

import core.dataset.DataObject;
import java.util.List;

public interface ErrorMechanism<T>
{
    boolean explains(T originalValue, T repairedValue, DataObject originalObject);
    
    List<T> explanations(T originalValue, DataObject originalObject);   
}
