/***********/
/* PACKAGE */
/***********/
package mips;

/*******************/
/* GENERAL IMPORTS */
/*******************/
import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/*******************/
/* PROJECT IMPORTS */
/*******************/
import temp.*;

public class MipsGenerator
{
	private static final int WORD_SIZE = 4;
	/** When true, error message includes site id: "Invalid Ptr (loadArray #3)" for easier debugging. Set false for submission if tests expect exact string. */
	private static final boolean DEBUG_LOAD_ARRAY_SITE = false;
	/** When true, emit MIPS comments for load/store/storeParam/call and print $a0 before each jal. Set false for submission. */
	private static final boolean DEBUG_TRACE_CALLS = false;
	/** When true, print index and value before storeArrayWithBaseInS2 (debug test 5). Set false for submission. */
	private static final boolean DEBUG_STORE_ARRAY = false;
	private static int internalLabelCounter = 0;
	private PrintWriter fileWriter;
	private Map<Integer, String> tempToReg;
	/** IR var name (e.g. x@5) -> MIPS label (e.g. var_0) */
	private Map<String, String> varToLabel = new HashMap<>();
	private int varCounter = 0;
	/** Labels for which we have already emitted .data (so we don't duplicate or miss). */
	private Set<String> dataEmittedLabels = new HashSet<>();
	/** True after we have emitted .text and the j main preamble (so all code is in .text). */
	private boolean textSegmentEmitted = false;
	/** Per-site ID for loadArray so debug output shows which site failed (e.g. "Invalid Ptr (loadArray #3)"). */
	private int loadArraySiteId = 0;
	private boolean concatHelperEmitted = false;
	/** Local var names (use stack); null = use global .data for all. */
	private Set<String> localVarNames = new HashSet<>();
	/** Per-function: varName -> byte offset from $sp. */
	private Map<String, Map<String, Integer>> functionVarOffsets = new HashMap<>();
	/** Per-function: frame size in bytes. */
	private Map<String, Integer> functionFrameSizes = new HashMap<>();
	/** Current function we're emitting (set at Label, used for load/store/jr). */
	private String currentFunction = null;
	/** Ensure we're in .text segment. */
	private void ensureTextWithPreamble() {
		fileWriter.print(".text\n");
	}

	/** Emit string_concat helper (once). $a0=s1, $a1=s2 -> $v0=new string. Null ptr -> ptr_error. */
	private void emitStringConcatHelper() {
		if (concatHelperEmitted) return;
		concatHelperEmitted = true;
		fileWriter.print("string_concat:\n");
		fileWriter.print("\tbne $a0,$zero,concat_ck1\n\tla $a0,dbg_concat_a0\n\tli $v0,4\n\tsyscall\n\tli $v0,10\n\tsyscall\n");
		fileWriter.print("concat_ck1:\n\tbne $a1,$zero,concat_ok\n\tla $a0,dbg_concat_a1\n\tli $v0,4\n\tsyscall\n\tli $v0,10\n\tsyscall\n");
		fileWriter.print("concat_ok:\n");
		fileWriter.print("\taddi $sp,$sp,-20\n");
		fileWriter.print("\tsw $ra,0($sp)\n\tsw $s0,4($sp)\n\tsw $s1,8($sp)\n\tsw $a0,12($sp)\n\tsw $a1,16($sp)\n");
		// len1: $s0 = strlen($a0)
		fileWriter.print("\tmove $t0,$a0\n\tli $s0,0\n");
		fileWriter.print("string_concat_len1:\n\tlb $t1,0($t0)\n\tbeq $t1,$zero,string_concat_len1_done\n");
		fileWriter.print("\taddi $t0,$t0,1\n\taddi $s0,$s0,1\n\tj string_concat_len1\n");
		fileWriter.print("string_concat_len1_done:\n");
		// len2: $s1 = strlen($a1)
		fileWriter.print("\tlw $t0,16($sp)\n\tli $s1,0\n");
		fileWriter.print("string_concat_len2:\n\tlb $t1,0($t0)\n\tbeq $t1,$zero,string_concat_len2_done\n");
		fileWriter.print("\taddi $t0,$t0,1\n\taddi $s1,$s1,1\n\tj string_concat_len2\n");
		fileWriter.print("string_concat_len2_done:\n");
		// allocate len1+len2+1
		fileWriter.print("\tadd $a0,$s0,$s1\n\taddi $a0,$a0,1\n\tli $v0,9\n\tsyscall\n");
		fileWriter.print("\tmove $t5,$v0\n\tmove $t4,$v0\n");
		// copy s1
		fileWriter.print("\tlw $t0,12($sp)\n");
		fileWriter.print("string_concat_cp1:\n\tlb $t1,0($t0)\n\tbeq $t1,$zero,string_concat_cp1_done\n");
		fileWriter.print("\tsb $t1,0($t4)\n\taddi $t0,$t0,1\n\taddi $t4,$t4,1\n\tj string_concat_cp1\n");
		fileWriter.print("string_concat_cp1_done:\n");
		// copy s2
		fileWriter.print("\tlw $t0,16($sp)\n");
		fileWriter.print("string_concat_cp2:\n\tlb $t1,0($t0)\n\tbeq $t1,$zero,string_concat_cp2_done\n");
		fileWriter.print("\tsb $t1,0($t4)\n\taddi $t0,$t0,1\n\taddi $t4,$t4,1\n\tj string_concat_cp2\n");
		fileWriter.print("string_concat_cp2_done:\n");
		fileWriter.print("\tsb $zero,0($t4)\n");
		fileWriter.print("\tlw $ra,0($sp)\n\tlw $s0,4($sp)\n\tlw $s1,8($sp)\n\tlw $a0,12($sp)\n\tlw $a1,16($sp)\n");
		fileWriter.print("\taddi $sp,$sp,20\n");
		fileWriter.print("\tmove $v0,$t5\n\tjr $ra\n");
	}

	private String reg(Temp t) {
		if (t == null) return "$zero";
		if (tempToReg != null) {
			String r = tempToReg.get(t.getSerialNumber());
			if (r != null) return r;
		}
		return "Temp_" + t.getSerialNumber();
	}

