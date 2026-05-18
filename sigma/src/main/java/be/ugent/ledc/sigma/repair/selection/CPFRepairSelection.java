package be.ugent.ledc.sigma.repair.selection;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;

import java.util.Set;

public interface CPFRepairSelection
{
    <T extends Comparable<? super T>> DataObject selectRepair(Set<CPF> minCostChangeExpressions, DataObject originalObject);
}
