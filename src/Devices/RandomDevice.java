package Devices;

import java.util.Random;

public class RandomDevice implements Device{

    private Random[] randoms;
    public RandomDevice()
    {
        randoms = new Random[10];
    }
    
    /** Opens a new Random with a seed input as a string.
     * @param s The input seed.
     * @return int The index of the new Random.
     */
    @Override
    public int Open(String s) {
        Random random;
        if(s != null && !s.equals(""))
        random = new Random();
        else random = new Random(Integer.parseInt(s));
        for(int i = 0; i < randoms.length;i++)
        {
            if(randoms[i]==null)
            {
                randoms[i] = random;
                return i;
            }
        }
        //if there are no open spots, return -1 to signify error.
        return -1;
    }

    /** Closes a random at the given ID.
     * @param id The index to remove the random from.
     */
    @Override
    public void Close(int id) {
        randoms[id] = null;
    }

    /** Generates random bytes using the random at the given ID 
     * with the number of them equal to the input size.
     * @param id The ID of the random.
     * @param size How many random numbers to generate.
     * @return byte[] The outputted random numbers.
     */
    @Override
    public byte[] Read(int id, int size) {
       byte[] bytes = new byte[size];
       for(int i = 0; i < size;i++)
        bytes[i] = (byte)randoms[id].nextInt();
       return bytes;
    }

    /** Generates random numbers but does not output them.
     * @param id
     * @param to
     */
    @Override
    public void Seek(int id, int to) {
        Read(id,to);
    }

    /** Non-functional method, defunct.
     * @param id
     * @param data
     * @return int
     */
    @Override
    public int Write(int id, byte[] data) {
       return 0;
    }
    
}