	private String varLabel(String varName) {
		String label = varToLabel.get(varName);
		if (label == null) {
			label = "var_" + (varCounter++);
			varToLabel.put(varName, label);
		}
		return label;
	}
	/** Emit .data for label if not yet emitted (for vars first used in load/store before their allocate). */
	private void ensureDataEmitted(String label) {
		if (dataEmittedLabels.add(label)) {
			fileWriter.print(".data\n");
			fileWriter.format("\t%s: .word 0\n", label);
			fileWriter.print(".text\n");
		}
	}

	/** Set stack layout for locals (so recursion works). Call after setRegisterAllocation. */
	public void setFunctionLayouts(Set<String> localVars, Map<String, Map<String, Integer>> varOffsets, Map<String, Integer> frameSizes) {
		this.localVarNames = (localVars != null) ? new HashSet<>(localVars) : new HashSet<>();
		this.functionVarOffsets = (varOffsets != null) ? new HashMap<>(varOffsets) : new HashMap<>();
		this.functionFrameSizes = (frameSizes != null) ? new HashMap<>(frameSizes) : new HashMap<>();
	}

	/** Set output writer and write .data section + code entry. Must be called before mipsMe(). */
	public void setOutput(PrintWriter pw) {
		this.fileWriter = pw;
		this.textSegmentEmitted = false;
		this.concatHelperEmitted = false;
		this.dataEmittedLabels.clear();
		this.loadArraySiteId = 0;
		fileWriter.print(".data\n");
		fileWriter.print("string_access_violation: .asciiz \"Access Violation\"\n");
		fileWriter.print("string_illegal_div_by_0: .asciiz \"Illegal Division By Zero\"\n");
		fileWriter.print("string_invalid_ptr_dref: .asciiz \"Invalid Pointer Dereference\"\n");
		// Debug: which null-check failed (remove or keep for easier debugging)
		fileWriter.print("string_ptr_loadField: .asciiz \"Invalid Ptr (loadField)\\n\"\n");
		fileWriter.print("string_ptr_loadArray: .asciiz \"Invalid Ptr (loadArray)\\n\"\n");
		fileWriter.print("string_ptr_loadArray_prefix: .asciiz \"Invalid Ptr (loadArray #\"\n");
		fileWriter.print("string_ptr_loadArray_suffix: .asciiz \")\\n\"\n");
		fileWriter.print("string_ptr_storeField: .asciiz \"Invalid Ptr (storeField)\\n\"\n");
		fileWriter.print("string_ptr_storeArray: .asciiz \"Invalid Ptr (storeArray)\\n\"\n");
		fileWriter.print("string_ptr_storeFieldS1: .asciiz \"Invalid Ptr (storeFieldBaseS1)\\n\"\n");
		fileWriter.print("string_ptr_s2: .asciiz \"Invalid Ptr (baseInS2)\\n\"\n");
		fileWriter.print("dbg_concat_a0: .asciiz \"DBG: concat $a0 null\\n\"\n");
		fileWriter.print("dbg_concat_a1: .asciiz \"DBG: concat $a1 null\\n\"\n");
		fileWriter.print("dbg_print_a0: .asciiz \"DBG: PrintString $a0 null\\n\"\n");
		emitVtables();
		fileWriter.print(".text\n");
	}

	/** Emit vtable .data for each class (for dynamic dispatch). */
	private void emitVtables() {
		for (String className : types.VtableBuilder.getClassNames()) {
			String[] entries = types.VtableBuilder.getVtableEntries(className);
			if (entries == null) continue;
			fileWriter.format("vtable_%s:\n", className);
			for (String label : entries) {
				if (label != null)
					fileWriter.format("\t.word %s\n", label);
			}
		}
	}

	/** Emit "main:" label so SPIM entry runs global init first; then jump to main_actual. */
	public void emitProgramEntry() {
		fileWriter.print("main:\n");
	}

	/** Emit j main_actual + error handlers. Call AFTER global init so init runs first. */
	public void emitPreambleAndHandlers() {
		fileWriter.print("j main_actual\n");
		fileWriter.print("ptr_error:\n");
		fileWriter.print("\tla $a0,string_invalid_ptr_dref\n");
		fileWriter.print("\tli $v0,4\n\tsyscall\n");
		fileWriter.print("\tli $v0,10\n\tsyscall\n");
		fileWriter.print("ptr_error_loadField:\n\tj ptr_error\n");
		fileWriter.print("ptr_error_loadArray:\n\tj ptr_error\n");
		fileWriter.print("ptr_error_loadArray_common:\n\tj ptr_error\n");
		fileWriter.print("ptr_error_storeField:\n\tj ptr_error\n");
		fileWriter.print("ptr_error_storeArray:\n\tj ptr_error\n");
		fileWriter.print("ptr_error_storeFieldS1:\n\tj ptr_error\n");
		fileWriter.print("ptr_error_s2:\n\tj ptr_error\n");
		fileWriter.print("bounds_error:\n");
		fileWriter.print("\tla $a0,string_access_violation\n");
		fileWriter.print("\tli $v0,4\n\tsyscall\n");
		fileWriter.print("\tli $v0,10\n\tsyscall\n");
		emitStringConcatHelper();
	}

	/** Set register allocation (temp serial -> "$t0".."$t9"). Must be called before mipsMe(). */
	public void setRegisterAllocation(Map<Integer, String> allocation) {
		this.tempToReg = allocation;
	}

	/***********************/
	/* The file writer ... */
	/***********************/
	public void finalizeFile()
	{
		fileWriter.print("\tli $v0,10\n");
		fileWriter.print("\tsyscall\n");
		fileWriter.close();
	}
	public void printInt(Temp t)
	{
		fileWriter.format("\tmove $a0,%s\n", reg(t));
		fileWriter.format("\tli $v0,1\n");
		fileWriter.format("\tsyscall\n");
		fileWriter.format("\tli $a0,32\n");
		fileWriter.format("\tli $v0,11\n");
		fileWriter.format("\tsyscall\n");
	}
	public void allocate(String varName)
	{
		// Locals get stack slot at function entry; no .data
		if (localVarNames.contains(varName))
			return;
		String label = varLabel(varName);
		ensureDataEmitted(label);
	}

	/** Malloc numBytes, result in dst. Store vtable ptr at offset 0 if vtableLabel non-null. */
	public void allocateClass(Temp dst, int numBytes, String vtableLabel) {
		fileWriter.format("\tli $a0,%d\n", numBytes);
		fileWriter.print("\tli $v0,9\n\tsyscall\n");
		fileWriter.format("\tmove %s,$v0\n", reg(dst));
		if (vtableLabel != null) {
			fileWriter.format("\tla $a0,%s\n", vtableLabel);
			fileWriter.format("\tsw $a0,0(%s)\n", reg(dst));
		}
	}

