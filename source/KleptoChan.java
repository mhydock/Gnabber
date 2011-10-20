//==============================================================================
// Date Created:		25 July 2009
// Last Updated:		17 October 2011
//
// File name:			KleptoChan.java
// File author:			Matthew Hydock
// File description:	Contains the GUI for KleptoChan, along with private
//						classes that deal with actions and display updates.
//
// Program name:		Klepto-chan
// Program description:	A web crawling image downloader built for sites like
//						Danbooro, Gelbooru, moe.imouto, etc.
//==============================================================================

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.net.URL;

public class KleptoChan extends JFrame
{
	// Hopefully self-explanatory GUI components.
	private JLabel header;
	private JLabel url;
	private JLabel saveto;
	private JLabel output;
	private JTextField urlField;
	private JTextField saveField;
	private JTable outputTable;
	private JScrollPane listScroller;
	private JButton download;
	private JButton fixFiles;
	private JButton clearList;
	private JButton browse;
	
	private JFrame globalThis;
	
	private GUIRefresher watcher;
	
	// Booleans to force synchronization and mode switching.
	private volatile boolean isDownloading = false;
	private volatile boolean repairMode = false;
	private volatile String lastDir = null;
	
	// Singleton that acts as global debugger.
	private Debugger d = Debugger.getInstance();
	
	// Singleton that holds all core information.
	private KleptoChanCore core = KleptoChanCore.getInstance();
	
//==============================================================================
// Oh look, a massive constructor...
//==============================================================================	
	public KleptoChan(boolean lf)
	{
		super("Klepto-chan, the kleptomaniacal web crawler!");
		
		// Attempt to make a log file.
		d.setActive(lf);
		
		// Initialize the global variables.
		core.init();
		
		// Instantiate all the components.
		Panel p	= new Panel(new GridBagLayout());
		Panel buttons = new Panel(new GridBagLayout());
		
		header	= new JLabel("",new ImageIcon("../resources/banner.png"),JLabel.LEFT);
		header.setOpaque(true);
		
		url 	= new JLabel("Start URL:");
		saveto	= new JLabel("Save To:");
		output	= new JLabel("Download Progress -- (0/0)");
		
		urlField	= new JTextField();
		saveField	= new JTextField();
		
		browse		= new JButton("", new ImageIcon("../resources/browse.png"));
		download	= new JButton("", new ImageIcon("../resources/start.png"));
		fixFiles	= new JButton("Fix Downloads");
		clearList	= new JButton("Clear List");
		
		browse.setMargin(new Insets(-2,-2,-2,-2));
		download.setMargin(new Insets(-2,-2,-2,-2));
		fixFiles.setMargin(new Insets(0,0,0,0));
		clearList.setMargin(new Insets(0,0,0,0));

		outputTable	= new FilesTable();
		
		listScroller = new JScrollPane(outputTable);
		outputTable.setFillsViewportHeight(true);

		// Add action listeners to the buttons.
		DownloadListener temp = new DownloadListener();
		download.addActionListener(temp);
		fixFiles.addActionListener(temp);
		browse.addActionListener(new BrowseListener());
		clearList.addActionListener(new ClearListListener());
		
		// Start adding the components, using a GridBagLayout and GridBagConstraints.
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		// Program logo/banner
		c.weightx = 1;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 3;
		c.gridheight = 1;
		p.add(header,c);
		
		// URL input label.
		c.weightx = 0;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		c.gridheight = 1;
		p.add(url,c);

		// Field for input of URL.
		c.weightx = 1;
		c.gridx = 1;
		c.gridwidth = 2;
		p.add(urlField,c);

		// Label for the files to be saved to.
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 1;
		p.add(saveto,c);

		// Field to input the directory where the files will be saved.
		c.weightx = 1;
		c.gridx = 1;
		c.gridwidth = 1;
		p.add(saveField,c);
		
		// Button to browse for a directory.
		c.weightx = 0;
		c.gridx = 2;
		c.gridwidth = 1;
		p.add(browse,c);

		// Panel to hold various buttons to control the downloading.
		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 3;
		p.add(buttons,c);
		
		// Label for the file download list.
		c.gridx = 0;
		c.gridy = 5;
		c.gridwidth = 3;
		p.add(output,c);
		
		// Scrollable list for all the downloads.
		c.weighty = 1;
		c.gridy = 6;
		c.gridheight = 4;
		p.add(listScroller,c);
		
		// Set up the download managing buttons.
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		buttons.add(download,c);

		c.gridx = 1;
		buttons.add(fixFiles,c);
		
		c.gridx = 2;
		buttons.add(clearList,c);
		
		// Start the frame.
		setContentPane(p);
		pack();
		setSize(640,480);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		validate();
		setVisible(true);
		
		globalThis = this;
		
		// Create the threads.
		core.initThreads();
		
		watcher	= new GUIRefresher(core.fileDownloader,core.listUpdater);
	}
//==============================================================================


//==============================================================================
// Listener classes, for actions.
//==============================================================================	
	class DownloadListener implements ActionListener
	// Listen to the download button; activate or deactivate downloads.
	{
		public void actionPerformed (ActionEvent e)
		{
			if (isDownloading)
			// Stop the threads and unfreeze the buttons.
			{
				// Attempt to stop the threads.
				try
				{
					core.fileDownloader.interrupt();
					core.listUpdater.interrupt();
					watcher.interrupt();
					
					d.debug("Threads stopped.");
				}catch (Exception ex)
				{
					d.debug(ex.getMessage());
				}
				
				isDownloading = false;
				
				// Reenable fields and buttons.
				urlField.setEnabled(true);
				saveField.setEnabled(true);
				browse.setEnabled(true);
				clearList.setEnabled(true);
				fixFiles.setEnabled(true);
				download.setIcon(new ImageIcon("../resources/start.png"));
			}
			else
			// Start up the downloading threads, and freeze the buttons.
			{
				isDownloading = true;

				// Disable stuff and change button text.
				urlField.setEnabled(false);
				saveField.setEnabled(false);
				browse.setEnabled(false);
				clearList.setEnabled(false);
				fixFiles.setEnabled(false);
				download.setIcon(new ImageIcon("../resources/stop.png"));
				
				// Connect to page and start downloading.
				try
				{
					// If fix button was clicked, re-add unfinished files, and continue scanning.
					if (e.getSource() == fixFiles)
					{
						FileConnection temp;
						
						for (int i = 0; i < core.fileList.size() && core.brokenFiles > 0; i++)
						{
							temp = core.fileList.get(i);
							if (temp.hasError())
							{
								temp.resetError();
								core.brokenFiles--;
							}
						}
						d.debug("[[Repair mode entered]]");
					}
					
					core.pageURL	= urlField.getText();
					core.saveTo		= saveField.getText();
					
					// Recreated the dead threads.
					core.initThreads();
					
					// Start running the threads.
					core.listUpdater.start();
					core.fileDownloader.start();
					
					// Reset and start the watcher.
					watcher = null;
					watcher	= new GUIRefresher(core.fileDownloader,core.listUpdater);
					watcher.start();
					
					d.debug("Threads started.");
				}catch (Exception ex)
				{
					d.debug(ex.getMessage());
					actionPerformed(null);
				}
			}
		}
	}
	
