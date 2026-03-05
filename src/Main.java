import java.io.*;
import java_cup.runtime.Symbol;
import ast.*;
import ir.*;
import cfg.*;
import mips.*;
import java.util.*;

public class Main
{
	/** Build per-function local/param layout so MIPS can use stack for locals (fixes recursion).
	 *  Functions that make calls get $ra saved in frame so jr $ra returns to caller. */
	private static void buildFunctionLayouts(List<IrCommand> commands,
			Map<String, Map<String, Integer>> functionVarOffsets,
			Map<String, Integer> functionFrameSizes,
			Map<String, Integer> functionParamCount,
			Set<String> allLocalVarNames,
			Set<String> functionSavesRa) {
		// Find function entry indices
		List<Integer> entryIndices = new ArrayList<>();
		for (int i = 0; i < commands.size(); i++) {
			String ln = commands.get(i).getLabelName();
			if (ln != null && !ln.startsWith("Label_"))
				entryIndices.add(i);
		}
		// Build call graph and find recursive functions; also find functions that make any call
		Map<String, Set<String>> callGraph = new HashMap<>();
		Set<String> allFuncs = new HashSet<>();
		Set<String> makesCall = new HashSet<>();
		for (int k = 0; k < entryIndices.size(); k++) {
			String fn = commands.get(entryIndices.get(k)).getLabelName();
			allFuncs.add(fn);
			callGraph.put(fn, new HashSet<>());
		}
		for (int k = 0; k < entryIndices.size(); k++) {
			int start = entryIndices.get(k);
			int end = (k + 1 < entryIndices.size()) ? entryIndices.get(k + 1) : commands.size();
			String caller = commands.get(start).getLabelName();
			for (int i = start; i < end; i++) {
				IrCommand c = commands.get(i);
				if (c instanceof IrCommandCall) {
					makesCall.add(caller);
					String callee = ((IrCommandCall) c).getFuncName();
					if (callee != null && allFuncs.contains(callee))
						callGraph.get(caller).add(callee);
				} else if (c instanceof IrCommandConcatStrings || c instanceof IrCommandCallAndStoreFieldWithBaseInS2
						|| c instanceof IrCommandEqStrings) {
					// These emit jal; caller must save $ra
					makesCall.add(caller);
				}
			}
		}
		Set<String> recursive = new HashSet<>();
		for (String f : allFuncs) {
			Set<String> visited = new HashSet<>();
			if (reaches(callGraph, f, f, visited))
				recursive.add(f);
		}
		for (int k = 0; k < entryIndices.size(); k++) {
			int start = entryIndices.get(k);
			int end = (k + 1 < entryIndices.size()) ? entryIndices.get(k + 1) : commands.size();
			String funcName = commands.get(start).getLabelName();
			java.util.List<Object[]> paramList = new ArrayList<>();
			for (int i = start; i < end; i++) {
				IrCommand c = commands.get(i);
				if (c instanceof IrCommandStoreParam) {
					IrCommandStoreParam sp = (IrCommandStoreParam) c;
					paramList.add(new Object[]{ sp.getVarName(), sp.getParamIndex() });
				}
			}
			paramList.sort((a, b) -> Integer.compare((Integer)a[1], (Integer)b[1]));
			List<String> paramNames = new ArrayList<>();
			for (Object[] p : paramList)
				paramNames.add((String) p[0]);
			Set<String> paramSet = new HashSet<>(paramNames);
			List<String> localNames = new ArrayList<>();
			for (int i = start; i < end; i++) {
				IrCommand c = commands.get(i);
				if (c instanceof IrCommandAllocate) {
					String v = ((IrCommandAllocate) c).getVarName();
					if (!paramSet.contains(v) && !localNames.contains(v))
						localNames.add(v);
				}
			}
			List<String> allVars = new ArrayList<>(paramNames);
			for (String v : localNames) if (!allVars.contains(v)) allVars.add(v);
			// If function makes a call, reserve word at 0($sp) for $ra; vars at 4,8,... Recursive with 2+ params: save $fp, raSlot=8.
			boolean savesRa = makesCall.contains(funcName);
			boolean useFp = recursive.contains(funcName) && allVars.size() > 1;
			int raSlot = useFp ? 8 : (savesRa ? 4 : 0);
			Map<String, Integer> offsets = new HashMap<>();
			for (int i = 0; i < allVars.size(); i++)
				offsets.put(allVars.get(i), raSlot + i * 4);
			int frameSize = raSlot + allVars.size() * 4;
			if (recursive.contains(funcName)) {
				functionVarOffsets.put(funcName, offsets);
				functionFrameSizes.put(funcName, frameSize);
				functionParamCount.put(funcName, paramNames.size());
				allLocalVarNames.addAll(allVars);
				if (savesRa) functionSavesRa.add(funcName);
			} else if (savesRa) {
				functionVarOffsets.put(funcName, new HashMap<>());
				functionFrameSizes.put(funcName, 4);
				functionSavesRa.add(funcName);
			} else {
				functionVarOffsets.put(funcName, new HashMap<>());
				functionFrameSizes.put(funcName, 0);
			}
		}
	}

