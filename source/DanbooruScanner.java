//==============================================================================
// Date Created:		13 September 2010
// Last Updated:		30 January 2013
//
// File name:			DanbooruScanner.java
// File author:			Matthew Hydock
// File description:	An implementation of PageScanner for Danbooru boards.
//==============================================================================

import java.io.*;
import java.net.*;

public class DanbooruScanner extends PageScanner
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
//------------------------------------------------------------------------------


//------------------------------------------------------------------------------
// The following conditional finds connections, and modifies the current line.
//------------------------------------------------------------------------------	
		if (currLine.contains("/post/show/"))
		// If a link to an image page is encountered, follow it and download
		// the image.
		{
			int start = currLine.indexOf("/post/show/");
			String imgPage = currLine.substring(start,currLine.indexOf('\"',start));
			URL url = getDirectLink(serverName+imgPage);

			// In case the page actually didn't have anything to save, normally
			// caused by not accounting for a specific file type that warrented
			// a specialized page formatting (like flash files).
			if (url != null)	connection = new FileConnection(url,saveTo,serverName+imgPage);
			
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
		
		// Save page URL, open a new buffered stream
		pageLink = new URL(pageName);		
		BufferedReader newPageReader = new BufferedReader(new InputStreamReader(pageLink.openStream()));

		for (String currLine = newPageReader.readLine(); currLine != null; currLine = newPageReader.readLine())
		// Scan the html looking for a link that leads to the desired image.
		{			
			if (currLine.contains("id=\"highres\"") || currLine.contains("Original image") || currLine.contains("name=\"movie\""))
			// Found link, save link location as URL.
			{
				String url = "";
				
				int start = currLine.indexOf("http://");

				if (start == -1)
				// In case files are stored on the same server, and their
				// address is relational.
				{
					start = currLine.indexOf("href=\"") + 7;
					url += serverName;
				}
					
				int end = currLine.indexOf('\"',start);
				
				link = new URL(url+currLine.substring(start,end));
				break;
			}
		}
		
		// The page reader is no longer needed, close it.
		newPageReader.close();
		
		return link;
	}
	
	public static String replaceAll(String source, String pattern, String replacement)
	// courtesy of teh interwebs.
	{
		if (source == null)
			return "";
       
		StringBuffer sb = new StringBuffer();
		int idx = -1;
		int patIdx = 0;

		while ((idx = source.indexOf(pattern, patIdx)) != -1)
		{
			sb.append(source.substring(patIdx, idx));
			sb.append(replacement);
			patIdx = idx + pattern.length();
		}
		
		sb.append(source.substring(patIdx));
		
		return sb.toString();
	}
//==============================================================================
}
