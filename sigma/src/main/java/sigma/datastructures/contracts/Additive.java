package sigma.datastructures.contracts;

public interface Additive<T>
{
    T add(T value, long units) throws SigmaContractException;
    
    default T subtract(T value, long units) throws SigmaContractException
    {
        return add(value, - units);
    }
}
