package edu.buffalo.cse.cse486586.simpledynamo;

import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;


import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;

import android.net.Uri;


import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.util.Log;

import android.content.Context;
import android.telephony.TelephonyManager;
import java.net.ServerSocket;
import java.net.Socket;import java.io.IOException;
import android.util.Log;
import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.UnknownHostException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import static android.content.ContentValues.TAG;


public class SimpleDynamoProvider extends ContentProvider {
	public static final String KEY_FIELD = "key";
	public static final String VALUE_FIELD = "value";
	public static final String STORAGE_FILE = "filedb";
	String[] REMOTE_PORTS = {"11108","11112","11116","11120","11124"};
	String myPort;
	String portStr;
	int count = 0;
	TreeMap<String, String> ring = new TreeMap<String, String>(  );
	private Utility utility;
	public static final String GLOBAL = "*";
	public static final String LOCAL = "@";
	public static final int SERVER_PORT = 10000;
	public static final String FAILED = "failed";



	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		HashMap<String,String> h = new HashMap<String, String>(  );
		Message msg = new Message();
		msg.setKey( selection );
		msg.setType( "del" );
		msg.setOrigin( myPort );

		if (selection.contains( GLOBAL )) {
			msg.setType( "delAll" );
			for (Map.Entry<String, String>
					entry : ring.entrySet()){

				String temp_msg = msg.getKey() + "%%" + "null" + "%%" + msg.getOrigin() + "%%" + msg.getType() + "%%"+ "null";
				new ClientTask().executeOnExecutor( AsyncTask.SERIAL_EXECUTOR, temp_msg , String.valueOf(Integer.parseInt(entry.getValue()) * 2));

			}

		}
		else if (selection.contains( LOCAL )) {
			utility.delAllLocalData( getContext() );

		}
		else {
			try {
				HashSet<String> preferenceList = utility.getPreferenceList( msg,ring );
				Iterator<String> lr = preferenceList.iterator();
				while(lr.hasNext()) {

					String temp_msg = msg.getKey() + "%%" + "null" + "%%" + msg.getOrigin() + "%%" + msg.getType() + "%%"+ "null";
					new ClientTask().executeOnExecutor( AsyncTask.SERIAL_EXECUTOR, temp_msg , String.valueOf(Integer.parseInt(lr.next()) * 2));


				}

			}

			catch (NoSuchAlgorithmException e) {
				e.printStackTrace( );
			}

		}

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
		Log.i("in insert", values.toString());

