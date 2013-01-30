//==============================================================================
// Date Created:		25 July 2009
// Last Updated:		30 January 2013
//
// File name:			FileConnection.java
// File author:			Matthew Hydock
// File description:	Holds streams and data that represents a link to a file
//						on a server, with its name and location, and the
//						location of the duplicated file on the local machine.
//==============================================================================

import java.io.*;
import java.net.*;

public class FileConnection
{
	// Relevant links.
	private URL link;
	
	// File attributes.
	private String saveTo;
	private String referer;
	private File savedFile;
	private long currSize;
	private long finalSize;
	private long lastMod;
	
	// Global stream variables.
	private InputStream istream;
	private FileOutputStream ostream;
	private byte buffer[];
	private int read;
	
	// Error flags.
	public String error;
	
	public FileConnection(URL link, String path, String refer) throws Exception
	// Parse the html of an image page, and save the desired data.
	{
		// Flags and counters, for connection issues;
		error = null;
		this.link = link;
		
		// Set the path for the File to be downloaded to.
		saveTo = path;
		
		// Set the referer address (if provided).
		referer = (refer != null)?refer:"";
//		System.out.println("referer: " + referer);
//		System.out.flush();
		
		// Make a new local file with the name of the server file.
		String filename = this.link.getFile();
		filename = filename.substring(filename.lastIndexOf('/')+1,filename.length());
		filename = filename.replace("%20"," ");
		filename = filename.replace("%21","!");
		filename = filename.replace("%28","(");
		filename = filename.replace("%29",")");
		filename = filename.replace("%3A",":");
		filename = filename.replace("%3C","<");
		filename = filename.replace("%3E",">");
		
		savedFile = new File(saveTo,filename);
		
		currSize = savedFile.length();
		finalSize = -1;
	}
	
//==============================================================================
// The following method should be run only after connections are closed.
//==============================================================================
	public void renameFile(String newName)
	// Rename the file.
	{
		savedFile.renameTo(new File(newName));
	}
	
	public boolean changePath(String path)
	// Set the path for the File to be downloaded to.
	{
		File newDir = new File(path);
		
		boolean success = savedFile.renameTo(new File(newDir,savedFile.getName()));
		return success; 
	}
	
	public void getFileInfo() throws Exception
	// Initialize various data describing the downloaded file.
	{
		try
		{
			HttpURLConnection serverFile = (HttpURLConnection)link.openConnection();
			serverFile.addRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.11 (KHTML, like Gecko) Ubuntu Chromium/23.0.1271.97 Chrome/23.0.1271.97 Safari/537.11");
			serverFile.addRequestProperty("REFERER", referer);
			serverFile.connect();
			finalSize = serverFile.getContentLength();
			lastMod = serverFile.getLastModified();
		
			currSize = savedFile.length();
		} catch (Exception e)
		{
			error = e.toString();
			throw e;
		}
	}
//==============================================================================


//==============================================================================
// Open the connections when download is about to start, and close them when
// download is finished.
//==============================================================================	
	public void initConnections() throws Exception
	// Open up the streams and initialize the buffer space.
	{	
		try
		{
			if (finalSize == -1)
			// Obtain data on the server file.
				getFileInfo();
		
			// Image file on server, and local image file.
			istream = link.openStream();
			ostream = new FileOutputStream(savedFile,savedFile.exists());

			// 2kb buffer space.
			buffer = new byte[8 * 1024];

			if (currSize > 0 && !isDone())
				istream.skip(currSize);
		} catch (Exception e)
		{
			error = e.toString();
			throw e;
		}
	}
	
	public void closeConnections() throws Exception
	// Close the streams.
	{
		try
		{
			istream.close();
			ostream.close();
		
			istream = null;
			ostream = null;
			buffer = null;
		
			savedFile.setLastModified(lastMod);
		} catch (Exception e)
		{
			error = e.toString();
			throw e;
		}
	}
//==============================================================================


//==============================================================================
// This method is to be run in a loop, in a thread started from the GUI, for
// security and performance reasons.
//==============================================================================
	public boolean writeDataToFile() throws Exception
	// Read data and write to file.
	{
		try
		{
			read = istream.read(buffer);
			if (read == -1)
				return false;
			
			ostream.write(buffer, 0, read);
			currSize += read;
		
			// Force write to file, for security.
			ostream.flush();
			return true;
		} catch (Exception e)
		{
			error = e.toString();
			throw e;
		}
	}
//==============================================================================


//==============================================================================
// These methods are for convenience.
//==============================================================================
	public String getName()
	{
		return savedFile.getName();
	}
	
	public long getFinalSize()
	{
		return finalSize;
	}
	
	public long getCurrentSize()
	{
		return currSize;
	}
	
	public String getPrettySize()
	// Obtain the current and final sizes of the file, and transform them
	// into a human-readable format.
	{
		if (getFinalSize() == -1)
			return "-/-";

		double cs,fs;
		String currsuff,finalsuff;
		
		cs = getCurrentSize();
		fs = getFinalSize();
		
		currsuff = " b";
		if (cs > 1024)
		{
			currsuff = " Kb";
			cs /= 1024;
		}
		if (cs > 1024)
		{
			currsuff = " Mb";
			cs /= 1024;
		}
		
		finalsuff = "b";
		if (fs > 1024)
		{
			finalsuff = " Kb";
			fs /= 1024;
		}
		if (fs > 1024)
		{
			finalsuff = " Mb";
			fs /= 1024;
		}
		
		return String.format("%.1f",cs) + currsuff + " / " + String.format("%.1f",fs) + finalsuff;
	}
	
	public URL getLinkURL()
	{
		return link;
	}
	
	public String getStatus()
	{
		if (hasError())
			return error;
		
		if (isDone())
			return "Completed";
		
		if (read == 0)
			return "Waiting...";
		else if (read > 0)
			return "Downloading";
			
		return null;
	}
	
	public boolean isDone()
	{
		return (getFinalSize() != -1) && (getCurrentSize() >= getFinalSize());
	}
	
	public boolean hasError()
	{
		return error != null;
	}
	
	public void resetError()
	{
		error = null;
	}
//==============================================================================	
}
