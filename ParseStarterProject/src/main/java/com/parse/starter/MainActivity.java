package com.parse.starter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class MainActivity extends AppCompatActivity {
  Switch choice;
  String userType;

  public void redirectActivity(){
      if(ParseUser.getCurrentUser().getString("typeOfUser").equals("Rider")){
          Intent intent = new Intent(getApplicationContext(),  RiderActivity.class);
          startActivity(intent);
      } else{
          Intent intent = new Intent (getApplicationContext(), ViewRequestsActivity.class);
          startActivity(intent);
      }

  }

  public void getStarted(View view) {
    choice = (Switch) findViewById(R.id.choice);

//      Toast.makeText(MainActivity.this, String.valueOf(choice.isChecked()), Toast.LENGTH_SHORT).show();
    if (choice.isChecked()) {
      userType = "Driver";
    } else if (!choice.isChecked()) {
      userType = "Rider";
    }
    ParseUser.getCurrentUser().put("typeOfUser", userType);
      Toast.makeText(MainActivity.this, "Logging as" + userType.toString(), Toast.LENGTH_SHORT).show();
      ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
          @Override
          public void done(ParseException e) {
              redirectActivity();

          }
      });
  }


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    getSupportActionBar().hide();


    if (ParseUser.getCurrentUser() == null) {
      ParseAnonymousUtils.logIn(new LogInCallback() {
        @Override
        public void done(ParseUser user, ParseException e) {
          if (e == null) {
            Log.i("lechakk", "success");
          } else {
            Log.i("ohsh!", "galt hogya");

          }
        }
      });
    } else if (ParseUser.getCurrentUser().get("typeOfUser") != null) {
      Toast.makeText(MainActivity.this, "User Already set as," +
              ParseUser.getCurrentUser().get("typeOfUSer"), Toast.LENGTH_SHORT).show();
        redirectActivity();
    }


    ParseAnalytics.trackAppOpenedInBackground(getIntent());
  }

}