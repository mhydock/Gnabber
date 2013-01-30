//==============================================================================
// Date Created:		2 October 2012
// Last Updated:		30 January 2013
//
// File name:			GelbooruScanner.java
// File author:			Matthew Hydock
// File description:	A minimal implementation of a PageScanner, designed
//						specifically for Gelbooru. Piggybacks off of
//						DanbooruScanner.
//==============================================================================

import java.io.*;
import java.net.*;

public class GelbooruScanner extends DanbooruScanner
{
	private boolean isPool;

	public void connectToPage(String pagename, String saveLoc, int maxPage) throws Exception
	// Adds the possibility of the page being a pool to the connectToPage
	// method. Normally this isn't needed, but Gelbooru has subtle differences
	// when it is a pool compared to a normal gallery.
	{
		super.connectToPage(pagename,saveLoc,maxPage);
		isPool = false;	
	}

	public FileConnection parsePage() throws Exception
	{
		FileConnection connection = null;

//------------------------------------------------------------------------------
// The following conditionals modify the parser's state.
//------------------------------------------------------------------------------
		if (!isPool && currLine.contains("id=\"pool-show\""))
		// If the line includes the given phrase, the page is a pool.
		{
			isPool = true;
			
			// Snip off this part of the current line, and continue parsing.
			currLine = currLine.substring(currLine.indexOf("id=\"pool-show\"")+14,currLine.length());
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

			if (nextPage != null)
				Debugger.report("Next page located at " + nextPage);
		}
//------------------------------------------------------------------------------


//------------------------------------------------------------------------------
// The following conditional finds connections, and modifies the current line.
//------------------------------------------------------------------------------			
		if (currLine.contains("class=\"thumb\""))
		// An image thumbnail has been encountered, follow the link and parse
		// the resulting page.
		{
			int start = 0;
			
			if (isPool)
			// The structure is slightly different for pools for some reason.
			// The link to the image page is on the next line instead.
			{
				currLine = getNextLine();
				start = currLine.indexOf("index.php");
			}
			else
				start = currLine.indexOf("index.php", currLine.indexOf("class=\"thumb\""));

			String imgPage = currLine.substring(start,currLine.indexOf('\"',start));
			imgPage = replaceAll(imgPage,"&amp;","&");
			
			connection = new FileConnection(getDirectLink(serverName+imgPage),saveTo,serverName+imgPage);
			
			// Snip off this part of the current line, and continue parsing.
			currLine = currLine.substring(currLine.indexOf('\"',start),currLine.length());
		}
//------------------------------------------------------------------------------

		if (connection == null)
			currLine = "";
			
		return connection;
	}
}
