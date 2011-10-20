//==============================================================================
// Date Created:		10 October 2009
// Last Updated:		17 October 2011
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
		d.debug("List updater thread started.");
		try
		{
			core.scanner.connectToPage(core.pageURL,core.saveTo,core.maxPageNavi);
	
			while (!isInterrupted() && core.scanner.hasMoreLinks())
			// While there are more links to be added...
			{
				int size = core.totalFiles - core.doneFiles;
					
				if (size <= core.maxQueueLength)
				// If the list of links is less than the amount of simultanious downloads...
				{
					for (int i = size; !isInterrupted() && i < core.maxLookAhead && core.scanner.hasMoreLinks(); i++)
					// Add links to the list until the amount of links equals the max look ahead.
					{
						newCon = null;
						newCon = core.scanner.getFileConnection();
						if (newCon == null) break;
						
						synchronized (core.fileList)
						{
							core.fileList.add(newCon);
						}
						core.totalFiles++;
						
						d.debug("Scanner added new link");
					}
					if (core.scanner.hasMoreLinks())	d.debug("Scanner has more links");
					d.debug("Scanner is taking a break...");
				}
				else
				// Otherwise, pause and let another thread do some work.
					yield();
			}
		}catch (Exception e)
		{
			d.debug(e.getMessage());
		}
			
		d.debug("Done looking for links");
		interrupt();
	}
}
