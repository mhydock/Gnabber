//==============================================================================
// Date Created:		2 August 2009
// Last Updated:		17 October 2011
//
// File name:			PageScanner.java
// File author:			Matthew Hydock
// File description:	A separate class for scanning webpages and generating
//						lists of downloadable content.
//==============================================================================

import java.io.*;
import java.net.*;
import java.util.HashMap;

abstract class PageScanner
{
	protected URL startURL;
	protected URL currPage;
	protected String serverName;
	protected String nextPage;
	protected String currLine;
	protected String saveTo;
	
	protected BufferedReader pageReader;

	protected int pageNavi;
	protected int readLines;
	protected HashMap<URL,Integer> history;

	protected boolean scannerInit;
	
	protected Thread parent;
//==============================================================================
// Constructor, fairly bare-bones.
//==============================================================================
	public PageScanner()
	// Basic constructor, set all global variables to null.
	{
		saveTo		= null;
		serverName	= null;
		nextPage	= null;
		currLine	= null;
		
		startURL	= null;
		currPage	= null;
		
		pageReader	= null;
		pageNavi	= 0;
		
		history = new HashMap<URL,Integer>();
		readLines = 0;
		
		scannerInit	= false;
		
		parent = null;
	}
//==============================================================================


//==============================================================================
// Page connection management.
//==============================================================================
	public void connectToPage(String pagename, String saveLoc, int maxPage) throws Exception
	// Sets the URL variables and opens a reader interface to the data stream.
	{
		scannerInit = false;
		
		// Set the file path, and make directories (if necessary).
		saveTo = saveLoc;
		File temp = new File(saveTo);
		temp.mkdirs();
		
		// Get the base name of the server.
		serverName = pagename.substring(0,pagename.indexOf('/',8));
		serverName += '/';
		
		// Set the urls.
		startURL = new URL(pagename);
		currPage = startURL;
		
		// Set scanner control variables.
		pageNavi = maxPage;
		
		if (!history.containsKey(currPage))
		// If the page isn't already in the history, add it.
		{
			readLines = 0;
			history.put(currPage,0);
			
			// Open the reader and read in the first line.
			pageReader = new BufferedReader(new InputStreamReader(currPage.openStream()));
			currLine = pageReader.readLine();
			readLines++;

			if (currLine != null)
				currLine.trim();
		}
		else
		// Otherwise, just reconnect to the page.
			reconnect();
			
		scannerInit = true;
	}
	
	public void reconnect() throws Exception
	// If the scanner is disconnected or a page is revisited, return to the last read line.
	{
		scannerInit = false;
		
		pageReader.close();
		pageReader = null;
		currLine = null;
		
		pageReader = new BufferedReader(new InputStreamReader(currPage.openStream()));
		readLines = history.get(currPage);
		
		for (int i = 0; i < readLines; i++)
			currLine = pageReader.readLine();
		
		if (currLine != null)
			currLine.trim();

		scannerInit = true;
	}
	
	public boolean hasMoreLinks() throws Exception
	// Checks if there are more images to be downloaded.
	{
		if (scannerInit == false) throw new Exception("Scanner not initialized yet.");
		
		return scannerInit = (currLine != null) || (nextPage != null);
	}
	
	public void clearHistory()
	// Erase the history.
	{
		history.clear();
	}
	
	public boolean isInitialized()
	// Check to see if the scanner has been initialized yet.
	{
		return scannerInit;
	}
	
	public void setParentThread(Thread t)
	// Set the parent thread (helps make this thread-safe).
	{
		parent = t;
	}
	
	public Thread getParentThread(Thread t)
	// Return the parent thread (helps make this thread-safe).
	{
		return parent;
	}
//==============================================================================	
	
	
//==============================================================================
// User method to obtain a file connection.
//==============================================================================
	public FileConnection getFileConnection() throws Exception
	{
		FileConnection connection = null;
		boolean interrupted = false;
		
		while (!interrupted && (nextPage != null || currLine != null) && connection == null && pageNavi != 0)
		// Read lines until there are no more lines, a connection has been
		// found, or the max page navigation limit has been reached.
		{	
			// Skip empty lines.
			while (currLine != null && currLine.length() == 0)
			{
				currLine = pageReader.readLine();
				
				if (currLine != null)
					currLine = currLine.trim();
					
				readLines++;
			}
			
			// The reader has reached the end of the page, but there are still
			// more pages to be read.
			if (currLine == null && nextPage != null)
			{
				goNextPage();
				currLine = pageReader.readLine();
				
				if (currLine != null)
					currLine = currLine.trim();
				
				readLines++;
			}
			
			// Attempt to create a connection.	
			connection = parsePage();
			
			if (connection != null)
				System.out.println(connection.getName());
			
			if (parent != null)
				interrupted = parent.isInterrupted();
		}
			
		// The reader has reached the predetermined limit.
		if (pageNavi == 0)
		{
			nextPage = null;
			currLine = null;
		}
		
		// Close the stream only if no more links remain.
		if (nextPage == null && currLine == null)
			pageReader.close();

		// Record the page, and the number of lines read.
		history.put(currPage,(Integer)readLines);

		return connection;
	}
//==============================================================================	
	
	
//==============================================================================
// Private methods to clean up the connection generator.
//==============================================================================z	
	private void goNextPage() throws Exception
	// Go to the next page, set next page variable to null.
	{
		history.put(currPage,(Integer)readLines);

		// Close stream and remove references.
		pageReader.close();
		pageReader = null;
		currPage = null;
					
		// Change the URL and connect the streams again.
		currPage = new URL(nextPage);
		pageReader = new BufferedReader(new InputStreamReader(currPage.openStream()));
				
		// Remove any indication that there was a next page.
		nextPage = null;
		
		// Keep track of how many pages have been navigated through. When this
		// is zero, the main loop exits.
		pageNavi--;
		
		if (!history.containsKey(currPage))
		{
			history.put(currPage,0);
			readLines = 0;
		}
		else
			readLines = history.get(currPage);
	}
//==============================================================================


//==============================================================================
// Abstract method. Go nuts.
//==============================================================================
	public abstract FileConnection parsePage() throws Exception;
	// Parse the page. 
//==============================================================================
}
