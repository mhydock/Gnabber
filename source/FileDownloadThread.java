//==============================================================================
// Date Created:		10 October 2009
// Last Updated:		30 August 2012
//
// File name:			FileDownloadThread.java
// File author:			Matthew Hydock
// File description:	A thread-based class that scrolls through a list of
//						file connections and proceeds to download multiple files
//						at a time.
//==============================================================================


class FileDownloadThread extends ThreadPlus
// Class to download a file through a connection.
{
	private int aLoc;					// Array location.
	private int qPos;					// Queue position.
	private FileConnection[] queue;		// File queue.
	
	public void run()
	// Pull an available file connection from the list, and start downloading.
	{
		Debugger.report("FileDownloadThread: Started.");

		// Allow "resume" functionality.
		aLoc = core.getNumBroken() + core.getNumFinished();

		// Always start with an empty queue.
		qPos = 0;

		queue = new FileConnection[core.getMaxQueueLength()];
				
		// Wait for the page scanner to be initialized.
		while (core.isRunning() && !core.isScannerInitialized())
			yield();

		while (core.isRunning() && core.isScannerInitialized())
		// Start retrieving links and downloading files. This loop is kind of
		// dumb; if there are no more links in the file list, and if the queue
		// is empty, it will still iterate through the queue looking for work.
		// The only way it will stop is if an outside force, like the core,
		// interrupts it.
		{
			if (queue[qPos] == null && aLoc < core.getFileList().size())
			{
				queue[qPos] = core.getFileConnection(aLoc);

				Debugger.report("FileDownloadThread: " + queue[qPos].getName() + " added to download queue.");
				
				try
				// Attempt to initialize the file connection. If the file is
				// not done yet, add it to the queue and advance the index
				// in the file array.
				{
					queue[qPos].initConnections();

					if (queue[qPos].isDone())
					{
						Debugger.report("FileDownloadThread: " + queue[qPos].getName() + " already completed. Skipping...");

						queue[qPos].closeConnections();
						queue[qPos] = null;
						core.incrementFinished();
					}
				}
				catch (Exception e)
				// There was a problem connecting to the file. Make note of
				// this, and add the file to the broken list.
				{
					core.incrementBroken();
					Debugger.report("FileDownloadThread: Error in initializing connections with file " + queue[qPos].getLinkURL());
					Debugger.report(e.getMessage());
				}

				aLoc++;
			}

			if (queue[qPos] != null)
			// If the current position in the queue is an active connection,
			// update the connection.
			{
				// Attempt to download a chunk of data
				downloadChunk();

				// Download step has completed, put the link where it belongs.
				cleanup();
			}
			
			// Advance the position of the queue.
			qPos++;

			// The end of the queue has been reached, loop back to beginning.
			if (qPos >= core.getMaxQueueLength())
				qPos = 0;

			// Sleep a little, to prevent event coalescence.
			safeSleep(5);
		}

		for (int i = 0; i < core.getMaxQueueLength(); i++)
		// Close all connections that might have been open at the time of thread
		// interruption.
		{
			if (queue[i] != null)
			{
				if (queue[i].isDone())
					core.incrementFinished();

				try
				{
					queue[i].closeConnections();
				}
				catch (Exception e)
				{
					Debugger.report(e.getMessage());
				}
				queue[i] = null;
			}
		}
		
		Debugger.report("FileDownloadThread: Finished work");
	}
//==============================================================================
// Private methods. Separated from run() to clean it up.
//==============================================================================
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
			queue[qPos].writeDataToFile();
		}
		catch (Exception e)
		{
			Debugger.report("FileDownloadThread: Download of " + queue[qPos].getName() + "(" + queue[qPos].getCurrentSize() + "/" + queue[qPos].getFinalSize() + ") halted.\n" + 
					"\t\tDownload the full file through another app using this address:\n" + queue[qPos].getLinkURL());
			Debugger.report(e.getMessage());
			
			try
			{
				queue[qPos].closeConnections();
				queue[qPos] = null;
			}
			catch (Exception e2)
			{
				Debugger.report(e2.getMessage());
			}
			core.incrementBroken();
		}
	}
	
	private void cleanup()
	// A chunk of the file has been downloaded, and there were no errors, so put
	// the connection where it belongs, either in the finished list or back in
	// the download queue. 
	{
		if (!queue[qPos].hasError())
		// If the current connection does not have an error...
		{
			if (queue[qPos].isDone())
			// If file is done downloading, add connection to finished list.
			{
				Debugger.report("FileDownloadThread: Download of " + queue[qPos].getName() + "(" + queue[qPos].getCurrentSize() + "/" + queue[qPos].getFinalSize() + ") completed.");

				try
				{
					queue[qPos].closeConnections();
					queue[qPos] = null;
				}
				catch (Exception e)
				{
					Debugger.report(e.getMessage());
				}
				core.incrementFinished();
			}
		}
	}
//------------------------------------------------------------------------------

//==============================================================================
}
