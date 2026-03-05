/***********/
/* PACKAGE */
/***********/
package mips;

/*******************/
/* GENERAL IMPORTS */
/*******************/
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
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
	/** When true, emit trace chars (M=main, E=main returned, R=func entry, L=while start, C=before concat, W=while end, B=loop-back j, X=jr $ra). Set false for submission. */
	private static final boolean DEBUG_MIPS_TRACE = false;
	/** When true (e.g. -DDEBUG_RECURSION=1): emit R on function entry, print $v0 before jr $ra, then X. For TEST_27 deep recursion. */
	private static boolean debugRecursion() { return "1".equals(System.getProperty("DEBUG_RECURSION")); }
	private static boolean debugRecursionTrace() { return DEBUG_MIPS_TRACE || debugRecursion(); }
	/** When true: emit MIPS comments and stderr for vtable/virtual call (TEST_127 debug). Set false for test run (clean stderr). */
	private static final boolean DEBUG_VTABLE = false;
	/** When true (and DEBUG_VTABLE): emit MIPS that prints $a0 (receiver addr) before each virtual call so you can see object vs string. Set false for tests (pollutes stdout). */
	private static final boolean DEBUG_VTABLE_PRINT_A0 = false;
	/** When true: stderr at compile time + MIPS that prints $a0 and $a1 before each jal (for nested-call arg debug). Set false for tests. */
	private static final boolean DEBUG_ARGS = false;
		/** When true: at each storeParam emit MIPS to print the param value (param 0, 1, ...) so you see what sum8/product4 receive. Set false for tests. */
		private static final boolean DEBUG_PARAMS = false;
		/** When set (e.g. -DDEBUG_CALL_ARGS=1): emit MIPS to print all args right before jal so you see actual values passed. */
		private static boolean debugCallArgs() { return System.getProperty("DEBUG_CALL_ARGS") != null; }
	/** When set (e.g. -DDEBUG_FUNC_ARGS=hanoi): at entry to listed functions, print $a0 $a1 $a2 $a3 as integers (for TEST_168, TEST_53, etc.). */
	private static boolean debugFuncArgs(String funcName) {
		String list = System.getProperty("DEBUG_FUNC_ARGS");
		if (list == null || list.isEmpty() || funcName == null) return false;
		for (String s : list.split(",")) if (s.trim().equals(funcName)) return true;
		return false;
	}
	/** When set (e.g. -DDEBUG_OVERFLOW=1): before each clamp emit MIPS to print the value in the reg (one int per line). For TEST_31 / saturation debugging. */
	private static boolean debugOverflow() { return "1".equals(System.getProperty("DEBUG_OVERFLOW")); }
	private static int internalLabelCounter = 0;
	private PrintWriter fileWriter;
	private Map<Integer, String> tempToReg;
	/** IR var name (e.g. x@5) -> MIPS label (e.g. var_0) */
	private Map<String, String> varToLabel = new HashMap<>();
	private int varCounter = 0;
	/** Labels for which we have already emitted .data (so we don't duplicate or miss). */
	private Set<String> dataEmittedLabels = new HashSet<>();
	/** String literal labels already emitted (avoid duplicate .asciiz for same label). */
	private Set<String> stringLabelsEmitted = new HashSet<>();
	/** True after we have emitted .text and the j main preamble (so all code is in .text). */
	private boolean textSegmentEmitted = false;
	/** Per-site ID for loadArray so debug output shows which site failed (e.g. "Invalid Ptr (loadArray #3)"). */
	private int loadArraySiteId = 0;
	private boolean concatHelperEmitted = false;
	private boolean stringEqualsHelperEmitted = false;
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
	/** Emit MIPS to print one character (for trace). Preserves $a0,$v0. */
	private void emitTraceChar(int ascii) {
		if (!debugRecursionTrace()) return;
		fileWriter.print("\taddi $sp,$sp,-8\n\tsw $a0,0($sp)\n\tsw $v0,4($sp)\n");
		fileWriter.format("\tli $a0,%d\n\tli $v0,11\n\tsyscall\n", ascii);
		fileWriter.print("\tlw $a0,0($sp)\n\tlw $v0,4($sp)\n\taddi $sp,$sp,8\n");
	}
	/** DEBUG_RECURSION: print $v0 as integer then space (so you see return values before jr $ra). Preserves $v0,$a0. */
	private void emitDebugPrintV0() {
		if (!debugRecursion()) return;
		fileWriter.print("\taddi $sp,$sp,-8\n\tsw $v0,0($sp)\n\tsw $a0,4($sp)\n");
		fileWriter.print("\tmove $a0,$v0\n\tli $v0,1\n\tsyscall\n");
		fileWriter.print("\tli $a0,32\n\tli $v0,11\n\tsyscall\n");
		fileWriter.print("\tlw $v0,0($sp)\n\tlw $a0,4($sp)\n\taddi $sp,$sp,8\n");
	}
	/** DEBUG_RECURSION: print $sp (frame base) before loading $ra; newline. Preserves $sp,$ra,$a0,$v0. */
	private void emitDebugPrintSp() {
		if (!debugRecursion()) return;
		fileWriter.print("\taddi $sp,$sp,-8\n\tsw $a0,0($sp)\n\tsw $v0,4($sp)\n");
		fileWriter.print("\taddi $a0,$sp,8\n\tli $v0,1\n\tsyscall\n");
		fileWriter.print("\tli $a0,10\n\tli $v0,11\n\tsyscall\n");
		fileWriter.print("\tlw $a0,0($sp)\n\tlw $v0,4($sp)\n\taddi $sp,$sp,8\n");
	}
	/** DEBUG_RECURSION: at function entry print $a0 $a1 as integers (params m,n). Preserves $a0,$a1. */
	private void emitDebugPrintA0A1() {
		if (!debugRecursion()) return;
		fileWriter.print("\taddi $sp,$sp,-16\n\tsw $a0,0($sp)\n\tsw $a1,4($sp)\n\tsw $v0,8($sp)\n\tsw $ra,12($sp)\n");
		fileWriter.print("\tli $v0,1\n\tsyscall\n\tli $a0,32\n\tli $v0,11\n\tsyscall\n");
		fileWriter.print("\tlw $a0,4($sp)\n\tli $v0,1\n\tsyscall\n\tli $a0,10\n\tli $v0,11\n\tsyscall\n");
		fileWriter.print("\tlw $a0,0($sp)\n\tlw $a1,4($sp)\n\tlw $v0,8($sp)\n\tlw $ra,12($sp)\n\taddi $sp,$sp,16\n");
	}
	/** DEBUG_FUNC_ARGS: at function entry print $a0 $a1 $a2 $a3 as integers (n from to aux for hanoi). Preserves all. */
	private void emitDebugPrintA0A1A2A3() {
		fileWriter.print("\taddi $sp,$sp,-24\n\tsw $a0,0($sp)\n\tsw $a1,4($sp)\n\tsw $a2,8($sp)\n\tsw $a3,12($sp)\n\tsw $v0,16($sp)\n\tsw $ra,20($sp)\n");
		fileWriter.print("\tli $v0,1\n\tsyscall\n\tli $a0,32\n\tli $v0,11\n\tsyscall\n");
		fileWriter.print("\tlw $a0,4($sp)\n\tli $v0,1\n\tsyscall\n\tli $a0,32\n\tli $v0,11\n\tsyscall\n");
		fileWriter.print("\tlw $a0,8($sp)\n\tli $v0,1\n\tsyscall\n\tli $a0,32\n\tli $v0,11\n\tsyscall\n");
		fileWriter.print("\tlw $a0,12($sp)\n\tli $v0,1\n\tsyscall\n\tli $a0,10\n\tli $v0,11\n\tsyscall\n");
		fileWriter.print("\tlw $a0,0($sp)\n\tlw $a1,4($sp)\n\tlw $a2,8($sp)\n\tlw $a3,12($sp)\n\tlw $v0,16($sp)\n\tlw $ra,20($sp)\n\taddi $sp,$sp,24\n");
	}

	/** Emit string_concat helper (once). $a0=s1, $a1=s2 -> $v0=new string. Null ptr -> ptr_error. */
	private void emitStringConcatHelper() {
		if (concatHelperEmitted) return;
		concatHelperEmitted = true;
		fileWriter.print("string_concat:\n");
		fileWriter.print("\tbne $a0,$zero,concat_ck1\n\tla $a0,dbg_concat_a0\n\tli $v0,4\n\tsyscall\n\tli $v0,10\n\tsyscall\n");
		fileWriter.print("concat_ck1:\n\tbne $a1,$zero,concat_ok\n\tla $a0,dbg_concat_a1\n\tli $v0,4\n\tsyscall\n\tli $v0,10\n\tsyscall\n");
		fileWriter.print("concat_ok:\n");
		fileWriter.print("\taddi $sp,$sp,-36\n");
		fileWriter.print("\tsw $ra,0($sp)\n\tsw $s0,4($sp)\n\tsw $s1,8($sp)\n\tsw $a0,12($sp)\n\tsw $a1,16($sp)\n");
		fileWriter.print("\tsw $t0,20($sp)\n\tsw $t1,24($sp)\n\tsw $t4,28($sp)\n\tsw $t5,32($sp)\n");
		// len1: $s0 = strlen($a0), max 65535 to avoid infinite loop on bad ptr
		fileWriter.print("\tmove $t0,$a0\n\tli $s0,0\n");
		fileWriter.print("string_concat_len1:\n\tlb $t1,0($t0)\n\tbeq $t1,$zero,string_concat_len1_done\n");
		fileWriter.print("\taddi $t0,$t0,1\n\taddi $s0,$s0,1\n\tli $t1,65536\n\tbge $s0,$t1,string_concat_len1_done\n");
		fileWriter.print("\tj string_concat_len1\n");
		fileWriter.print("string_concat_len1_done:\n");
		// len2: $s1 = strlen($a1), max 65535
		fileWriter.print("\tlw $t0,16($sp)\n\tli $s1,0\n");
		fileWriter.print("string_concat_len2:\n\tlb $t1,0($t0)\n\tbeq $t1,$zero,string_concat_len2_done\n");
		fileWriter.print("\taddi $t0,$t0,1\n\taddi $s1,$s1,1\n\tli $t1,65536\n\tbge $s1,$t1,string_concat_len2_done\n");
		fileWriter.print("\tj string_concat_len2\n");
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
		fileWriter.print("\tmove $v0,$t5\n");
		fileWriter.print("\tlw $ra,0($sp)\n\tlw $s0,4($sp)\n\tlw $s1,8($sp)\n\tlw $a0,12($sp)\n\tlw $a1,16($sp)\n");
		fileWriter.print("\tlw $t0,20($sp)\n\tlw $t1,24($sp)\n\tlw $t4,28($sp)\n\tlw $t5,32($sp)\n");
		fileWriter.print("\taddi $sp,$sp,36\n");
		fileWriter.print("\tjr $ra\n");
	}

	/** Map logical function name to MIPS label (avoids SPIM reserved words like "abs"). Internal labels (Label_*) are unchanged. */
	private String toMipsFuncLabel(String logicalName) {
		if (logicalName == null) return logicalName;
		if (logicalName.startsWith("Label_")) return logicalName;  // internal label, don't prefix
		return "main".equals(logicalName) ? "main_actual" : "func_" + logicalName;
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

	/** Functions that make calls: save $ra at entry, restore before jr $ra. */
	private Set<String> functionSavesRa = new HashSet<>();
	/** Per-function param count so prologue only stores $a0-$a3 for param slots, not locals. */
	private Map<String, Integer> functionParamCount = new HashMap<>();
	/** Set stack layout for locals (so recursion works). Call after setRegisterAllocation. */
	public void setFunctionLayouts(Set<String> localVars, Map<String, Map<String, Integer>> varOffsets, Map<String, Integer> frameSizes, Map<String, Integer> paramCount, Set<String> savesRa) {
		this.localVarNames = (localVars != null) ? new HashSet<>(localVars) : new HashSet<>();
		this.functionVarOffsets = (varOffsets != null) ? new HashMap<>(varOffsets) : new HashMap<>();
		this.functionFrameSizes = (frameSizes != null) ? new HashMap<>(frameSizes) : new HashMap<>();
		this.functionParamCount = (paramCount != null) ? new HashMap<>(paramCount) : new HashMap<>();
		this.functionSavesRa = (savesRa != null) ? new HashSet<>(savesRa) : new HashSet<>();
	}

	/** Set output writer and write .data section + code entry. Must be called before mipsMe(). */
	public void setOutput(PrintWriter pw) {
		this.fileWriter = pw;
		this.textSegmentEmitted = false;
		this.concatHelperEmitted = false;
		this.stringEqualsHelperEmitted = false;
		this.dataEmittedLabels.clear();
		this.stringLabelsEmitted.clear();
		this.loadArraySiteId = 0;
		fileWriter.print(".data\n");
		fileWriter.print("string_access_violation: .asciiz \"Access Violation\"\n");
		fileWriter.print("string_illegal_div_by_0: .asciiz \"Illegal Division By Zero\"\n");
		fileWriter.print("string_invalid_ptr_dref: .asciiz \"Invalid Pointer Dereference\"\n");
		// Debug: which null-check failed (remove or keep for easier debugging)
		fileWriter.print("string_ptr_loadField: .asciiz \"Invalid Ptr (loadField)\\n\"\n");
		fileWriter.print("string_ptr_loadArray: .asciiz \"Invalid Ptr (loadArray)\\n\"\n");
		fileWriter.print("string_ptr_storeField: .asciiz \"Invalid Ptr (storeField)\\n\"\n");
		fileWriter.print("string_ptr_storeArray: .asciiz \"Invalid Ptr (storeArray)\\n\"\n");
		fileWriter.print("string_ptr_storeFieldS1: .asciiz \"Invalid Ptr (storeFieldBaseS1)\\n\"\n");
		fileWriter.print("string_ptr_s2: .asciiz \"Invalid Ptr (baseInS2)\\n\"\n");
		fileWriter.print("dbg_concat_a0: .asciiz \"DBG: concat $a0 null\\n\"\n");
		fileWriter.print("dbg_concat_a1: .asciiz \"DBG: concat $a1 null\\n\"\n");
		fileWriter.print("dbg_print_a0: .asciiz \"DBG: PrintString $a0 null\\n\"\n");
		/* Vtables emitted at end of file in .text so .word func_* gets full 32-bit address (SPIM) */
		fileWriter.print(".text\n");
		fileWriter.print("__compiler_entry:\n\tj main\n");
	}

	/** Emit vtable .data for each class (for dynamic dispatch). Must emit one word per slot so slot N is at offset N*4. */
	private void emitVtables() {
		int numSlots = types.VtableBuilder.getNumSlots();
		if (DEBUG_VTABLE) System.err.println("[DEBUG_VTABLE] numSlots=" + numSlots);
		for (String className : types.VtableBuilder.getClassNames()) {
			String[] entries = types.VtableBuilder.getVtableEntries(className);
			if (entries == null) continue;
			fileWriter.format("vtable_%s:\n", className);
			if (DEBUG_VTABLE) System.err.println("[DEBUG_VTABLE] vtable_" + className);
			for (int slot = 0; slot < numSlots; slot++) {
				String label = (entries.length > slot) ? entries[slot] : null;
				String mipsLabel = label != null ? toMipsFuncLabel(label) : null;
				if (label != null) {
					if (DEBUG_VTABLE) {
						fileWriter.format("# vtable slot %d -> %s\n", slot, mipsLabel);
						System.err.println("[DEBUG_VTABLE]   slot " + slot + " -> " + mipsLabel);
					}
					fileWriter.format("\t.word %s\n", mipsLabel);
				} else {
					fileWriter.print("\t.word 0\n");
				}
			}
		}
	}

	/** Emit "main:" label so SPIM entry runs global init first; then jump to main_actual. */
	public void emitProgramEntry() {
		fileWriter.print("main:\n");
		emitTraceChar('M');
	}

	/** Emit jal main_actual then exit; when main returns we must exit (otherwise jr $ra goes to garbage -> timeout). */
	public void emitPreambleAndHandlers() {
		fileWriter.print("jal main_actual\n");
		emitTraceChar('E');
		fileWriter.print("\tli $v0,10\n\tsyscall\n");
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
		emitStringEqualsHelper();
	}

	/** string_equals: $a0=s1, $a1=s2 -> $v0=1 if equal else 0. Null-safe. */
	private void emitStringEqualsHelper() {
		if (stringEqualsHelperEmitted) return;
		stringEqualsHelperEmitted = true;
		fileWriter.print("string_equals:\n");
		fileWriter.print("\tbne $a0,$zero,str_eq_ck2\n");
		fileWriter.print("\tbne $a1,$zero,str_eq_false\n");
		fileWriter.print("\tli $v0,1\n\tjr $ra\n");
		fileWriter.print("str_eq_ck2:\n\tbne $a1,$zero,str_eq_loop\n");
		fileWriter.print("str_eq_false:\n\tli $v0,0\n\tjr $ra\n");
		fileWriter.print("str_eq_loop:\n");
		fileWriter.print("\tlb $t0,0($a0)\n\tlb $t1,0($a1)\n");
		fileWriter.print("\tbne $t0,$t1,str_eq_false\n");
		fileWriter.print("\tbeq $t0,$zero,str_eq_true\n");
		fileWriter.print("\taddi $a0,$a0,1\n\taddi $a1,$a1,1\n\tj str_eq_loop\n");
		fileWriter.print("str_eq_true:\n\tli $v0,1\n\tjr $ra\n");
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
		/* Emit vtables in .data (after all code) so .word func_* gets full 32-bit address; in .text SPIM stores only offset */
		fileWriter.print(".data\n");
		emitVtables();
		fileWriter.print(".text\n");
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

	/** Print constant int (saves/restores $a0,$v0). Used when DEBUG_EVAL_ORDER: prints arg index 1,2,3... so you see eval order at runtime. */
	public void emitDebugPrintConstInt(int value) {
		fileWriter.print("\taddi $sp,$sp,-8\n\tsw $a0,0($sp)\n\tsw $v0,4($sp)\n");
		fileWriter.format("\tli $a0,%d\n\tli $v0,1\n\tsyscall\n", value);
		fileWriter.print("\tlw $a0,0($sp)\n\tlw $v0,4($sp)\n\taddi $sp,$sp,8\n");
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
		if (DEBUG_VTABLE && vtableLabel != null)
			System.err.println("[DEBUG_VTABLE] allocateClass numBytes=" + numBytes + " vtable=" + vtableLabel + " -> store at 0(" + reg(dst) + ")");
		fileWriter.format("\tli $a0,%d\n", numBytes);
		fileWriter.print("\tli $v0,9\n\tsyscall\n");
		fileWriter.format("\tmove %s,$v0\n", reg(dst));
		if (vtableLabel != null) {
			if (DEBUG_VTABLE) fileWriter.format("# DEBUG_VTABLE: store vtable %s at 0(%s)\n", vtableLabel, reg(dst));
			fileWriter.format("\tla $a0,%s\n", vtableLabel);
			fileWriter.format("\tsw $a0,0(%s)\n", reg(dst));
		}
	}

	/** Allocate 4+size*4 bytes, store length at 0, result in dst. Use $s0 (not in allocator pool). */
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
				String base = frameBase();
				if (DEBUG_TRACE_CALLS) fileWriter.format("# load %s <- %s (%s+%d)\n", reg(dst), varName, base, off);
				fileWriter.format("\tlw %s,%d(%s)\n", reg(dst), off, base);
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

	/** Emit .data section for a string literal (null-terminated). Skip if label already emitted (same literal in IR twice). */
	public void allocateString(String label, String value) {
		if (stringLabelsEmitted.contains(label))
			return;
		stringLabelsEmitted.add(label);
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
		// index==null: use $t9 for 0 and save/restore (base may be in $s1)
		String indexReg = index == null ? "$t9" : reg(index);
		if (index == null) fileWriter.print("\taddi $sp,$sp,-4\n\tsw $t9,0($sp)\n\tli $t9,0\n");
		String errLabel = DEBUG_LOAD_ARRAY_SITE ? String.format("ptr_error_loadArray_%d", siteId) : "ptr_error_loadArray";
		fileWriter.format("\tbeq %s,$zero,%s\n", reg(arrayBase), errLabel);
		fileWriter.format("\tlw $s0,0(%s)\n", reg(arrayBase));
		fileWriter.format("\tblt %s,$zero,bounds_error\n", indexReg);
		fileWriter.format("\tbge %s,$s0,bounds_error\n", indexReg);
		fileWriter.format("\tli $s0,4\n");
		fileWriter.format("\tmul $s0,$s0,%s\n", indexReg);
		fileWriter.format("\tadd $s0,$s0,%s\n", reg(arrayBase));
		fileWriter.format("\tlw %s,4($s0)\n", reg(dst));
		if (index == null) fileWriter.print("\tlw $t9,0($sp)\n\taddi $sp,$sp,4\n");
		if (DEBUG_LOAD_ARRAY_SITE) {
			String doneLabel = "loadArray_done_" + siteId;
			fileWriter.format("\tj %s\n", doneLabel);
			fileWriter.format("%s:\n\tj ptr_error_loadArray_common\n", errLabel);
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

	/** Store to array[index]; index may be null (constant 0). index==null: use $t9 and save/restore. */
	public void storeArray(Temp arrayBase, Temp index, Temp src) {
		String indexReg = index == null ? "$t9" : reg(index);
		if (index == null) fileWriter.print("\taddi $sp,$sp,-4\n\tsw $t9,0($sp)\n\tli $t9,0\n");
		fileWriter.format("\tbeq %s,$zero,ptr_error_storeArray\n", reg(arrayBase));
		fileWriter.format("\tlw $s0,0(%s)\n", reg(arrayBase));
		fileWriter.format("\tblt %s,$zero,bounds_error\n", indexReg);
		fileWriter.format("\tbge %s,$s0,bounds_error\n", indexReg);
		fileWriter.format("\tli $s0,4\n");
		fileWriter.format("\tmul $s0,$s0,%s\n", indexReg);
		fileWriter.format("\tadd $s0,$s0,%s\n", reg(arrayBase));
		fileWriter.format("\tsw %s,4($s0)\n", reg(src));
		if (index == null) fileWriter.print("\tlw $t9,0($sp)\n\taddi $sp,$sp,4\n");
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
				String base = frameBase();
				if (DEBUG_TRACE_CALLS) fileWriter.format("# store %s -> %s (%s+%d)\n", reg(src), varName, base, off);
				fileWriter.format("\tsw %s,%d(%s)\n", reg(src), off, base);
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
	/** Store incoming argument (from $a0-$a3 or from stack for param 5+) into param slot at function entry. */
	public void storeParam(int paramIndex, String varName) {
		int frameSize = currentFunction != null ? functionFrameSizes.getOrDefault(currentFunction, 0) : 0;
		// Param 5+ are on stack at 56+frameSize+4*(paramIndex-4) from current $sp (callee already did addi $sp,-frameSize)
		boolean fromStack = paramIndex >= 4;
		int stackArgOffset = fromStack ? (56 + frameSize + 4 * (paramIndex - 4)) : -1;

		if (currentFunction != null && localVarNames.contains(varName)) {
			Map<String, Integer> offsets = functionVarOffsets.get(currentFunction);
			if (offsets != null && offsets.containsKey(varName)) {
				int off = offsets.get(varName);
				String base = frameBase();
				if (fromStack) {
					fileWriter.format("\tlw $t0,%d(%s)\n", stackArgOffset, base);
					if (DEBUG_PARAMS) fileWriter.format("\t# DEBUG_PARAMS param %d (%s)=\n\tmove $a0,$t0\n\tli $v0,1\n\tsyscall\n\tli $a0,32\n\tli $v0,11\n\tsyscall\n", paramIndex, varName);
					if (DEBUG_TRACE_CALLS) fileWriter.format("# storeParam stack arg %d -> %s (%s+%d)\n", paramIndex, varName, base, off);
					fileWriter.format("\tsw $t0,%d(%s)\n", off, base);
				} else {
					String reg = "$a" + paramIndex;
					if (DEBUG_PARAMS) fileWriter.format("\t# DEBUG_PARAMS param %d (%s)=\n\tmove $t9,%s\n\tmove $a0,$t9\n\tli $v0,1\n\tsyscall\n\tli $a0,32\n\tli $v0,11\n\tsyscall\n", paramIndex, varName, reg);
					if (DEBUG_TRACE_CALLS) fileWriter.format("# storeParam $a%d -> %s (%s+%d)\n", paramIndex, varName, base, off);
					fileWriter.format("\tsw %s,%d(%s)\n", reg, off, base);
					if (DEBUG_TRACE_CALLS && paramIndex == 0) {
						fileWriter.format("# DEBUG storeParam: value stored in $sp+%d (param 0):\n", off);
						fileWriter.format("\tmove $t0,%s\n\tmove $a0,$t0\n\tli $v0,1\n\tsyscall\n\tli $a0,10\n\tli $v0,11\n\tsyscall\n", reg);
					}
				}
				return;
			}
		}
		String label = varLabel(varName);
		ensureDataEmitted(label);
		if (fromStack) {
			String base = frameBase();
			fileWriter.format("\tlw $t0,%d(%s)\n", stackArgOffset, base);
			if (DEBUG_PARAMS) fileWriter.format("\t# DEBUG_PARAMS param %d (%s)=\n\tmove $a0,$t0\n\tli $v0,1\n\tsyscall\n\tli $a0,32\n\tli $v0,11\n\tsyscall\n", paramIndex, varName);
			if (DEBUG_TRACE_CALLS) fileWriter.format("# storeParam stack arg %d -> %s (%s)\n", paramIndex, varName, label);
			fileWriter.format("\tsw $t0,%s\n", label);
		} else {
			String reg = "$a" + paramIndex;
			if (DEBUG_PARAMS) fileWriter.format("\t# DEBUG_PARAMS param %d (%s)=\n\tmove $t9,%s\n\tmove $a0,$t9\n\tli $v0,1\n\tsyscall\n\tli $a0,32\n\tli $v0,11\n\tsyscall\n", paramIndex, varName, reg);
			if (DEBUG_TRACE_CALLS) fileWriter.format("# storeParam $a%d -> %s (%s)\n", paramIndex, varName, label);
			fileWriter.format("\tsw %s,%s\n", reg, label);
			if (DEBUG_TRACE_CALLS && paramIndex == 0) {
				fileWriter.format("# DEBUG storeParam: value stored in %s (param 0):\n", label);
				fileWriter.format("\tmove $t0,%s\n\tmove $a0,$t0\n\tli $v0,1\n\tsyscall\n\tli $a0,10\n\tli $v0,11\n\tsyscall\n", reg);
			}
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
	/** Push $s0 onto stack so nested eval can overwrite $s0 (outer addPairs arg preserved). */
	public void pushS0() {
		fileWriter.format("\taddi $sp,$sp,-4\n");
		fileWriter.print("\tsw $s0,0($sp)\n");
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
	/** Clamp value in reg to L integer range [-32768, 32767]. Use $k0 so we never clobber $s0 (may be live). */
	private void clampToS16(String reg) {
		if (debugOverflow()) {
			// DEBUG_OVERFLOW: print value in reg before clamp (one int per line), preserve reg and $a0,$v0
			fileWriter.print("\t# DEBUG_OVERFLOW: value in reg before clamp\n");
			fileWriter.print("\taddi $sp,$sp,-12\n");
			fileWriter.format("\tsw %s,0($sp)\n\tsw $a0,4($sp)\n\tsw $v0,8($sp)\n", reg);
			fileWriter.format("\tlw $a0,0($sp)\n\tli $v0,1\n\tsyscall\n");
			fileWriter.print("\tli $a0,10\n\tli $v0,11\n\tsyscall\n");
			fileWriter.format("\tlw %s,0($sp)\n\tlw $a0,4($sp)\n\tlw $v0,8($sp)\n\taddi $sp,$sp,12\n", reg);
		}
		int id = internalLabelCounter++;
		String sh = "sat_high_" + id;
		String sl = "sat_low_" + id;
		String done = "sat_done_" + id;
		fileWriter.format("\tli $k0,32767\n");
		fileWriter.format("\tbgt %s,$k0,%s\n", reg, sh);
		fileWriter.format("\tli $k0,-32768\n");
		fileWriter.format("\tblt %s,$k0,%s\n", reg, sl);
		fileWriter.format("\tj %s\n", done);
		fileWriter.format("%s:\n\tli $k0,32767\n\tmove %s,$k0\n\tj %s\n", sh, reg, done);
		fileWriter.format("%s:\n\tli $k0,-32768\n\tmove %s,$k0\n", sl, reg);
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
		// Function-end labels (Label_N_end): do NOT emit .text here, and emit function-unique end label
		// so "j func_X_end" has exactly one definition (avoids wrong target with multiple .text segments).
		boolean isEndLabel = inlabel != null && inlabel.matches("Label_\\d+_end");
		if (!isEndLabel)
			ensureTextWithPreamble();
		if (isEndLabel && currentFunction != null)
			fileWriter.format("%s_end:\n", toMipsFuncLabel(currentFunction));
		String mipsLabel = toMipsFuncLabel(inlabel);
		fileWriter.format("%s:\n", mipsLabel);
		if (debugRecursionTrace() && functionVarOffsets.containsKey(inlabel) && !"main".equals(inlabel)) {
			emitTraceChar('R');
			emitDebugPrintA0A1();
		}
		if (DEBUG_MIPS_TRACE && inlabel != null && inlabel.contains("while_start"))
			emitTraceChar('L');
		if (DEBUG_MIPS_TRACE && inlabel != null && inlabel.contains("while_end"))
			emitTraceChar('W');
		// If this is a function entry, allocate stack frame for locals/params (after label so jal lands here)
		if (functionVarOffsets.containsKey(inlabel)) {
			currentFunction = inlabel;
			int frameSize = functionFrameSizes.getOrDefault(inlabel, 0);
			if (frameSize > 0) {
				fileWriter.format("\taddi $sp,$sp,-%d\n", frameSize);
				boolean usesFp = functionUsesFp(inlabel);
				if (usesFp) {
					// 0($sp)=saved $fp, 4($sp)=$ra; then $fp=$sp so params at 8($fp),12($fp),... (push/pop don't move $fp)
					fileWriter.print("\tsw $fp,0($sp)\n\tsw $ra,4($sp)\n\tmove $fp,$sp\n");
					if (DEBUG_MIPS_TRACE) emitTraceChar('S');
					int nParam = functionParamCount.getOrDefault(inlabel, 0);
					for (int i = 0; i < Math.min(4, nParam); i++)
						fileWriter.format("\tsw $a%d,%d($fp)\n", i, 8 + i * 4);
					if (debugFuncArgs(inlabel)) emitDebugPrintA0A1A2A3();
				} else if (functionSavesRa.contains(inlabel)) {
					fileWriter.print("\tsw $ra,0($sp)\n");
					if (DEBUG_MIPS_TRACE) emitTraceChar('S');
					int nParam = functionParamCount.getOrDefault(inlabel, 0);
					for (int i = 0; i < Math.min(4, nParam); i++)
						fileWriter.format("\tsw $a%d,%d($sp)\n", i, 4 + i * 4);
					if (debugFuncArgs(inlabel)) emitDebugPrintA0A1A2A3();
				}
			}
		}
	}
	/** True if this function's frame uses $fp (slots at 8+), so load/store must use $fp. */
	private boolean functionUsesFp(String funcName) {
		Map<String, Integer> offsets = functionVarOffsets.get(funcName);
		if (offsets == null || offsets.isEmpty()) return false;
		int min = offsets.values().stream().min(Integer::compare).orElse(0);
		return min >= 8;
	}
	/** Base register for current function's frame ($fp if usesFp else $sp). */
	private String frameBase() {
		return (currentFunction != null && functionUsesFp(currentFunction)) ? "$fp" : "$sp";
	}	
	public void jump(String inlabel)
	{
		// Use function-unique end label so "j X_end" has exactly one definition (avoids wrong target with multiple .text segments).
		String target = inlabel;
		if (inlabel != null && inlabel.matches("Label_\\d+_end") && currentFunction != null)
			target = toMipsFuncLabel(currentFunction) + "_end";
		if (DEBUG_MIPS_TRACE && target != null && target.contains("while_start"))
			emitTraceChar('B');  // loop-back jump
		fileWriter.format("\tj %s\n", target);
	}
	/** Emit jr $ra (return to caller). Required at function end. Deallocates frame if function has locals. */
	public void jrRa()
	{
		if (debugRecursion()) emitDebugPrintSp();  // only when debugging: print frame base before loading $ra
		if (currentFunction != null) {
			int frameSize = functionFrameSizes.getOrDefault(currentFunction, 0);
			if (frameSize > 0) {
				if (functionUsesFp(currentFunction)) {
					// $sp may be below $fp due to push; restore $sp from $fp then restore $ra,$fp
					fileWriter.format("\tmove $t0,$fp\n\taddi $sp,$t0,%d\n\tlw $ra,4($t0)\n\tlw $fp,0($t0)\n", frameSize);
					if (DEBUG_MIPS_TRACE) emitTraceChar('Y');
				} else if (functionSavesRa.contains(currentFunction)) {
					fileWriter.print("\tlw $ra,0($sp)\n");
					if (DEBUG_MIPS_TRACE) emitTraceChar('Y');
					fileWriter.format("\taddi $sp,$sp,%d\n", frameSize);
				} else {
					fileWriter.format("\taddi $sp,$sp,%d\n", frameSize);
				}
			}
		}
		emitDebugPrintV0();  // DEBUG_RECURSION: print return value before jr $ra
		if (debugRecursionTrace()) emitTraceChar('X');  // about to return
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
		emitTraceChar('C');
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
	/** String value equality: dst = 1 if left equals right else 0. */
	public void eqStrings(Temp dst, Temp left, Temp right) {
		ensureTextWithPreamble();
		fileWriter.print("\taddi $sp,$sp,-8\n");
		fileWriter.format("\tsw %s,0($sp)\n", reg(left));
		fileWriter.format("\tsw %s,4($sp)\n", reg(right));
		fileWriter.print("\tlw $a0,0($sp)\n");
		fileWriter.print("\tlw $a1,4($sp)\n");
		fileWriter.print("\taddi $sp,$sp,8\n");
		fileWriter.print("\tjal string_equals\n");
		fileWriter.format("\tmove %s,$v0\n", reg(dst));
	}
	/** Frame: 56 bytes = $t0..$t9 (0..36) + $ra (40) + $s0 (44) + $s1 (48) + $s2 (52). Saves $s0/$s1/$s2 so nested call arg survives. */
	public void callFunc(String funcName, java.util.List<Temp> args, Temp dst, String receiverVarName)
	{
		callFunc(funcName, args, dst, receiverVarName, false, false);
	}
	public void callFunc(String funcName, java.util.List<Temp> args, Temp dst, String receiverVarName, boolean firstArgFromS2)
	{
		callFunc(funcName, args, dst, receiverVarName, firstArgFromS2, false, false);
	}
	public void callFunc(String funcName, java.util.List<Temp> args, Temp dst, String receiverVarName, boolean firstArgFromS2, boolean arg0InS0)
	{
		callFunc(funcName, args, dst, receiverVarName, firstArgFromS2, arg0InS0, false);
	}
	public void callFunc(String funcName, java.util.List<Temp> args, Temp dst, String receiverVarName, boolean firstArgFromS2, boolean arg0InS0, boolean arg0OnStack) {
		callFunc(funcName, args, dst, receiverVarName, firstArgFromS2, arg0InS0, arg0OnStack, false, false);
	}
	public void callFunc(String funcName, java.util.List<Temp> args, Temp dst, String receiverVarName, boolean firstArgFromS2, boolean arg0InS0, boolean arg0OnStack, boolean arg1OnStack, boolean argsPushedLTR3)
	{
		final int saveAreaBytes = 56;
		final int stackStartIndex = firstArgFromS2 ? 3 : 4;
		final int extraArgsCount = (args == null) ? 0 : Math.max(0, args.size() - stackStartIndex);
		final int frameBytes = saveAreaBytes + 4 * extraArgsCount;
		int n = args != null ? args.size() : 0;
		// n>=4 and arg0OnStack: for n==4 only, load 9th/10th (actually 1st/3rd) from stack before frame; for n>=5 do NOT load before frame (would clobber 7th/8th). Load 9th/10th from above frame after storing 5th-8th; pop 2 words after call returns.
		boolean n4EarlySetup = arg0OnStack && n >= 4 && !firstArgFromS2;
		if (n4EarlySetup && n == 4) {
			fileWriter.print("\tlw $t8,0($sp)\n\tlw $t9,4($sp)\n\taddi $sp,$sp,8\n");
		}
		// For n==2 or n==3, pop into $a1 or $a2 before frame (no early setup needed)
		if (arg0OnStack && args != null && n >= 2 && n < 4) {
			if (n == 2) fileWriter.print("\tlw $a1,0($sp)\n\taddi $sp,$sp,4\n");
			else fileWriter.print("\tlw $a2,0($sp)\n\taddi $sp,$sp,4\n");
		}
		fileWriter.format("\taddi $sp,$sp,-%d\n", frameBytes);
		fileWriter.format("\tsw $t0,0($sp)\n\tsw $t1,4($sp)\n\tsw $t2,8($sp)\n\tsw $t3,12($sp)\n\tsw $t4,16($sp)\n\tsw $t5,20($sp)\n\tsw $t6,24($sp)\n\tsw $t7,28($sp)\n\tsw $t8,32($sp)\n\tsw $t9,36($sp)\n\tsw $ra,40($sp)\n\tsw $s0,44($sp)\n\tsw $s1,48($sp)\n\tsw $s2,52($sp)\n");
		// 2- or 3-arg LTR: first args were pushed. push is addi -4 then sw. n==2: one push -> 1st at frameBytes($sp). n==3: two pushes -> 2nd at frameBytes, 1st at frameBytes+4.
		// Load from above frame only; pop the pushed words after the call returns (below)
		if (argsPushedLTR3 && (n == 2 || n == 3) && !firstArgFromS2) {
			if (n == 2) {
				fileWriter.format("\tlw $a0,%d($sp)\n", frameBytes);
				fileWriter.format("\tmove $a1,%s\n", reg(args.get(1)));
			} else {
				fileWriter.format("\tlw $a0,%d($sp)\n\tlw $a1,%d($sp)\n", frameBytes + 4, frameBytes);
				fileWriter.format("\tmove $a2,%s\n", reg(args.get(2)));
			}
		}
		if (!n4EarlySetup && !argsPushedLTR3) {
			if (firstArgFromS2)
				fileWriter.print("\tmove $a0,$s2\n");
			else if (receiverVarName != null)
				loadVarToA0(receiverVarName);
			else if (arg0InS0)
				fileWriter.print("\tmove $a0,$s0\n");
			else if (args != null && args.size() > 0 && args.get(args.size() - 1) != null)
				fileWriter.format("\tmove $a0,%s\n", reg(args.get(args.size() - 1)));
			if (firstArgFromS2) {
				if (args != null && args.size() > 0 && !arg0OnStack) {
					if (arg0InS0) fileWriter.print("\tmove $a1,$s0\n");
					else if (args.get(0) != null) fileWriter.format("\tmove $a1,%s\n", reg(args.get(0)));
				}
				if (args != null && args.size() > 1 && args.get(1) != null) fileWriter.format("\tmove $a2,%s\n", reg(args.get(1)));
				if (args != null && args.size() > 2 && args.get(2) != null) fileWriter.format("\tmove $a3,%s\n", reg(args.get(2)));
			} else {
				if (n > 1 && !(arg0OnStack && n == 2) && args.get(n - 2) != null) fileWriter.format("\tmove $a1,%s\n", reg(args.get(n - 2)));
				if (n > 2 && !(arg0OnStack && n == 3)) {
					if (arg1OnStack)
						fileWriter.format("\tlw $a2,%d($sp)\n", frameBytes);
					else if (args.get(n - 3) != null) fileWriter.format("\tmove $a2,%s\n", reg(args.get(n - 3)));
				}
				if (n > 3 && !(arg0OnStack && n == 4) && args.get(n - 4) != null) fileWriter.format("\tmove $a3,%s\n", reg(args.get(n - 4)));
			}
		}
		// Extra args: store 5th..8th at saveAreaBytes, saveAreaBytes+4, ... paramIndex = n-1-i so args.get(paramIndex) is the (i+1)th param; stack slot (i-stackStartIndex) holds param i (5th param = index 4).
		if (args != null && extraArgsCount > 0) {
			for (int i = stackStartIndex; i < n; i++) {
				int paramIndex = n - 1 - i;
				int off = saveAreaBytes + 4 * (i - stackStartIndex);
				if (paramIndex >= 0 && args.get(paramIndex) != null)
					fileWriter.format("\tsw %s,%d($sp)\n", reg(args.get(paramIndex)), off);
			}
		}
		// n4EarlySetup: set $a0-$a3 after storing stack args (so we don't overwrite regs that held 5th-8th).
		if (n4EarlySetup && args != null && n >= 4) {
			if (n >= 5) {
				fileWriter.format("\tmove $a0,%s\n", reg(args.get(n - 1)));
				fileWriter.format("\tmove $a1,%s\n", reg(args.get(n - 2)));
				fileWriter.format("\tmove $a2,%s\n", reg(args.get(n - 3)));
				fileWriter.format("\tmove $a3,%s\n", reg(args.get(n - 4)));
			} else if (n == 4) {
				// We pushed to (when i=2) then n (PushS0 when i=0); so 0($sp)=n, 4($sp)=to. We loaded $t8=n, $t9=to.
				fileWriter.print("\tmove $a0,$t8\n");
				fileWriter.format("\tmove $a1,%s\n", reg(args.get(2)));
				fileWriter.print("\tmove $a2,$t9\n");
				fileWriter.format("\tmove $a3,%s\n", reg(args.get(0)));
			}
		}
		if (DEBUG_TRACE_CALLS) {
			fileWriter.format("# call %s: $a0 (first arg) = %s\n", funcName, firstArgFromS2 ? "$s2" : (args != null && args.size() > 0 ? reg(args.get(0)) : "?"));
			fileWriter.print("\tmove $t0,$a0\n");
			fileWriter.print("\tli $v0,1\n\tsyscall\n");
			fileWriter.print("\tli $a0,10\n\tli $v0,11\n\tsyscall\n");
			fileWriter.print("\tmove $a0,$t0\n");
		}
		if (DEBUG_ARGS) {
			System.err.println("[DEBUG_ARGS] callFunc " + funcName + " arg0InS0=" + arg0InS0 + " nargs=" + n);
			if (n >= 2 && !firstArgFromS2)
				System.err.println("[DEBUG_ARGS]   $a0=1st param <- args.get(" + (n-1) + ") " + (args.get(n-1) != null ? reg(args.get(n-1)) : "null") + ", $a1=2nd param <- " + (arg0InS0 ? "$s0 (was args.get(0))" : "args.get(" + (n-2) + ") " + reg(args.get(n-2))));
			// Print $a0 and $a1 before jal (as integers; for pointers you see addresses)
			fileWriter.print("\t# DEBUG_ARGS: print $a0 $a1 before jal\n");
			fileWriter.print("\taddi $sp,$sp,-8\n\tsw $a0,0($sp)\n\tsw $a1,4($sp)\n");
			fileWriter.print("\tli $v0,1\n\tlw $a0,0($sp)\n\tsyscall\n");
			fileWriter.print("\tli $v0,11\n\tli $a0,32\n\tsyscall\n");
			fileWriter.print("\tli $v0,1\n\tlw $a0,4($sp)\n\tsyscall\n");
			fileWriter.print("\tli $v0,11\n\tli $a0,10\n\tsyscall\n");
			fileWriter.print("\tlw $a0,0($sp)\n\tlw $a1,4($sp)\n\taddi $sp,$sp,8\n");
		}
		// DEBUG_CALL_ARGS: print all n args before jal (param 1..n as integers, space-sep, then newline)
		if (debugCallArgs() && n >= 2 && !firstArgFromS2 && args != null) {
			fileWriter.print("\t# DEBUG_CALL_ARGS: print args 1.." + n + " before jal " + funcName + "\n");
			fileWriter.print("\taddi $sp,$sp,-16\n\tsw $a0,0($sp)\n\tsw $a1,4($sp)\n\tsw $a2,8($sp)\n\tsw $a3,12($sp)\n");
			for (int k = 0; k < 4 && k < n; k++) {
				fileWriter.print("\tli $v0,1\n\tlw $a0," + (k * 4) + "($sp)\n\tsyscall\n\tli $v0,11\n\tli $a0,32\n\tsyscall\n");
			}
			int stackBase = 56 + 16; // 5th param at 56($sp) with our 16-byte save above it
			for (int k = 4; k < n; k++) {
				int off = stackBase + 4 * (k - 4);
				fileWriter.print("\tli $v0,1\n\tlw $a0," + off + "($sp)\n\tsyscall\n\tli $v0,11\n\tli $a0,32\n\tsyscall\n");
			}
			fileWriter.print("\tli $v0,11\n\tli $a0,10\n\tsyscall\n");
			fileWriter.print("\tlw $a0,0($sp)\n\tlw $a1,4($sp)\n\tlw $a2,8($sp)\n\tlw $a3,12($sp)\n\taddi $sp,$sp,16\n");
		}
		if (DEBUG_MIPS_TRACE) emitTraceChar('J');
		fileWriter.format("\tjal %s\n", toMipsFuncLabel(funcName));
		fileWriter.format("\tlw $ra,40($sp)\n\tlw $t0,0($sp)\n\tlw $t1,4($sp)\n\tlw $t2,8($sp)\n\tlw $t3,12($sp)\n\tlw $t4,16($sp)\n\tlw $t5,20($sp)\n\tlw $t6,24($sp)\n\tlw $t7,28($sp)\n\tlw $t8,32($sp)\n\tlw $t9,36($sp)\n\tlw $s0,44($sp)\n\tlw $s1,48($sp)\n\tlw $s2,52($sp)\n");
		fileWriter.format("\taddi $sp,$sp,%d\n", frameBytes);
		// LTR: we pushed args before the frame; pop them after restoring the frame
		if (argsPushedLTR3 && n == 2)
			fileWriter.print("\taddi $sp,$sp,4\n");
		else if (argsPushedLTR3 && n == 3)
			fileWriter.print("\taddi $sp,$sp,8\n");
		// 4 args with arg0OnStack and arg1OnStack: we pushed n and to (2 words); pop them (unless n4EarlySetup already popped at start)
		if (arg0OnStack && arg1OnStack && n == 4 && !n4EarlySetup)
			fileWriter.print("\taddi $sp,$sp,8\n");
		// n4EarlySetup with 5+ args: we pushed 9th/10th before frame; pop them after restoring the frame
		if (n4EarlySetup && n >= 5)
			fileWriter.print("\taddi $sp,$sp,8\n");
		if (dst != null) fileWriter.format("\tmove %s,$v0\n", reg(dst));
	}
	/** Virtual method call: load method from object's vtable at slot, jalr. Receiver in $a0. */
	public void callFuncVirtual(int methodSlot, java.util.List<Temp> args, Temp dst, boolean firstArgFromS2)
	{
		final int saveAreaBytes = 56;
		final int stackStartIndex = firstArgFromS2 ? 3 : 4;
		final int extraArgsCount = (args == null) ? 0 : Math.max(0, args.size() - stackStartIndex);
		final int frameBytes = saveAreaBytes + 4 * extraArgsCount;
		fileWriter.format("\taddi $sp,$sp,-%d\n", frameBytes);
		fileWriter.format("\tsw $t0,0($sp)\n\tsw $t1,4($sp)\n\tsw $t2,8($sp)\n\tsw $t3,12($sp)\n\tsw $t4,16($sp)\n\tsw $t5,20($sp)\n\tsw $t6,24($sp)\n\tsw $t7,28($sp)\n\tsw $t8,32($sp)\n\tsw $t9,36($sp)\n\tsw $ra,40($sp)\n\tsw $s0,44($sp)\n\tsw $s1,48($sp)\n\tsw $s2,52($sp)\n");
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
		// Extra args on stack above save area
		if (args != null && extraArgsCount > 0) {
			for (int i = stackStartIndex; i < args.size(); i++) {
				if (args.get(i) != null) {
					int off = saveAreaBytes + 4 * (i - stackStartIndex);
					fileWriter.format("\tsw %s,%d($sp)\n", reg(args.get(i)), off);
				}
			}
		}
		fileWriter.format("\tbeq $a0,$zero,ptr_error_loadField\n");
		if (DEBUG_VTABLE) {
			fileWriter.format("# virtual call: slot=%d vtable+%d -> $t0 then jalr $t0\n", methodSlot, methodSlot * 4);
			System.err.println("[DEBUG_VTABLE] virtual call slot=" + methodSlot + " offset=" + (methodSlot * 4) + " firstArgFromS2=" + firstArgFromS2 + " argsSize=" + (args != null ? args.size() : 0));
			if (DEBUG_VTABLE_PRINT_A0) {
				fileWriter.print("# DEBUG: print $a0 (receiver) before lw vtable\n");
				fileWriter.print("\taddi $sp,$sp,-4\n\tsw $a0,0($sp)\n");
				fileWriter.print("\tli $v0,1\n\tsyscall\n");
				fileWriter.print("\tli $a0,32\n\tli $v0,11\n\tsyscall\n");
				fileWriter.print("\tlw $a0,0($sp)\n\taddi $sp,$sp,4\n");
			}
		}
		fileWriter.format("\tlw $t0,0($a0)\n");
		fileWriter.format("\tlw $t0,%d($t0)\n", methodSlot * 4);
		if (DEBUG_MIPS_TRACE) emitTraceChar('V');
		fileWriter.print("\tjalr $t0\n");
		fileWriter.format("\tlw $ra,40($sp)\n\tlw $t0,0($sp)\n\tlw $t1,4($sp)\n\tlw $t2,8($sp)\n\tlw $t3,12($sp)\n\tlw $t4,16($sp)\n\tlw $t5,20($sp)\n\tlw $t6,24($sp)\n\tlw $t7,28($sp)\n\tlw $t8,32($sp)\n\tlw $t9,36($sp)\n\tlw $s0,44($sp)\n\tlw $s1,48($sp)\n\tlw $s2,52($sp)\n");
		fileWriter.format("\taddi $sp,$sp,%d\n", frameBytes);
		if (dst != null) fileWriter.format("\tmove %s,$v0\n", reg(dst));
	}
	/** Copy base temp into $s2 so base survives arg evaluation and call; base must be in reg before this. */
	public void moveBaseToS2(Temp base) {
		if (DEBUG_TRACE_CALLS) fileWriter.format("# moveBaseToS2: $s2 <- %s (first arg)\n", reg(base));
		if (DEBUG_VTABLE) fileWriter.format("# DEBUG_VTABLE: moveBaseToS2 $s2 <- %s (receiver)\n", reg(base));
		fileWriter.format("\tmove $s2,%s\n", reg(base));
	}
	/** Copy temp into $s0 so it survives following arg evaluation (nested calls). */
	public void moveTempToS0(Temp t) {
		if (t != null) {
			if (DEBUG_ARGS) System.err.println("[DEBUG_ARGS] moveTempToS0: $s0 <- " + reg(t) + " (temp " + (t != null ? t.toString() : "?") + ")");
			fileWriter.format("\tmove $s0,%s\n", reg(t));
		}
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
		fileWriter.format("\tjal %s\n", toMipsFuncLabel(funcName));
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
		if (DEBUG_VTABLE) {
			fileWriter.format("# virtual call (store field): slot=%d vtable+%d -> jalr $t0 (receiver from $s2)\n", methodSlot, methodSlot * 4);
			System.err.println("[DEBUG_VTABLE] virtual call (store) slot=" + methodSlot + " offset=" + (methodSlot * 4) + " argsSize=" + (args != null ? args.size() : 0));
			if (DEBUG_VTABLE_PRINT_A0) {
				fileWriter.print("# DEBUG: print $a0 (receiver from $s2) before lw vtable\n");
				fileWriter.print("\taddi $sp,$sp,-4\n\tsw $a0,0($sp)\n");
				fileWriter.print("\tli $v0,1\n\tsyscall\n");
				fileWriter.print("\tli $a0,32\n\tli $v0,11\n\tsyscall\n");
				fileWriter.print("\tlw $a0,0($sp)\n\taddi $sp,$sp,4\n");
			}
		}
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
