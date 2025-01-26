import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

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

    private static Mat frame;

    private static Speak objDetection;
    private static Speak sensors;
    private static Speak ocr;

    public static void main(String[] args) throws Exception {
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        setupSerialPort();

        VideoCapture video = new VideoCapture(0);
        while (true) {
            frame = new Mat();
            video.read(frame);

            Mat output = processs(frame);
            HighGui.imshow("frame", output);
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
            if ((objDetection == null || !objDetection.isAlive()) && (ocr == null || !ocr.isAlive())) {
                objDetection = new Speak(new String(strArray));
                objDetection.start();
            }
            socket.close();

            return output;
        } catch (Exception e) {
            e.printStackTrace();
            return frame;
        }
    }

    private static void setupSerialPort() {
        SerialPort current;
        try { current = SerialPort.getCommPort("ttyACM0");} catch (Exception e) { return;}
        current.setBaudRate(9600);
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
                String message = new String(event.getReceivedData());
                for (int i = 0; i < message.length(); i++) {
                    if (message.charAt(i) == '\n') {
                        System.out.println(partial);
                        String[] sensorArray = partial.split(",");
                        if (sensorArray.length > 1 && sensorArray[1].substring(0, sensorArray[1].length() - 1).equals("0")) {
                            if (ocr == null || !sensors.isAlive()) {
                                ocr = new Speak(readText());
                                ocr.start();
                            }
                        }

                        if ((sensors == null || !sensors.isAlive()) && (ocr == null || !ocr.isAlive())) {
                            sensors = new Speak(sensorArray[0]);
                            sensors.start();
                        }
                        partial = "";
                        continue;
                    }
                    partial += message.charAt(i);
                }
            }

            private String readText() {
                if (frame.empty()) return "";
                try (Socket socket = new Socket("", 1080)) {
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            
                    MatOfByte mob = new MatOfByte();
                    Imgcodecs.imencode(".jpg", frame, mob);
                    byte[] inputArray = mob.toArray();
                    dos.writeInt(inputArray.length);
                    dos.write(inputArray);
            
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
        
                    byte[] strArray = new byte[dis.readInt()];
                    dis.readFully(strArray);
                    socket.close();
        
                    return new String(strArray);
                } catch (Exception e) {
                    e.printStackTrace();
                    return "";
                }
            }
            
        });
    }
}
