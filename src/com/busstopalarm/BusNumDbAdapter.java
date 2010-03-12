/**
 * Author: Huy Dang
 * Date: 03/02/2010
 * 
 * Simple database access helper class. The Database contains all
 * valid bus numbers which are in Seattle area. This database is used to 
 * validate the input.
 */
package com.busstopalarm;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class BusNumDbAdapter {
	
	private final Context mCtx;
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
	
	/*
	 * busnum TABLE KEYS
     * _id: the id is generated by system to keep track of entry (Primary key)
     * route_id: the bus route id
	 */
    private static final String DATABASE_TABLE_DEST = "busnum";
    private static final String TAG = "BusNumDbAdapter";
    private static final String DATABASE_NAME = "data";
    private static final int DATABASE_VERSION = 2;
	
    // Database numbers. (We only have two, so no need for enum)
    public static final int KINGCOUNTY_DB = 0;
    public static final int SOUNDTRANSIT_DB = 1;
    
    /**
     * Database Query Statements
     */
    private static final String DATABASE_CREATE_DEST=
        "CREATE TABLE "
        + " destination (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + " route_id TEXT NOT NULL, route_desc TEXT NOT NULL, " 
        + " stop_id TEXT NOT NULL, stop_desc TEXT NOT NULL, " 
        + " count INTEGER NOT NULL, time TEXT NOT NULL, "
        + " major INTEGER NOT NULL);";
    
    private static final String DATABASE_CREATE_BUSNUM=
            "CREATE TABLE "
            + " busnum  (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + " route_id TEXT NOT NULL); ";

    private static final String DATABASE_INSERT_DEST =
    		" INSERT INTO busnum (route_id) VALUES (?);";	
    	
    private static final String DATABASE_RETRIEVE_BY_ROUTEID =
    		" SELECT * FROM busnum WHERE route_id = ?;";
    
    private static final String DATABASE_RETRIEVE_BY_ROWID =
    		" SELECT * FROM busnum WHERE _id = ? ;";
          	
    private static final String DATABASE_FETCH_ALL_BUS =
    		" SELECT * FROM busnum ;";
        
    private static final String DATABASE_FETCH_ALL_BUS_LIMIT =
			" SELECT * FROM busnum LIMIT ?  ;";

    /**
     * Private class DatabaseHelper is used for create/initialize the table
     * at the begin and help to update to new version
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	db.execSQL(DATABASE_CREATE_DEST);
        	db.execSQL(DATABASE_CREATE_BUSNUM);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, 
        					  int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS destination");
            db.execSQL("DROP TABLE IF EXISTS busnum ");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public BusNumDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Opens the data database. If it cannot be opened, try to create a 
     * new instance of the database. If it cannot be created, throw an exception 
     * to signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public BusNumDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    /**
     * Closes the busnum database. If there's no current database running,
     * simply do nothing
     */
    public void close() {
        mDbHelper.close();
    }

    /**
     * Deletes all bus entries from the busnum table
     * 
     * @return true if all bus entries deleted, false otherwise
     */
    public boolean deleteAllBusEntries() {
        return mDb.delete(DATABASE_TABLE_DEST, null, null) > 0;
    }

    /**
     * Creates a new entry using the route_id. This is private function to 
     * initialize busnum table by reading well formatted text file.
     */
    private int createBusEntry(String route_id) {
    	 	
    	/*
    	 * Check if the entry is already in the table
    	 */
    	if (checkIfBusEntryExist(route_id)) {
    		return 0;
    	}
    	String[] args = {route_id};
    	Cursor mCursor = mDb.rawQuery(DATABASE_INSERT_DEST, args);
    	
    	if (mCursor == null) {
    		return 0;
    	}
    	return mCursor.getCount();
    }
   
    /**
     * Returns a cursor over the list of all entries in busnum table
     * 
     * @return Cursor over all bus entries
     */
    public Cursor fetchAllBusEntries() {
    	return mDb.rawQuery(DATABASE_FETCH_ALL_BUS, new String[]{});
    }
    
    /**
     * Returns a list of bus routes in the database as a list of integers, 
     * instead of a Cursor. The list is read-only.
     * 
     * @return A sorted, immutable list of integers of available bus routes.
     * 
     * @author Derek Cheng
     */
    public List<Integer> getBusRoutesList() {
    	
    	// Get the cursor, and read all of its entries.
    	Cursor c = fetchAllBusEntries();
    	Log.v(TAG, "Total entries in result:" + c.getCount() + "");
    	List<Integer> list = new ArrayList<Integer>();
    	c.moveToFirst();
    	
    	// Which column is route_id?
    	int column = c.getColumnIndex("route_id");
    	// Cannot find the column corresponding to "route_id", so return 
    	// an empty list.
    	if (column < 0) {
    		return Collections.unmodifiableList(list);
    	}
    	
    	for (int i = 0; i < c.getCount(); i++) {
    		String routeS = null;
    		try {
    			routeS = c.getString(column);
    			int route = Integer.parseInt(routeS);
    			Log.v(TAG, "got route number: " + route+"");
    			list.add(route);
    		} catch (NumberFormatException e) {
    			Log.v(TAG, "got route number FAIL " + routeS);
    			// do nothing with improperly formatted entries.
    		}
    		c.moveToNext();
    	}
    	
    	Collections.sort(list);
    	return Collections.unmodifiableList(list);
    }
    
    /**
     * Returns the number of rows in the current busnum table
     * 
     * @return number of rows in the busnum table
     */
    public int getBusNumTbSize() {
    	Cursor mCursor = mDb.rawQuery(DATABASE_FETCH_ALL_BUS, new String[]{});
    	
    	if (mCursor == null) {
    		return -1;
    	}
    	
    	return mCursor.getCount();
    }
  
   /**
    * Checks if a entry is already in the database based on route_id
    * 
    * @param route_id Id of the bus route
    * @return true if the entry is already in the table. false otherwise.
    */
   	public boolean checkIfBusEntryExist(String route_id) {
   		if (route_id == null) { 
   			return false;
   		}
   		
   		Cursor mCursor = null;
   		mCursor = mDb.rawQuery(DATABASE_RETRIEVE_BY_ROUTEID, 
   							   new String[]{route_id});

   		if (mCursor.getCount() != 0) {
               return true;
   		}
   		
        return false;
   	}
   	   	
   	/**
   	 * Private function to return specific number of bus entries.
   	 * Using those wrapper classes above instead.
   	 * 
   	 * @param limit number of entries to return
   	 * @return the Cursor of the list of returned entries
   	 */
   	public Cursor getListBusNum(int limit) {
   		
   		Cursor mCursor = mDb.rawQuery(DATABASE_FETCH_ALL_BUS_LIMIT, 
   							          new String[]{Integer.toString(limit)});
   		
   		if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
   	}
   	
   	/**
   	 * Reads the text file in /res/raw/ folder to create the new database for
   	 * bus route number. This is used to validate bus input.
   	 
   	 * @param testFlag reads kingcounty file if the flag value is 0
   	 * 				   reads soundtransit file if the the flag value is 1
   	 * 				   
   	 * @return True if reading the file and put new entries to 
   	 * 		   database successfully. False if not reading the file at all.
   	 */
   	public boolean readDbFile(int testFlag) throws IOException, 
   												   FileNotFoundException {
   		
   		InputStream in=null;
   		if (testFlag == KINGCOUNTY_DB) {
   			in = mCtx.getResources().openRawResource(R.raw.kingcounty);
   		} else if (testFlag == SOUNDTRANSIT_DB) {
   			in = mCtx.getResources().openRawResource(R.raw.soundtransit);
   		} 
   		
  		BufferedReader bin = new BufferedReader(new InputStreamReader(in));
  		if (bin == null) {
  			return false;
  		}
  		
  		String line;
  		String result;
  		while (true) {
    		line = bin.readLine();
  			if (line == null) {
  				break;
  			}
  			
    		result = line.toString();
    		Log.v("BusNumDbAdapter 1st", result);
    		int num = createBusEntry(result);
    		
    		Log.v("BusNumDBAdapter, test Entry", num + " .");
    	}
  		bin.close();
  		return true;
   	}

}