package be.ugent.ledc.sigma.datastructures.contracts;

import be.ugent.ledc.core.dataset.contractors.TypeContractor;

public abstract class NominalContractor<T extends Comparable<? super T>> extends SigmaContractor<T>
{
    public NominalContractor(TypeContractor<T> embeddedContractor)
    {
        super(embeddedContractor);
    }
}
