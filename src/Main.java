import java.io.*;
import java.io.PrintWriter;
import java_cup.runtime.Symbol;
import ast.*;
import ir.*;
import mips.*;

public class Main
{
	static public void main(String argv[])
	{
		Lexer l;
		Parser p;
		Symbol s;
		AstDecList ast;
		FileReader fileReader;
		PrintWriter fileWriter;
		String inputFileName = argv[0];
		String outputFileName = argv[1];

		try
		{
			/********************************/
			/* [1] Initialize a file reader */
			/********************************/
			fileReader = new FileReader(inputFileName);

			/********************************/
			/* [2] Initialize a file writer */
			/********************************/
			fileWriter = new PrintWriter(outputFileName);

			/******************************/
			/* [3] Initialize a new lexer */
			/******************************/
			l = new Lexer(fileReader);

			/*******************************/
			/* [4] Initialize a new parser */
			/*******************************/
			p = new Parser(l);

			/***********************************/
			/* [5] 3 ... 2 ... 1 ... Parse !!! */
			/***********************************/
			ast = (AstDecList) p.parse().value;

			/*************************/
			/* [6] Print the AST ... */
			/*************************/
			ast.printMe();

			/**************************/
			/* [7] Semant the AST ... */
			/**************************/
			ast.semantMe();

			/**********************/
			/* [8] Ir the AST ... */
			/**********************/
			ast.irMe();

			/***********************/
			/* [9] MIPS the Ir ... */
			/***********************/
			Ir.getInstance().mipsMe();

			/**************************************/
			/* [10] Finalize AST GRAPHIZ DOT file */
			/**************************************/
			AstGraphviz.getInstance().finalizeFile();

			/***************************/
			/* [11] Finalize MIPS file */
			/***************************/
			MipsGenerator.getInstance().finalizeFile();

			/**************************/
			/* [12] Close output file */
			/**************************/
			fileWriter.close();
		}

		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}