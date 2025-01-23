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

class Sensors extends Thread {
    @Override
    public void run() {
        SerialPort current = SerialPort.getCommPort("ttyACM0");
        current.openPort();
        if (!current.isOpen()) {
           
            System.err.println("Serial port is not open");
            this.interrupt();
        }
        
        while (true) {
            while (current.bytesAvailable() == 0)
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            byte[] messageBytes = new byte[current.bytesAvailable()];
            current.readBytes(messageBytes, messageBytes.length);
            System.out.println(new String(messageBytes));
        }
    }
    
}

public class App {


    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) throws Exception {
        Sensors thread = new Sensors();
        thread.start();

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
        thread.interrupt();
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
}
