package org.kiwix.kiwixmobile.webserver;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import org.kiwix.kiwixmobile.R;

public class WebServerHelper extends Activity {
  private static Context context;
  private static TextView textViewIpAccess;
  private static EditText editTextPort;
  public static boolean isStarted;
  private static int port;
  private static WebServer webServer;
  private static final int DEFAULT_PORT = 8080;
  private static CoordinatorLayout coordinatorLayout;

  public WebServerHelper(Context context) {
    this.context = context;
  }

  public static void startServerDialog() {
    AlertDialog.Builder alert = new AlertDialog.Builder(context);
    alert.setTitle("Start the server");
    alert.setMessage("Happy sharing");

    LinearLayout layout = new LinearLayout(context);
    layout.setOrientation(LinearLayout.HORIZONTAL);

    textViewIpAccess = new TextView(context);
    textViewIpAccess.setText("http://000.000.000.000");
    textViewIpAccess.setTextSize(20);
    layout.addView(textViewIpAccess);

    TextView colonTextView = new TextView(context);
    colonTextView.setTextSize(20);
    colonTextView.setText(":");
    layout.addView(colonTextView);

    editTextPort = new EditText(context);
    editTextPort.setInputType(InputType.TYPE_CLASS_NUMBER);
    editTextPort.setHint(R.string.port_hint);
    editTextPort.setText(R.string.port_hint);
    editTextPort.setFilters(new InputFilter[] { new InputFilter.LengthFilter(4) });
    editTextPort.setTextSize(20);
    layout.addView(editTextPort);

    alert.setView(layout);

    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        if (!isStarted && startAndroidWebServer()) {
          isStarted = true;
          editTextPort.setEnabled(false);
        } else if (stopAndroidWebServer()) {
          isStarted = false;
          editTextPort.setEnabled(true);
        }
      }
    });

    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        // Canceled.
      }
    });
    stopAndroidWebServer();
    isStarted = false;
    alert.show();

    setIpAccess();
  }

  public static boolean stopAndroidWebServer() {
    if (isStarted && webServer != null) {
      webServer.stop();
      return true;
    }
    return false;
  }

  private static boolean startAndroidWebServer() {
    if (!isStarted) {
      port = getPortFromEditText();
      try {
        if (port == 0) {
          throw new Exception();
        }
        webServer = new WebServer(port);
        webServer.start();
        return true;
      } catch (Exception e) {
        e.printStackTrace();
        Snackbar.make(coordinatorLayout,
            "The PORT " + port + " doesn't work, please change it between 1000 and 9999.",
            Snackbar.LENGTH_LONG).show();
      }
    }
    return false;
  }

  private static int getPortFromEditText() {
    String valueEditText = editTextPort.getText().toString();
    return (valueEditText.length() > 0) ? Integer.parseInt(valueEditText) : DEFAULT_PORT;
  }

  private static void setIpAccess() {
    textViewIpAccess.setText(getIpAddress());
  }

  // get Ip address of the device's wireless access point i.e. wifi hotspot OR wifi network
  private static String getIpAddress() {
    Log.v("DANG", "Inside getIpAdress()");
    String ip = "";
    try {
      Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
          .getNetworkInterfaces();
      while (enumNetworkInterfaces.hasMoreElements()) {
        NetworkInterface networkInterface = enumNetworkInterfaces
            .nextElement();
        Enumeration<InetAddress> enumInetAddress = networkInterface
            .getInetAddresses();
        while (enumInetAddress.hasMoreElements()) {
          InetAddress inetAddress = enumInetAddress.nextElement();

          if (inetAddress.isSiteLocalAddress()) {
            ip += inetAddress.getHostAddress() + "\n";
          }
        }
      }
    } catch (SocketException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      ip += "Something Wrong! " + e.toString() + "\n";
    }
    Log.v("DANG", "Returning : " + "http://" + ip);
    return "http://" + ip;
  }
}