	/** Allocate 4+size*4 bytes, store length at 0, result in dst. */
	public void allocateArray(Temp dst, Temp sizeTemp) {
		fileWriter.format("\tli $s0,4\n");
		fileWriter.format("\tmul $s0,$s0,%s\n", reg(sizeTemp));
		fileWriter.print("\taddi $s0,$s0,4\n");
		fileWriter.print("\tmove $a0,$s0\n");
		fileWriter.print("\tli $v0,9\n\tsyscall\n");
		fileWriter.format("\tsw %s,0($v0)\n", reg(sizeTemp));
		fileWriter.format("\tmove %s,$v0\n", reg(dst));
	}
	public void load(Temp dst, String varName)
	{
		if (currentFunction != null && localVarNames.contains(varName)) {
			Map<String, Integer> offsets = functionVarOffsets.get(currentFunction);
			if (offsets != null && offsets.containsKey(varName)) {
				int off = offsets.get(varName);
				if (DEBUG_TRACE_CALLS) fileWriter.format("# load %s <- %s ($sp+%d)\n", reg(dst), varName, off);
				fileWriter.format("\tlw %s,%d($sp)\n", reg(dst), off);
				return;
			}
		}
		String label = varLabel(varName);
		ensureDataEmitted(label);
		if (DEBUG_TRACE_CALLS) fileWriter.format("# load %s <- %s (%s)\n", reg(dst), varName, label);
		fileWriter.format("\tlw %s,%s\n", reg(dst), label);
	}

	/** Load address of label into dst (e.g. for string literals). */
	public void loadAddress(Temp dst, String label) {
		fileWriter.format("\tla %s,%s\n", reg(dst), label);
	}

	/** Emit .data section for a string literal (null-terminated). */
	public void allocateString(String label, String value) {
		String content = value;
		if (content.length() >= 2 && content.startsWith("\"") && content.endsWith("\""))
			content = content.substring(1, content.length() - 1);
		content = content.replace("\\", "\\\\").replace("\"", "\\\"");
		fileWriter.print(".data\n");
		fileWriter.format("\t%s: .asciiz \"%s\"\n", label, content);
		ensureTextWithPreamble();
	}

	/** Load from base+offset with null check (invalid pointer -> exit). */
	public void loadField(Temp dst, Temp base, int offset) {
		fileWriter.format("\tbeq %s,$zero,ptr_error_loadField\n", reg(base));
		fileWriter.format("\tlw %s,%d(%s)\n", reg(dst), offset, reg(base));
	}

	/** Load from array[index]; length at 0(base), elements at 4(base)+. Null and bounds check. Index may be null (constant 0). */
	public void loadArray(Temp dst, Temp arrayBase, Temp index) {
		int siteId = loadArraySiteId++;
		if (DEBUG_LOAD_ARRAY_SITE) fileWriter.format("# loadArray #%d base=%s index=%s\n", siteId, reg(arrayBase), index == null ? "0" : reg(index));
		if (DEBUG_TRACE_CALLS) {
			fileWriter.format("# DEBUG loadArray #%d: printing array base value (save/restore to avoid corrupting base reg):\n", siteId);
			fileWriter.format("\taddi $sp,$sp,-4\n\tsw %s,0($sp)\n", reg(arrayBase));
			fileWriter.format("\tmove $a0,%s\n\tli $v0,1\n\tsyscall\n\tli $a0,10\n\tli $v0,11\n\tsyscall\n", reg(arrayBase));
			fileWriter.format("\tlw %s,0($sp)\n\taddi $sp,$sp,4\n", reg(arrayBase));
		}
		String indexReg = index == null ? "$s1" : reg(index);
		if (index == null) fileWriter.format("\tli $s1,0\n");
		String errLabel = DEBUG_LOAD_ARRAY_SITE ? String.format("ptr_error_loadArray_%d", siteId) : "ptr_error_loadArray";
		fileWriter.format("\tbeq %s,$zero,%s\n", reg(arrayBase), errLabel);
		fileWriter.format("\tlw $s0,0(%s)\n", reg(arrayBase));
		fileWriter.format("\tblt %s,$zero,bounds_error\n", indexReg);
		fileWriter.format("\tbge %s,$s0,bounds_error\n", indexReg);
		fileWriter.format("\tli $s0,4\n");
		fileWriter.format("\tmul $s0,$s0,%s\n", indexReg);
		fileWriter.format("\tadd $s0,$s0,%s\n", reg(arrayBase));
		fileWriter.format("\tlw %s,4($s0)\n", reg(dst));
		if (DEBUG_LOAD_ARRAY_SITE) {
			String doneLabel = "loadArray_done_" + siteId;
			fileWriter.format("\tj %s\n", doneLabel);
			fileWriter.format("%s:\n\tli $s1,%d\n\tj ptr_error_loadArray_common\n", errLabel, siteId);
			fileWriter.format("%s:\n", doneLabel);
		}
	}

	public void storeField(Temp base, int offset, Temp src) {
		// Copy base to $s1 so base and src can be the same register (e.g. after call).
		// $s0 is used by array/saturation; $s1 is free per spec ($s0-$s9 for aux).
		fileWriter.format("\tmove $s1,%s\n", reg(base));
		fileWriter.format("\tbeq $s1,$zero,ptr_error_storeField\n");
		fileWriter.format("\tsw %s,%d($s1)\n", reg(src), offset);
	}

