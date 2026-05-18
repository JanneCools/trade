package be.ugent.ledc.core.datastructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class represents a dependency graph between objects of type {@code T}.
 *
 * @param <T> the type of object that is contained within the dependency graph
 * and between which dependencies exist.
 */
public class DependencyGraph<T> {
    /**
     * The graph of T that are dependent on each other.
     * Each T is associated with a list of T that are its dependencies.
     */
    private final Map<T, Set<T>> dependencyGraph;

    private boolean validityChecked;

    public DependencyGraph() {
        this.dependencyGraph = new HashMap<>();
        this.validityChecked = true;
    }

    /**
     * Add a new object to the graph and specify that it has dependees:
     * objects on which it is dependent on, in any order.
     *
     * @param dependent
     * @param dependencies
     */
    @SafeVarargs
    public final void addDependent(T dependent, T... dependencies)
    {
        addDependent(dependent);

        for (T dependency : dependencies)
        {
            //add the dependency
            dependencyGraph.get(dependent).add(dependency);

            //add the dependee
            addDependent(dependency);
        }

        //After new dependents are added, invalidate the structure
        invalidate();
    }


    /**
     * Add a new object of type {@code T} to the graph.
     * This implies that the object has no dependees.
     *
     * @param dependency
     */
    private void addDependent(T dependency)
    {
        if (!dependencyGraph.containsKey(dependency))
        {
            dependencyGraph.put(dependency, new HashSet<>());
        }
    }

    /**
     * Remove everything from the graph.
     */
    public void clear() {
        dependencyGraph.clear();
    }

    /**
     * Flatten the dependency graph in an order list. This means that each element
     * in the list does not have dependencies that occur behind it in the list.
     *
     * @return a flattened version of the dependency graph
     * @throws DependencyGraphException If something goes wrong in flattening the dependency graph,
     * typically when a cycle occurs.
     */
    public List<T> toList() throws DependencyGraphException
    {
        if (!validityChecked)
        {
            //Sanity check on the dependency graph
            verifyValidity();
        }

        //Initialize a list of all dependents
        List<T> flatList = new ArrayList<>();

        final Map<T, Set<T>> fullGraph = new HashMap<>();

        for (T dependent : dependencyGraph.keySet())
        {
            HashSet<T> reach = new HashSet<>();
            buildReach(dependent, reach);
            fullGraph.put(dependent, reach);
        }

        while (flatList.size() < dependencyGraph.keySet().size())
        {
            for (T dependent : dependencyGraph.keySet())
            {
                if (!flatList.contains(dependent) &&
                    fullGraph.get(dependent).stream().allMatch(flatList::contains))
                {
                    flatList.add(dependent);
                }
            }
        }


        return flatList;
    }

    /**
     * Check if the dependent is depending on the dependency.
     *
     * @param dependent the object we're checking
     * @param dependency the dependency we're checking
     * @return whether the dependent needs/is dependant on the dependency
     * @throws DependencyGraphException
     */
    public boolean isDependentOn(T dependent, T dependency) throws DependencyGraphException
    {
        if (!validityChecked)
        {
            verifyValidity();
        }

        if (dependencyGraph.containsKey(dependent))
        {
            HashSet<T> reach = new HashSet<>();
            buildReach(dependent, reach);
            return reach.contains(dependency);
        }

        return false;
    }

    public void verifyValidity() throws DependencyGraphException
    {
        //Sanity check 1: There is at least one starting symbol
        if(!hasStart())
            throw new DependencyGraphException("Dependency graph is not valid."
                + " Cause: there is no start point.");
        
        
        //Sanity check 2: Each dependee occurs as a key in the map
        if(hasUnreachableDependees())
            throw new DependencyGraphException("Some dependees are not properly registered.");

        //Sanity check 3: There are no circular dependencies
        if(hasCircularDependencies())
            throw new DependencyGraphException("Dependency graph is not valid."
                + "Cause: there are circular dependencies");

        //If we reach this stage, mark that the structure has been checked for validity
        this.validityChecked = true;
    }
    
    /**
     * Returns true if there is at least one value that is a starting point and
     * is not dependent
     * @return 
     */
    public boolean hasStart()
    {
        return !startingPoints().isEmpty();
    }
    
    public Set<T> startingPoints()
    {
        return dependencyGraph
            .entrySet()
            .stream()
            .filter(e -> e.getValue().isEmpty())
            .map(e -> e.getKey())
            .collect(Collectors.toSet());
    }

    /**
     * Returns true if the dependency graph contains a cycle.
     * @return 
     */
    public boolean hasCircularDependencies()
    {
        for (T from : dependencyGraph.keySet())
        {
            Set<T> reach = new HashSet<>();
            buildReach(from, reach);

            if (reach.contains(from))
            {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean hasUnreachableDependees()
    {
        for (T dependent : dependencyGraph.keySet())
        {
            for (T dependee : dependencyGraph.get(dependent))
            {
                if (!dependencyGraph.containsKey(dependee))
                {
                    return true;
                }
            }
        }
        
        return false;
    }

    public Set<T> reach(Set<T> starting)
    {
        Set<T> reach = new HashSet<>();
        
        for(T start: starting)
        {
            Set<T> lReach = new HashSet<>();
            buildReach(start, lReach);
            reach.addAll(lReach);
        }
        
        reach.addAll(starting);

        return reach;
    }

    private void buildReach(T from, Set<T> reach)
    {
        for (T r : dependencyGraph.get(from))
        {
            boolean goDeeper = !reach.contains(r);

            reach.add(r);

            if (goDeeper) {
                buildReach(r, reach);
            }
        }
    }

    private void invalidate()
    {
        this.validityChecked = false;
    }
}
