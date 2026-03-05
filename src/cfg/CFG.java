package cfg;

import ir.*;
import java.util.*;

/**
 * Control Flow Graph - represents the flow of execution through IR commands.
 */
public class CFG
{
    /****************/
    /* DATA MEMBERS */
    /****************/
    public CFGNode entry;               // Entry point (first node)
    public CFGNode exit;                // Exit point (last node)
    public List<CFGNode> nodes;         // All nodes in the CFG
    public Map<String, CFGNode> labels; // Map from label names to nodes
    
    /******************/
    /* CONSTRUCTOR(S) */
    /******************/
    public CFG()
    {
        this.nodes = new ArrayList<>();
        this.labels = new HashMap<>();
        this.entry = null;
        this.exit = null;
    }
    
    /***************/
    /* ADD NODE    */
    /***************/
    public CFGNode addNode(IrCommand command)
    {
        CFGNode node = new CFGNode(command, nodes.size());
        nodes.add(node);
        return node;
    }
    
    /***************/
    /* GET NODES   */
    /***************/
    public List<CFGNode> getNodes()
    {
        return nodes;
    }
    
    /***************/
    /* TO STRING   */
    /***************/
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Control Flow Graph ===\n");
        sb.append(String.format("Total nodes: %d\n", nodes.size()));
        sb.append(String.format("Entry: %s\n", entry));
        sb.append(String.format("Exit: %s\n", exit));
        sb.append("\nNodes:\n");
        for (CFGNode node : nodes)
        {
            sb.append(String.format("  %s -> successors: %d\n", 
                node, node.successors.size()));
        }
        return sb.toString();
    }
}