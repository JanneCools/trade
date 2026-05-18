package be.ugent.ledc.core.binding.jdbc.schema;

import be.ugent.ledc.core.binding.jdbc.schema.generator.TableSchemaFilter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Builds a visualisation of a Database schema by building a mermaid ER diagram
 * of the schema.
 *
 * @author abronsel
 */
public class Diagrammer
{
    private static final Logger LOGGER = Logger.getLogger(Diagrammer.class.getName());
    
    public static void buildDiagram(DatabaseSchema databaseSchema, File outputFile, DiagrammerOptions options) throws FileNotFoundException
    {
        if(!outputFile.getName().endsWith(".html"))
            LOGGER.log(Level.WARNING, "Trying to output to file {0}. Expected .html file.", outputFile.getName());
        
        PrintWriter writer = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(outputFile),
                                StandardCharsets.UTF_8)
                ));

        //Write open tag
        writer.println("<html>");
        
        writer.println("<head>");
	writer.println("<style>");
	writer.println(".er.entityBox");
	writer.println("{");
	writer.println("\tstroke: black !important;");
        writer.println("\tfill: #EEEEEE !important;");
	writer.println("}");
	writer.println(".er.attributeBoxOdd");
	writer.println("{");
	writer.println("\tstroke: black !important;");
	writer.println("}");
	writer.println(".er.attributeBoxEven");
	writer.println("{");
	writer.println("\tstroke: black !important;");
	writer.println("}");
	writer.println("</style>");
        writer.println("</head>");
        
        //Write script preamble
        writer.println("<script src=\"https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js\"></script>");
        writer.println("<script>mermaid.initialize({startOnLoad:true});</script>");
        
        writer.println("<div class=\"mermaid\">");
        writer.println("erDiagram");
        
        TableSchemaFilter filter = options.createFilter();
        
        for(TableSchema tableSchema : databaseSchema.getTableSchemas())
        {
            if(filter.accept(tableSchema) 
            && (!options.isNoViews() || !tableSchema.getType().equals("VIEW")))
            {
                if(options.isShowRowCount())
                    writer.print(tableSchema.getName() + "_" + tableSchema.getRowCount() +"_rows");
                else
                    writer.print(tableSchema.getName());

                Set<String> displayNames = new HashSet<>();

                for(String aName: tableSchema.getAttributeNames())
                {
                    //Construct the display name
                    String displayName = tableSchema.getAttributeProperty(aName, TableSchema.PROP_DATATYPE)
                        + " " + aName;

                    //Is the attribute part of the primary key?
                    if(tableSchema.getPrimaryKey().contains(aName))
                        displayName += " PK";

                    if(options.isShowCheckConstraints() && tableSchema
                        .getCheckConstraints()
                        .stream()
                        .anyMatch(cc -> cc.getInvolvedAttributes().contains(aName))
                    )
                    {
                        displayName += " " + tableSchema
                            .getCheckConstraints()
                            .stream()
                            .filter(cc -> cc.getInvolvedAttributes().contains(aName))
                            .map(cc -> cc.getCheckClause())
                            .collect(Collectors.joining(";", "\"Checks: ", "\""));
                    }

                    displayNames.add(displayName);
                }

                writer.println(
                    displayNames
                        .stream()
                        .collect(Collectors.joining("\n\t", "{", "}"))
                );
            }
        }
        
        for(TableSchema tableSchema : databaseSchema.getTableSchemas())
        {
            if(!filter.accept(tableSchema))
                continue;
            
            for(Reference r : tableSchema.getReferences())
            {
                if(!filter.accept(r.getReferenced()))
                    continue;
                
                //Is the FK also a primary key?
                boolean fkIsPrimary = r.keySet().equals(tableSchema.getPrimaryKey());
                
                //Is the FK obliged?
                boolean fkRequired = fkIsPrimary
                    || r.keySet()
                        .stream()
                        .allMatch(a ->
                            tableSchema.getAttributeProperty(a, TableSchema.PROP_IS_NULLABLE) != null
                        &&  tableSchema.getAttributeProperty(a, TableSchema.PROP_IS_NULLABLE).equals(Boolean.FALSE));
                
                //Is the FK unique?
                boolean fkUnique = fkIsPrimary
                    || tableSchema
                        .getUniqueConstraints()
                        .values()
                        .stream()
                        .anyMatch(uc -> uc.containsAll(r.keySet()));
                
                //If the FK is unique AND obliged then we have 1 to 1
                //We can never enforce existence of a row in this relation,
                //so lower bound is zero
                String connector = fkRequired && fkUnique? "|o" : "}o";
                
                connector += "--";
                
                //If the FK is required, the minimal is 1 else 0
                connector += fkRequired ? "||" : "o|";
                
                List<String> lhs = new ArrayList<>();
                List<String> rhs = new ArrayList<>();
                
                for(String k: r.keySet())
                {
                    lhs.add(k);
                    rhs.add(r.get(k));
                }
                
                
                String lhsLabel = lhs
                    .stream()
                    .collect(Collectors.joining(",","(",")"));
                
                String rhsLabel = rhs
                    .stream()
                    .collect(Collectors.joining(",","(",")"));
                
                //If FK mapping is the identity mapping, simplify notation
                String label = lhsLabel.equals(rhsLabel)
                    ? lhsLabel
                    : lhsLabel + " -> " + rhsLabel;
                
                String leftName = options.isShowRowCount()
                    ? tableSchema.getName() + "_" + tableSchema.getRowCount() +"_rows"
                    : tableSchema.getName();
                
                String rightName = options.isShowRowCount()
                    ? r.getReferenced().getName() + "_" + r.getReferenced().getRowCount() +"_rows"
                    : r.getReferenced().getName();
                
                writer.println(leftName
                    + " "
                    + connector
                    + " "
                    + rightName
                    + ": \""
                    + label
                    + "\"");
            }
        }
        
        
        //Write close tags
        writer.println("</div>");
        writer.println("</html>");
        
        //Flush and close
        writer.flush();
        writer.close();
    }
}
