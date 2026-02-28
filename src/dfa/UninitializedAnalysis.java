package dfa;

import cfg.*;
import ir.*;
import temp.*;
import java.util.*;

/**
 * Data Flow Analysis to detect used-before-set errors.
 * 
 * This is a more sophisticated analysis where a definition sets a variable x
 * to an initialized value ONLY IF the value assigned to x is itself initialized.
 * 
 * Key insight: We track which variables AND temps hold initialized values.
 * - A Store to variable x initializes x only if the source temp is initialized
 * - A Load from variable x to temp t makes t initialized only if x is initialized
 * - A Binop produces an initialized temp only if all operands are initialized
 * - A ConstInt always produces an initialized temp
 * 
 * Algorithm:
 * - Forward analysis with intersection at merge points
 * - Track set of initialized variables AND temps at each program point
 * - Report variables that are used (Loaded) when not in the initialized set
 */
public class UninitializedAnalysis
{
    private CFG cfg;
    private Set<String> allVariables;         // All user-declared variables
    private Set<String> uninitializedAccesses; // Variables accessed before initialization
    
    public UninitializedAnalysis(CFG cfg)
    {
        this.cfg = cfg;
        this.allVariables = new HashSet<>();
        this.uninitializedAccesses = new HashSet<>();
    }
    
    /**
     * Run the analysis and return set of uninitialized variable names.
     */
    public Set<String> analyze()
    {
        // Step 1: Collect all user variables (not temps)
        collectAllVariables();
        
        // Step 2: Run the dataflow analysis
        runDataFlowAnalysis();
        
        return uninitializedAccesses;
    }
    
    /**
     * Collect all user-declared variables (filter out temps).
     */
    private void collectAllVariables()
    {
        for (CFGNode node : cfg.getNodes())
        {
            IrCommand cmd = node.command;
            
            if (cmd instanceof IrCommandAllocate)
            {
                String varName = getVarName((IrCommandAllocate) cmd);
                if (!isTemp(varName))
                {
                    allVariables.add(varName);
                }
            }
        }
    }
    
    /**
     * Run the dataflow analysis.
     * 
     * State at each node: Set of initialized names (variables AND temps)
     * Transfer function depends on command type.
     */
    private void runDataFlowAnalysis()
    {
        // Initialize all nodes
        for (CFGNode node : cfg.getNodes())
        {
            node.in.clear();
            node.out.clear();
            
            // Optimistic initialization for non-entry nodes
            if (node != cfg.entry)
            {
                node.in.addAll(allVariables);
                node.out.addAll(allVariables);
            }
        }
        
        // Iterate until fixpoint
        boolean changed = true;
        int iterations = 0;
        int maxIterations = 1000;
        
        while (changed && iterations < maxIterations)
        {
            changed = false;
            iterations++;
            
            for (CFGNode node : cfg.getNodes())
            {
                // Compute IN: intersection of predecessors' OUT
                Set<String> newIn = new HashSet<>();
                
                if (node.predecessors.isEmpty())
                {
                    // Entry node: nothing initialized
                    newIn.clear();
                }
                else
                {
                    // Intersection of all predecessors
                    boolean first = true;
                    for (CFGNode pred : node.predecessors)
                    {
                        if (first)
                        {
                            newIn.addAll(pred.out);
                            first = false;
                        }
                        else
                        {
                            newIn.retainAll(pred.out);
                        }
                    }
                }
                
                // Compute OUT using transfer function
                Set<String> newOut = applyTransferFunction(node, newIn);
                
                // Check if changed
                if (!newIn.equals(node.in) || !newOut.equals(node.out))
                {
                    changed = true;
                    node.in = newIn;
                    node.out = newOut;
                }
            }
        }
        
        // Now find uninitialized accesses
        findUninitializedAccesses();
    }
    
