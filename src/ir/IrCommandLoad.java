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

public class IrCommandLoad extends IrCommand
{
	Temp dst;
	String varName;

	public IrCommandLoad(Temp dst, String varName)
	{
		this.dst      = dst;
		this.varName = varName;
	}

	@Override
	public Set<Temp> getDef() { Set<Temp> s = new HashSet<>(); if (dst != null) s.add(dst); return s; }

	@Override
	public void mipsMe() { MipsGenerator.getInstance().load(dst, varName); }

	@Override
	public String toString()
	{
		return "LOAD T_" + dst.getSerialNumber() + " <- " + varName;
	}
}
