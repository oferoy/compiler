package ir;

import java.util.*;

/**
 * Maps variable names to unique IR names to handle variable shadowing.
 * 
 * Each variable declaration gets a unique IR name based on its AST serial number.
 * Format: originalName@serialNumber
 * 
 * This allows the DFA to distinguish between different variables with the same name
 * in different scopes.
 */
public class VarNameMapper
{
    // Singleton instance
    private static VarNameMapper instance = null;
    
    // Maps from AST serial number to unique IR name
    private Map<Integer, String> serialToIrName = new HashMap<>();
    
    // Maps from unique IR name back to original name (for output)
    private Map<String, String> irNameToOriginal = new HashMap<>();
    
    // Stack of active variable mappings (name -> IR name) for current scope chain
    // We use a stack of maps to handle scope entry/exit
    private Deque<Map<String, String>> scopeStack = new ArrayDeque<>();
    
    // Current scope mapping (name -> IR name for the most recent declaration)
    private Map<String, String> currentMapping = new HashMap<>();
    
    private VarNameMapper()
    {
        // Start with global scope
        scopeStack.push(new HashMap<>());
    }
    
    public static VarNameMapper getInstance()
    {
        if (instance == null)
        {
            instance = new VarNameMapper();
        }
        return instance;
    }
    
    /**
     * Reset the mapper (useful for testing multiple files)
     */
    public static void reset()
    {
        instance = null;
    }
    
    /**
     * Register a variable declaration with its AST serial number.
     * Returns the unique IR name for this variable.
     */
    public String registerVariable(String originalName, int astSerialNumber)
    {
        String irName = originalName + "@" + astSerialNumber;
        
        serialToIrName.put(astSerialNumber, irName);
        irNameToOriginal.put(irName, originalName);
        
        // Update current scope mapping
        if (!scopeStack.isEmpty())
        {
            scopeStack.peek().put(originalName, irName);
        }
        currentMapping.put(originalName, irName);
        
        return irName;
    }
    
    /**
     * Get the IR name for a variable usage.
     * Returns the most recently declared variable with this name.
     */
    public String getIrName(String originalName)
    {
        return currentMapping.get(originalName);
    }
    
    /**
     * Get the original name from an IR name.
     */
    public String getOriginalName(String irName)
    {
        String original = irNameToOriginal.get(irName);
        return (original != null) ? original : irName;
    }
    
    /**
     * Check if an IR name represents a user variable (not a temp)
     */
    public boolean isUserVariable(String irName)
    {
        return irName != null && irName.contains("@");
    }
    
    /**
     * Begin a new scope - save current mappings
     */
    public void beginScope()
    {
        scopeStack.push(new HashMap<>(currentMapping));
        currentMapping = scopeStack.peek();
    }

    /**
     * End current scope - restore previous mappings
     */
    public void endScope()
    {
        if (scopeStack.size() > 1)
        {
            scopeStack.pop();
            currentMapping = scopeStack.peek();
        }
    }
}
