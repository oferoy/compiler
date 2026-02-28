import java.io.*;
import java_cup.runtime.Symbol;
import ast.*;
import ir.*;
import cfg.*;
import mips.*;
import java.util.*;

public class Main
{
	/** Build per-function local/param layout so MIPS can use stack for locals (fixes recursion). */
	private static void buildFunctionLayouts(List<IrCommand> commands,
			Map<String, Map<String, Integer>> functionVarOffsets,
			Map<String, Integer> functionFrameSizes,
			Set<String> allLocalVarNames) {
		// Find function entry indices
		List<Integer> entryIndices = new ArrayList<>();
		for (int i = 0; i < commands.size(); i++) {
			String ln = commands.get(i).getLabelName();
			if (ln != null && !ln.startsWith("Label_"))
				entryIndices.add(i);
		}
		// Build call graph and find recursive functions
		Map<String, Set<String>> callGraph = new HashMap<>();
		Set<String> allFuncs = new HashSet<>();
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
				if (commands.get(i) instanceof IrCommandCall) {
					String callee = ((IrCommandCall) commands.get(i)).getFuncName();
					if (callee != null && allFuncs.contains(callee))
						callGraph.get(caller).add(callee);
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
			Map<String, Integer> offsets = new HashMap<>();
			for (int i = 0; i < allVars.size(); i++)
				offsets.put(allVars.get(i), i * 4);
			int frameSize = allVars.size() * 4;
			// Use stack only for recursive functions; others use .data
			if (recursive.contains(funcName)) {
				functionVarOffsets.put(funcName, offsets);
				functionFrameSizes.put(funcName, frameSize);
				allLocalVarNames.addAll(allVars);
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
			java.util.Set<String> allLocalVarNames = new java.util.HashSet<>();
			buildFunctionLayouts(commands, functionVarOffsets, functionFrameSizes, allLocalVarNames);

			MipsGenerator.getInstance().setOutput(fileWriter);
			MipsGenerator.getInstance().setRegisterAllocation(allocation);
			MipsGenerator.getInstance().setFunctionLayouts(allLocalVarNames, functionVarOffsets, functionFrameSizes);
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
		catch (Exception e)
		{
			String msg = e.getMessage();
			if (msg != null && msg.startsWith("ERROR("))
			{
				try
				{
					PrintWriter errorWriter = new PrintWriter(outputFileName);
					errorWriter.print(msg);
					errorWriter.close();
				}
				catch (Exception writeEx)
				{
					writeEx.printStackTrace();
				}
			}
			else if (msg != null && msg.equals("ERROR"))
			{
				try
				{
					PrintWriter errorWriter = new PrintWriter(outputFileName);
					errorWriter.print("ERROR");
					errorWriter.close();
				}
				catch (Exception writeEx)
				{
					writeEx.printStackTrace();
				}
			}
			else
			{
				e.printStackTrace();
			}
		}
	}
}
