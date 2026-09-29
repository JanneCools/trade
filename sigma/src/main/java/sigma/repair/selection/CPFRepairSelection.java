package sigma.repair.selection;

import core.dataset.DataObject;
import sigma.datastructures.formulas.CPF;

import java.util.Set;

public interface CPFRepairSelection
{
    <T extends Comparable<? super T>> DataObject selectRepair(Set<CPF> minCostChangeExpressions, DataObject originalObject);
}