	/** Store to array[index]; index may be null (constant 0). */
	public void storeArray(Temp arrayBase, Temp index, Temp src) {
		String indexReg = index == null ? "$s1" : reg(index);
		if (index == null) fileWriter.format("\tli $s1,0\n");
		fileWriter.format("\tbeq %s,$zero,ptr_error_storeArray\n", reg(arrayBase));
		fileWriter.format("\tlw $s0,0(%s)\n", reg(arrayBase));
		fileWriter.format("\tblt %s,$zero,bounds_error\n", indexReg);
		fileWriter.format("\tbge %s,$s0,bounds_error\n", indexReg);
		fileWriter.format("\tli $s0,4\n");
		fileWriter.format("\tmul $s0,$s0,%s\n", indexReg);
		fileWriter.format("\tadd $s0,$s0,%s\n", reg(arrayBase));
		fileWriter.format("\tsw %s,4($s0)\n", reg(src));
	}
	/** Store src at array[index]; base is in $s2 (e.g. after moveBaseToS2). */
	public void storeArrayWithBaseInS2(Temp index, Temp src) {
		String indexReg = index == null ? "$s1" : reg(index);
		// When index is null, caller has already set $s1 (e.g. via PopS1 after PushS1).
		if (DEBUG_STORE_ARRAY) {
			fileWriter.format("\t# DEBUG storeArray: index=%s value=%s base=$s2\n", indexReg, reg(src));
			fileWriter.format("\taddi $sp,$sp,-8\n\tsw $a0,0($sp)\n\tsw $v0,4($sp)\n");
			fileWriter.format("\tli $a0,91\n\tli $v0,11\n\tsyscall\n");
			fileWriter.format("\tmove $a0,%s\n\tli $v0,1\n\tsyscall\n\tli $a0,32\n\tli $v0,11\n\tsyscall\n", indexReg);
			fileWriter.format("\tmove $a0,%s\n\tli $v0,1\n\tsyscall\n\tli $a0,32\n\tli $v0,11\n\tsyscall\n", reg(src));
			fileWriter.format("\tlw $a0,0($sp)\n\tlw $v0,4($sp)\n\taddi $sp,$sp,8\n");
		}
		fileWriter.format("\tbeq $s2,$zero,ptr_error_storeArray\n");
		fileWriter.format("\tlw $s0,0($s2)\n");
		fileWriter.format("\tblt %s,$zero,bounds_error\n", indexReg);
		fileWriter.format("\tbge %s,$s0,bounds_error\n", indexReg);
		fileWriter.format("\tli $s0,4\n");
		fileWriter.format("\tmul $s0,$s0,%s\n", indexReg);
		fileWriter.format("\tadd $s0,$s0,$s2\n");
		fileWriter.format("\tsw %s,4($s0)\n", reg(src));
	}
	public void store(String varName, Temp src)
	{
		if (currentFunction != null && localVarNames.contains(varName)) {
			Map<String, Integer> offsets = functionVarOffsets.get(currentFunction);
			if (offsets != null && offsets.containsKey(varName)) {
				int off = offsets.get(varName);
				if (DEBUG_TRACE_CALLS) fileWriter.format("# store %s -> %s ($sp+%d)\n", reg(src), varName, off);
				fileWriter.format("\tsw %s,%d($sp)\n", reg(src), off);
				return;
			}
		}
		String label = varLabel(varName);
		ensureDataEmitted(label);
		if (DEBUG_TRACE_CALLS) fileWriter.format("# store %s -> %s (%s)\n", reg(src), varName, label);
		fileWriter.format("\tsw %s,%s\n", reg(src), label);
	}

	/** Copy $a0 to $s2 (used by IrCommandCopyA0ToS2). */
	public void copyA0ToS2() {
		fileWriter.print("\tmove $s2,$a0\n");
	}
	/** Load from $s2 into dst (used by IrCommandLoadThis). */
	public void loadThis(Temp dst) {
		fileWriter.format("\tmove %s,$s2\n", reg(dst));
	}
	/** Store incoming argument register $a0..$a3 into param slot at function entry. */
	public void storeParam(int paramIndex, String varName) {
		if (currentFunction != null && localVarNames.contains(varName)) {
			Map<String, Integer> offsets = functionVarOffsets.get(currentFunction);
			if (offsets != null && offsets.containsKey(varName)) {
				int off = offsets.get(varName);
				String reg = paramIndex <= 3 ? "$a" + paramIndex : "$a0";
				if (DEBUG_TRACE_CALLS) fileWriter.format("# storeParam $a%d -> %s ($sp+%d)\n", paramIndex, varName, off);
				fileWriter.format("\tsw %s,%d($sp)\n", reg, off);
				if (DEBUG_TRACE_CALLS && paramIndex == 0) {
					fileWriter.format("# DEBUG storeParam: value stored in $sp+%d (param 0):\n", off);
					fileWriter.format("\tmove $t0,%s\n\tmove $a0,$t0\n\tli $v0,1\n\tsyscall\n\tli $a0,10\n\tli $v0,11\n\tsyscall\n", reg);
				}
				return;
			}
		}
		String label = varLabel(varName);
		ensureDataEmitted(label);
		String reg = paramIndex <= 3 ? "$a" + paramIndex : "$a0";
		if (DEBUG_TRACE_CALLS) fileWriter.format("# storeParam $a%d -> %s (%s)\n", paramIndex, varName, label);
		fileWriter.format("\tsw %s,%s\n", reg, label);
		if (DEBUG_TRACE_CALLS && paramIndex == 0) {
			fileWriter.format("# DEBUG storeParam: value stored in %s (param 0):\n", label);
			fileWriter.format("\tmove $t0,%s\n\tmove $a0,$t0\n\tli $v0,1\n\tsyscall\n\tli $a0,10\n\tli $v0,11\n\tsyscall\n", reg);
		}
	}

