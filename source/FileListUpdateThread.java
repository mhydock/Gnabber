//==============================================================================
// Date Created:		10 October 2009
// Last Updated:		30 August 2012
//
// File name:			FileListUpdateThread.java
// File author:			Matthew Hydock
// File description:	A thread-based class that scans a page and adds links
//						to files to a master list.
//==============================================================================

class FileListUpdateThread extends ThreadPlus
// Class to update the download list.
{
	FileConnection newCon;
	
	public void run()
	{		
		Debugger.report("FileListUpdaterThread: Started.");
		try
		{
			// Wait for the page scanner to be initialized.
			while (core.isRunning() && !core.isScannerInitialized())
				yield();
			
			while (core.isRunning() && core.scannerHasMoreLinks())
			// While there are more links to be added...
			{
				int size = core.getNumTotal() - core.getNumFinished() - core.getNumBroken();
					
				if (size <= core.getMaxQueueLength())
				// If the list of unfinished/unbroken links is less than the
				// amount of simultanious downloads...
				{
					for (int i = size; core.isRunning() && i < core.getMaxLookAhead() && core.scannerHasMoreLinks(); i++)
					// Add links to the list until the amount of links equals the max look ahead.
					{
						newCon = null;
						newCon = core.produceFileConnection();
						if (newCon == null) break;
						
						core.addFileConnection(newCon);
						core.incrementTotal();
						
						safeSleep(5);
					}
					if (core.scannerHasMoreLinks())	Debugger.report("FileListUpdateThread: Scanner has more links");
					Debugger.report("FileListUpdateThread: Scanner is taking a break...");
				}
				else
				// Otherwise, pause and let another thread do some work.
					yield();
			}
		}catch (Exception e)
		{
			Debugger.report(e.getMessage());
		}
			
		Debugger.report("FileListUpdaterThread: Done looking for links");
	}
}
