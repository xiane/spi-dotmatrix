package odroid.hardkernel.com.dotMatrix;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

import odroid.hardkernel.com.VincentFont.VincentFont;

public class DotMatrix {
    private final static int SHIFT_DELAY_MS = 30; // Scroll Speed
    private final static int FIRST_DISPLAY_DELAY_MS = 500; // First Displayed time
    private final static int MAX_SHIFT_CHAR_COUNT = 256;
    private final static int DOT_MATRIX_8X8_COUNT = MAX_SHIFT_CHAR_COUNT;

    private final static String SPI = "SPI0.0";
    private SpiDevice spiDevice;

    private byte[][] FrameBuffer = new byte[DOT_MATRIX_8X8_COUNT][8];

    private final static byte[][] dotled_init = new byte[][]{
            {0x09, 0x00, 0x09, 0x00},
            {0x0a, 0x03, 0x0a, 0x03},
            {0x0b, 0x07, 0x0b, 0x07},
            {0x0c, 0x01, 0x0c, 0x01},
            {0x0f, 0x00, 0x0f, 0x00},
    };

    private int shiftDelayMs = SHIFT_DELAY_MS;
    private int FirstDelayMs = FIRST_DISPLAY_DELAY_MS;

    public DotMatrix(int spi_speed) throws IOException {
        PeripheralManager manager = PeripheralManager.getInstance();

        spiDevice = manager.openSpiDevice(SPI);
        spiDevice.setFrequency(spi_speed);
        spiDevice.setBitsPerWord(8);
        spiDevice.setDelay(0);
        spiDevice.setCsChange(false);
        spiDevice.setMode(SpiDevice.MODE2);

        for (int i=0;i < 5; i++)
            spiDevice.write(dotled_init[i], 4);
    }

    public void setScrollSpeed(int speed_ms) {
        shiftDelayMs = speed_ms;
    }

    public void setFirstDelay(int delay_ms) {
        FirstDelayMs = delay_ms;
    }

    public void display(String msg) throws IOException, InterruptedException{
        int shift_length = buffer_update(msg.toCharArray());

        while (shift_length-- > 0)
            buffer_shift();
    }

    private int buffer_update(char[] buffer) throws IOException, InterruptedException {
        int i;
        int buf_pos = 0, bits_count = 0;

        for (char ch: buffer) {
            if (ch == 0)
                break;

            for (i=0; i < 8; i++) {
                int convert_ch = (int)ch;
                FrameBuffer[buf_pos][i] = VincentFont.font[convert_ch][i];
                bits_count++;
            }
            buf_pos++;
        }

        update();

        Thread.sleep(FirstDelayMs);

        return bits_count;
    }

    private void buffer_shift() throws IOException, InterruptedException {
        int i, j;
        for (j = 0; j < DOT_MATRIX_8X8_COUNT; j++) {
            for (i = 0; i < 8; i++) {
                FrameBuffer[j][i] <<= 1;
                if ((j != DOT_MATRIX_8X8_COUNT - 1)
                        && ((FrameBuffer[j + 1][i] & 0x80) != 0))
                    FrameBuffer[j][i] |= 0x01;
            }
        }
        update();

        Thread.sleep(shiftDelayMs);
    }

    private void update() throws IOException{
        byte[] spi_data = new byte[4];

        for (int i=0; i < 8; i++) {
            spi_data[0] = (byte)(i + 1);
            spi_data[1] = FrameBuffer[1][i];
            spi_data[2] = (byte)(i + 1);
            spi_data[3] = FrameBuffer[0][i];

            spiDevice.write(spi_data, spi_data.length);
        }
    }
}