	/** Push temp onto stack (4 bytes). */
	public void push(Temp t) {
		fileWriter.format("\taddi $sp,$sp,-4\n");
		fileWriter.format("\tsw %s,0($sp)\n", reg(t));
	}
	/** Pop temp from stack (4 bytes). */
	public void pop(Temp t) {
		fileWriter.format("\tlw %s,0($sp)\n", reg(t));
		fileWriter.format("\taddi $sp,$sp,4\n");
	}
	/** Pop stack into $s1 (so we don't overwrite call result in base's register). */
	public void popToS1() {
		fileWriter.format("\tlw $s1,0($sp)\n");
		fileWriter.format("\taddi $sp,$sp,4\n");
	}
	/** Push $s2 onto stack (preserve array base across RHS that may call functions). */
	public void pushS2() {
		fileWriter.format("\taddi $sp,$sp,-4\n");
		fileWriter.print("\tsw $s2,0($sp)\n");
	}
	/** Pop stack into $s2 (restore array base after RHS). */
	public void popS2() {
		fileWriter.format("\tlw $s2,0($sp)\n");
		fileWriter.format("\taddi $sp,$sp,4\n");
	}
	/** Move temp into $s1 (preserve index across RHS). */
	public void moveToS1(Temp t) {
		fileWriter.format("\tmove $s1,%s\n", reg(t));
		if (DEBUG_STORE_ARRAY) fileWriter.print("\t# DBG:afterMoveToS1 print $s1:\n\taddi $sp,$sp,-8\n\tsw $a0,0($sp)\n\tsw $v0,4($sp)\n\tmove $a0,$s1\n\tli $v0,1\n\tsyscall\n\tli $a0,10\n\tli $v0,11\n\tsyscall\n\tlw $a0,0($sp)\n\tlw $v0,4($sp)\n\taddi $sp,$sp,8\n");
	}
	/** Push $s1 onto stack (preserve index across RHS). */
	public void pushS1() {
		fileWriter.format("\taddi $sp,$sp,-4\n");
		fileWriter.print("\tsw $s1,0($sp)\n");
		if (DEBUG_STORE_ARRAY) fileWriter.print("\t# DBG:afterPushS1 (pushed $s1)\n");
	}
	/** Pop stack into $s1 (restore index after RHS). */
	public void popS1() {
		fileWriter.format("\tlw $s1,0($sp)\n");
		fileWriter.format("\taddi $sp,$sp,4\n");
		if (DEBUG_STORE_ARRAY) fileWriter.print("\t# DBG:afterPopS1 print $s1:\n\taddi $sp,$sp,-8\n\tsw $a0,0($sp)\n\tsw $v0,4($sp)\n\tmove $a0,$s1\n\tli $v0,1\n\tsyscall\n\tli $a0,10\n\tli $v0,11\n\tsyscall\n\tlw $a0,0($sp)\n\tlw $v0,4($sp)\n\taddi $sp,$sp,8\n");
	}
	/** Store src at offset($s1) with null check. Base is in $s1 (e.g. after popToS1). */
	public void storeFieldBaseS1(int offset, Temp src) {
		fileWriter.format("\tbeq $s1,$zero,ptr_error_storeFieldS1\n");
		fileWriter.format("\tsw %s,%d($s1)\n", reg(src), offset);
	}

	public void li(Temp t, int value)
	{
		fileWriter.format("\tli %s,%d\n", reg(t), value);
	}
	public void moveReg(String destReg, Temp src)
	{
		fileWriter.format("\tmove %s,%s\n", destReg, reg(src));
	}
	/** Clamp value in reg to L integer range [-32768, 32767] using $s0. */
	private void clampToS16(String reg) {
		int id = internalLabelCounter++;
		String sh = "sat_high_" + id;
		String sl = "sat_low_" + id;
		String done = "sat_done_" + id;
		fileWriter.format("\tli $s0,32767\n");
		fileWriter.format("\tbgt %s,$s0,%s\n", reg, sh);
		fileWriter.format("\tli $s0,-32768\n");
		fileWriter.format("\tblt %s,$s0,%s\n", reg, sl);
		fileWriter.format("\tj %s\n", done);
		fileWriter.format("%s:\n\tli $s0,32767\n\tmove %s,$s0\n\tj %s\n", sh, reg, done);
		fileWriter.format("%s:\n\tli $s0,-32768\n\tmove %s,$s0\n", sl, reg);
		fileWriter.format("%s:\n", done);
	}

