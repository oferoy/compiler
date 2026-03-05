package types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds vtables for dynamic dispatch. Call build() once before IR generation.
 * Method slots are assigned in order of first appearance when traversing the
 * class hierarchy (base classes first, depth-first).
 */
public class VtableBuilder {
    private static Map<String, Integer> methodToSlot = new HashMap<>();
    private static List<String> slotToMethod = new ArrayList<>();
    private static Map<String, String[]> classToVtableEntries = new HashMap<>();
    private static boolean built = false;

    /** Build vtables from all classes in the program. */
    public static void build(List<TypeClass> allClasses) {
        if (built) return;
        built = true;
        methodToSlot.clear();
        slotToMethod.clear();
        classToVtableEntries.clear();

        // Topological sort: parents before children
        List<TypeClass> sorted = new ArrayList<>();
        List<TypeClass> remaining = new ArrayList<>(allClasses);
        while (!remaining.isEmpty()) {
            TypeClass added = null;
            for (TypeClass c : remaining) {
                if (c.father == null || sorted.stream().anyMatch(x -> x.name.equals(c.father.name))) {
                    added = c;
                    break;
                }
            }
            if (added == null) break; // cycle or missing parent
            sorted.add(added);
            remaining.remove(added);
        }
        if (!remaining.isEmpty()) sorted.addAll(remaining);

        // Assign slot indices: traverse hierarchy, assign slot to each method on first appearance
        int nextSlot = 0;
        for (TypeClass cls : sorted) {
            List<TypeClass> chain = new ArrayList<>();
            for (TypeClass c = cls; c != null; c = c.father)
                chain.add(0, c);
            for (TypeClass c : chain) {
                for (TypeClassMember m = c.dataMembers; m != null; m = m.next) {
                    if (m.kind == TypeClassMember.METHOD && !methodToSlot.containsKey(m.name)) {
                        methodToSlot.put(m.name, nextSlot);
                        slotToMethod.add(m.name);
                        nextSlot++;
                    }
                }
            }
        }

        // Build vtable for each class: for each slot in order, put the implementation label
        int numSlots = slotToMethod.size();
        for (TypeClass cls : sorted) {
            String[] entries = new String[numSlots];
            for (int slot = 0; slot < numSlots; slot++) {
                String methodName = slotToMethod.get(slot);
                TypeClass defining = cls.getMethodDefiningClass(methodName);
                if (defining != null)
                    entries[slot] = defining.name + "_" + methodName;
            }
            classToVtableEntries.put(cls.name, entries);
        }
    }

    public static int getMethodSlot(String methodName) {
        Integer slot = methodToSlot.get(methodName);
        return slot != null ? slot : -1;
    }

    public static String[] getVtableEntries(String className) {
        return classToVtableEntries.get(className);
    }

    public static int getNumSlots() {
        return methodToSlot.size();
    }

    public static java.util.Set<String> getClassNames() {
        return classToVtableEntries.keySet();
    }
}
