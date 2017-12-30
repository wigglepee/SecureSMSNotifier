package com.securesms.acn.securesmsserver;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class SecureSMSServer
{
	String startString = ". . .";
	static final int socketServerPORT = 6323;
	static final int MSG_TIME = 3000;
	Socket socket = null;
	ServerSocket inputSocket = null;
	Thread thread = null;
	TrayIcon trayIcon = null;
	MenuItem connectedItem = null;
	DataOutputStream out = null;
	DataInputStream in = null;
	Image image;

	public static void main(String[] args) 
	{
		try
		{
			System.setProperty("apple.awt.UIElement", "true"); 
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}


		EventQueue.invokeLater(new Runnable() 
		{
			public void run() 
			{
				try
				{
					//RemoteServer window = 
					new SecureSMSServer();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});		
	}

	/**
	 * Create the application.
	 */
	@SuppressWarnings("resource")
	public SecureSMSServer() 
	{		
		try 
		{
			new Socket("localhost", AppThread.PORT);
			System.exit(0);
		}
		catch (Exception e) 
		{
			new Thread(new AppThread()).start();
		}
		initialize();
		OpenServer();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize()
	{
		if (SystemTray.isSupported())
		{
			SystemTray tray = SystemTray.getSystemTray();

			image = Toolkit.getDefaultToolkit().getImage("icon.png");
			Dimension trayIconSize = tray.getTrayIconSize();
			Image trayImage = image.getScaledInstance(trayIconSize.width, trayIconSize.height, Image.SCALE_SMOOTH);

			PopupMenu popup = new PopupMenu();

//			inputItem = new MenuItem("Input");
//			inputItem.addActionListener(new ActionListener()
//			{
//				@Override
//				public void actionPerformed(ActionEvent e)
//				{
//					
//				}
//			});
//			//inputItem.setEnabled(false);
//			popup.add(inputItem);

			connectedItem = new MenuItem(startString);
			popup.add(connectedItem);
			popup.add("-");

			CheckboxMenuItem launchItem = new CheckboxMenuItem("Run At Login");
			launchItem.setState(false);
			launchItem.addItemListener(new ItemListener()
			{
				@Override
				public void itemStateChanged(ItemEvent e)
				{

				}

			});
			popup.add(launchItem);

			String ip_adress = "N/A";
			try
			{
				InetAddress ip = InetAddress.getLocalHost();
				ip_adress = ip.getHostAddress();
			}
			catch (UnknownHostException e)
			{
				e.printStackTrace();
			}
			MenuItem ipItem = new MenuItem("IP Address: " + ip_adress);
			ipItem.setEnabled(false);
			popup.add(ipItem);
			popup.add("-");

			MenuItem quitItem = new MenuItem("Quit");
			quitItem.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					System.exit(0);
				}
			});
			popup.add(quitItem);

			trayIcon = new TrayIcon(trayImage, "Remote Server", popup);

			//trayIcon.addActionListener(listener);

			try
			{
				tray.add(trayIcon);
			}
			catch (AWTException e)
			{
				System.err.println(e);
			}
		}
		else
		{
			JOptionPane.showMessageDialog(null, "not true : SystemTray.isSupported()");
		}

		//if (trayIcon != null) {
		//    trayIcon.setImage(updatedImage);
		//}
	}

	public void OpenServer()
	{
		thread = new Thread(new ServerThread());
		thread.start();
	}

	class ServerThread implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				inputSocket = new ServerSocket(socketServerPORT);

				while(true)
				{
					try
					{
						socket = inputSocket.accept();

						in = new DataInputStream(socket.getInputStream());
						out = new DataOutputStream(socket.getOutputStream());
						List<String> inputs = new ArrayList<String>();
						String input;
						boolean newMessage = false;

						while((input = in.readUTF()) != null)
						{
							if(input.equals("SMS END"))
								break;
							if(newMessage)
								inputs.add(input);
							if(input.equals("SMS BEGIN"))
								newMessage = true;
						}

						if(inputs.size() != 5)
							return;
						final List<String> finalInputs = inputs; 

						new Thread(new Runnable()
						{
							@Override
							public void run()
							{
								final String message = finalInputs.get(2) + "\n\n" + finalInputs.get(3) + " (" + finalInputs.get(4) + ")";
								connectedItem.setLabel(finalInputs.get(2).substring(0, finalInputs.get(2).length() > 30 ? 30 : finalInputs.get(2).length()) + "...");
								new Thread(new Runnable() {
									public void run() {
										JOptionPane opt = new JOptionPane(message,JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{});
										final JDialog dlg = opt.createDialog("SMS from " + finalInputs.get(0) + " (" + finalInputs.get(1) +")");
										new Thread(new Runnable() {
											public void run() {
												try {
													Thread.sleep(MSG_TIME);
													dlg.dispose();
											    }
											    catch ( Throwable th ) { }
											}
										}).start();
										dlg.setVisible(true);
										dlg.toFront();
									}
								}).start();
							}
						}).run();
					}
					catch (IOException e)
					{
						//e.printStackTrace();
					}
				}
			}
			catch (IOException e)
			{
				//e.printStackTrace();
			}
		}
	}

	class AppThread implements Runnable
	{
		public static final int PORT = 4323;
		@Override
		public void run()
		{
			try
			{
				inputSocket = new ServerSocket(PORT);

				while(true)
				{
					try
					{
						Socket socket = inputSocket.accept();
						socket.close();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public void sendToClient(String output)
	{
		if(socket != null)
		{
			try
			{
				out.writeUTF(output);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	//	public enum OSType
	//	{
	//		Windows, MacOS, Linux, Other
	//	};
	//
	//	protected static OSType detectedOS;
	//
	//	public static OSType getOperatingSystemType()
	//	{
	//		if (detectedOS == null) 
	//		{
	//			String OS = System.getProperty("os.name", "generic").toLowerCase();
	//			if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) 
	//			{
	//				detectedOS = OSType.MacOS;
	//			} 
	//			else if (OS.indexOf("win") >= 0) 
	//			{
	//				detectedOS = OSType.Windows;
	//			} 
	//			else if (OS.indexOf("nux") >= 0) 
	//			{
	//				detectedOS = OSType.Linux;
	//			} 
	//			else 
	//			{
	//				detectedOS = OSType.Other;
	//			}
	//		}
	//		return detectedOS;
	//	}

}