package org.kiwix.kiwixmobile.webserver;

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

import static org.kiwix.kiwixmobile.utils.StyleUtils.dialogStyle;

/**
 * WebServerHelper class is used to set up the suitable environment i.e. getting the
 * ip address and port no. before starting the WebServer
 * Created by Adeel Zafar on 18/07/2019.
 */

public class WebServerHelper {
  private Context context;
  private TextView textViewIpAccess;
  private EditText editTextPort;
  public static boolean isStarted;
  private int port;
  private static WebServer webServer;
  private CoordinatorLayout coordinatorLayout;

  public WebServerHelper(Context context) {
    this.context = context;
  }
  
  //Dialog to start the server where user is shown the hotspot ip address can edit the port no.
  public void startServerDialog() {
    AlertDialog.Builder alert = new AlertDialog.Builder(context);
    alert.setTitle(R.string.start_server_dialog_title);
    alert.setMessage(R.string.start_server_dialog_message);

    LinearLayout layout = new LinearLayout(context);
    layout.setOrientation(LinearLayout.HORIZONTAL);

    coordinatorLayout = new CoordinatorLayout(context);

    textViewIpAccess = new TextView(context);
    textViewIpAccess.setText(context.getString(R.string.sample_ip_address));
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

    alert.setPositiveButton("START SERVER", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        if (!isStarted && startAndroidWebServer()) {
          isStarted = true;
          serverStartedDialog();
        }
      }
    });

    alert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        // Canceled.
      }
    });

    alert.show();

    setIpAccess();
  }

  public static boolean stopAndroidWebServer() {
    if (isStarted && webServer != null) {
      webServer.stop();
      isStarted = false;
      return true;
    }
    return false;
  }

  boolean startAndroidWebServer() {
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

  private int getPortFromEditText() {
    String valueEditText = editTextPort.getText().toString();
    int DEFAULT_PORT = 8080;
    return (valueEditText.length() > 0) ? Integer.parseInt(valueEditText) : DEFAULT_PORT;
  }

  private void setIpAccess() {
    textViewIpAccess.setText(getIpAddress());
  }

  // get Ip address of the device's wireless access point i.e. wifi hotspot OR wifi network
  private String getIpAddress() {
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
      //To remove extra characters from IP for Android Pie
      if (ip.length() > 14) {
        for (int i = 15, j = 12; i < 18; i++, j++) {
          if ((ip.charAt(i) == '.')) {
            ip = ip.substring(0, j + 1);
            break;
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

  //Once server is started successfully, this dialog is shown.
  void serverStartedDialog() {

    AlertDialog.Builder builder = new AlertDialog.Builder(context, dialogStyle());
    builder.setPositiveButton(android.R.string.ok, (dialog, id) -> {

    });
    builder.setTitle(context.getString(R.string.server_started_title));
    builder.setMessage(
        context.getString(R.string.server_started_message) + "\n " + getIpAddress() + ":" + port);
    AlertDialog dialog = builder.create();
    dialog.show();
  }
}
