package be.ugent.ledc.sigma.datastructures.contracts;

public interface BackwardNavigator<T>
{
    boolean hasPrevious(T current);

    T previous(T current) throws SigmaContractException;
    
    T last();
}
