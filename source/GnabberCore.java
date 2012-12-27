//==============================================================================
// Date Created:		10 October 2009
// Last Updated:		29 August 2012
//
// File name:			GnabberCore.java
// File author:			Matthew Hydock
// File description:	A singleton class to store common data for Gnabber.	This
//						was removed from the Gnabber GUI in an attempt to make
//						the various components more modular. This class contains
//						the minimum components required to make a semi-automated
//						mass downloader. Future versions will leave the setting
//						of the parser to the UI.
//==============================================================================

import java.util.Vector;
import javax.swing.*;

public class GnabberCore
{
	private static GnabberCore core  = new GnabberCore();

	// List to contain all files. Specific lists, such as only broken or only
	// finished, will be generated on demand.
	private Vector<FileConnection> fileList;
	
	// Object to scan the thumbnail pages, looking for image links.
	private PageScanner scanner;
	
	// Threads that control the download list and the file connections.
	private FileDownloadThread fileDownloader;
	private FileListUpdateThread listUpdater;

	// Status of the file downloads.
	private int doneFiles;
	private int totalFiles;
	private int brokenFiles;
	
	// Max limits for download queue and total file listing.
	private int maxQueueLength = 5;
	private int maxLookAhead = 50;
	private int maxPageNavi = -1;

	// Thread control.
	private boolean running = false;
		
	private GnabberCore()
	{
	}
	
	public static synchronized GnabberCore getInstance()
	{
		return core;
	}

	public Object clone() throws CloneNotSupportedException
	{
		throw new CloneNotSupportedException();
	}
//==============================================================================
// Initializers.
//==============================================================================
	public void init()
	// Initialize core variables.
	{
		// Instantiate the ArrayLists and other objects.
		scanner		= new BooruMetaScanner();
		
		fileList	= new Vector<FileConnection>(maxLookAhead, maxLookAhead);
		
		doneFiles = 0;
		totalFiles = 0;
		brokenFiles = 0;
	}
	
	public void initThreads()
	// Initialize the threads.
	{
		// Make sure all threads are null first.
		listUpdater = null;
		fileDownloader = null;
		
		Debugger.report("GnabberCore: Threads cleared.");
		
		// Initialize threads.
		listUpdater		= new FileListUpdateThread();
		fileDownloader	= new FileDownloadThread();
				
		Debugger.report("GnabberCore: Threads created.");
	}
//==============================================================================	


//==============================================================================
// Getters.
//==============================================================================
	public int getNumTotal()
	{
		return totalFiles;
	}

	public int getNumBroken()
	{
		return brokenFiles;
	}

	public int getNumFinished()
	{
		return doneFiles;
	}

	public int getMaxQueueLength()
	{
		return maxQueueLength;
	}

	public int getMaxLookAhead()
	{
		return maxLookAhead;
	}

	public int getMaxPageNavigation()
	{
		return maxPageNavi;
	}

	public PageScanner getPageScanner()
	{
		return scanner;
	}

	public Vector<FileConnection> getFileList()
	{
		return fileList;
	}
//==============================================================================


//==============================================================================
// Setters and incrementers.
//==============================================================================
	public void incrementTotal()
	{
		totalFiles++;
	}
	
	public void incrementBroken()
	{
		brokenFiles++;
	}

	public void incrementFinished()
	{
		doneFiles++;
	}

	public void setMaxQueueLength(int mql)
	{
		if (areThreadsRunning())
			Debugger.report("GnabberCore: Can't change queue length, threads already running.");
		else
			maxQueueLength = mql;
	}

	public void setMaxLookAhead(int mla)
	{
		if (areThreadsRunning())
			Debugger.report("GnabberCore: Can't change look ahead, threads already running.");
		else
			maxLookAhead = mla;
	}

	public void setMaxPageNavi(int mpn)
	{
		if (areThreadsRunning())
			Debugger.report("GnabberCore: Can't change page navigation limit, threads already running.");
		else
			maxPageNavi = mpn;
	}

	public void setScannerParams(String pageURL, String saveTo)
	{
		try
		{
			scanner.connectToPage(pageURL,saveTo,maxPageNavi);
		}
		catch (Exception e)
		{
			Debugger.report(e.getMessage());
		}
	}
//==============================================================================


//==============================================================================
// Integrity tests.
//==============================================================================
	public boolean isScannerInitialized()
	{
		return scanner != null && scanner.isInitialized();
	}

	public boolean scannerHasMoreLinks()
	{
		try
		{
			return scanner.hasMoreLinks();
		}
		catch (Exception e)
		{
			Debugger.report(e.getMessage());
			return false;
		}
	}

	public boolean areThreadsInitialized()
	{
		return listUpdater != null && fileDownloader != null;
	}

	public boolean areThreadsRunning()
	{
		return areThreadsInitialized() && listUpdater.isAlive() && fileDownloader.isAlive();
	}
//==============================================================================


//==============================================================================
// Thread control.
//==============================================================================
	public boolean stillDownloading()
	{
		return scannerHasMoreLinks() || (totalFiles != (doneFiles+brokenFiles));
	}

	public boolean isRunning()
	{
		return running;
	}

	public void startThreads()
	// If threads are initialized but not currently running, start them.
	{
		if (areThreadsInitialized() && !areThreadsRunning())
		{
			running = true;
			
			fileDownloader.start();
			listUpdater.start();
		}
	}

	public void stopThreads()
	// If threads are currently running, stop them. Interrupts are NOT used; a
	// control boolean is used instead.
	{
		if (areThreadsRunning())
		{
			Debugger.report("GnabberCore: Threads being asked to stop...");
			
			running = false;
		}
	}
//==============================================================================


//==============================================================================
// Short cut methods.
//==============================================================================
	public void resetFileLists()
	{
		scanner.clearHistory();
		fileList.clear();
			
		doneFiles = 0;
		totalFiles = 0;
		brokenFiles = 0;
	}

	public void addFileConnection(FileConnection fc)
	{
		fileList.add(fc);
	}

	public FileConnection produceFileConnection()
	{
		try
		{
			return scanner.getFileConnection();
		}
		catch (Exception e)
		{
			Debugger.report(e.getMessage());
			return null;
		}
	}

	public FileConnection getFileConnection(int i)
	{
		return fileList.get(i);
	}

	public void resetFileConnectionError(int i)
	{
		if (fileList.get(i).hasError())
		{
			fileList.get(i).resetError();
			brokenFiles--;
		}
	}
//==============================================================================
}
	
