package be.ugent.ledc.chronos.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Plotter
{
    public static int PLOTS_PER_LINE = 3;
    
    public static <I extends Comparable<? super I>, N extends Number> void plot(List<Plot<I,N>> signals, File output) throws IOException
    {
        if(output == null)
            throw new IOException("No output file specified.");
        
        if(!output.getName().toLowerCase().endsWith(".html"))
            throw new IOException("Output file must an .html file. Found: " + output.getName());
        
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(output)));
        
        writer.println("<html>");
        writer.println("<script src=\"https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js\"></script>");
        writer.println("<script>mermaid.initialize({startOnLoad:true});</script>");
       
        writer.println("<table width=\"100%\">");
        
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.CEILING);
        DecimalFormatSymbols symb = DecimalFormatSymbols.getInstance();
        symb.setDecimalSeparator('.');
        df.setDecimalFormatSymbols(symb);
        
        int column = 0;
        
        for(Plot<I,N> plot: signals)
        {
            if(column == 0)
            {
                //New line
                writer.println("<tr>");
            }
            
            //Write config
            writer.println("<td>");
            writer.println("<div class=\"mermaid\">");
            writer.println("---");
            writer.println("config:");
            writer.println("  themeVariables:");
            writer.println("    xyChart:");
            writer.println("      plotColorPalette: "
                + plot
                    .getOptions()
                    .getHexColors()
                    .stream()
                    .map(h -> "'" + h + "'")
                    .collect(Collectors.joining(",")));
            writer.println("---");
            
            writer.println("xychart");

            if(plot.getOptions().getTitle() != null && !plot.getOptions().getTitle().isBlank())
            {
                writer.println("  title \"" + plot.getOptions().getTitle() + "\"");
            }
            
            List<String> x = new ArrayList<>();
            List<String> y = new ArrayList<>();
                        
            int stepSize = (int)Math.floor(1 / plot.getOptions().getSampleRate().getValue());
            
            I pointer = plot.getSignal().start();
                        
            while(pointer != null && pointer.compareTo(plot.getSignal().end()) <= 0)
            {
                double v = plot.getSignal().valueAt(pointer).doubleValue();
                        
                x.add(pointer.toString());
                y.add(df.format(v));
                pointer = plot.getSignal().jumpRight(pointer, stepSize);
            }
            

            writer.println("  x-axis " + x.stream().collect(Collectors.joining(",", "[", "]")));
            writer.println("  y-axis "
                + plot.getOptions().getyAxisCaption()
                + " "
                + df.format(plot.getOptions().getMinY())
                + " --> " + df.format(plot.getOptions().getMaxY())
            );
            writer.println("  line " + y.stream().collect(Collectors.joining(",", "[", "]")));
            
            writer.println("</div>");
            writer.println("</td>");
            
            //Handle column count
            column++;
            
            //Check if end of line
            if(column == PLOTS_PER_LINE)
            {
                column=0;
                writer.println("</tr>");
            }   
        }
        
        if(column < PLOTS_PER_LINE)
            writer.println("</tr>");
        
        writer.println("</table>");
        writer.println("</html>");

        
        writer.flush();
        writer.close();
        
    }
}
