/***********/
/* PACKAGE */
/***********/
package ir;

/*******************/
/* GENERAL IMPORTS */
/*******************/
import java.util.HashSet;
import java.util.Set;
import temp.*;
import mips.*;

public class IrCommandStore extends IrCommand
{
	String varName;
	Temp src;

	public IrCommandStore(String varName, Temp src)
	{
		this.src      = src;
		this.varName = varName;
	}

	@Override
	public Set<Temp> getUse() { Set<Temp> s = new HashSet<>(); if (src != null) s.add(src); return s; }

	@Override
	public void mipsMe() { MipsGenerator.getInstance().store(varName, src); }

	@Override
	public String toString()
	{
		return "STORE " + varName + " <- T_" + src.getSerialNumber();
	}
}
