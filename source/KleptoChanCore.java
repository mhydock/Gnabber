//==============================================================================
// Date Created:		10 October 2009
// Last Updated:		17 October 2011
//
// File name:			KleptoChanCore.java
// File author:			Matthew Hydock
// File description:	A singleton class to store common data for Klepto-chan.
//						This was removed from the Klepto-chan GUI in an attempt
//						to make the various components more modular. This class
//						contains the minimum components to make an image scanner
//						for Danbooru-based image boards. Future versions will
//						leave the setting of the scanner to the UI.
//==============================================================================

import java.util.Vector;
import javax.swing.*;

public class KleptoChanCore
{
	private static KleptoChanCore core  = new KleptoChanCore();

	// Various lists to hold the file queues, with files left, and files where
	// errors occured.
	public Vector<FileConnection> fileList;
	
	// Object to scan the thumbnail pages, looking for image links.
	public PageScanner scanner;
	
	// Threads that control the download list and the file connections.
	public FileDownloadThread fileDownloader;
	public FileListUpdateThread listUpdater;
	
	public int doneFiles;
	public int totalFiles;
	public int brokenFiles;
	
	// Max limits for download queue and total file listing.
	public int maxQueueLength = 5;
	public int maxLookAhead = 50;
	public int maxPageNavi = -1;
	
	public String pageURL;
	public String saveTo;
	
	private Debugger d = Debugger.getInstance();
	
	private KleptoChanCore()
	{
	}
	
	public static synchronized KleptoChanCore getInstance()
	{
		return core;
	}
	
	public void init()
	// Initialize core variables.
	{
		// Instantiate the ArrayLists and other objects.
		scanner			= new DanbooruScanner();
		
		fileList		= new Vector<FileConnection>(maxLookAhead, maxLookAhead);
		
		pageURL = null;
		saveTo = null;
		
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
		
		d.debug("Threads cleared.");
		
		// Initialize threads.
		listUpdater		= new FileListUpdateThread();
		fileDownloader	= new FileDownloadThread();
		
		scanner.setParentThread(listUpdater);
		
		d.debug("Threads created.");
	}
	
	public Object clone() throws CloneNotSupportedException
	{
		throw new CloneNotSupportedException();
	}

}
	
