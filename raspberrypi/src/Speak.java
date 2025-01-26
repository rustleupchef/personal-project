import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

public class Speak extends Thread{
    private String text;

    Speak() {

    }

    Speak(String text) {
        this.text = text;
    }

    @Override
    public void run() {
        try {
            Voice voice = VoiceManager.getInstance().getVoice("kevin16");
            voice.allocate();
            voice.speak(text);
            voice.deallocate();
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
