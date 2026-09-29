package sigma.repair.selection;

import core.dataset.DataObject;
import sigma.datastructures.atoms.AbstractAtom;
import sigma.datastructures.rules.SigmaRule;
import sigma.repair.NoDonorException;

import java.util.Map;
import java.util.Set;

public interface DonorSelector {

    DataObject selectDonor(Set<String> solution, DataObject dirtyOriginal, Map<String, Set<AbstractAtom<?, ?, ?>>> permittedValuesMapping, Set<SigmaRule> variableConditions) throws NoDonorException;

}
