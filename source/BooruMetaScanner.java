//==============================================================================
// Date Created:		2 October 2012
// Last Updated:		28 January 2013
//
// File name:			BooruMetaScanner.java
// File author:			Matthew Hydock
// File description:	A wrapper class to ease the maintanence of the Danbooru
//						family of PageScanners. Originally, all supported image
//						boards had their parsing controls in DanbooruScanner.
//						This became unmanageable as more boards were added, each
//						with their own specific quirks. This meta class is an
//						attempt to remedy this mess.
//==============================================================================

public class BooruMetaScanner extends PageScanner
{
	private PageScanner scanner = null;

	public BooruMetaScanner()
	// No need to instantiate anything, as all methods will be forwarded to the
	// nested scanner.
	{
	}

	private void selectScanner(String pagename)
	// Identify the server, and select an appropriate scanner for that site.
	{
		// Get the base name of the server.
		serverName = pagename.substring(0,pagename.indexOf('/',8));
		serverName += '/';

		if (serverName.contains("danbooru"))		scanner = new DanbooruScanner();
		else if (serverName.contains("gelbooru"))	scanner = new GelbooruScanner();
		else if (serverName.contains("yande.re"))	scanner = new MoeImoutoScanner();
		else if (serverName.contains("konachan"))	scanner = new MoeImoutoScanner();
		else if (serverName.contains("sankaku"))	scanner = new SankakuScanner();
		else if (serverName.contains("derpiboo"))	scanner = new DerpibooruScanner();
		else 										scanner = new DanbooruScanner();
	}

//==============================================================================
// Wrappers for PageScanner methods. Each one forwards data to the inner scanner
// that was instantiated in selectScanner().
//==============================================================================
	public void connectToPage(String pagename, String saveLoc, int maxPage) throws Exception
	// Select the appropriate scanner, then have it connect to the given page.
	{
		selectScanner(pagename);

		scanner.connectToPage(pagename,saveLoc,maxPage);
	}
	
	public void reconnect() throws Exception
	{
		if (scanner != null)
			scanner.reconnect();
	}

	public void disconnect() throws Exception
	{
		if (scanner != null)
			scanner.disconnect();
	}

	public boolean hasMoreLinks() throws Exception
	{
		if (scanner != null)
			return scanner.hasMoreLinks();

		return false;
	}

	public void clearHistory()
	{
		if (scanner != null)
			scanner.clearHistory();
	}

	public boolean isInitialized()
	{
		if (scanner != null)
			return scanner.isInitialized();

		return false;
	}

	public void setParent(Thread p)
	{
		if (scanner != null)
			scanner.setParent(p);
	}

	public Thread getParent()
	{
		if (scanner != null)
			return scanner.getParent();

		return null;
	}

	public FileConnection getFileConnection() throws Exception
	{
		if (scanner != null)
			return scanner.getFileConnection();

		return null;
	}
//==============================================================================


//==============================================================================
// The following methods are empty, here for the sake of completion, but not
// much else.
//==============================================================================
	private void goNextPage() throws Exception
	{
	}
		
	public FileConnection parsePage() throws Exception
	{
		return null;
	}
//==============================================================================
}
