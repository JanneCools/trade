package be.ugent.ledc.sigma.repair.selection;

import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.repair.NoDonorException;

import java.util.Map;
import java.util.Set;

public interface DonorSelector {

    DataObject selectDonor(Set<String> solution, DataObject dirtyOriginal, Map<String, Set<AbstractAtom<?, ?, ?>>> permittedValuesMapping, Set<SigmaRule> variableConditions) throws NoDonorException;

}
