//==============================================================================
// Date Created:		2 October 2012
// Last Updated:		3 October 2012 
//
// File name:			DerpibooruScanner.java
// File author:			Matthew Hydock
// File description:	A minimal implementation of a PageScanner, designed
//						specifically for Derpibooru. Piggybacks off of
//						DanbooruScanner.
//==============================================================================

import java.io.*;
import java.net.*;

public class DerpibooruScanner extends DanbooruScanner
{
	public FileConnection parsePage() throws Exception
	// Parse the page. 
	{
		FileConnection connection = null;

//------------------------------------------------------------------------------
// The following conditionals modify the parser's state.
//------------------------------------------------------------------------------	
		if (nextPage == null)
		{
			if (currLine.contains("=\"next\""))
			// If a line is encountered that indicates there is a next page,
			// then remember the link. On Derpibooru, the link is on the next
			// line, so advance the line first.
			{
				currLine = pageReader.readLine();
				
				int start = currLine.indexOf("\"")+1;
				String temp = serverName;
				
				if (currLine.charAt(start) == '/')
					start++;
			
				temp += currLine.substring(start,currLine.indexOf('\"',start));
	
				nextPage = replaceAll(temp,"&amp;","&");
				
				// Snip off this part of the current line, and continue parsing.
				for (; currLine.charAt(start) != '>' && start < currLine.length(); start++);
				currLine = currLine.substring(start, currLine.length());
			}

			if (nextPage != null)
				Debugger.report("Next page located at " + nextPage);				
		}
//------------------------------------------------------------------------------


//------------------------------------------------------------------------------
// The following conditional finds connections, and modifies the current line.
//------------------------------------------------------------------------------	
		if (currLine.contains("class=\"image_container\""))
		// If a link to an image page is encountered, follow it and download
		// the image.
		{
			int start = currLine.indexOf("href");
			String imgPage = currLine.substring(start,currLine.indexOf('\"',start));
				
			connection = new FileConnection(getDirectLink(serverName+imgPage),saveTo);
			
			// Snip off this part of the current line, and continue parsing.
			currLine = currLine.substring(start+imgPage.length(),currLine.length());
		}
//------------------------------------------------------------------------------

		if (connection == null)
			currLine = "";
			
		return connection;
	}

	protected URL getDirectLink(String pageName) throws Exception
	// Parse a page for a direct link.
	{
		URL link = null;
		URL pageLink;
		
		// Save page URL, open a buffered stream
		pageLink = new URL(pageName);		
		BufferedReader pageReader = new BufferedReader(new InputStreamReader(pageLink.openStream()));

		for (String currLine = pageReader.readLine(); currLine != null; currLine = pageReader.readLine())
		// Scan the html looking for a link that leads to the desired image.
		{
			if (currLine.contains(">View</a>"))
			{
				int start = currLine.indexOf("http://");					
				int end = currLine.indexOf('\"',start);
				
				link = new URL(currLine.substring(start,end));
				break;
			}
		}
		
		// The page reader is no longer needed, close it.
		pageReader.close();
		
		return link;
	}
}
