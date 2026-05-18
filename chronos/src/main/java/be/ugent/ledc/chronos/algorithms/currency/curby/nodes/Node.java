package be.ugent.ledc.chronos.algorithms.currency.curby.nodes;

import be.ugent.ledc.chronos.algorithms.currency.curby.Sequence;

public abstract class Node<I> implements INode<I>
{
    private final String attribute;
    
    private final int id;

    public Node(String attribute)
    {
        this.attribute = attribute;
        this.id = Sequence.getInstance().pop();
    }

    public String getAttribute()
    {
        return attribute;
    }

    public int getId() {
        return id;
    }
    
    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 71 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final Node<?> other = (Node<?>) obj;
        return this.id == other.id;
    }
}
