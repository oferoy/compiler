/***********/
/* PACKAGE */
/***********/
package ir;

import java.util.HashSet;
import java.util.Set;
import temp.*;
import mips.*;

public class IrCommandBinopGtIntegers extends IrCommand
{
	public Temp t1;
	public Temp t2;
	public Temp dst;

	public IrCommandBinopGtIntegers(Temp dst, Temp t1, Temp t2)
	{
		this.dst = dst;
		this.t1 = t1;
		this.t2 = t2;
	}

	@Override
	public Set<Temp> getUse() { Set<Temp> s = new HashSet<>(); if (t1 != null) s.add(t1); if (t2 != null) s.add(t2); return s; }
	@Override
	public Set<Temp> getDef() { Set<Temp> s = new HashSet<>(); if (dst != null) s.add(dst); return s; }

	@Override
	public void mipsMe() { MipsGenerator.getInstance().compareGt(dst, t1, t2); }
}