	class ClearListListener implements ActionListener
	// Clear all of the lists and the page history.
	{
		public void actionPerformed(ActionEvent e)
		{
			core.scanner.clearHistory();
			core.fileList.clear();
			
			core.doneFiles = 0;
			core.totalFiles = 0;
			core.brokenFiles = 0;
			
			output.setText("Download Progress -- (0/0)");

			listScroller.repaint();
			output.repaint();
		}
	}
	
	class BrowseListener implements ActionListener
	// File choosing dialog box.
	{
		public void actionPerformed(ActionEvent e)
		{
			// If there's already text in the "Save To" field, save it.			
			if (saveField.getText() != null)
				lastDir = saveField.getText();			
			
			JFileChooser fileChooser = new JFileChooser();

			// Otherwise, navigate to the last saved directory.			
			if (lastDir != null)
				fileChooser.setCurrentDirectory(new java.io.File(lastDir));
			else
				fileChooser.setCurrentDirectory(new java.io.File("./"));
				
			fileChooser.setDialogTitle("Save to Folder...");
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fileChooser.setAcceptAllFileFilterUsed(false);
   
			if (fileChooser.showOpenDialog(globalThis) == JFileChooser.APPROVE_OPTION)
			// If "Open" has been clicked, return the directory, and save it as
			// the last opened directory
			{
				saveField.setText(fileChooser.getSelectedFile().toString());
				lastDir = saveField.getText();
			}
        }
	}
//==============================================================================


//==============================================================================
// Thread classe to refresh the interface.
//==============================================================================
	class GUIRefresher extends Thread
	{
		FileDownloadThread fdthread;
		FileListUpdateThread fluthread;
	
		public GUIRefresher(FileDownloadThread fd, FileListUpdateThread flu)
		{
			super();
			
			fdthread = fd;
			fluthread = flu;
		}
	
		public void run()
		// Redraw the GUI as long as the other threads are active.
		{
			while (!isInterrupted() && (!fluthread.isInterrupted() || !fdthread.isInterrupted()))
			{
				try
				{
					refresh();
					Thread.sleep(5);
				}catch (Exception e)
				{
					d.debug(e.getMessage());
				}
			}
			
			refresh();
			if (!isInterrupted() && isDownloading) download.doClick();
			
			System.out.println("stuff done.");
			if (isDownloading)	System.out.println("isDownloading true");
			else				System.out.println("isDownloading false");
			System.out.flush();
			
			interrupt();
		}
						
		private synchronized void refresh()
		// Refresh various fields with updated data.
		{
			output.setText("Download Progress -- (" + core.doneFiles + "/" + core.totalFiles + ")");
			output.validate();			output.repaint();
			listScroller.validate();	listScroller.repaint();
		}	
	}
//==============================================================================


//==============================================================================
// The all-important main method.
//==============================================================================
	public static void main (String[] args)
	{
		KleptoChan kc = new KleptoChan(args.length==1 && args[0].equals("log"));
	}
}
