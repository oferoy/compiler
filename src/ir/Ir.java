/***********/
/* PACKAGE */
/***********/
package ir;

/*******************/
/* GENERAL IMPORTS */
/*******************/
import java.util.ArrayList;
import java.util.List;

/*******************/
/* PROJECT IMPORTS */
/*******************/

public class Ir
{
	private IrCommand head=null;
	private IrCommandList tail=null;
	/** Used by AstStmtReturn to jump to function end. */
	private static String currentFunctionEndLabel = null;
	public static void setCurrentFunctionEndLabel(String s) { currentFunctionEndLabel = s; }
	public static String getCurrentFunctionEndLabel() { return currentFunctionEndLabel; }

	/******************/
	/* Add Ir command */
	/******************/
	public void AddIrCommand(IrCommand cmd)
	{
		if ((head == null) && (tail == null))
		{
			this.head = cmd;
		}
		else if ((head != null) && (tail == null))
		{
			this.tail = new IrCommandList(cmd,null);
		}
		else
		{
			IrCommandList it = tail;
			while ((it != null) && (it.tail != null))
			{
				it = it.tail;
			}
			it.tail = new IrCommandList(cmd,null);
		}
	}

	/** Return linear list of IR commands for CFG and register allocation. */
	public List<IrCommand> getCommandList() {
		List<IrCommand> list = new ArrayList<>();
		if (head != null) list.add(head);
		for (IrCommandList it = tail; it != null; it = it.tail)
			if (it.head != null) list.add(it.head);
		return list;
	}

	/** Emit MIPS for all commands. */
	public void mipsMe() {
		if (head != null) head.mipsMe();
		if (tail != null) tail.mipsMe();
	}

	/**************************************/
	/* USUAL SINGLETON IMPLEMENTATION ... */
	/**************************************/
	private static Ir instance = null;

	/*****************************/
	/* PREVENT INSTANTIATION ... */
	/*****************************/
	protected Ir() {}

	/******************************/
	/* GET SINGLETON INSTANCE ... */
	/******************************/
	public static Ir getInstance()
	{
		if (instance == null)
		{
			/*******************************/
			/* [0] The instance itself ... */
			/*******************************/
			instance = new Ir();
		}
		return instance;
	}
}
