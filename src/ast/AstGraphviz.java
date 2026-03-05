package ast;

import java.io.PrintWriter;
import java.io.File;
import java.net.URL;

public class AstGraphviz
{
	/***********************/
	/* The file writer ... */
	/***********************/
	private PrintWriter fileWriter;
	
	/**************************************/
	/* USUAL SINGLETON IMPLEMENTATION ... */
	/**************************************/
	private static AstGraphviz instance = null;

	/*****************************/
	/* PREVENT INSTANTIATION ... */
	/*****************************/
	private AstGraphviz() {}

	/******************************/
	/* GET SINGLETON INSTANCE ... */
	/******************************/
	public static AstGraphviz getInstance()
	{
		if (instance == null)
		{
			instance = new AstGraphviz();
			
			/****************************/
			/* Initialize a file writer */
			/****************************/
			try
			{
				// String dirname="./output/";
				// File dir = new File(dirname);
				// String filename="AST_IN_GRAPHVIZ_DOT_FORMAT.txt";
				// if (!dir.exists()) {
				// 	dir.mkdirs();
				// }
				// instance.fileWriter = new PrintWriter(dirname+filename);

				// מצא את התיקייה של ANALYZER
				File jarDir = getJarDirectory();

				// צור output/ שם
				File outputDir = new File(jarDir, "output");
				outputDir.mkdirs();

				// כתוב את הקובץ
				File outputFile = new File(outputDir, "AST_IN_GRAPHVIZ_DOT_FORMAT.txt");
				instance.fileWriter = new PrintWriter(outputFile);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			/******************************************************/
			/* Print Directed Graph header in Graphviz dot format */
			/******************************************************/
			instance.fileWriter.print("digraph\n");
			instance.fileWriter.print("{\n");
			instance.fileWriter.print("graph [ordering = \"out\"]\n");
		}
		return instance;
	}

	/**
	 * Get the directory where the JAR file or class files are located.
	 */
	private static File getJarDirectory()
	{
		try
		{
			// Get the location of AstGraphviz class
			URL url = AstGraphviz.class.getProtectionDomain().getCodeSource().getLocation();
			File file = new File(url.toURI());
			
			// If it's a JAR file, get its parent directory
			// If it's a directory (classes), go up to find the root
			if (file.isFile())
			{
				// It's a JAR file - return its parent directory
				return file.getParentFile();
			}
			else
			{
				// It's a directory (bin/ast/) - go up to root (ex4/)
				// Assuming structure: ex4/bin/ast/AstGraphviz.class
				File current = file;
				// Go up from bin/ast/ to bin/
				if (current.getName().equals("ast"))
				{
					current = current.getParentFile();
				}
				// Go up from bin/ to ex4/
				if (current.getName().equals("bin"))
				{
					current = current.getParentFile();
				}
				return current;
			}
		}
		catch (Exception e)
		{
			// Fallback to current directory
			return new File(".");
		}
	}

	/***********************************/
	/* Log node in graphviz dot format */
	/***********************************/
	public void logNode(int nodeSerialNumber,String nodeName)
	{
		fileWriter.format(
			"v%d [label = \"%s\"];\n",
			nodeSerialNumber,
			nodeName);
	}

	/***********************************/
	/* Log edge in graphviz dot format */
	/***********************************/
	public void logEdge(
		int fatherNodeSerialNumber,
		int sonNodeSerialNumber)
	{
		fileWriter.format(
			"v%d -> v%d;\n",
			fatherNodeSerialNumber,
			sonNodeSerialNumber);
	}
	
	/******************************/
	/* Finalize graphviz dot file */
	/******************************/
	public void finalizeFile()
	{
		fileWriter.print("}\n");
		fileWriter.close();
	}
}