	public void add(Temp dst, Temp oprnd1, Temp oprnd2)
	{
		fileWriter.format("\tadd %s,%s,%s\n", reg(dst), reg(oprnd1), reg(oprnd2));
		clampToS16(reg(dst));
	}
	public void mul(Temp dst, Temp oprnd1, Temp oprnd2)
	{
		fileWriter.format("\tmul %s,%s,%s\n", reg(dst), reg(oprnd1), reg(oprnd2));
		clampToS16(reg(dst));
	}
	public void sub(Temp dst, Temp oprnd1, Temp oprnd2)
	{
		fileWriter.format("\tsub %s,%s,%s\n", reg(dst), reg(oprnd1), reg(oprnd2));
		clampToS16(reg(dst));
	}
	public void div(Temp dst, Temp oprnd1, Temp oprnd2)
	{
		int id = internalLabelCounter++;
		String lz = "div_by_zero_" + id;
		String lok = "div_ok_" + id;
		fileWriter.format("\tbeq %s,$zero,%s\n", reg(oprnd2), lz);
		fileWriter.format("\tdiv %s,%s\n", reg(oprnd1), reg(oprnd2));
		fileWriter.format("\tmflo %s\n", reg(dst));
		clampToS16(reg(dst));
		fileWriter.format("\tj %s\n", lok);
		fileWriter.format("%s:\n", lz);
		fileWriter.format("\tla $a0,string_illegal_div_by_0\n");
		fileWriter.format("\tli $v0,4\n\tsyscall\n");
		fileWriter.format("\tli $v0,10\n\tsyscall\n");
		fileWriter.format("%s:\n", lok);
	}
	public void label(String inlabel)
	{
		ensureTextWithPreamble();
		// User's main is at main_actual so SPIM entry "main" is our global init.
		String mipsLabel = "main".equals(inlabel) ? "main_actual" : inlabel;
		fileWriter.format("%s:\n", mipsLabel);
		// If this is a function entry, allocate stack frame for locals/params (after label so jal lands here)
		if (functionVarOffsets.containsKey(inlabel)) {
			currentFunction = inlabel;
			int frameSize = functionFrameSizes.getOrDefault(inlabel, 0);
			if (frameSize > 0)
				fileWriter.format("\taddi $sp,$sp,-%d\n", frameSize);
		}
	}	
	public void jump(String inlabel)
	{
		fileWriter.format("\tj %s\n",inlabel);
	}
	/** Emit jr $ra (return to caller). Required at function end. Deallocates frame if function has locals. */
	public void jrRa()
	{
		if (currentFunction != null) {
			int frameSize = functionFrameSizes.getOrDefault(currentFunction, 0);
			if (frameSize > 0)
				fileWriter.format("\taddi $sp,$sp,%d\n", frameSize);
		}
		fileWriter.format("\tjr $ra\n");
	}
	public void blt(Temp oprnd1, Temp oprnd2, String label)
	{
		fileWriter.format("\tblt %s,%s,%s\n", reg(oprnd1), reg(oprnd2), label);				
	}
	public void bge(Temp oprnd1, Temp oprnd2, String label)
	{
		fileWriter.format("\tbge %s,%s,%s\n", reg(oprnd1), reg(oprnd2), label);				
	}
	public void bne(Temp oprnd1, Temp oprnd2, String label)
	{
		fileWriter.format("\tbne %s,%s,%s\n", reg(oprnd1), reg(oprnd2), label);				
	}
	public void beq(Temp oprnd1, Temp oprnd2, String label)
	{
		fileWriter.format("\tbeq %s,%s,%s\n", reg(oprnd1), reg(oprnd2), label);				
	}
	public void beqz(Temp oprnd1, String label)
	{
		// If condition temp is null (e.g. call result not passed), use $v0 (return value from last jal)
		String regToUse = (oprnd1 == null) ? "$v0" : reg(oprnd1);
		fileWriter.format("\tbeq %s,$zero,%s\n", regToUse, label);
	}
	/** Emit only a label (no .text). For use inside comparison sequences. */
	public void emitLabelOnly(String name) {
		fileWriter.format("%s:\n", name);
	}
	/** Resolve operand to register; use $v0 if null (e.g. call result not in a temp). */
	private String regOrV0(Temp t) { return (t == null) ? "$v0" : reg(t); }
	/** Set dst to 1 if t1 < t2 else 0. */
	public void compareLt(Temp dst, Temp t1, Temp t2) {
		int id = internalLabelCounter++;
		String L1 = "cmp_lt_true_" + id;
		String L2 = "cmp_lt_done_" + id;
		fileWriter.format("\tblt %s,%s,%s\n", regOrV0(t1), regOrV0(t2), L1);
		fileWriter.format("\tli %s,0\n", reg(dst));
		fileWriter.format("\tj %s\n", L2);
		emitLabelOnly(L1);
		fileWriter.format("\tli %s,1\n", reg(dst));
		emitLabelOnly(L2);
	}
	/** Set dst to 1 if t1 > t2 else 0. (t1 > t2 <=> t2 < t1) */
	public void compareGt(Temp dst, Temp t1, Temp t2) {
		int id = internalLabelCounter++;
		String L1 = "cmp_gt_true_" + id;
		String L2 = "cmp_gt_done_" + id;
		fileWriter.format("\tblt %s,%s,%s\n", regOrV0(t2), regOrV0(t1), L1);
		fileWriter.format("\tli %s,0\n", reg(dst));
		fileWriter.format("\tj %s\n", L2);
		emitLabelOnly(L1);
		fileWriter.format("\tli %s,1\n", reg(dst));
		emitLabelOnly(L2);
	}
	/** Set dst to 1 if t1 == t2 else 0. */
	public void compareEq(Temp dst, Temp t1, Temp t2) {
		int id = internalLabelCounter++;
		String L1 = "cmp_eq_true_" + id;
		String L2 = "cmp_eq_done_" + id;
		fileWriter.format("\tbeq %s,%s,%s\n", regOrV0(t1), regOrV0(t2), L1);
		fileWriter.format("\tli %s,0\n", reg(dst));
		fileWriter.format("\tj %s\n", L2);
		emitLabelOnly(L1);
		fileWriter.format("\tli %s,1\n", reg(dst));
		emitLabelOnly(L2);
	}
	public void printString(Temp t)
	{
		int id = internalLabelCounter++;
		String okLabel = "ps_ok_" + id;
		fileWriter.format("\tmove $a0,%s\n", reg(t));
		fileWriter.format("\tbne $a0,$zero,%s\n\tla $a0,dbg_print_a0\n\tli $v0,4\n\tsyscall\n\tli $v0,10\n\tsyscall\n", okLabel);
		fileWriter.format("%s:\n\tli $v0,4\n\tsyscall\n", okLabel);
	}

