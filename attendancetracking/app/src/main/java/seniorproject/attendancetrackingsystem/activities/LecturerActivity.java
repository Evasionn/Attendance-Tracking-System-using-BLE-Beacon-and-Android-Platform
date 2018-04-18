package seniorproject.attendancetrackingsystem.activities;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import seniorproject.attendancetrackingsystem.R;
import seniorproject.attendancetrackingsystem.fragments.ReportFragmentLecturer;
import seniorproject.attendancetrackingsystem.fragments.WelcomeFragmentLecturer;
import seniorproject.attendancetrackingsystem.helpers.BeaconBuilder;
import seniorproject.attendancetrackingsystem.helpers.DatabaseManager;
import seniorproject.attendancetrackingsystem.helpers.SessionManager;

public class LecturerActivity extends AppCompatActivity {
  private Receiver mReceiver;
  private BeaconBuilder beaconBuilder;
  private boolean mServiceBound = false;
  private BottomNavigationView mainNav;
  private AlertDialog alertDialog;
  private ProgressDialog progressDialog;
  private WelcomeFragmentLecturer welcomeFragmentLecturer;
  private ReportFragmentLecturer reportFragmentLecturer;
  private ServiceConnection serviceConnection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
          BeaconBuilder.ServiceBinder binder = (BeaconBuilder.ServiceBinder) service;
          beaconBuilder = binder.getService();
          mServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
          mServiceBound = false;
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    SessionManager session = new SessionManager(getApplicationContext());
    session.checkLogin();
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_lecturer);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    mainNav = findViewById(R.id.main_nav);
    welcomeFragmentLecturer = new WelcomeFragmentLecturer();
    reportFragmentLecturer = new ReportFragmentLecturer();

    setFragment(welcomeFragmentLecturer);
    Objects.requireNonNull(getSupportActionBar()).setLogo(R.drawable.kdefault);
    getSupportActionBar().setTitle("Ç.Ü. Attendance Tracking System");
    getSupportActionBar().setSubtitle("/Home");
    mainNav.setOnNavigationItemSelectedListener(
        new BottomNavigationView.OnNavigationItemSelectedListener() {
          @Override
          public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
              case R.id.nav_home:
                setFragment(welcomeFragmentLecturer);
                Objects.requireNonNull(getSupportActionBar()).setLogo(R.drawable.kdefault);
                getSupportActionBar().setTitle("Ç.Ü. Attendance Tracking System");
                getSupportActionBar().setSubtitle("/Home");
                break;

              case R.id.nav_report:
                setFragment(reportFragmentLecturer);
                Objects.requireNonNull(getSupportActionBar()).setLogo(R.drawable.kdefault);
                getSupportActionBar().setTitle("Ç.Ü. Attendance Tracking System");
                getSupportActionBar().setSubtitle("/Report");
                break;

              case R.id.logout:
                SessionManager session = new SessionManager(getApplicationContext());
                session.logoutUser();
                break;
              default:
                break;
            }
            return true;
          }
        });
    alertDialog = new AlertDialog.Builder(this).create();
    alertDialog.setCanceledOnTouchOutside(false);
    progressDialog = new ProgressDialog(this);
    progressDialog.setCanceledOnTouchOutside(false);
  }

  private void setFragment(Fragment fragment) {
    FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
    fragmentTransaction.replace(R.id.main_frame, fragment);
    fragmentTransaction.commit();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.toolbar_menu_lecturer, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    setFinishOnTouchOutside(false);
    if (item.toString().equals("Beacon Configuration")) {
      showProgressDialog();
      Intent intent = new Intent(this, BeaconBuilder.class);
     // startService(intent);
      bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
      // new BeaconBuilder();
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onBackPressed() {
    setFragment(welcomeFragmentLecturer);
    mainNav.setSelectedItemId(R.id.nav_home);
  }
private void showProgressDialog(){
    progressDialog.setTitle("Beacon syncronizer");
    progressDialog.setMessage("Searching nearyby beacons");
    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        BluetoothAdapter.getDefaultAdapter().disable();
        if(mServiceBound){
          unbindService(serviceConnection);
          mServiceBound = false;
        }
      }
    });
    progressDialog.show();
}
  private void showAlertDialog(final String mac) {
    progressDialog.hide();
    alertDialog.setTitle("Found Beacon");
    alertDialog.setMessage("MAC: " + mac);
    alertDialog.setButton(
        DialogInterface.BUTTON_NEGATIVE,
        "Ignore",
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            beaconBuilder.addToIgnoreList(mac);
            beaconBuilder.continueTracking();
            showProgressDialog();
          }
        });
    alertDialog.setButton(
        DialogInterface.BUTTON_POSITIVE,
        "Save",
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            Map<String, String> postParameters = new HashMap<>();
            postParameters.put("beacon_mac", mac);
            postParameters.put(
                "user_id",
                new SessionManager(getApplicationContext())
                    .getUserDetails()
                    .get(SessionManager.KEY_USER_ID));
            DatabaseManager.getmInstance(getApplicationContext())
                .execute("set-beacon", postParameters);
            BluetoothAdapter.getDefaultAdapter().disable();
            if(mServiceBound){
              unbindService(serviceConnection);
              mServiceBound = false;
            }
          //  stopService(new Intent(LecturerActivity.this, BeaconBuilder.class));
          }
        });

    alertDialog.show();
  }

  @Override
  protected void onStop() {
    super.onStop();
    if(mServiceBound){
      unbindService(serviceConnection);
      mServiceBound = false;
    }
    progressDialog.hide();
    alertDialog.hide();
    unregisterReceiver(mReceiver);
    Log.i("reciever", "unregistered");
  }

  @Override
  protected void onStart() {
    super.onStart();
    mReceiver = new Receiver();
    IntentFilter filter = new IntentFilter();
    filter.addAction(BeaconBuilder.ACTION);
    registerReceiver(mReceiver, filter);
    Log.i("receiver", "registered");
  }

  private class Receiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      showAlertDialog(intent.getStringExtra("MAC"));
    }
  }
}
