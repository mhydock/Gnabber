//==============================================================================
// Date Created:		13 September 2010
// Last Updated:		30 August 2012
//
// File name:			DanbooruScanner.java
// File author:			Matthew Hydock
// File description:	An implementation of PageScanner for Danbooru boards.
//==============================================================================

import java.io.*;
import java.net.*;
import java.util.HashMap;

public class DanbooruScanner extends PageScanner
{
	private boolean isPool;
	
	public void connectToPage(String pagename, String saveLoc, int maxPage) throws Exception
	// Adds the possibility of the page being a pool to the connectToPage
	// method. 
	{
		super.connectToPage(pagename,saveLoc,maxPage);
		isPool = false;	
	}
		
	public FileConnection parsePage() throws Exception
	// Parse the page. 
	{
		FileConnection connection = null;

//------------------------------------------------------------------------------
// The following conditionals modify the parser's state.
//------------------------------------------------------------------------------
		if (currLine.contains("Featured_Imageleft"))
		{
			while (currLine.contains("Featured_Imageleft"))
				currLine = pageReader.readLine();
				
			if (currLine != null)
				currLine = currLine.trim();
		}
		
		if (currLine.contains("popular-preview"))
		// Skip the preview of the most popular images, because they'll be
		// picked up eventually, and will cause conflicts otherwise.
		{
			while (currLine != null && !currLine.contains("/div"))
				currLine = pageReader.readLine();
				
			if (currLine != null)
				currLine = currLine.trim();
		}
		
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
		// If a link to an image page is encountered, follow it and download
		// the image.
		{
			int start = currLine.indexOf("/post/show/");
			String imgPage = currLine.substring(start,currLine.indexOf('\"',start));
				
			connection = new FileConnection(getDirectLink(serverName+imgPage),saveTo);
			
			// Snip off this part of the current line, and continue parsing.
			currLine = currLine.substring(start+imgPage.length(),currLine.length());
		}
		
		else if (currLine.contains("class=\"thumb\""))
		// This is for Gelbooru, which seems to have a different front-end.
		{
			int start = currLine.indexOf("index.php", currLine.indexOf("class=\"thumb\""));
			
			String imgPage = currLine.substring(start,currLine.indexOf('\"',start));
			imgPage = replaceAll(imgPage,"&amp;","&");
			
			connection = new FileConnection(getDirectLink(serverName+"/"+imgPage),saveTo);
			
			// Snip off this part of the current line, and continue parsing.
			currLine = currLine.substring(currLine.indexOf('\"',start),currLine.length());
		}
//------------------------------------------------------------------------------

		if (connection == null)
			currLine = "";
			
		return connection;
	}
	
	private URL getDirectLink(String pageName) throws Exception
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
			if (currLine.contains("id=\"highres\"") || currLine.contains("Original image"))
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
		pageReader.close();
		
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
