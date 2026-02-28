package ir;

import temp.Temp;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.ArrayList;

/**
 * Simplification-based register allocation for IR.
 * Allocates temporaries to $t0-$t9 (10 registers). Returns null if allocation fails.
 */
public class RegisterAllocator
{
	private static final int K = 10;
	private static final String[] REGS = { "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7", "$t8", "$t9" };

	/**
	 * @param commands linear list of IR commands
	 * @return map from temp serial number to register name ("$t0".."$t9"), or null if allocation fails
	 */
	public static Map<Integer, String> allocate(List<IrCommand> commands)
	{
		if (commands == null || commands.isEmpty())
			return new HashMap<Integer, String>();

		int n = commands.size();
		// Map label name -> first instruction index with that label
		Map<String, Integer> labelToIndex = new HashMap<String, Integer>();
		for (int i = 0; i < n; i++) {
			String L = commands.get(i).getLabelName();
			if (L != null)
				labelToIndex.put(L, i);
		}

		// Successors: out[i] = set of instruction indices that can follow i
		List<Set<Integer>> successors = new ArrayList<Set<Integer>>(n);
		for (int i = 0; i < n; i++)
			successors.add(new HashSet<Integer>());
		for (int i = 0; i < n; i++) {
			IrCommand cmd = commands.get(i);
			String jumpLabel = cmd.getJumpLabel();
			boolean isUnconditionalJump = (cmd instanceof IrCommandJumpLabel);
			if (isUnconditionalJump) {
				if (jumpLabel != null) {
					Integer j = labelToIndex.get(jumpLabel);
					if (j != null) successors.get(i).add(j);
				}
			} else {
				if (i + 1 < n) successors.get(i).add(i + 1);
				if (jumpLabel != null) {
					Integer j = labelToIndex.get(jumpLabel);
					if (j != null) successors.get(i).add(j);
				}
			}
		}

		// Use/def per instruction (by temp serial number)
		List<Set<Integer>> use = new ArrayList<Set<Integer>>(n);
		List<Set<Integer>> def = new ArrayList<Set<Integer>>(n);
		Set<Integer> allTemps = new HashSet<Integer>();
		for (int i = 0; i < n; i++) {
			IrCommand cmd = commands.get(i);
			Set<Integer> useI = new HashSet<Integer>();
			Set<Integer> defI = new HashSet<Integer>();
			for (Temp t : cmd.getUse()) { if (t != null) { useI.add(t.getSerialNumber()); allTemps.add(t.getSerialNumber()); } }
			for (Temp t : cmd.getDef()) { if (t != null) { defI.add(t.getSerialNumber()); allTemps.add(t.getSerialNumber()); } }
			use.add(useI);
			def.add(defI);
		}

		// Backward liveness: in[i] = use[i] ∪ (out[i] \ def[i]), out[i] = ∪ in[j] for j in successors(i)
		List<Set<Integer>> in = new ArrayList<Set<Integer>>(n);
		List<Set<Integer>> out = new ArrayList<Set<Integer>>(n);
		for (int i = 0; i < n; i++) {
			in.add(new HashSet<Integer>());
			out.add(new HashSet<Integer>());
		}
		boolean changed = true;
		while (changed) {
			changed = false;
			for (int i = n - 1; i >= 0; i--) {
				Set<Integer> outI = out.get(i);
				for (Integer j : successors.get(i))
					outI.addAll(in.get(j));
				Set<Integer> inI = new HashSet<Integer>(use.get(i));
				for (Integer t : out.get(i))
					if (!def.get(i).contains(t)) inI.add(t);
				if (!in.get(i).equals(inI)) {
					in.set(i, inI);
					changed = true;
				}
				out.set(i, outI);
			}
		}

		// Interference: two temps interfere if one is live at the definition of the other
		// For each instruction i: for d in def[i], for t in in[i], t != d => edge (d,t)
		Map<Integer, Set<Integer>> neighbors = new HashMap<Integer, Set<Integer>>();
		for (Integer t : allTemps)
			neighbors.put(t, new HashSet<Integer>());
		for (int i = 0; i < n; i++) {
			Set<Integer> inI = in.get(i);
			for (Integer d : def.get(i))
				for (Integer t : inI)
					if (!d.equals(t)) {
						neighbors.get(d).add(t);
						neighbors.get(t).add(d);
					}
		}

		// Simplification: repeatedly remove a node with degree < K and push onto stack; if none, fail
		Stack<Integer> stack = new Stack<Integer>();
		Map<Integer, Set<Integer>> workGraph = new HashMap<Integer, Set<Integer>>();
		for (Map.Entry<Integer, Set<Integer>> e : neighbors.entrySet())
			workGraph.put(e.getKey(), new HashSet<Integer>(e.getValue()));

		while (!workGraph.isEmpty()) {
			Integer low = null;
			for (Map.Entry<Integer, Set<Integer>> e : workGraph.entrySet()) {
				if (e.getValue().size() < K) { low = e.getKey(); break; }
			}
			if (low == null)
				return null; // allocation failed
			stack.push(low);
			Set<Integer> adj = workGraph.remove(low);
			for (Integer a : adj)
				workGraph.get(a).remove(low);
		}

		// Color: pop and assign smallest color not used by (already colored) neighbors
		// Use original neighbors for "already colored" check
		Map<Integer, Integer> color = new HashMap<Integer, Integer>();
		while (!stack.isEmpty()) {
			Integer node = stack.pop();
			Set<Integer> used = new HashSet<Integer>();
			for (Integer a : neighbors.get(node))
				if (color.containsKey(a))
					used.add(color.get(a));
			int c = 0;
			while (used.contains(c)) c++;
			if (c >= K)
				return null;
			color.put(node, c);
		}

		// Map serial -> register name
		Map<Integer, String> allocation = new HashMap<Integer, String>();
		for (Map.Entry<Integer, Integer> e : color.entrySet())
			allocation.put(e.getKey(), REGS[e.getValue()]);
		return allocation;
	}
}
