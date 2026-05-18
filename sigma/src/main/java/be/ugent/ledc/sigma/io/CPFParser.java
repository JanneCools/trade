package be.ugent.ledc.sigma.io;

import be.ugent.ledc.core.ParseException;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CPFParser {

    public static CPF parseCPF(String line, Map<String, SigmaContractor<?>> contractors) throws ParseException
    {
        Set<AbstractAtom<?, ?, ?>> allAtoms = new HashSet<>();

        String[] stringAtoms = line.trim().split("\\s+(?<!\\\\)".concat(CPF.ATOM_SEPARATOR).concat("\\s+"));

        for (String stringAtom : stringAtoms)
        {
            AbstractAtom<?, ?, ?> a = AtomParser.parseAtom(stringAtom.trim(), contractors);
            allAtoms.add(a);
        }

        CPF cpf = new CPF(allAtoms);

        for(String attribute: cpf.getAttributes())
        {
            if(!contractors.containsKey(attribute))
                throw new ParseException("A CPF must involve only attributes specified in the contractors."
                        + "\nFound attribute: " + attribute
                        + "\nContracted attributes: " + contractors.keySet());
        }

        return cpf;
    }

}
