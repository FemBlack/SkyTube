package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
 
public class MySQLiteHelper extends SQLiteOpenHelper {
 
    // Database Version
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "VibeDB";
    
    private static MySQLiteHelper mInstance = null;
    
    public static MySQLiteHelper getInstance(Context ctx) {

        // Use the application context, which will ensure that you 
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
          mInstance = new MySQLiteHelper(ctx.getApplicationContext());
        }
        return mInstance;
      }

	/**
	 * Constructor should be private to prevent direct instantiation. make call
	 * to static factory method "getInstance()" instead.
	 */
	private MySQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_IAP_TABLE = "CREATE TABLE iap ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
                "skus TEXT" + " )";

        db.execSQL(CREATE_IAP_TABLE);
    }
 
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS iap");
 
        this.onCreate(db);
    }
    //---------------------------------------------------------------------
 
    /**
     * CRUD operations (create "add", read "get", update, delete) book + get all books + delete all books
     */
 
    // Books table name

    private static final String TABLE_IAP = "iap";
    private static final String KEY_SKUS = "skus";
 

    public void addIAP(String skus){
        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
 
        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_SKUS,skus); // get title 
 
        // 3. insert
        db.insert(TABLE_IAP, // table
                null, //nullColumnHack
                values); // key/value -> keys = column names/ values = column values
 
        // 4. close
        db.close(); 
        
    }
    

 // Get All Books
    public long getAllIAP() {
    	long count  = 0;
        // 1. build the query
        String query = "SELECT COUNT(*) FROM " + TABLE_IAP;
 
        // 2. get reference to writable DB
        SQLiteDatabase db = this.getReadableDatabase();
        SQLiteStatement s = db.compileStatement(query);
 
        count = s.simpleQueryForLong();
        
        return count;
    }
 
}
