//==============================================================================
// Date Created:		10 October 2009
// Last Updated:		17 October 2011
//
// File name:			FileDownloadThread.java
// File author:			Matthew Hydock
// File description:	A thread-based class that scrolls through a list of
//						file connections and proceeds to download multiple files
//						at a time.
//==============================================================================

import java.util.ArrayDeque;

class FileDownloadThread extends ThreadPlus
// Class to download a file through a connection.
{
	private int arrayLocation;
	private FileConnection thisCon;
	private ArrayDeque<FileConnection> fileQueue;
	
	public void run()
	// Pull an available file connection from the list, and start downloading.
	{
		d.debug("File download thread started.");
			
		arrayLocation = 0;
		thisCon = null;
		fileQueue = new ArrayDeque<FileConnection>(core.maxQueueLength);
		
		// Wait for the page scanner to be initialized.
		while (!isInterrupted() && !core.scanner.isInitialized())
			yield();

		while (!isInterrupted() && (core.scanner.isInitialized() || !fileQueue.isEmpty()))
		// Start retrieving links and downloading files.
		{			
			// Fill the download queue to max capacity.	
			populateQueue();

			// Pull off the first connection.
			thisCon = fileQueue.poll();				
				
			// If the current connection isn't null, do stuff with it.
			if (thisCon != null)
			{
				// Attempt to download a chunk of data
				downloadChunk();

				// Download step has completed, put the link where it belongs.
				cleanup();
			}
			
			try
			{
				Thread.sleep(5);
			}catch (Exception e)
			{
				d.debug(e.getMessage());
			}
		}
							
		d.debug("Thread has finished work.");
		interrupt();
	}
//==============================================================================
// Private methods. Separated from run() to clean it up.
//==============================================================================

//------------------------------------------------------------------------------
// Queue management.
//------------------------------------------------------------------------------	
	private void populateQueue()
	// Populate the download queue to max capacity.
	{
		FileConnection temp = null;
		
		if (!isInterrupted() && fileQueue.size() < core.maxQueueLength && arrayLocation < core.fileList.size())
		// Populate the queue.
		{
			synchronized (core.fileList)
			{
				temp = core.fileList.get(arrayLocation);
			}
					
			if (!temp.isDone() && !temp.hasError())
			{
				try
				// Attempt to initialize the file connection.
				{
					initConnection(temp);
				}catch (Exception e)
				// There was a problem connecting to the file. Make note of this,
				// and add the file to the broken list.
				{
					d.debug("Error in initializing connections with file " + temp.getLinkURL());
					d.debug(e.getMessage());
					core.brokenFiles++;
				}
			}
			
			arrayLocation++;
		}
	}
	
	private void initConnection(FileConnection conn) throws Exception
	// Initialize a file connection.
	{
		conn.initConnections();

		// If file isn't already done, add to queue.
		if (!conn.isDone())
		{
			fileQueue.offer(conn);
			d.debug("Connection made, proceeding to download...");
		}
				
		// If file is already done, just skip it.
		else
		{
			conn.closeConnections();
			core.doneFiles++;
			d.debug("File already completed.");
		}
	}
//------------------------------------------------------------------------------


//------------------------------------------------------------------------------
// File connection management.
//------------------------------------------------------------------------------	
	private void downloadChunk()
	// Try to download a chunk of the current connection. If there is an error,
	// close the connection, add the connection the broken list, and output some
	// debug info.
	{
		try
		{
			thisCon.writeDataToFile();
		} catch (Exception e)
		{
			try
			{
				thisCon.closeConnections();
			}catch (Exception e2)
			{
				d.debug(e2.getMessage());
			}
			core.brokenFiles++;

			d.debug("Download of " + thisCon.getName() + "(" + thisCon.getCurrentSize() + "/" + thisCon.getFinalSize() + ") halted.\n" + 
					"Download the full file through another app using this address:\n" + thisCon.getLinkURL());
			d.debug(e.getMessage());
		}
	}
	
	private void cleanup()
	// A chunk of the file has been downloaded, and there were no errors, so put
	// the connection where it belongs, either in the finished list or back in
	// the download queue. 
	{
		if (!thisCon.hasError())
		// If the current connection does not have an error...
		{
			if (thisCon.isDone())
			// If file is done downloading, add connection to finished list.
			{
				try
				{
					thisCon.closeConnections();
				}catch (Exception e)
				{
					d.debug(e.getMessage());
				}
				core.doneFiles++;

				d.debug("Download of " + thisCon.getName() + "(" + thisCon.getCurrentSize() + "/" + thisCon.getFinalSize() + ") completed.");
			}
			else
			// Otherwise, return it to the queue.
				fileQueue.offer(thisCon);
		}
	}
//------------------------------------------------------------------------------

//==============================================================================
}