		String key = values.getAsString(KEY_FIELD );
		String value = values.getAsString(VALUE_FIELD );
		Message msg = new Message() ;
		msg.setKey( key );
		msg.setValue( value );
		msg.setOrigin( myPort );
		msg.setType( "save" );
		msg.setVersion( String.valueOf(System.currentTimeMillis() ));
		try {
			HashSet<String> pl = utility.getPreferenceList(msg,ring);
			Iterator<String> lr = pl.iterator();
			while(lr.hasNext()) {
				//call client
				//modify message
				String temp_msg = msg.getKey() + "%%" + msg.getValue() + "%%" + msg.getOrigin() + "%%" + msg.getType() + "%%"+ msg.getVersion();

				new ClientTask().executeOnExecutor( AsyncTask.SERIAL_EXECUTOR, temp_msg , String.valueOf(Integer.parseInt(lr.next()) * 2));
			}


		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace( );
		}
		return uri;

	}


	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		utility = new Utility();
		Context context = getContext();
		TelephonyManager tel = (TelephonyManager)   context.getSystemService(Context.TELEPHONY_SERVICE);
		portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		myPort = String.valueOf((Integer.parseInt(portStr) * 2));
		try {
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);


		} catch (IOException e) {
			Log.e( "can't create socket", "Cant create server socket" );
			return false;
		}
		try {
			utility.delAllLocalData( getContext() );
			ring.put( utility.genHash( "5554" ), "5554" );
			ring.put( utility.genHash( "5556" ), "5556" );
			ring.put( utility.genHash( "5558" ), "5558" );
			ring.put( utility.genHash( "5560" ), "5560" );
			ring.put( utility.genHash( "5562" ), "5562" );
		} catch (NoSuchAlgorithmException e) {
			Log.v( TAG, "Could not hash" );
		}
			getNeighboursData( );
		return true;
	}

	public  void getNeighboursData() {

		try {
			String emu = String.valueOf(Integer.parseInt(myPort)/2);

			String hashKey = utility.genHash( emu );
			String a1 = ring.higherKey( hashKey );
			if (a1 == null) {
				a1 = ring.firstKey( );
			}
			String next =  ring.get( a1 );

			String a = ring.lowerKey( utility.genHash( emu ) );
			if (a == null) {
				a = ring.lastKey( );
			}
			String prev =  ring.get( a );
			LinkedList<String> l = new LinkedList<String>(  );
			l.add( next );
			l.add( prev );
			String temp_msg = "null" + "%%" + "null" + "%%" +myPort + "%%" + "getAll";
			ListIterator<String> ir = l.listIterator(  );
			String fin = "";
			while(ir.hasNext()){
				try {
					String rec = new ClientTask().executeOnExecutor( AsyncTask.SERIAL_EXECUTOR, temp_msg , String.valueOf(Integer.parseInt(ir.next()) * 2)).get();

					if  ((!rec.equals( "failed" )) && (rec.length() > 0) ) {
						fin = fin + "!" + rec;
					}
				}catch (NullPointerException e){
					continue;
				}
				catch (InterruptedException e) {
					e.printStackTrace( );
				} catch (ExecutionException e) {
					e.printStackTrace( );
				}
			}
			if(fin.length() >0) {
				fin = fin.substring( 1 );
				String[] msgs = fin.split( "!" );
				ArrayList<String> fin_list = new ArrayList<String>( );
				for (String m : msgs) {
					String[] arr_m = m.split( "%%" );
					Message temp_m = new Message( );
					temp_m.setKey( arr_m[0] );
					HashSet<String> pref = utility.getPreferenceList( temp_m, ring );
					if (pref.contains( portStr )) {
						fin_list.add( m );
					}
				}
				utility.saveListData( fin_list, getContext( ) );

			}
			return;



		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace( );
		}


	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
						String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		HashMap<String,String> h = new HashMap<String, String>(  );
		Message msg = new Message();
		msg.setKey( selection );
		msg.setType( "get" );
		msg.setOrigin( myPort );
		if (selection.contains( GLOBAL )) {
			msg.setType( "getAll" );
			String fin ="";
			for (Map.Entry<String, String>
					entry : ring.entrySet()){

				String temp_msg = msg.getKey() + "%%" + "null" + "%%" + msg.getOrigin() + "%%" + msg.getType();
				try {
					String rec = new ClientTask( ).executeOnExecutor( AsyncTask.SERIAL_EXECUTOR, temp_msg, String.valueOf( Integer.parseInt( entry.getValue( ) ) * 2 ) ).get( );

					if (!rec.equals( "failed" )) {
						fin = fin + "!" + rec;

					}
				}catch (NullPointerException e){
					Log.i( "got null", "null pointer" );

				} catch (InterruptedException e) {
					e.printStackTrace( );
				} catch (ExecutionException e) {
					e.printStackTrace( );
				}

			}
			fin = fin.substring( 1 );
			h = utility.stringToKeyValue( fin );

		}
		else if (selection.contains( LOCAL )) {

			String temp = utility.getLocalData( getContext() );
			if (temp.length() > 0) {
				h = utility.stringToKeyValue( temp );
			}
		}
		else {
			// get data using key
			//h = new HashMap<String, String>(  );
			try {
			HashSet<String> preferenceList = utility.getPreferenceList( msg, ring );
				Iterator<String> itr = preferenceList.iterator( );
				while (itr.hasNext( )) {
					//call client
					//modify message
					String temp_msg = msg.getKey( ) + "%%" + "null" + "%%" + msg.getOrigin( ) + "%%" + msg.getType( );
					String rec_msg = new ClientTask( ).executeOnExecutor( AsyncTask.SERIAL_EXECUTOR, temp_msg, String.valueOf( Integer.parseInt( itr.next( ) ) * 2 ) ).get( );
					try {
						if (!rec_msg.equals( FAILED )) {
							if (rec_msg.length( ) != 0) {
								h = utility.stringToKeyValue( rec_msg );
							}

						}
					}catch (NullPointerException e){

					}

				}


			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace( );
			} catch (InterruptedException e) {
				e.printStackTrace( );
			} catch (ExecutionException e) {
				e.printStackTrace( );
			}


		}


		MatrixCursor c = new MatrixCursor(
				new String[]{KEY_FIELD, VALUE_FIELD}
		);

		for (Map.Entry<String, String> m : h.entrySet()) {

			c.newRow()
					.add(KEY_FIELD, m.getKey())
					.add(VALUE_FIELD, m.getValue());
		}
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}





	private class ServerTask extends AsyncTask<ServerSocket, String, Void> {


		@Override
		protected Void doInBackground(ServerSocket... sockets) {
			ServerSocket serverSocket = sockets[0];
			try {
				String msg;
				while (true) {
					Socket socket = serverSocket.accept( );
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					if ((msg = in.readLine()) != null) {
						String[] arr_msg = msg.split( "%%" );

						String type = arr_msg[3];
						if (type.compareTo( "save" ) == 0){

							utility.saveData(msg, getContext());
							PrintWriter out = new PrintWriter( socket.getOutputStream( ), true );
							out.println( "done" );
							out.flush( );
						}else if (type.compareTo( "get" ) == 0) {

							//get data of the key
							String temp_data = utility.getLocalDataByKey( arr_msg[0], getContext( ) );
							//temp data has msg strings with ! delimeter
							PrintWriter out = new PrintWriter( socket.getOutputStream( ), true );
							out.println( temp_data );
							out.flush( );

						}else if (type.compareTo( "getAll" ) == 0) {
							//get all local data from
							String temp_data = utility.getLocalData( getContext() );
							Log.i( "all data ", temp_data );
							PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
							out.println( temp_data );
							out.flush();

						} else if (type.compareTo( "del" ) == 0) {
							String key = arr_msg[0];
							utility.delLocalDataByKey( key, getContext( ) );
							PrintWriter out = new PrintWriter( socket.getOutputStream( ), true );
							out.println( "done del" );
							out.flush( );

						} else if (type.compareTo( "delAll" ) == 0) {
							utility.delAllLocalData( getContext() );
							PrintWriter out = new PrintWriter( socket.getOutputStream( ), true );
							out.println( "done del all" );
							out.flush( );



						}
						else {
							Log.i( ">>>>", ">>>>>>>>>>" );
						}


					}
					socket.close();
				}


			}catch(Exception e) {
				Log.i( "Exception in server", e.toString() );
			}
			return null;

		}
	}


	private class ClientTask extends AsyncTask<String, Void , String> {

		@Override
		protected String doInBackground( String... msgs ){
			String res_string = "";
			try {
				Socket socket = new Socket( InetAddress.getByAddress( new byte[]{10, 0, 2, 2} ),
						Integer.parseInt( msgs[1] ) );
				socket.setSoTimeout( 1000 );
				String msgToSend = msgs[0];
				try {
					PrintWriter out = new PrintWriter( socket.getOutputStream( ), true );
					out.println( msgToSend );
					out.flush( );
				} catch (IOException e) {
					Log.i( "unable to send mesasges", "no msg" );
				}
				BufferedReader client_in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				while(true){
					res_string = client_in.readLine();
					break;
				}
				socket.close( );

			}catch (SocketTimeoutException e){
				Log.v( TAG,"soket time out" );
				res_string = FAILED;
			}
			catch (UnknownHostException e){
				Log.v( TAG,"unknown host exception" );
				res_string = FAILED;
			}
			catch (IOException e) {
				Log.i( "clientsocket IO exp", "here in client" );
				res_string = FAILED;
			}

			return res_string;
		}
	}


}

