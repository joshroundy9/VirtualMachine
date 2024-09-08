package Devices;

import java.io.IOException;
import java.io.RandomAccessFile;

public class FakeFileSystem implements Device{
    private RandomAccessFile[] files;
    public class InvalidFileNameException extends Exception{
        public InvalidFileNameException(String input)
        {
            super(input+"");
        }
    }
    public FakeFileSystem()
    {
        files = new RandomAccessFile[10];
    }
    
    /** 
     * @param fileName
     * @return int
     * @throws Exception
     */
    @Override
    public int Open(String fileName) throws Exception {
        if(fileName==null || fileName.isEmpty())
        throw new InvalidFileNameException("");
        RandomAccessFile newFile;
            newFile = new RandomAccessFile(fileName, "rwd");
        for(int i = 0;i<files.length;i++)
        {
            if(files[i]==null)
            {
                files[i] = newFile;
                return i;
            }
        }
        //to signify error (no open spaces in array)
        return -1;
    }
    /** 
     * @param id
     * @throws IOException
     */
    @Override
    public void Close(int id) throws IOException {
        files[id].close();
        files[id] = null;
    }
    /** 
     * @param id
     * @param size
     * @return byte[]
     * @throws IOException
     */
    @Override
    public byte[] Read(int id, int size) throws IOException {
        byte[] bytes = new byte[size];
            files[id].read(bytes,0,size);
        return bytes; 
    }
    /** 
     * @param id
     * @param to
     * @throws IOException
     */
    @Override
    public void Seek(int id, int to) throws IOException {
            files[id].seek(to);
    }
    /** 
     * @param id
     * @param data
     * @return int
     * @throws IOException
     */
    @Override
    public int Write(int id, byte[] data) throws IOException {
        files[id].write(data);
        return 1;
    }
    
}
