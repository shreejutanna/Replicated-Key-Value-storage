package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import static android.content.Context.TELEPHONY_SERVICE;
import static java.lang.Integer.parseInt;

public class SimpleDynamoProvider extends ContentProvider {

	static final String REMOTE_PORT0 = "11108";
	static final String REMOTE_PORT1 = "11112";
	static final String REMOTE_PORT2 = "11116";
	static final String REMOTE_PORT3 = "11120";
	static final String REMOTE_PORT4 = "11124";
	static final int SERVER_PORT = 10000;
	List<String> avd_ports=Arrays.asList("11124", "11112", "11108", "11116", "11120");
	ConcurrentHashMap<String, String> content=new ConcurrentHashMap<String, String>();
	String myPort=null;
	BlockingQueue<String[]> blockingQueue= new ArrayBlockingQueue<String[]>(1000);
	BlockingQueue<String[]> blockingQueue1= new ArrayBlockingQueue<String[]>(1000);
	ConcurrentHashMap<String, ConcurrentHashMap<String,String>> port2key=new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();
	//ReentrantLock lock=new ReentrantLock();
	//Semaphore semaphore=new Semaphore(1);
	//static volatile int flag=0;
	ConcurrentHashMap<String, Long> key2timestamp=new ConcurrentHashMap<String, Long>();
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		//lock.lock();
		if(selection.equals("*")){
			;
		}
		else if(selection.equals("@")){
			content.clear();
		}
		else{
			content.remove(selection);
			List<String> deleteList=new ArrayList<String>(avd_ports);
			deleteList.add(selection);
			Collections.sort(deleteList, new Comparator<String>() {
				@Override
				public int compare(String lhs, String rhs) {
					Integer LeftHandSide=0, RightHandSide=0;
					if(lhs.length()==5) {
						LeftHandSide = parseInt(lhs) / 2;
						lhs=LeftHandSide.toString();
					}
					if(rhs.length()==5) {
						RightHandSide = parseInt(rhs) / 2;
						rhs=RightHandSide.toString();
					}

					try {
						return genHash(lhs).compareTo(genHash(rhs));
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
					}
					return -1;
				}
			});
			int list_index = deleteList.indexOf(selection);
			Log.v("List_Index",String.valueOf(list_index));
			Log.v("avd_ports",avd_ports.toString());
			if (list_index==avd_ports.size())
			{
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "delete-" + avd_ports.get(0)+"-"+selection+"-" + avd_ports.get(0), myPort);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "delete-" + avd_ports.get(1)+"-"+selection+"-" + avd_ports.get(0), myPort);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "delete-" + avd_ports.get(2)+"-"+selection+"-" + avd_ports.get(0), myPort);
			}
			else if(list_index!=avd_ports.indexOf(myPort)){
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "delete-" + avd_ports.get(list_index)+"-"+selection+"-" + avd_ports.get(list_index), myPort);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "delete-" + avd_ports.get((list_index+1)%5)+"-"+selection+ "-" + avd_ports.get(list_index), myPort);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "delete-" + avd_ports.get((list_index+2)%5)+"-"+selection+ "-" + avd_ports.get(list_index), myPort);
			}
			else if(list_index==avd_ports.indexOf(myPort)){
				content.remove(selection);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "delete-" + avd_ports.get((list_index+1)%5)+"-"+selection+ "-" + avd_ports.get(list_index), myPort);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "delete-" + avd_ports.get((list_index+2)%5)+"-"+selection+ "-" + avd_ports.get(list_index), myPort);
				ConcurrentHashMap <String, String> hm=port2key.get(myPort);
				hm.remove(selection);

			}

		}
		//lock.unlock();
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		//lock.lock();
		String key= (String) values.get("key");
		String value = (values.get("value")).toString();
		Date date=new Date();
		String timestamp=String.valueOf(date.getTime());
		List<String> insertlist=new ArrayList<String>(avd_ports);
		insertlist.add(key);
		Collections.sort(insertlist, new Comparator<String>() {
			@Override
			public int compare(String lhs, String rhs) {
				Integer LeftHandSide=0, RightHandSide=0;
				if(lhs.length()==5) {
					LeftHandSide = parseInt(lhs) / 2;
					lhs=LeftHandSide.toString();
				}
				if(rhs.length()==5) {
					RightHandSide = parseInt(rhs) / 2;
					rhs=RightHandSide.toString();
				}

				try {
					return genHash(lhs).compareTo(genHash(rhs));
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
				return -1;
			}
		});
		int list_index = insertlist.indexOf(key);
		Log.v("List_Index",String.valueOf(list_index));
		Log.v("avd_ports",avd_ports.toString());
		if (list_index==avd_ports.size())
		{
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "insert-" + avd_ports.get(0)+"-"+key+"-"+value + "-" + avd_ports.get(0) + "-" + timestamp, myPort);
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "insert-" + avd_ports.get(1)+"-"+key+"-"+value + "-" + avd_ports.get(0) + "-" + timestamp, myPort);
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "insert-" + avd_ports.get(2)+"-"+key+"-"+value + "-" + avd_ports.get(0) + "-" + timestamp, myPort);
		}
		else if(list_index!=avd_ports.indexOf(myPort)){
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "insert-" + avd_ports.get(list_index)+"-"+key+"-"+value + "-" + avd_ports.get(list_index) + "-" + timestamp, myPort);
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "insert-" + avd_ports.get((list_index+1)%5)+"-"+key+"-"+value + "-" + avd_ports.get(list_index) + "-" + timestamp, myPort);
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "insert-" + avd_ports.get((list_index+2)%5)+"-"+key+"-"+value + "-" + avd_ports.get(list_index) + "-" + timestamp, myPort);
		}
		else if(list_index==avd_ports.indexOf(myPort)){
			content.put(key, value);
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "insert-" + avd_ports.get((list_index+1)%5)+"-"+key+"-"+value + "-" + avd_ports.get(list_index) + "-" + timestamp, myPort);
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "insert-" + avd_ports.get((list_index+2)%5)+"-"+key+"-"+value + "-" + avd_ports.get(list_index) + "-" + timestamp, myPort);
			ConcurrentHashMap <String, String> hm=port2key.get(myPort);
			hm.put(key,value);
			key2timestamp.put(key,Long.valueOf(timestamp));

		}
		Log.v("HashMap",content.toString());
		//lock.unlock();
		return uri;

	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub


		TelephonyManager tel = (TelephonyManager) getContext().getSystemService(TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		myPort = String.valueOf((parseInt(portStr) * 2));

		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(SERVER_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.v("BlockingQueue Size", String.valueOf(blockingQueue.size()));
		new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
		port2key.put(REMOTE_PORT0, new ConcurrentHashMap<String, String>());
		port2key.put(REMOTE_PORT1, new ConcurrentHashMap<String, String>());
		port2key.put(REMOTE_PORT2, new ConcurrentHashMap<String, String>());
		port2key.put(REMOTE_PORT3, new ConcurrentHashMap<String, String>());
		port2key.put(REMOTE_PORT4, new ConcurrentHashMap<String, String>());
		//lock.lock();
		//flag=0;
		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "join-" + myPort, myPort);
		//while (flag==0);
		//lock.unlock();
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
						String sortOrder) {
		// TODO Auto-generated method stub
		String [] columns={"key","value"};
		String value = null;
		MatrixCursor mCursor = new MatrixCursor(columns);
		//blockingQueue.removeAll(blockingQueue);

		Log.v("Selection:", selection);
		/*if(content.get(selection)!=null) {
			mCursor.addRow(new String[]{selection, content.get(selection)});
			//lock.unlock();

			return mCursor;
		}*/
		if(selection.equals("*")){
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "*query-" +myPort, myPort);
			while(true) {
				String starquery[] = new String[0];
				try {
					starquery = blockingQueue1.poll(4, TimeUnit.SECONDS);
					if(starquery==null)
						break;
					for(int j=0;j<starquery.length;j++){
						mCursor.addRow(new String[]{starquery[j],starquery[++j]});
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}
		else if(selection.equals("@")){
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			for(String key:content.keySet()){
				value=content.get(key);
				/*Log.v("@Key", key);
				Log.v("@Value", value);*/
				mCursor.addRow(new String[]{key, value});
			}
			//lock.unlock();
			return mCursor;
		}
		else {
			List<String> insertlist = new ArrayList<String>(avd_ports);
			insertlist.add(selection);
			Collections.sort(insertlist, new Comparator<String>() {
				@Override
				public int compare(String lhs, String rhs) {
					Integer LeftHandSide = 0, RightHandSide = 0;
					if (lhs.length() == 5) {
						LeftHandSide = Integer.parseInt(lhs) / 2;
						lhs = LeftHandSide.toString();
					}
					if (rhs.length() == 5) {
						RightHandSide = Integer.parseInt(rhs) / 2;
						rhs = RightHandSide.toString();
					}

					try {
						return genHash(lhs).compareTo(genHash(rhs));
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
					}
					return -1;
				}
			});
			int list_index = insertlist.indexOf(selection);
			Log.v("List_Index", String.valueOf(list_index));
			Log.v("avd_ports", avd_ports.toString());
			Date date=new Date();
			String timestamp=date.toString();
			if(list_index==avd_ports.size()){
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "query-" + avd_ports.get(0)+"-"+myPort+"-"+selection+"-"+timestamp, myPort);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "query-" + avd_ports.get(1)+"-"+myPort+"-"+selection+"-"+timestamp, myPort);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "query-" + avd_ports.get(2)+"-"+myPort+"-"+selection+"-"+timestamp, myPort);
				String values[]=null;
				String value1=null;
				String value2=null;
				try {
					while(true) {
						values = blockingQueue.take();
						//Log.v("Values", values.toString());
						if(values[0].equals(selection)&& values[2].equals(timestamp))/*{
							if(value1==null)
								value1=values[1];
							else if(value1.equals(values[1]))
								break;
							else if(value2==null)
								value2=values[1];
							else if (value2.equals(values[1]))
								break;
							continue;
						}*/
							break;
						blockingQueue.put(values);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mCursor.addRow(new String[]{selection,values[1]});
			}
			else if(list_index!=avd_ports.indexOf(myPort)){
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "query-" + avd_ports.get(list_index)+"-"+myPort+"-"+selection+"-"+timestamp, myPort);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "query-" + avd_ports.get((list_index+1)%5)+"-"+myPort+"-"+selection+"-"+timestamp, myPort);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "query-" + avd_ports.get((list_index+2)%5)+"-"+myPort+"-"+selection+"-"+timestamp, myPort);
				String values[]=null;
				String value1=null;
				String value2=null;
				try {
					while(true) {
						values = blockingQueue.take();
						//Log.v("Values", values.toString());
						if(values[0].equals(selection) && values[2].equals(timestamp))/*{
							if(value1==null)
								value1=values[1];
							else if(value1.equals(values[1]))
								break;
							else if(value2==null)
								value2=values[1];
							else if (value2.equals(values[1]))
								break;
							continue;
						}*/
							break;
						blockingQueue.put(values);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mCursor.addRow(new String[]{selection,values[1]});
				//lock.unlock();
				return mCursor;
			}
			else if(list_index==avd_ports.indexOf(myPort)){
				if(content.get(selection)==null)
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				mCursor.addRow(new String[]{selection,content.get(selection)});
				//lock.unlock();
				return mCursor;
			}
		}


		//lock.unlock();
		return mCursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
					  String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	private String genHash(String input) throws NoSuchAlgorithmException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		byte[] sha1Hash = sha1.digest(input.getBytes());
		Formatter formatter = new Formatter();
		for (byte b : sha1Hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}
	private class ServerTask extends AsyncTask <ServerSocket, String, Void> {
		protected Void doInBackground(ServerSocket... sockets) {
			ServerSocket serverSocket = sockets[0];
			Socket socket = null;
			while (true) {
				try {
					socket = serverSocket.accept();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					BufferedReader in =
							new BufferedReader(new InputStreamReader(socket.getInputStream()));
					String msgReceived = in.readLine();
					//lock.lock();
					if (msgReceived != null) {
						Log.v("msgReceived", msgReceived);
						publishProgress(msgReceived + "");
					}
					//lock.unlock();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		protected void onProgressUpdate(String... strings) {
			String strReceived = strings[0];
			String receivedStrings[]=strReceived.split("-");
			Log.v("String received",receivedStrings[1]);
			if (receivedStrings[0].equals("join")){
				//lock.lock();
				Date date=new Date();
				String timestamp=String.valueOf(date.getTime());

				int index=avd_ports.indexOf(receivedStrings[1]);
				if(avd_ports.indexOf(myPort)==(index+1)%5 || avd_ports.indexOf(myPort)==(index+2)%5){
					//ConcurrentHashMap <String, String>  hm=port2key.get(receivedStrings[1]);
					for(String key:port2key.get(receivedStrings[1]).keySet()){
						new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "insert-" + receivedStrings[1]+"-"+key+"-"+port2key.get(receivedStrings[1]).get(key)+"-"+receivedStrings[1]+"-"+timestamp, myPort);
					}
					//new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "insert-"+ receivedStrings[1]+ "-joincomplete", myPort);
				}
				else{
					//ConcurrentHashMap <String, String>  hm=port2key.get(myPort);
					for(String key:port2key.get(myPort).keySet()){
						new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "insert-" + receivedStrings[1]+"-"+key+"-"+port2key.get(myPort).get(key)+"-"+myPort+"-"+timestamp, myPort);
					}
					//new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "insert-"+ receivedStrings[1]+ "-joincomplete", myPort);
				}
				//lock.unlock();

			}
			if (receivedStrings[0].equals("insert")){

				/*if(receivedStrings[2].equals("joincomplete")){
				}
				else {*/
				if(content.containsKey(receivedStrings[2])) {
					if (key2timestamp.get(receivedStrings[2]) > Long.valueOf(receivedStrings[5])) {

					}
					else{
						content.put(receivedStrings[2], receivedStrings[3]);
						ConcurrentHashMap hm = port2key.get(receivedStrings[4]);
						hm.put(receivedStrings[2], receivedStrings[3]);
						key2timestamp.put(receivedStrings[2], Long.valueOf(receivedStrings[5]));
					}
				}
				else {
					content.put(receivedStrings[2], receivedStrings[3]);
					ConcurrentHashMap hm = port2key.get(receivedStrings[4]);
					hm.put(receivedStrings[2], receivedStrings[3]);
					//Log.v("HashMap", content.toString());
					key2timestamp.put(receivedStrings[2], Long.valueOf(receivedStrings[5]));

				}

			}
			else if (receivedStrings[0].equals("query")){
				String value=content.get(receivedStrings[3]);
				if(value!=null)
					new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "reply-"+receivedStrings[2]+"-"+receivedStrings[3]+"-"+value+"-"+receivedStrings[4]);

			}
			else if (receivedStrings[0].equals("reply")){
				try {
					blockingQueue.put(new String[]{receivedStrings[2], receivedStrings[3], receivedStrings[4]});
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
			else if (receivedStrings[0].equals("*query")){
				for(String key:content.keySet()) {
					String value = content.get(key);
					new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "*reply-"+receivedStrings[1]+"-"+key+"-"+value);

				}

			}
			else if (receivedStrings[0].equals("*reply")){
				try {
					Log.v("Star Reply", "Inserted in blockingQueue");
					blockingQueue1.put(new String[]{receivedStrings[2],receivedStrings[3]});
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			else if (receivedStrings[0].equals("delete")){
				content.remove(receivedStrings[2]);
				ConcurrentHashMap hm=port2key.get(receivedStrings[3]);
				hm.remove(receivedStrings[2]);
			}

		}
	}
	private class ClientTask extends AsyncTask<String, Void, Void> {
		protected Void doInBackground(String... msgs) {
			Log.v("Message To Send", msgs[0]);
			if(msgs[0].substring(0,4).equals("join")){
				try {

					Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							parseInt(avd_ports.get((avd_ports.indexOf(myPort)+1)%5)));
					Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							parseInt(avd_ports.get((avd_ports.indexOf(myPort)+3)%5)));
					Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							parseInt(avd_ports.get((avd_ports.indexOf(myPort)+4)%5)));
					Socket socket3 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							parseInt(avd_ports.get((avd_ports.indexOf(myPort)+2)%5)));
					PrintWriter out0 =
							new PrintWriter(socket0.getOutputStream(),true);
					PrintWriter out1 =
							new PrintWriter(socket1.getOutputStream(),true);
					PrintWriter out2 =
							new PrintWriter(socket2.getOutputStream(),true);
					PrintWriter out3 =
							new PrintWriter(socket3.getOutputStream(),true);
					out0.println(msgs[0]);
					out1.println(msgs[0]);
					out2.println(msgs[0]);
					out3.println(msgs[0]);
					Thread.sleep(10);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
			if(msgs[0].substring(0,6).equals("insert")){
				try {
					Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							parseInt(msgs[0].substring(7,12)));

					PrintWriter out1 =
							new PrintWriter(socket1.getOutputStream(), true);
					out1.println(msgs[0]);
					Log.v("Message Sent", msgs[0]);
					Thread.sleep(10);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
			if(msgs[0].substring(0,5).equals("query")){
				try {
					Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							parseInt(msgs[0].substring(6,11)));

					PrintWriter out1 =
							new PrintWriter(socket0.getOutputStream(), true);
					out1.println(msgs[0]);
					Thread.sleep(10);
					//socket0.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}


			}
			if(msgs[0].substring(0,5).equals("reply")){
				try {
					Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							parseInt(msgs[0].substring(6,11)));

					PrintWriter out1 =
							new PrintWriter(socket0.getOutputStream(), true);
					out1.println(msgs[0]);
					Thread.sleep(10);
					//socket0.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}


			}
			if(msgs[0].substring(0,6).equals("*query")) {


				try {

					Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							parseInt(REMOTE_PORT0));

					Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							parseInt(REMOTE_PORT1));

					Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							parseInt(REMOTE_PORT2));
					Socket socket3 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							parseInt(REMOTE_PORT3));
					Socket socket4 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							parseInt(REMOTE_PORT4));

					PrintWriter out0 =
							new PrintWriter(socket0.getOutputStream(), true);
					PrintWriter out1 =
							new PrintWriter(socket1.getOutputStream(), true);
					PrintWriter out2 =
							new PrintWriter(socket2.getOutputStream(), true);
					PrintWriter out3 =
							new PrintWriter(socket3.getOutputStream(), true);
					PrintWriter out4 =
							new PrintWriter(socket4.getOutputStream(), true);

					out0.println(msgs[0]);
					out1.println(msgs[0]);
					out2.println(msgs[0]);
					out3.println(msgs[0]);
					out4.println(msgs[0]);
					Thread.sleep(10);

				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if(msgs[0].substring(0,6).equals("*reply")){
				try {
					Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							parseInt(msgs[0].substring(7,12)));

					PrintWriter out1 =
							new PrintWriter(socket0.getOutputStream(), true);
					out1.println(msgs[0]);
					Thread.sleep(10);
					//socket0.close();
					Log.v("Star Reply", "Sent");
					Log.v("HashMap Size", String.valueOf(content.size()));
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}


			}
			if(msgs[0].substring(0,6).equals("delete")){
				try {
					Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							parseInt(msgs[0].substring(7,12)));

					PrintWriter out1 =
							new PrintWriter(socket1.getOutputStream(), true);
					out1.println(msgs[0]);
					Log.v("Message Sent", msgs[0]);
					Thread.sleep(10);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}

			return null;
		}
	}
}