	/** Concatenate two strings; result in dst. left and right are string pointers. */
	public void concatStrings(Temp dst, Temp left, Temp right) {
		ensureTextWithPreamble();
		// Push both to stack so we don't overwrite when left/right share regs with $a0/$a1 or each other
		fileWriter.print("\taddi $sp,$sp,-8\n");
		fileWriter.format("\tsw %s,0($sp)\n", reg(left));
		fileWriter.format("\tsw %s,4($sp)\n", reg(right));
		fileWriter.print("\tlw $a0,0($sp)\n");
		fileWriter.print("\tlw $a1,4($sp)\n");
		fileWriter.print("\taddi $sp,$sp,8\n");
		fileWriter.print("\tjal string_concat\n");
		fileWriter.format("\tmove %s,$v0\n", reg(dst));
	}
	/** Frame: 52 bytes = $t0..$t9 (0..36) + $ra (40) + $s1 (44) + $s2 (48). Saves $s1/$s2 so callee (e.g. storeField) cannot clobber caller's index/base. */
	public void callFunc(String funcName, java.util.List<Temp> args, Temp dst, String receiverVarName)
	{
		callFunc(funcName, args, dst, receiverVarName, false);
	}
	public void callFunc(String funcName, java.util.List<Temp> args, Temp dst, String receiverVarName, boolean firstArgFromS2)
	{
		fileWriter.format("\taddi $sp,$sp,-52\n");
		fileWriter.format("\tsw $t0,0($sp)\n\tsw $t1,4($sp)\n\tsw $t2,8($sp)\n\tsw $t3,12($sp)\n\tsw $t4,16($sp)\n\tsw $t5,20($sp)\n\tsw $t6,24($sp)\n\tsw $t7,28($sp)\n\tsw $t8,32($sp)\n\tsw $t9,36($sp)\n\tsw $ra,40($sp)\n\tsw $s1,44($sp)\n\tsw $s2,48($sp)\n");
		if (firstArgFromS2)
			fileWriter.print("\tmove $a0,$s2\n");
		else if (receiverVarName != null)
			loadVarToA0(receiverVarName);
		else if (args != null && args.size() > 0 && args.get(0) != null)
			fileWriter.format("\tmove $a0,%s\n", reg(args.get(0)));
		if (firstArgFromS2) {
			if (args != null && args.size() > 0 && args.get(0) != null) fileWriter.format("\tmove $a1,%s\n", reg(args.get(0)));
			if (args != null && args.size() > 1 && args.get(1) != null) fileWriter.format("\tmove $a2,%s\n", reg(args.get(1)));
			if (args != null && args.size() > 2 && args.get(2) != null) fileWriter.format("\tmove $a3,%s\n", reg(args.get(2)));
		} else {
			if (args != null && args.size() > 1 && args.get(1) != null) fileWriter.format("\tmove $a1,%s\n", reg(args.get(1)));
			if (args != null && args.size() > 2 && args.get(2) != null) fileWriter.format("\tmove $a2,%s\n", reg(args.get(2)));
			if (args != null && args.size() > 3 && args.get(3) != null) fileWriter.format("\tmove $a3,%s\n", reg(args.get(3)));
		}
		if (DEBUG_TRACE_CALLS) {
			fileWriter.format("# call %s: $a0 (first arg) = %s\n", funcName, firstArgFromS2 ? "$s2" : (args != null && args.size() > 0 ? reg(args.get(0)) : "?"));
			fileWriter.print("\tmove $t0,$a0\n");
			fileWriter.print("\tli $v0,1\n\tsyscall\n");
			fileWriter.print("\tli $a0,10\n\tli $v0,11\n\tsyscall\n");
			fileWriter.print("\tmove $a0,$t0\n");
		}
		fileWriter.format("\tjal %s\n", funcName);
		fileWriter.format("\tlw $ra,40($sp)\n\tlw $t0,0($sp)\n\tlw $t1,4($sp)\n\tlw $t2,8($sp)\n\tlw $t3,12($sp)\n\tlw $t4,16($sp)\n\tlw $t5,20($sp)\n\tlw $t6,24($sp)\n\tlw $t7,28($sp)\n\tlw $t8,32($sp)\n\tlw $t9,36($sp)\n\tlw $s1,44($sp)\n\tlw $s2,48($sp)\n");
		fileWriter.format("\taddi $sp,$sp,52\n");
		if (dst != null) fileWriter.format("\tmove %s,$v0\n", reg(dst));
	}
	/** Virtual method call: load method from object's vtable at slot, jalr. Receiver in $a0. */
	public void callFuncVirtual(int methodSlot, java.util.List<Temp> args, Temp dst, boolean firstArgFromS2)
	{
		fileWriter.format("\taddi $sp,$sp,-52\n");
		fileWriter.format("\tsw $t0,0($sp)\n\tsw $t1,4($sp)\n\tsw $t2,8($sp)\n\tsw $t3,12($sp)\n\tsw $t4,16($sp)\n\tsw $t5,20($sp)\n\tsw $t6,24($sp)\n\tsw $t7,28($sp)\n\tsw $t8,32($sp)\n\tsw $t9,36($sp)\n\tsw $ra,40($sp)\n\tsw $s1,44($sp)\n\tsw $s2,48($sp)\n");
		if (firstArgFromS2)
			fileWriter.print("\tmove $a0,$s2\n");
		else if (args != null && args.size() > 0 && args.get(0) != null)
			fileWriter.format("\tmove $a0,%s\n", reg(args.get(0)));
		if (firstArgFromS2) {
			if (args != null && args.size() > 0 && args.get(0) != null) fileWriter.format("\tmove $a1,%s\n", reg(args.get(0)));
			if (args != null && args.size() > 1 && args.get(1) != null) fileWriter.format("\tmove $a2,%s\n", reg(args.get(1)));
			if (args != null && args.size() > 2 && args.get(2) != null) fileWriter.format("\tmove $a3,%s\n", reg(args.get(2)));
		} else {
			if (args != null && args.size() > 1 && args.get(1) != null) fileWriter.format("\tmove $a1,%s\n", reg(args.get(1)));
			if (args != null && args.size() > 2 && args.get(2) != null) fileWriter.format("\tmove $a2,%s\n", reg(args.get(2)));
			if (args != null && args.size() > 3 && args.get(3) != null) fileWriter.format("\tmove $a3,%s\n", reg(args.get(3)));
		}
		fileWriter.format("\tlw $t0,0($a0)\n");
		fileWriter.format("\tlw $t0,%d($t0)\n", methodSlot * 4);
		fileWriter.print("\tjalr $t0\n");
		fileWriter.format("\tlw $ra,40($sp)\n\tlw $t0,0($sp)\n\tlw $t1,4($sp)\n\tlw $t2,8($sp)\n\tlw $t3,12($sp)\n\tlw $t4,16($sp)\n\tlw $t5,20($sp)\n\tlw $t6,24($sp)\n\tlw $t7,28($sp)\n\tlw $t8,32($sp)\n\tlw $t9,36($sp)\n\tlw $s1,44($sp)\n\tlw $s2,48($sp)\n");
		fileWriter.format("\taddi $sp,$sp,52\n");
		if (dst != null) fileWriter.format("\tmove %s,$v0\n", reg(dst));
	}
	/** Copy base temp into $s2 so base survives arg evaluation and call; base must be in reg before this. */
	public void moveBaseToS2(Temp base) {
		if (DEBUG_TRACE_CALLS) fileWriter.format("# moveBaseToS2: $s2 <- %s (first arg)\n", reg(base));
		fileWriter.format("\tmove $s2,%s\n", reg(base));
	}
	/** Load variable (by IR name) directly into $s2; no temp, so base cannot be clobbered by arg eval. */
	public void loadVarToS2(String varIrName) {
		if (currentFunction != null && localVarNames.contains(varIrName)) {
			Map<String, Integer> offsets = functionVarOffsets.get(currentFunction);
			if (offsets != null && offsets.containsKey(varIrName)) {
				fileWriter.format("\tlw $s2,%d($sp)\n", offsets.get(varIrName));
				return;
			}
		}
		String label = varLabel(varIrName);
		ensureDataEmitted(label);
		fileWriter.format("\tlw $s2,%s\n", label);
	}
	/** Store $s2 back to variable slot (restore after call so callee did not overwrite outer activation's local). */
	public void storeS2ToVar(String varIrName) {
		if (currentFunction != null && localVarNames.contains(varIrName)) {
			Map<String, Integer> offsets = functionVarOffsets.get(currentFunction);
			if (offsets != null && offsets.containsKey(varIrName)) {
				fileWriter.format("\tsw $s2,%d($sp)\n", offsets.get(varIrName));
				return;
			}
		}
		String label = varLabel(varIrName);
		ensureDataEmitted(label);
		fileWriter.format("\tsw $s2,%s\n", label);
	}
	/** Load variable (by IR name) into $a0 so method call gets receiver from memory (avoids clobbered temp). */
	public void loadVarToA0(String varIrName) {
		if (currentFunction != null && localVarNames.contains(varIrName)) {
			Map<String, Integer> offsets = functionVarOffsets.get(currentFunction);
			if (offsets != null && offsets.containsKey(varIrName)) {
				fileWriter.format("\tlw $a0,%d($sp)\n", offsets.get(varIrName));
				return;
			}
		}
		String label = varLabel(varIrName);
		ensureDataEmitted(label);
		fileWriter.format("\tlw $a0,%s\n", label);
	}
	/** Call function and store $v0 at base+offset. Base must already be in $s2 (call moveBaseToS2 first). Frame 48 bytes: $t0-$t9, $ra, $s2. */
	public void callFuncAndStoreResultAtFieldWithBaseInS2(String funcName, java.util.List<Temp> args, int fieldOffset)
	{
		fileWriter.format("\taddi $sp,$sp,-48\n");
		fileWriter.format("\tsw $t0,0($sp)\n\tsw $t1,4($sp)\n\tsw $t2,8($sp)\n\tsw $t3,12($sp)\n\tsw $t4,16($sp)\n\tsw $t5,20($sp)\n\tsw $t6,24($sp)\n\tsw $t7,28($sp)\n\tsw $t8,32($sp)\n\tsw $t9,36($sp)\n\tsw $ra,40($sp)\n\tsw $s2,44($sp)\n");
		if (args != null && args.size() > 0 && args.get(0) != null) fileWriter.format("\tmove $a0,%s\n", reg(args.get(0)));
		if (args != null && args.size() > 1 && args.get(1) != null) fileWriter.format("\tmove $a1,%s\n", reg(args.get(1)));
		if (args != null && args.size() > 2 && args.get(2) != null) fileWriter.format("\tmove $a2,%s\n", reg(args.get(2)));
		if (args != null && args.size() > 3 && args.get(3) != null) fileWriter.format("\tmove $a3,%s\n", reg(args.get(3)));
		fileWriter.format("\tjal %s\n", funcName);
		fileWriter.format("\tlw $ra,40($sp)\n\tlw $t0,0($sp)\n\tlw $t1,4($sp)\n\tlw $t2,8($sp)\n\tlw $t3,12($sp)\n\tlw $t4,16($sp)\n\tlw $t5,20($sp)\n\tlw $t6,24($sp)\n\tlw $t7,28($sp)\n\tlw $t8,32($sp)\n\tlw $t9,36($sp)\n\tlw $s2,44($sp)\n");
		fileWriter.format("\taddi $sp,$sp,48\n");
		fileWriter.format("\tbeq $s2,$zero,ptr_error_s2\n");
		fileWriter.format("\tsw $v0,%d($s2)\n", fieldOffset);
	}
	/** Virtual call and store $v0 at base+offset. Base (receiver) in $s2. */
	public void callFuncVirtualAndStoreResultAtFieldWithBaseInS2(int methodSlot, java.util.List<Temp> args, int fieldOffset)
	{
		fileWriter.format("\taddi $sp,$sp,-48\n");
		fileWriter.format("\tsw $t0,0($sp)\n\tsw $t1,4($sp)\n\tsw $t2,8($sp)\n\tsw $t3,12($sp)\n\tsw $t4,16($sp)\n\tsw $t5,20($sp)\n\tsw $t6,24($sp)\n\tsw $t7,28($sp)\n\tsw $t8,32($sp)\n\tsw $t9,36($sp)\n\tsw $ra,40($sp)\n\tsw $s2,44($sp)\n");
		fileWriter.print("\tmove $a0,$s2\n");
		if (args != null && args.size() > 0 && args.get(0) != null) fileWriter.format("\tmove $a1,%s\n", reg(args.get(0)));
		if (args != null && args.size() > 1 && args.get(1) != null) fileWriter.format("\tmove $a2,%s\n", reg(args.get(1)));
		if (args != null && args.size() > 2 && args.get(2) != null) fileWriter.format("\tmove $a3,%s\n", reg(args.get(2)));
		fileWriter.format("\tlw $t0,0($a0)\n");
		fileWriter.format("\tlw $t0,%d($t0)\n", methodSlot * 4);
		fileWriter.print("\tjalr $t0\n");
		fileWriter.format("\tlw $ra,40($sp)\n\tlw $t0,0($sp)\n\tlw $t1,4($sp)\n\tlw $t2,8($sp)\n\tlw $t3,12($sp)\n\tlw $t4,16($sp)\n\tlw $t5,20($sp)\n\tlw $t6,24($sp)\n\tlw $t7,28($sp)\n\tlw $t8,32($sp)\n\tlw $t9,36($sp)\n\tlw $s2,44($sp)\n");
		fileWriter.format("\taddi $sp,$sp,48\n");
		fileWriter.format("\tbeq $s2,$zero,ptr_error_s2\n");
		fileWriter.format("\tsw $v0,%d($s2)\n", fieldOffset);
	}
	/** For legacy IR: call and store $v0 at base+offset; copies base to $s2 then uses WithBaseInS2. */
	public void callFuncAndStoreResultAtField(String funcName, java.util.List<Temp> args, Temp base, int fieldOffset) {
		moveBaseToS2(base);
		callFuncAndStoreResultAtFieldWithBaseInS2(funcName, args, fieldOffset);
	}
	/** For legacy IR: base is on top of stack; pop into $s2 then call and store. */
	public void callFuncAndStoreResultAtFieldFromStack(String funcName, java.util.List<Temp> args, int fieldOffset) {
		fileWriter.format("\tlw $s2,0($sp)\n");
		fileWriter.format("\taddi $sp,$sp,4\n");
		callFuncAndStoreResultAtFieldWithBaseInS2(funcName, args, fieldOffset);
	}

	/**************************************/
	/* USUAL SINGLETON IMPLEMENTATION ... */
	/**************************************/
	private static MipsGenerator instance = null;

	/*****************************/
	/* PREVENT INSTANTIATION ... */
	/*****************************/
	protected MipsGenerator() {}

	/******************************/
	/* GET SINGLETON INSTANCE ... */
	/******************************/
	public static MipsGenerator getInstance()
	{
		if (instance == null)
			instance = new MipsGenerator();
		return instance;
	}
}