	private static boolean reaches(Map<String, Set<String>> graph, String start, String target, Set<String> visited) {
		for (String callee : graph.getOrDefault(start, Collections.emptySet())) {
			if (callee.equals(target)) return true;
			if (!visited.contains(callee)) {
				visited.add(callee);
				if (reaches(graph, callee, target, visited)) return true;
				visited.remove(callee);
			}
		}
		return false;
	}

	static public void main(String argv[])
	{
		Lexer l;
		Parser p;
		Symbol s;
		AstProgram ast;
		FileReader fileReader;
		PrintWriter fileWriter;
		String inputFileName = argv[0];
		String outputFileName = argv[1];

		try
		{
			fileReader = new FileReader(inputFileName);
			fileWriter = new PrintWriter(outputFileName);

			l = new Lexer(fileReader);
			p = new Parser(l);

			ast = (AstProgram) p.parse().value;

			// ast.printMe();  // uncomment for AST debug dump
			ast.semantMe();

			ast.irMe();

			List<IrCommand> commands = Ir.getInstance().getCommandList();
			Map<Integer, String> allocation = RegisterAllocator.allocate(commands);

			if (allocation == null)
			{
				fileWriter.print("Register Allocation Failed");
				fileWriter.close();
				return;
			}

			// Pre-pass: compute per-function local variable layout for stack allocation (fixes recursion)
			java.util.Map<String, java.util.Map<String, Integer>> functionVarOffsets = new java.util.HashMap<>();
			java.util.Map<String, Integer> functionFrameSizes = new java.util.HashMap<>();
			java.util.Map<String, Integer> functionParamCount = new java.util.HashMap<>();
			java.util.Set<String> allLocalVarNames = new java.util.HashSet<>();
			java.util.Set<String> functionSavesRa = new java.util.HashSet<>();
			buildFunctionLayouts(commands, functionVarOffsets, functionFrameSizes, functionParamCount, allLocalVarNames, functionSavesRa);

			MipsGenerator.getInstance().setOutput(fileWriter);
			MipsGenerator.getInstance().setRegisterAllocation(allocation);
			MipsGenerator.getInstance().setFunctionLayouts(allLocalVarNames, functionVarOffsets, functionFrameSizes, functionParamCount, functionSavesRa);
			// Global init is only the IR before IrCommandGlobalInitEnd (global var allocate/store).
			int globalInitEndIdx = 0;
			for (int i = 0; i < commands.size(); i++) {
				if (commands.get(i) instanceof IrCommandGlobalInitEnd) {
					globalInitEndIdx = i;
					break;
				}
			}
			MipsGenerator.getInstance().emitProgramEntry();
			for (int i = 0; i < globalInitEndIdx; i++)
				commands.get(i).mipsMe();
			MipsGenerator.getInstance().emitPreambleAndHandlers();
			for (int i = globalInitEndIdx + 1; i < commands.size(); i++)
				commands.get(i).mipsMe();
			MipsGenerator.getInstance().finalizeFile();

			AstGraphviz.getInstance().finalizeFile();
		}
		catch (Throwable e)
		{
			String msg = e.getMessage();
			String out = null;
			if (msg != null && msg.startsWith("ERROR("))
				out = msg + "\n";
			else if (msg != null && msg.equals("ERROR"))
				out = "ERROR\n";
			else if (e instanceof Error)
				out = "ERROR\n";  // Lexer zzScanError throws Error (e.g. "could not match input")
			if (out != null)
			{
				System.err.println("[DEBUG_ERROR_SOURCE] exception=" + e.getClass().getName() + " message=" + msg);
				e.printStackTrace(System.err);
				System.out.print(out);
				System.out.flush();
				if (outputFileName != null)
				{
					try
					{
						PrintWriter errorWriter = new PrintWriter(outputFileName);
						// Write without trailing newline so output file matches expected (e.g. "ERROR" or "ERROR(3)")
						errorWriter.print(out.trim());
						errorWriter.close();
					}
					catch (Exception writeEx)
					{
						writeEx.printStackTrace();
					}
				}
				System.exit(1);
			}
			else
			{
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}
