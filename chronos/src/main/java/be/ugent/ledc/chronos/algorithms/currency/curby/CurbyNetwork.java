package be.ugent.ledc.chronos.algorithms.currency.curby;

import be.ugent.ledc.chronos.ChronosException;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.AgeNode;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.INode;
import be.ugent.ledc.chronos.datastructures.Signal;
import be.ugent.ledc.chronos.datastructures.TemporalDataset;
import be.ugent.ledc.core.dataset.DataObject;
import be.ugent.ledc.core.util.SetOperations;
import be.ugent.ledc.sigma.datastructures.contracts.OrdinalContractor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import be.ugent.ledc.chronos.algorithms.currency.curby.nodes.Node;
import be.ugent.ledc.core.datastructures.DependencyGraph;
import be.ugent.ledc.core.datastructures.DependencyGraphException;
import be.ugent.ledc.core.operators.UnitScore;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Models a Bayesian network that is fed with the values of a Signal time by time.
 * @author abronsel
 * @param <I> Type of time indicator.
 */
public class CurbyNetwork<I extends Comparable<? super I>>
{    
    private final List<Node<?>> nodes;
    
    private final boolean skipNullObjects;
    
    public CurbyNetwork(Set<Node<?>> nodes, boolean skipNullObjects) throws ChronosException
    {
        this.skipNullObjects = skipNullObjects;
        
        DependencyGraph<Node<?>> dependencyGraph = new DependencyGraph<>();
        
        for(Node<?> n: nodes)
        {
            if(!n.getParents().isEmpty())
            {
                for(INode<?> parent: n.getParents())
                {
                    dependencyGraph.addDependent(n, (Node<?>)parent);
                }
            }
            else
            {
                dependencyGraph.addDependent(n);
            }
        }
        
        try
        {
            this.nodes = dependencyGraph.toList();
        }
        catch (DependencyGraphException ex)
        {
            throw new ChronosException(ex);
        }
    }
    
    public CurbyNetwork(Set<Node<?>> nodes) throws ChronosException
    {
        this(nodes, false);
    }
    
    public CurbyNetwork(Node<?> ... nodes) throws ChronosException
    {
        this(SetOperations.set(nodes));
    }

    public Map<String, Signal<I,UnitScore>> estimate(TemporalDataset<I> data) throws ChronosException
    {        
        OrdinalContractor<I> ctr = data.indexContractor();
        
        I time = data.start();
        I end  = data.end();
        
        Map<String, Signal<I,UnitScore>> currencySignalMap = new HashMap();

        for(Node n : nodes)
        {
            if(n instanceof AgeNode an)
                currencySignalMap.put(
                    an.getAttribute(),
                    new Signal<>(data.indexContractor())
                );
        }
        
        Signal<I,DataObject> oSignal = data.getObjectSignal();
               
        while(ctr.isBeforeOrEqual(time, end))
        {           
            DataObject current = new DataObject();
            
            if(oSignal.hasValueAt(time))
            {
                current = current.concat(oSignal.valueAt(time));
            }
            
//            System.out.println("Time " + time + " -> " + current);
            
            //Update model: we iterate nodes sensitive to dependencies in network
            for(Node<?> node: nodes)
            {
                node.updateState(current);
                node.updateBelief();
            }
    
            //Compute currency estimations
            if(!skipNullObjects || oSignal.hasValueAt(time))
            {
                for(Node n: nodes)
                {
                    if(n instanceof AgeNode an)
                    {
                        currencySignalMap
                            .get(n.getAttribute())
                            .put(time, an.currency());
                    }
                }
            }
            
            //Shift
            time = ctr.next(time);
        }
        
        return currencySignalMap;
    }
    
    public void clear()
    {
        for(Node<?> n: nodes)
        {
            n.clear();
        }
    }
    
    public void plotAsMermaid() throws IOException
    {
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter("curby-network.html")));
        
        writer.println("<html>");
        writer.println("<script src=\"https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js\"></script>");
        writer.println("<script>mermaid.initialize({startOnLoad:true});</script>");
        writer.println("<div class=\"mermaid\">");
        writer.println("graph LR");

        //Write nodes
        for(Node<?> n: nodes)
        {
            writer.println(n.getId() + "(" + n.getAttribute() + "</br>" + n.getType() + ");");
        }
        
        //Write edges
        for(Node<?> n: nodes)
        {
            for(INode<?> parent: n.getParents())
            {
                writer.println(n.getId() + " --> " + ((Node)parent).getId() + ";");
            }
        }
        
        //Write style info
        for(Node<?> n: nodes)
        {
            switch(n.getType())
            {
                case "quantified-aggregator", "change-conjunction" -> writer.println("style " + n.getId() + " fill:#DE6B48, stroke:#000000;");
                case "cc" -> writer.println("style " + n.getId() + " fill:#e5b181, stroke:#000000;");
                case "shelf-life", "geometric", "geometric-dynamic", "geometric-conditional" -> writer.println("style " + n.getId() + " fill:#f4b9b2, stroke:#000000;");
                case "poisson-age", "poisson-cusum" -> writer.println("style " + n.getId() + " fill:#daedbd, stroke:#000000;");
                case "data" -> writer.println("style " + n.getId() + " fill:#7dbbc3, stroke:#000000;");
            }
        }
        
        writer.println("</div>");

        
        writer.flush();
        writer.close();
    }
}
