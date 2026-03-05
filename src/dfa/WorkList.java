package dfa;
import cfg.*;
import java.util.*;

public class WorkList {
    private Queue<CFGNode> queue;
    
    public WorkList() {
        queue = new LinkedList<>();
    }
    
    public void add(CFGNode node) { queue.add(node); }
    public CFGNode remove() { return queue.poll(); }
    public boolean isEmpty() { return queue.isEmpty(); }
}