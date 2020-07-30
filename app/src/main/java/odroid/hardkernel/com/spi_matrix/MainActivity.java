package odroid.hardkernel.com.spi_matrix;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    final static int SPI_MAX_SPEED = 500000;
    final static int SHIFT_DELAY_MS = 30;
    final static int DISPLAY_DELAY_MS = 500;
    final static int MAX_SHIFT_CHAR_COUNT = 256;
    final static int DOT_MATRIX_8X8_COUNT = MAX_SHIFT_CHAR_COUNT;

    final static String START_STR = "Hello, ODROID!";

    private byte DotMatrixFb[][] = new byte[DOT_MATRIX_8X8_COUNT][8];

    private byte dotled_init[][]= new byte[][]{
            {0x09, 0x00, 0x09, 0x00},
            {0x0a, 0x03, 0x0a, 0x03},
            {0x0b, 0x07, 0x0b, 0x07},
            {0x0c, 0x01, 0x0c, 0x01},
            {0x0f, 0x00, 0x0f, 0x00},
    };

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
        } catch (Exception ex) { } // for now eat exceptions
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

        dotmatrix_init();

        Runnable matrixRunnable = new Runnable() {
            @Override
            public void run() {
                int shift_len;

                int sequence = 0;

                while(true) {

                    String displayChars;
                    if (sequence++ % 2 == 0)
                        displayChars= START_STR + getIPAddress(true);
                    else
                        displayChars = START_STR + getDate();

                    shift_len = dotmatrix_buffer_update(displayChars.toCharArray());

                    while(shift_len-- > 0) {
                        dotmatrix_buffer_shift();
                    }
                }
            }
        };
        new Thread(matrixRunnable).start();
    }
    class dotMatrix {
        private SpiDevice spi;

        public dotMatrix() {
            PeripheralManager manager = PeripheralManager.getInstance();
        }
    }

    private SpiDevice spi;

    private void dotmatrix_init() {
        PeripheralManager manager = PeripheralManager.getInstance();

        try {
            spi = manager.openSpiDevice("SPI0.0");
            spi.setFrequency(SPI_MAX_SPEED);
            spi.setBitsPerWord(8);
            spi.setDelay(0);
            spi.setCsChange(false);
            spi.setMode(SpiDevice.MODE2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            for (int i = 0; i < 5; i++)
                spi.write(dotled_init[i], 4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int dotmatrix_buffer_update(char[] buffer) {
        int i, buf_pos = 0, bits_count = 0;
        for(char ch: buffer) {
            if (ch == 0)
                break;

            for(i=0; i < 8; i++) {
                int convert_ch = (int)ch;
                DotMatrixFb[buf_pos][i] = font.VINCENT_FONT[convert_ch][i];
                bits_count++;
            }
            buf_pos++;
        }

        dotmatrix_update();

        try {
            Thread.sleep(DISPLAY_DELAY_MS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bits_count;
    }

    private void dotmatrix_buffer_shift() {
        int i, j;
        for (j = 0; j < DOT_MATRIX_8X8_COUNT; j++) {
            for (i = 0; i < 8; i++) {
                DotMatrixFb[j][i] <<= 1;
                if ((j != DOT_MATRIX_8X8_COUNT - 1)
                        && ((DotMatrixFb[j + 1][i] & 0x80) != 0))
                    DotMatrixFb[j][i] |= 0x01;
            }
        }
        dotmatrix_update();
        try {
            Thread.sleep(SHIFT_DELAY_MS);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void dotmatrix_update() {
        byte spi_data[] = new byte[4];

        for(int i=0; i < 8; i++) {
            spi_data[0] = (byte)(i + 1);
            spi_data[1] = DotMatrixFb[1][i];
            spi_data[2] = (byte)(i + 1);
            spi_data[3] = DotMatrixFb[0][i];
            try {
                spi.write(spi_data, spi_data.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
