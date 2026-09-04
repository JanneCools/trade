package be.ugent.ledc.sigma.io;

import be.ugent.ledc.core.ParseException;
import be.ugent.ledc.sigma.datastructures.atoms.AbstractAtom;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractor;
import be.ugent.ledc.sigma.datastructures.contracts.SigmaContractorUtil;
import be.ugent.ledc.sigma.datastructures.formulas.CPF;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;
import static be.ugent.ledc.sigma.io.ConstraintIo.COMMENT_PREFIX;
import static be.ugent.ledc.sigma.io.ConstraintIo.CONTRACT_PREFIX;
import static be.ugent.ledc.sigma.io.ConstraintIo.CONTRACT_SEPARATOR;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SigmaRuleParser
{
    public static final String NEGATION = "NOT";

    private static SigmaRule parseSigmaRule(String atomsAsString, Map<String, SigmaContractor<?>> contractors, boolean startWithNot) throws ParseException
    {
        //Parse CPF
        CPF cpf = CPFParser.parseCPF(atomsAsString, contractors);

        //Sanity check for format
        if(!startWithNot && cpf.getAtoms().size() > 1)
            throw new ParseException("As of ledc-sigma 1.2, selection rules with more than one atom "
                + "must adhere to the from "
                + NEGATION
                + "("
                + "atom_1 "
                + CPF.ATOM_SEPARATOR
                + " ... "
                + CPF.ATOM_SEPARATOR
                + " atom_k"
                + ")\n"
                + "Rules not starting with '" + NEGATION + "' are limited to a single atom."
            );
        
        //Sanity check for listed attributes
        for(String attribute: cpf.getAttributes())
        {
            if(!contractors.containsKey(attribute))
                throw new ParseException("A CPF must involve only attributes specified in the contractors."
                    + "\nFound attribute: " + attribute
                    + "\nContracted attributes: " + contractors.keySet());
        }
        
        return startWithNot
            ? new SigmaRule(cpf.getAtoms())
            : new SigmaRule(cpf.getAtoms()
                .stream()
                .map(AbstractAtom::getInverse)
                .collect(Collectors.toSet()));
    }
    
    public static SigmaRule parseSigmaRule(String line, Map<String, SigmaContractor<?>> contractors) throws ParseException
    {
        //General structure of an edit rule
        Pattern srPattern = Pattern.compile(NEGATION + "\\s+(.+)", Pattern.CASE_INSENSITIVE);
        Matcher srMatcher = srPattern.matcher(line.trim());

        if(srMatcher.matches())
        {
            return new SigmaRule(
                parseSigmaRule(
                    srMatcher.group(1).trim(),
                    contractors,
                    true
                ).getAtoms());
        }
        else
        {
            return new SigmaRule(
                parseSigmaRule(
                    line.trim(),
                    contractors,
                    false
                ).getAtoms());
        }
    }

    public static SigmaRuleset parseSigmaRuleSet(List<String> lines) throws ParseException {
        //Initialize the mapping of contractors
        Map<String, SigmaContractor<?>> contractors = new HashMap<>();
        Set<SigmaRule> sigmaRules = new HashSet<>();

        for(String line: lines)
        {
            //Comments are ignored
            if (line.startsWith(COMMENT_PREFIX) || line.isEmpty())
                continue;

            //If the line declares an attribute: we parse the name and contract
            if (line.startsWith(CONTRACT_PREFIX))
                parseAttributeTypedContract(line, contractors);
            //Else, we read the rule and add it
            else
                sigmaRules.add(parseSigmaRule(line, contractors));

        }
        return new SigmaRuleset(contractors, sigmaRules);
    }
    
    /**
     * A separate parser for contracts only.
     * @param lines
     * @return
     * @throws java.io.FileNotFoundException
     * @throws ParseException
    */
    public static Map<String, SigmaContractor<?>> parseContractors(List<String> lines) throws ParseException {
        //Initialize the mapping of contractors
        Map<String, SigmaContractor<?>> contractors = new HashMap<>();

        for(String line: lines)
        {
            //Comments are ignored
            if (line.startsWith(COMMENT_PREFIX) || line.trim().isEmpty())
                continue;

            //If the line declares an attribute: we parse the name and contract
            parseAttributeTypedContract(line, contractors);
        }
        return contractors;
    }

    /**
     * A separate parser for sigma rules only.
     * @param lines
     * @param contractors
     * @return 
     * @throws ParseException
    */
    public static SigmaRuleset parseSigmaRuleSet(List<String> lines, Map<String, SigmaContractor<?>> contractors) throws ParseException {
        //Initialize the mapping of contractors
        Set<SigmaRule> sigmaRules = new HashSet<>();

        for(String line: lines)
        {
            //Comments are ignored
            if (line.startsWith(COMMENT_PREFIX) || line.isEmpty())
                continue;

            sigmaRules.add(parseSigmaRule(line, contractors));

        }
        
        return new SigmaRuleset(contractors, sigmaRules);
    }
    
    private static void parseAttributeTypedContract(String line, Map<String, SigmaContractor<?>> contractors) throws ParseException
    {
        if(!line.contains(CONTRACT_SEPARATOR))
            throw new ParseException("Contract should be specified in the following format: "
                + CONTRACT_PREFIX 
                + "<ATTRIBUTE_NAME>"
                + CONTRACT_SEPARATOR
                + "<CONTRACTOR_NAME>");
        
        //Parse the attribute name
        String attribute = line
            .substring(1, line.trim().indexOf(CONTRACT_SEPARATOR))
            .trim();
        
        //Parse the contractor name
        String contractorName = line
            .substring(line.indexOf(":") + 1)
            .trim();
        
        //Convert into a contractor
        SigmaContractor<?> contractor = SigmaContractorUtil
            .getTypedContractorByName(contractorName);
        
        if(contractors.containsKey(attribute))
            throw new ParseException("Double specified contract for attribute '" + attribute  + "'.");
        
        //Register the contractor for that attribute
        contractors.put(attribute, contractor);
    }
}
