const char* message = "howdy y'all";
void setup() {
    Serial.begin(9600);
}

void loop() {
    Serial.write(message);
    delay(200);
}