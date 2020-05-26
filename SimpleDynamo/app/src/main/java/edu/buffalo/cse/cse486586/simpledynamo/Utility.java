package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

public class Utility {


    private static final String STORAGE_FILE = "filedb";
    public void saveData(String msg, Context context) {
            String[] arr_msg = msg.split( "%%" );
            SharedPreferences sp = context.getSharedPreferences( STORAGE_FILE, Context.MODE_PRIVATE );
            SharedPreferences.Editor e = sp.edit( );
            e.putString( arr_msg[0] + "&" + arr_msg[4], arr_msg[1] );
            e.apply( );

    }

    public void saveListData(ArrayList<String> msgs, Context context) {
        ListIterator<String> ir = msgs.listIterator();
        while(ir.hasNext()){
            String[] arr_msg = ir.next().split( "%%" );

            SharedPreferences sp = context.getSharedPreferences(STORAGE_FILE, Context.MODE_PRIVATE );
            SharedPreferences.Editor e = sp.edit( );
            e.putString( arr_msg[0]+ "&" + arr_msg[3] , arr_msg[1] );
            e.apply( );

        }


    }



    public String convertMapToString(HashMap<String, String> h) {

        ArrayList<String> arr = new ArrayList<String>();
        Iterator it = h.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = (Map.Entry) it.next();
            String temp = pair.getKey().split("&")[0] + "%%" + pair.getValue() +
                    "%%" + "get" + "%%" + pair.getKey().split("&")[1];

            arr.add(temp);
            it.remove();
        }


        return TextUtils.join("!", arr);


    }

    public String getLocalDataByKey(String key, Context context) {

        HashMap<String, String> h = new HashMap<String, String>();

        SharedPreferences sharedPref = context.getSharedPreferences(STORAGE_FILE, Context.MODE_PRIVATE);
        String str;
        Map<String, ?> keys = sharedPref.getAll();

        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            String temp = entry.getKey().split( "&" )[0];
            if (temp.equals( key )) {
                h.put( entry.getKey( ), entry.getValue( ).toString( ) );

            }

        }

            return convertMapToString( h );




    }


    public void delLocalDataByKey(String key, Context context) {

        HashSet<String> h = new HashSet<String>();

        SharedPreferences sp = context.getSharedPreferences(STORAGE_FILE, Context.MODE_PRIVATE);
        Map<String, ?> keys = sp.getAll();

        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            String temp = entry.getKey().split( "&" )[0];
            if (temp.equals( key )) {
                h.add( entry.getKey( ) );
            }

        }

        SharedPreferences.Editor e = sp.edit( );
        Iterator<String> it = h.iterator();

        while(it.hasNext()) {
            e.remove( it.next() );
        }
        e.apply();

    }





    public void delAllLocalData( Context context) {


        SharedPreferences sp = context.getSharedPreferences(STORAGE_FILE, Context.MODE_PRIVATE);

        SharedPreferences.Editor e = sp.edit( );
        e.clear();
        e.apply();

    }


    public HashMap<String, String> stringToKeyValue(String msgs){
        HashMap<String, String> hm = new HashMap<String, String>(  );
        String[] arr = msgs.split( "!" );
        ArrayList<Message> temp_arr = new ArrayList<Message>(  );
        for(String m : arr){
            Message t = new Message();

            String[] arr_m = m.split( "%%" );
            // key&version%%value%%get%%version
            t.setKey( arr_m[0] );
            t.setVersion( arr_m[3] );
            t.setValue( arr_m[1] );
            temp_arr.add( t );
        }

        Collections.sort( temp_arr );

        for (Message m : temp_arr) {
            hm.put( m.getKey(), m.getValue() );
        }

        return hm;
    }


    public String getLocalData(Context context) {

        HashMap<String, String> hm = new HashMap<String, String>();

        SharedPreferences sharedPref = context.getSharedPreferences(STORAGE_FILE, Context.MODE_PRIVATE);

        Map<String, ?> keys = sharedPref.getAll();

        for (Map.Entry<String, ?> entry : keys.entrySet()) {

            hm.put(entry.getKey(), entry.getValue().toString());

        }

            return convertMapToString( hm );


    }

    public String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }


    public HashSet<String> getPreferenceList( Message msg, TreeMap<String,String> ring ) throws NoSuchAlgorithmException{
        String key = msg.getKey();

            String hashKey2 = genHash( key );
            String a2 = ring.higherKey( hashKey2 );
            if (a2 == null) {
                a2 = ring.firstKey( );
            }
            String firstNode = ring.get( a2 );



            String hashKey = genHash( firstNode );
            String a = ring.higherKey( hashKey );
            if (a == null) {
                a = ring.firstKey( );
            }
             String secondNode = ring.get( a );


            String hashKey1 = genHash( secondNode );
            String a1 = ring.higherKey( hashKey1 );
            if (a1 == null) {
                a1 = ring.firstKey( );
            }
            String thirdNode = ring.get( a1 );

            HashSet<String> prefList = new HashSet<String>();
            prefList.add( firstNode );
            prefList.add( secondNode );
            prefList.add( thirdNode );
            return prefList;


    }









}

