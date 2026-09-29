package sigma.datastructures.contracts;

import core.dataset.contractors.TypeContractor;

public abstract class NominalContractor<T extends Comparable<? super T>> extends SigmaContractor<T>
{
    public NominalContractor(TypeContractor<T> embeddedContractor)
    {
        super(embeddedContractor);
    }
}
