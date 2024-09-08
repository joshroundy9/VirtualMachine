package Devices;

import java.io.IOException;

public interface Device {
    int Open(String s) throws Exception;
    void Close(int id) throws Exception;
    byte[] Read(int id, int size) throws Exception;
    void Seek(int id, int to) throws IOException;
    int Write(int id, byte[] data) throws IOException;
}
