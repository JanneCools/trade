package be.ugent.ledc.sigma.io;

import be.ugent.ledc.core.ParseException;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRule;
import be.ugent.ledc.sigma.datastructures.rules.SigmaRuleset;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ConstraintIo
{
    public static final String COMMENT_PREFIX   = "--";
    public static final String CONTRACT_PREFIX  = "@";
    public static final String CONTRACT_SEPARATOR  = ":";
    
    public static void writeSigmaRuleSet(SigmaRuleset ruleset, File file, Charset charset) throws FileNotFoundException
    {
        writeSigmaRuleSet(ruleset, file, charset, false, true);
    }

    public static void writeSigmaRuleSet(SigmaRuleset ruleset, File file, Charset charset, boolean append, boolean simplifyDomainConstraints) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream(file, append),
                        charset));

        writer.println("-- Contracts");

        //Write the contracts in format @<attribute>:<contractnam>
        ruleset
                .getContractors()
                .entrySet()
                .stream()
                .map(e -> COMMENT_PREFIX.concat(e.getKey()).concat(CONTRACT_SEPARATOR).concat(e.getValue().name()))
                .forEach(writer::println);

        writer.println("-- Domain constraints");

        ruleset
                .getRules()
                .stream()
                .filter(rule -> rule.getInvolvedAttributes().size() == 1)
                .flatMap(rule -> rule.getAtoms().stream())
                .map(atom -> simplifyDomainConstraints
                        ? atom.getInverse().toString()
                        : SigmaRuleParser.NEGATION + " " + atom.toString()
                )
                .forEach(writer::println);

        writer.println("-- Interaction constraints");

        ruleset
                .getRules()
                .stream()
                .filter(rule -> rule.getInvolvedAttributes().size() > 1)
                .map(SigmaRule::toString)
                .forEach(writer::println);

        writer.flush();
        writer.close();
    }
    
    public static void writeSigmaRuleSet(SigmaRuleset ruleset, File file) throws FileNotFoundException
    {
        writeSigmaRuleSet(ruleset, file, StandardCharsets.UTF_8);
    }
    

    public static SigmaRuleset readSigmaRuleSet(File file) throws IOException, ParseException {
        return readSigmaRuleSet(file, StandardCharsets.UTF_8);
    }

    public static SigmaRuleset readSigmaRuleSet(File file, Charset charset) throws IOException, ParseException {

        Scanner scanner = new Scanner(file, charset);

        List<String> lines = new ArrayList<>();

        while (scanner.hasNextLine())
        {
            //Read the line, ignore leading and trailing spaces
            lines.add(scanner.nextLine().trim());
        }

        return SigmaRuleParser.parseSigmaRuleSet(lines);
    }
}
