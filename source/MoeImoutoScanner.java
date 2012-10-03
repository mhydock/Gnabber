//==============================================================================
// Date Created:		2 October 2012
// Last Updated:		3 October 2012
//
// File name:			MoeImoutoScanner.java
// File author:			Matthew Hydock
// File description:	A minimal implementation of a PageScanner, designed
//						specifically for moe.imouto (now yande.re). Piggybacks
//						off of DanbooruScanner.
//
//						NOTE: This is incomplete. yande.re was having issues
//						loading, and I was unable to finish studying the HTML
//						while working on this.
//==============================================================================

import java.io.*;
import java.net.*;

public class MoeImoutoScanner extends DanbooruScanner
{
	public FileConnection parsePage() throws Exception
	// Parse the page. 
	{
		FileConnection connection = null;

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
// The following conditionals find connections, and modify the current line.
//------------------------------------------------------------------------------
		if (currLine.contains("directlink largeimg"))
		// If there are direct links to the fullsize image on the thumbnail
		// page, then don't screw around and just follow that link.
		{
			int start = currLine.indexOf("http",currLine.indexOf("directlink largeimg"));
			String imgPage = currLine.substring(start,currLine.indexOf('\"',start));
			
			connection = new FileConnection(new URL(imgPage),saveTo);
			
			// Snip off this part of the current line, and continue parsing.
			currLine = currLine.substring(start+imgPage.length(),currLine.length());
		}

		else if (currLine.contains("/post/show/"))
		// If direct links are unavailable for some reason, but a link to an
		// image page is encountered, follow it and download the image.
		{
			int start = currLine.indexOf("/post/show/");
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
}
