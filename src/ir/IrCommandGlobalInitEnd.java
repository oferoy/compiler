package ir;

/** Sentinel in the IR list: everything before this is global variable init only. */
public class IrCommandGlobalInitEnd extends IrCommand
{
	@Override
	public void mipsMe() { /* no-op */ }
}
