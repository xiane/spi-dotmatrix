package odroid.hardkernel.com.spi_matrix;

import androidx.appcompat.app.AppCompatActivity;
import odroid.hardkernel.com.dotMatrix.DotMatrix;

import android.os.Bundle;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    final static String START_STR = "Hello, ODROID!";
    final static int SPI_MAX_SPEED = 5000000;

    public String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }

    public String getDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
        return dateFormat.format(Calendar.getInstance().getTime());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Runnable matrixRunnable = new Runnable() {
            @Override
            public void run() {
                try {

                    final DotMatrix dotMatrix = new DotMatrix(SPI_MAX_SPEED);
                    int sequence = 0;

                    dotMatrix.setFirstDelay(1000);
                    dotMatrix.setScrollSpeed(10);
                    while (true) {
                        String displayChars;
                        if (sequence++ % 2 == 0)
                            displayChars = START_STR + getIPAddress(true);
                        else
                            displayChars = START_STR + getDate();

                        dotMatrix.display(displayChars);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(matrixRunnable).start();
    }
}
