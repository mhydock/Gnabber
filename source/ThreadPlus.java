//==============================================================================
// Date Created:		10 October 2009
// Last Updated:		16 October 2011
//
// File name:			KleptoChanCore.java
// File author:			Matthew Hydock
// File description:	An intermediate abstract class, adding minor
//						functionality to the original Thread class, 
//==============================================================================

public abstract class ThreadPlus extends Thread
{
	Debugger d = Debugger.getInstance();
	KleptoChanCore core = KleptoChanCore.getInstance();

	public abstract void run();
	
	protected void safesleep()
	// Since I use it more than once, figured it'd be cleaner this way.
	{
		try
		{
			Thread.sleep(100);
		}catch (Exception e)
		{
			d.debug(e.toString());
		}
	}
}
