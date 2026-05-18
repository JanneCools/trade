package be.ugent.ledc.sigma.datastructures.fptree;

import java.util.ArrayList;
import java.util.List;

public class FPNode<I> {

    private final I item;
    private final FPNode<I> parent;
    private final List<FPNode<I>> children;
    private final List<I> ancestors;

    private int count;
    private FPNode<I> nextNode;

    /**
     * Create a new FPNode object with empty parent
     *
     * @param item item of the FPNode object
     */
    public FPNode(I item) {
        this(item, null);
    }

    /**
     * Create a new FPNode object with count 0
     *
     * @param item   item of the FPNode object
     * @param parent parent node of the FPNode object
     */
    public FPNode(I item, FPNode<I> parent) {
        this(item, parent, 0);
    }

    /**
     * Create a new FPNode object
     *
     * @param item   item of the FPNode object
     * @param parent parent node of the FPNode object
     * @param count  count of the FPNode object
     */
    public FPNode(I item, FPNode<I> parent, int count) {
        this.item = item;
        this.parent = parent;
        this.count = count;
        this.children = new ArrayList<>();
        this.ancestors = new ArrayList<>();
        buildAncestors();
    }

    public int nodeCount() {
        int nodeCount = 1;

        for (FPNode child : children) {
            nodeCount += child.nodeCount();
        }

        return nodeCount;
    }

    /**
     * Get item of the FPNode object
     *
     * @return item of the FPNode object
     */
    public I getItem() {
        return item;
    }

    /**
     * Get parent node of the FPNode object
     *
     * @return parent node of the FPNode object
     */
    public FPNode<I> getParent() {
        return parent;
    }

    /**
     * Get List of children of the FPNode object
     *
     * @return List of children of the FPNode object
     */
    public List<FPNode<I>> getChildren() {
        return children;
    }

    /**
     * Add new child FPNode object to this FPNode object
     *
     * @param child new child FPNode object to add to this FPNode object
     */
    public void addChild(FPNode<I> child) {
        getChildren().add(child);
    }

    /**
     * Get count of the FPNode object
     *
     * @return count of the FPNode object
     */
    public int getCount() {
        return count;
    }

    /**
     * Increase count of the FPNode object by one
     */
    public void incrementCount() {
        increaseCount(1);
    }

    /**
     * Increase count of the FPNode object by n
     */
    public void increaseCount(int n) {
        count += n;
    }

    /**
     * Get next FPTNode object with the same value in an a FPTree
     *
     * @return next FPTNode object with the same value in a FPTree
     */
    public FPNode<I> getNextNode() {
        return nextNode;
    }

    /**
     * Set new next FPTNode object with the same value in an a FPTree
     *
     * @param nextNode new next FPTNode object with the same value in a FPTree
     */
    public void setNextNode(FPNode<I> nextNode) {
        this.nextNode = nextNode;
    }

    /**
     * Retrieve child FPNode object of this FPNode object with given value
     *
     * @param item item that is searched as item of the children of this FPNode object
     * @return child FPNode object of this FPNode that has given item, null if there is none
     */
    public FPNode<I> getChild(I item) {

        // Get all children of this FPNode object
        List<FPNode<I>> children = getChildren();

        // Loop over those children and check if one contains the given item
        for (FPNode<I> child : children) {
            if (child.getItem().equals(item)) {
                return child;
            }
        }

        return null;

    }

    public void print() {
        print("", true);
    }

    private void print(String prefix, boolean isTail) {
        System.out.println(prefix + (isTail ? "|___" : "|___") + toString());

        for (int i = 0; i < children.size() - 1; i++) {
            children.get(i).print(prefix + (isTail ? "    " : "|   "), false);
        }

        if (children.size() > 0) {
            children.get(children.size() - 1).print(prefix + (isTail ? "    " : "|   "), true);
        }
    }

    private void buildAncestors() {
        if (parent == null) {
            return;
        }

        FPNode<I> current = this;

        while (current.getParent() != null) {
            ancestors.add(current.getItem());
            current = current.getParent();
        }
    }

    public boolean hasAncestor(I item) {
        return ancestors.contains(item);
    }

    public List<I> getAncestors() {
        return ancestors;
    }

    @Override
    public String toString() {
        return "FPNode{" +
                "item=" + item +
                ", count=" + count +
                '}';
    }
}
