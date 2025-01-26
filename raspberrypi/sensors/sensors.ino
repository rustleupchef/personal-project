#define echoPin 13
#define trigPin 12
#define buttonPin 2
#define min 40

void setup() {
    pinMode(buttonPin, INPUT_PULLUP);

    pinMode(trigPin, OUTPUT);
    pinMode(echoPin, INPUT);

    Serial.begin(9600);
}

void loop() {
    String message = "";
    if (distance(echoPin, trigPin) <= min) {
        message += "An object is ahead of you";
    }

    if (digitalRead(buttonPin) == LOW) {
        message += ",0";
        delay(500);
    } else {
        message += ",1";
    }
    Serial.println(message);
    delay(200);
}

int distance(int echo, int trig) {
    digitalWrite(trig, LOW);
    delayMicroseconds(2);

    digitalWrite(trig, HIGH);
    delayMicroseconds(10);

    return pulseIn(echoPin, HIGH) * 0.0344 / 2;
}