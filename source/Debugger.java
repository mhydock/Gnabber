//==============================================================================
// Date Created:		October 10, 2009
// Last Updated:		October 11, 2010
//
// Programmer:			Matthew Hydock
//
// Program name:		Debugger
// Program Description:	A class to manage and write to log files. Originally
//						part of Klepto-chan, it was removed to improve
//						modularity, plus it can now be used by itself.
//==============================================================================

import java.util.Calendar;
import java.io.*;

public class Debugger
{
	private static Debugger d = new Debugger();
	
	// Variables dedicated to writing and synchronizing the log file.
	private PrintStream log;
	private File logFile;
	private int logFiles = 0;
	private boolean genLogFile = false;
	
	private Debugger()
	{
	}
	
	public static Debugger getInstance()
	{
		return d;
	}
	
	public void setActive(boolean lf)
	// Set whether the debugger should be active or not.
	{
		genLogFile = lf;
		
		if (lf)
		{			
			try
			{
				logFile = new File("log.txt");
				log = new PrintStream(new FileOutputStream(logFile));
				logFiles++;
			}catch (Exception e)
			{
				System.out.println("Could not generate log file.");
				System.exit(0);
			}
		}
		else
		{
			logFile = null;
			if (log != null) log.close();
			log = null;
		}
	}
	
	public void debug (String s)
	// Method to write progress/errors to a log file.
	{
		if (!genLogFile)
			return;
		
		synchronized(logFile)
		{
			// If the log file is larger than 4MB, make a new one.
			if (logFile.length() > 4194304)
			{
				logFile.renameTo(new File("old_log(" + logFiles + ").txt"));
				log.close();
			
				try
				{
					logFile = new File("log.txt");
					log = new PrintStream(new FileOutputStream(logFile));	
				}catch (Exception e)
				{
					System.out.println("Could not generate log file.");
					System.exit(0);
				}
			
				logFiles++;
			}		
			// Write message to log file.
			
			Calendar cal = Calendar.getInstance();
			log.format("%02d:%02d:%02d  %s%n",cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE),cal.get(Calendar.SECOND),s);
			log.flush();
		}
	}
}
