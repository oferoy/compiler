package cfg;

import ir.*;
import java.util.*;

/**
 * Represents a single node in the Control Flow Graph.
 * Each node contains one IR command and has edges to successor/predecessor nodes.
 */
public class CFGNode
{
    /****************/
    /* DATA MEMBERS */
    /****************/
    public IrCommand command;           // The IR command at this node
    public List<CFGNode> successors;    // Nodes that can execute after this one
    public List<CFGNode> predecessors;  // Nodes that can execute before this one
    public int id;                      // Unique identifier for debugging
    
    // For DFA: sets of variables
    public Set<String> gen;             // Variables defined (generated) at this node
    public Set<String> kill;            // Variables that might be killed at this node
    public Set<String> in;              // Variables initialized coming INTO this node
    public Set<String> out;             // Variables initialized going OUT of this node
    
    /******************/
    /* CONSTRUCTOR(S) */
    /******************/
    public CFGNode(IrCommand command, int id)
    {
        this.command = command;
        this.id = id;
        this.successors = new ArrayList<>();
        this.predecessors = new ArrayList<>();
        
        // Initialize DFA sets
        this.gen = new HashSet<>();
        this.kill = new HashSet<>();
        this.in = new HashSet<>();
        this.out = new HashSet<>();
    }
    
    /***************/
    /* ADD EDGE    */
    /***************/
    public void addSuccessor(CFGNode successor)
    {
        if (!successors.contains(successor))
        {
            successors.add(successor);
        }
        if (!successor.predecessors.contains(this))
        {
            successor.predecessors.add(this);
        }
    }
    
    /***************/
    /* TO STRING   */
    /***************/
    @Override
    public String toString()
    {
        String cmdType = (command != null) ? command.getClass().getSimpleName() : "null";
        return String.format("CFGNode[%d]: %s", id, cmdType);
    }
}