package com.slidenerd.exmples.three.dbase;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sp =getSharedPreferences("counter", Context.MODE_PRIVATE);
        int presentCounter = sp.getInt("counter",0);

        if(presentCounter == 0){

            String stringUrl = "https://techcards.wordpress.com";
            Fetch fetch = new Fetch();
            fetch.execute(stringUrl);
            Toast.makeText(this,"this is first use & counter value is " + presentCounter ,Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this,"Data already loaded,present counter is "+ presentCounter ,Toast.LENGTH_SHORT).show();

        }


        SharedPreferences sharedPreferences =getSharedPreferences("counter", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("counter",presentCounter+1);
        editor.commit();


    }
    protected void onResume(){
        super.onResume();
        DataAdapter dataAdapter = new DataAdapter(this);
        try {
            dataAdapter.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        List dataList = dataAdapter.getAllArticles();

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setAdapter(new ViewAdapter(dataList));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());




    }

    private class Fetch extends AsyncTask<String, Void, Element> {

        @Override
        protected Element doInBackground(String... params) {
            Element mainContent = null;

            //TODO: before downloading ,check for network connection ,if not available respond pleasantly .
            try {
                // Connect to the web site
                Document doc = Jsoup.connect(params[0]).get();
                mainContent =doc.getElementById("content").getElementById("primary").
                        getElementById("main");

            } catch (IOException e) {
                e.printStackTrace();
            }

            return mainContent;
        }

        protected void onPostExecute(Element result) {

            Elements art = result.getElementsByClass("entry-summary");
            Elements title = result.getElementsByClass("entry-title");

            ArrayList<String> summery = new ArrayList<String>();
            ArrayList<String> links = new ArrayList<String>();
            ArrayList<String> titles = new ArrayList<String>();

            try{
                for (int i =0;i<art.size() ;i++ ) {

                    Log.v("links",art.get(i).getElementsByTag("a").get(0).absUrl("href"));

                    links.add(i,art.get(i).getElementsByTag("a").get(0).absUrl("href"));
                    summery.add(i,art.get(i).getElementsByTag("p").text());
                    titles.add(i,title.get(i).text());
                }

            }catch(NullPointerException e){
                e.printStackTrace();
                System.out.println("art is null");
            }
            Toast.makeText(MainActivity.this,"data is being written to DB",Toast.LENGTH_SHORT).show();
            //writing data to DB
            DataAdapter dataAdapter = new DataAdapter(MainActivity.this);
            try {
                dataAdapter.open();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            //when ever u r downloading from internet,first delete data and then insert otherwise u will get many duplicates
            //this is not very efficient b'coz when articles are updated to 4 from 3 ,instead of downloading 4th we are downloading all 4
            dataAdapter.deleteAndCreateNewTable();

            for(int i =0;i<art.size() ;i++ ){

                Log.v("links in data base",links.get(i));
                dataAdapter.insertData(titles.get(i),summery.get(i),links.get(i));
            }

            dataAdapter.close();



        }

    }

    public class DataModel{
        private long id;
        private String title;
        private String summery;
        private String link;

        public String getTitle(){
            return title;
        }
        public String getSummery(){
            return summery;
        }
        public String getLink(){
            return link;
        }
        public long getId(){
            return id;
        }

        public void setTitle(String title){
            this.title = title;
        }
        public void  setSummery(String summery){
            this.summery = summery;
        }
        public void setLink(String link){
            this.link = link;
        }
        public void setId(Long id){
            this.id = id;
        }
    }

    public class ViewAdapter extends RecyclerView.Adapter<MyViewHolder>{
        List<DataModel> dataList;
        public ViewAdapter(List<DataModel> dataList){
            this.dataList = dataList;

        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int i) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view, parent, false);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(MyViewHolder myViewHolder, int i) {
           final DataModel dataModel = dataList.get(i);
            myViewHolder.title.setText(dataModel.getTitle());
            myViewHolder.summery.setText(dataModel.getSummery());

           myViewHolder.readMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(MainActivity.this,Read.class);
                    i.putExtra("title",dataModel.getLink());
                    startActivity(i);

                }
            });
        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{

        public TextView title;
        public TextView summery;
        public Button readMore;

        public MyViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.textView1);
            summery = (TextView) itemView.findViewById(R.id.textView2);
            readMore = (Button) itemView.findViewById(R.id.button1);

        }
    }


    public class MySQLiteHelper extends SQLiteOpenHelper {

        public static final String TABLE_ARTICLES = "articles";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_SUMMERY= "summery";
        public static final String COLUMN_LINK = "link";
        private static final String DATABASE_NAME = "articles.db";
        private static final int DATABASE_VERSION = 1;

        // Database creation sql statement
        private static final String DATABASE_CREATE = "create table "
                + TABLE_ARTICLES + "(" + COLUMN_ID
                + " integer primary key autoincrement, " + COLUMN_TITLE
                + " text not null, "+COLUMN_SUMMERY
                + " text not null, "+COLUMN_LINK
                + " text not null);";

        public MySQLiteHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(MySQLiteHelper.class.getName(),
                    "Upgrading database from version " + oldVersion + " to "
                            + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ARTICLES);
            onCreate(db);
        }

    }

    public class DataAdapter {

        // Database fields
        private SQLiteDatabase database;
        private MySQLiteHelper dbHelper;
        private String[] allColumns = { MySQLiteHelper.COLUMN_ID,
                MySQLiteHelper.COLUMN_TITLE,MySQLiteHelper.COLUMN_SUMMERY,MySQLiteHelper.COLUMN_LINK};

        public DataAdapter(Context context) {
            dbHelper = new MySQLiteHelper(context);
        }
        //open data base to create database obj when ever u r a doing db operation
        public void open() throws SQLException {
            database = dbHelper.getWritableDatabase();
        }
        //close ,after bd operation
        public void close() {
            dbHelper.close();
        }

        public void insertData(String title, String summery, String link) {
            ContentValues values = new ContentValues();
            values.put(MySQLiteHelper.COLUMN_TITLE, title);
            values.put(MySQLiteHelper.COLUMN_SUMMERY, summery);
            values.put(MySQLiteHelper.COLUMN_LINK, link);

             database.insert(MySQLiteHelper.TABLE_ARTICLES, null,
                    values);

        }
        public void deleteAndCreateNewTable(){
            dbHelper.onUpgrade(database,MySQLiteHelper.DATABASE_VERSION,MySQLiteHelper.DATABASE_VERSION + 1);
        }


        public List<DataModel> getAllArticles() {
            List<DataModel> articles = new ArrayList<DataModel>();

            Cursor cursor = database.query(MySQLiteHelper.TABLE_ARTICLES,
                    allColumns, null, null, null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                DataModel dataModel = cursorToDataModel(cursor);
                articles.add(dataModel);
                cursor.moveToNext();
            }
            // make sure to close the cursor
            cursor.close();
            return articles;
        }


        private DataModel cursorToDataModel(Cursor cursor) {
            DataModel dataModel = new DataModel();

            dataModel.setId(cursor.getLong(0));
            dataModel.setTitle(cursor.getString(1));
            dataModel.setSummery(cursor.getString(2));
            dataModel.setLink(cursor.getString(3));
            return dataModel;
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        if(id == R.id.refresh){
            String stringUrl = "https://techcards.wordpress.com";
            Fetch fetch = new Fetch();
            fetch.execute(stringUrl);
            onResume();
        }
        return super.onOptionsItemSelected(item);
    }




}
