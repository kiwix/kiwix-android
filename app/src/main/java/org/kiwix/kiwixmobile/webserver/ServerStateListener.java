package org.kiwix.kiwixmobile.webserver;

public interface ServerStateListener {
  
  void serverStarted(String ip);

  void serverStopped();
}
