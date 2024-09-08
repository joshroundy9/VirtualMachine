package Devices;

import java.io.IOException;
import java.util.HashMap;

public class VirtualFileSystem implements Device{

    //the device that corresponds to the id (in the VFS)
    private Device[] devices;
    //the id in the other device of the current operation
    private Integer[] deviceIndex;
    //used to get corresponding device from the input string
    private HashMap<String, Device> stringToDevice;
    
    public VirtualFileSystem()
    {
        devices = new Device[10];
        deviceIndex = new Integer[10];
        stringToDevice = new HashMap<>();
        stringToDevice.put("file", new FakeFileSystem());
        stringToDevice.put("random", new RandomDevice());
    }
    
    /** Opens a file using the input string in the format
     * (DEVICE_TYPE) (ARGUMENTS)
     * For example, random 100 returns the id in the vfs of a random device with the
     * seed 100. Or file randomFile which returns the id in the vfs of a file named
     * randomFile.
     * @param s
     * @return int
     * @throws Exception
     */
    @Override
    public int Open(String s) throws Exception {

        String sections[] = s.split(" ");
        /*gets the desired device from the first part of the input string
        Uses a hashmap instead of a method.*/
        Device device = stringToDevice.get(sections[0]);
        int i;
        for(i = 0; i < devices.length;i++)
        {
            if(devices[i] == null)
                devices[i] = device;   
        }
        deviceIndex[i]= device.Open(s);
        return i;
    }
    /** Closes the device on the given index.
     * @param id ID of the device
     * @throws Exception
     */
    @Override
    public void Close(int id) throws Exception {
        devices[id].Close(deviceIndex[id]);
        devices[id] = null;
        deviceIndex[id] = null;
    }
    /** Calls the read method from the input device ID
     * @param id The id of the device.
     * @param size The size of data to read.
     * @return byte[] The output data.
     * @throws Exception
     */
    @Override
    public byte[] Read(int id, int size) throws Exception {
        return devices[id].Read(deviceIndex[id], size);
    }
    /** Calls seek on a device with the given ID.
     * @param id The ID of the device to read from.
     * @param to How far to seek.
     * @throws IOException
     */
    @Override
    public void Seek(int id, int to) throws IOException {
        devices[id].Seek(deviceIndex[id], to);
    }
    /** Writes to a device with the given ID.
     * @param id The ID of the device.
     * @param data The data to write to the device.
     * @return int Returns 1 if successful.
     * @throws IOException
     */
    @Override
    public int Write(int id, byte[] data) throws IOException {
        return devices[id].Write(deviceIndex[id], data);
    }
}
