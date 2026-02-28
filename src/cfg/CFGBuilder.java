package cfg;

import ir.*;
import temp.*;
import java.util.*;

/**
 * Builds a Control Flow Graph from a list of IR commands.
 */
public class CFGBuilder
{
    /**
     * Build CFG from IR singleton.
     * We need to add a method to Ir to get all commands.
     */
    public static CFG buildFromIr(Ir ir)
    {
        // Get all IR commands
        List<IrCommand> commands = getAllCommands(ir);
        
        if (commands.isEmpty())
        {
            return new CFG();
        }
        
        return buildFromCommandList(commands);
    }
    
    /**
     * Extract all IR commands from the Ir singleton.
     */
    private static List<IrCommand> getAllCommands(Ir ir)
    {
        return ir.getCommandList();
    }
    
    /**
     * Build CFG from a list of IR commands.
     */
    public static CFG buildFromCommandList(List<IrCommand> commands)
    {
        CFG cfg = new CFG();
        
        if (commands.isEmpty())
        {
            return cfg;
        }
        
        // Create a node for each command
        List<CFGNode> nodes = new ArrayList<>();
        Map<String, CFGNode> labelToNode = new HashMap<>();
        
        for (IrCommand cmd : commands)
        {
            CFGNode node = cfg.addNode(cmd);
            nodes.add(node);
            
            // If this is a label, map it
            String labelName = cmd.getLabelName();
            if (labelName != null)
                labelToNode.put(labelName, node);
        }
        
        // Set entry and exit
        cfg.entry = nodes.get(0);
        cfg.exit = nodes.get(nodes.size() - 1);
        
        // Build edges
        for (int i = 0; i < nodes.size(); i++)
        {
            CFGNode current = nodes.get(i);
            IrCommand cmd = current.command;
            
            // Check command type and add appropriate edges
            String jumpTarget = cmd.getJumpLabel();
            boolean unconditionalJump = (cmd instanceof IrCommandJumpLabel || cmd instanceof IrCommandReturn);

            if (unconditionalJump && jumpTarget != null)
            {
                CFGNode target = labelToNode.get(jumpTarget);
                if (target != null)
                    current.addSuccessor(target);
            }
            else if (cmd instanceof IrCommandJumpIfEqToZero && jumpTarget != null)
            {
                CFGNode target = labelToNode.get(jumpTarget);
                if (target != null)
                    current.addSuccessor(target);
                if (i + 1 < nodes.size())
                    current.addSuccessor(nodes.get(i + 1));
            }
            else
            {
                if (i + 1 < nodes.size())
                    current.addSuccessor(nodes.get(i + 1));
                if (jumpTarget != null)
                {
                    CFGNode target = labelToNode.get(jumpTarget);
                    if (target != null)
                        current.addSuccessor(target);
                }
            }
        }

        return cfg;
    }
}