import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public class App {

    public static String partial = "";

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) throws Exception {
        setupSerialPort();

        VideoCapture video = new VideoCapture(0);
        while (true) {
            Mat frame = new Mat();
            video.read(frame);

            frame = processs(frame);
            HighGui.imshow("frame", frame);
            if (HighGui.waitKey(10) == 27) {
                video.release();
                HighGui.destroyAllWindows();
                break;
            }
        }
        System.exit(0);
    }

    private static Mat processs(Mat frame) throws IOException{
        try (Socket socket = new Socket("", 2080)) {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
    
            MatOfByte mob = new MatOfByte();
            Imgcodecs.imencode(".jpg", frame, mob);
            byte[] inputArray = mob.toArray();
            dos.writeInt(inputArray.length);
            dos.write(inputArray);
    
            DataInputStream dis = new DataInputStream(socket.getInputStream());

            byte[] strArray = new byte[dis.readInt()];
            dis.readFully(strArray);
            byte[] matArray = new byte[dis.readInt()];
            dis.readFully(matArray);

            Mat output = Imgcodecs.imdecode(new MatOfByte(matArray), Imgcodecs.IMREAD_COLOR);
            System.out.println(new String(strArray));
            socket.close();

            return output;
        } catch (Exception e) {
            e.printStackTrace();
            return frame;
        }
    }

    private static void setupSerialPort() {
        SerialPort current;
        try { current = SerialPort.getCommPort("ttyACM0"); } catch (Exception e) { return;}
        current.openPort();
        if (!current.isOpen()) {
           
            System.err.println("Serial port is not open");
            return;
        }

        current.addDataListener(new SerialPortDataListener() {

            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
            }


            @Override
            public void serialEvent(SerialPortEvent event) {

                byte[] messageBytes = event.getReceivedData();
                for (byte messageByte : messageBytes) {
                    if ((char) messageByte == '\n') {
                        System.out.println(partial);
                        partial = "";
                        continue;
                    }
                    partial += (char) messageByte;
                }
            }
            
        });
    }
}
