//==============================================================================
// Date Created:		2 October 2012
// Last Updated:		3 October 2012
//
// File name:			SankakuScanner.java
// File author:			Matthew Hydock
// File description:	A minimal implementation of a PageScanner, designed
//						specifically for Sankaku Complex. Piggybacks off of
//						DanbooruScanner.
//==============================================================================

import java.io.*;
import java.net.*;

public class SankakuScanner extends DanbooruScanner
{
	public FileConnection parsePage() throws Exception
	// Parse the page. 
	{
		FileConnection connection = null;

		if (currLine.contains("popular-preview"))
		// Skip the preview of the most popular images, because they'll be
		// picked up eventually, and will cause conflicts otherwise.
		{
			while (currLine != null && !currLine.contains("/div"))
				currLine = pageReader.readLine();
				
			if (currLine != null)
				currLine = currLine.trim();
		}

		if (nextPage == null)
		{	
			if (currLine.contains("=\"next\""))
			// If a line is encountered that indicates there is a next page,
			// then remember the link.
			{
				int start = currLine.lastIndexOf('<', currLine.indexOf("=\"next\""));
			
				start = currLine.indexOf("\"", start)+1;
				String temp = serverName;
				
				if (currLine.charAt(start) == '/')
					start++;
			
				temp += currLine.substring(start,currLine.indexOf('\"',start));
	
				nextPage = replaceAll(temp,"&amp;","&");
				
				// Snip off this part of the current line, and continue parsing.
				for (; currLine.charAt(start) != '>' && start < currLine.length(); start++);
				currLine = currLine.substring(start, currLine.length());
			}

			else if (currLine.contains("&gt;&gt;") && !currLine.contains("disabled"))
			// If the page is a pool, and the current line indicates that
			// there is a next page, then find and save the link.
			{
				int i = currLine.lastIndexOf('<',currLine.indexOf("&gt;&gt;"));
				
				int start = currLine.indexOf('/',i)+1;
				int end = currLine.indexOf('\"',start);
				
				if (end == -1)
					end = currLine.indexOf('\'',start);
					
				nextPage = serverName + currLine.substring(start,end);
				
				// Snip off this part of the current line, and continue parsing.
				currLine = currLine.substring(end, currLine.length());
			}

			if (nextPage != null)
				Debugger.report("Next page located at " + nextPage);
		}

		if (currLine.contains("/post/show/"))
		// If a link to an image page is encountered, follow it and download
		// the image.
		{
			int start = currLine.indexOf("/post/show/");
			String imgPage = currLine.substring(start,currLine.indexOf('\"',start));
				
			connection = new FileConnection(getDirectLink(serverName+imgPage),saveTo);
			
			// Snip off this part of the current line, and continue parsing.
			currLine = currLine.substring(start+imgPage.length(),currLine.length());
		}

		if (connection == null)
			currLine = "";
			
		return connection;
	}
}