    /**
     * Apply transfer function for a node.
     * Returns the OUT set given the IN set.
     */
    private Set<String> applyTransferFunction(CFGNode node, Set<String> in)
    {
        Set<String> out = new HashSet<>(in);
        IrCommand cmd = node.command;
        
        if (cmd instanceof IrCommandAllocate)
        {
            // Allocate doesn't initialize
            // Variable starts uninitialized (remove from set if it was there)
            String varName = getVarName((IrCommandAllocate) cmd);
            if (!isTemp(varName))
            {
                out.remove(varName);
            }
        }
        else if (cmd instanceof IrCommandStore)
        {
            // Store: x := temp
            // x becomes initialized ONLY IF temp is initialized
            String varName = getStoreVarName((IrCommandStore) cmd);
            Temp srcTemp = getStoreValue((IrCommandStore) cmd);
            
            if (!isTemp(varName) && allVariables.contains(varName))
            {
                String tempName = getTempName(srcTemp);
                if (in.contains(tempName))
                {
                    // Source is initialized, so target becomes initialized
                    out.add(varName);
                }
                else
                {
                    // Source is NOT initialized, so target is NOT initialized
                    out.remove(varName);
                }
            }
        }
        else if (cmd instanceof IrCommandLoad)
        {
            // Load: temp := x
            // temp becomes initialized ONLY IF x is initialized
            String varName = getLoadVarName((IrCommandLoad) cmd);
            Temp dstTemp = getLoadDest((IrCommandLoad) cmd);
            String tempName = getTempName(dstTemp);
            
            if (!isTemp(varName) && in.contains(varName))
            {
                out.add(tempName);
            }
            else if (!isTemp(varName))
            {
                out.remove(tempName);
            }
        }
        else if (cmd instanceof IRcommandConstInt)
        {
            // ConstInt: temp := constant
            // temp is always initialized
            Temp dstTemp = getConstIntDest((IRcommandConstInt) cmd);
            String tempName = getTempName(dstTemp);
            out.add(tempName);
        }
        else if (cmd instanceof IrCommandBinopAddIntegers ||
                 cmd instanceof IrCommandBinopSubIntegers ||
                 cmd instanceof IrCommandBinopMulIntegers ||
                 cmd instanceof IrCommandBinopDivIntegers ||
                 cmd instanceof IrCommandBinopLtIntegers ||
                 cmd instanceof IrCommandBinopGtIntegers ||
                 cmd instanceof IrCommandBinopEqIntegers)
        {
            // Binop: dst := op1 op op2
            // dst is initialized ONLY IF both op1 and op2 are initialized
            Temp[] operands = getBinopOperands(cmd);
            Temp dst = operands[0];
            Temp op1 = operands[1];
            Temp op2 = operands[2];
            
            String dstName = getTempName(dst);
            String op1Name = getTempName(op1);
            String op2Name = getTempName(op2);
            
            if (in.contains(op1Name) && in.contains(op2Name))
            {
                out.add(dstName);
            }
            else
            {
                out.remove(dstName);
            }
        }
        // Labels, jumps don't change initialization state
        
        return out;
    }
    
    /**
     * Find variables that are accessed before being initialized.
     */
    private void findUninitializedAccesses()
    {
        for (CFGNode node : cfg.getNodes())
        {
            IrCommand cmd = node.command;
            
            if (cmd instanceof IrCommandLoad)
            {
                String irName = getLoadVarName((IrCommandLoad) cmd);
                if (!isTemp(irName) && allVariables.contains(irName))
                {
                    if (!node.in.contains(irName))
                    {
                        // Get original name for output
                        String originalName = VarNameMapper.getInstance().getOriginalName(irName);
                        uninitializedAccesses.add(originalName);
                    }
                }
            }
        }
    }
    
    // ==================== Helper methods ====================
    
    private String getVarName(IrCommandAllocate cmd)
    {
        try
        {
            java.lang.reflect.Field field = IrCommandAllocate.class.getDeclaredField("varName");
            field.setAccessible(true);
            return (String) field.get(cmd);
        }
        catch (Exception e)
        {
            return "unknown";
        }
    }
    
    private String getStoreVarName(IrCommandStore cmd)
    {
        try
        {
            java.lang.reflect.Field field = IrCommandStore.class.getDeclaredField("varName");
            field.setAccessible(true);
            return (String) field.get(cmd);
        }
        catch (Exception e)
        {
            return "unknown";
        }
    }
    
    private Temp getStoreValue(IrCommandStore cmd)
    {
        try
        {
            java.lang.reflect.Field field = IrCommandStore.class.getDeclaredField("src");
            field.setAccessible(true);
            return (Temp) field.get(cmd);
        }
        catch (Exception e)
        {
            return null;
        }
    }
    
    private String getLoadVarName(IrCommandLoad cmd)
    {
        try
        {
            java.lang.reflect.Field field = IrCommandLoad.class.getDeclaredField("varName");
            field.setAccessible(true);
            return (String) field.get(cmd);
        }
        catch (Exception e)
        {
            return "unknown";
        }
    }
    
    private Temp getLoadDest(IrCommandLoad cmd)
    {
        try
        {
            java.lang.reflect.Field field = IrCommandLoad.class.getDeclaredField("dst");
            field.setAccessible(true);
            return (Temp) field.get(cmd);
        }
        catch (Exception e)
        {
            return null;
        }
    }
    
    private Temp getConstIntDest(IRcommandConstInt cmd)
    {
        try
        {
            java.lang.reflect.Field field = IRcommandConstInt.class.getDeclaredField("t");
            field.setAccessible(true);
            return (Temp) field.get(cmd);
        }
        catch (Exception e)
        {
            return null;
        }
    }
    
    private Temp[] getBinopOperands(IrCommand cmd)
    {
        try
        {
            // All binop commands have dst, t1, t2 fields
            java.lang.reflect.Field dstField = cmd.getClass().getDeclaredField("dst");
            java.lang.reflect.Field t1Field = cmd.getClass().getDeclaredField("t1");
            java.lang.reflect.Field t2Field = cmd.getClass().getDeclaredField("t2");
            
            dstField.setAccessible(true);
            t1Field.setAccessible(true);
            t2Field.setAccessible(true);
            
            return new Temp[] {
                (Temp) dstField.get(cmd),
                (Temp) t1Field.get(cmd),
                (Temp) t2Field.get(cmd)
            };
        }
        catch (Exception e)
        {
            return new Temp[] { null, null, null };
        }
    }
    
    private String getTempName(Temp t)
    {
        if (t == null) return "null_temp";
        return "T_" + t.getSerialNumber();
    }
    
    private boolean isTemp(String name)
    {
        // Temps start with T_, user variables contain @
        return name != null && name.startsWith("T_") && !name.contains("@");
    }
}
