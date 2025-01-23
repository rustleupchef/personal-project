from ultralytics import YOLO
import cv2
from dotenv import load_dotenv, find_dotenv
import socket
import struct
import os
import numpy as np
import json

frame = 0

def grabPriorityDetections(JSON: str) -> str:
    priorities = set()
    detections = json.loads(JSON)
    for detection in detections:
        confidence = detection["confidence"]
        print(f"{type(confidence)=} {confidence}")
        if float(confidence) < 0.6: continue
        name = detection["name"]
        print(f"{type(name)=} {name=}")
        if name in priorities:
            priorities.remove(name)
            priorities.add(f"{name}s")
            continue
        priorities.add(name)
    prioritiesStr = ""
    for priority in priorities: prioritiesStr += f"{priority},"
    return prioritiesStr[0:-1] 



def recv_all(sock, length):
    data = b""
    while len(data) < length:
        packet = sock.recv(length - len(data))
        if not packet:
            raise ConnectionError("Socket connection broken")
        data += packet
    return data

def main():
    model = YOLO("models/yolov5su.pt")
    while True:
        try:
            serverSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            serverSocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 20)
            serverSocket.bind(('', 2080))
            serverSocket.listen(2)

            client,_ = serverSocket.accept()
            length = struct.unpack(">I", client.recv(4))[0]
            print(length)
            frameBytes = recv_all(client, length)

            frameBuffer = np.frombuffer(frameBytes, np.uint8)
            frame = cv2.imdecode(frameBuffer, cv2.IMREAD_COLOR)

            results = model.track(frame)[0]
            _,encoded = cv2.imencode(".jpg", results.plot())
            byteArray = encoded.tobytes()
            encodedString = grabPriorityDetections(results.to_json()).encode("utf-8")

            client.sendall(struct.pack(">I", len(encodedString)))
            client.sendall(encodedString);
            client.sendall(struct.pack(">I", len(byteArray)))
            client.sendall(byteArray)
            client.close()
        except Exception as e:
            client.close()
            print(e)
        


if __name__ == "__main__":
    load_dotenv(find_dotenv())
    main()