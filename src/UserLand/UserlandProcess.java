package UserLand;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import Kernel.Scheduler;
import OperatingSystem.OS;

public abstract class UserlandProcess implements Runnable{

    //initialized in OS.startup
    public static byte[] memory;
    public static byte[][] tlb;
    //the thread of the process
    protected Thread thread;
    //the semaphore to let the process know when to cooperate
    protected Semaphore semaphore;
    //the quantum is the amount of run time this process is assigned
    protected Boolean quantumExpired;
    
    private int processID;

    public byte Read(int address)
    {
        int virtualPage = address/1024
        , pageOffset = address%1024;
        //puts the mapping for the input virtual page into the tlb if missing.
        if(tlb[0][0] != virtualPage && tlb[0][1] != virtualPage)
        OS.GetMapping(virtualPage);
        for(int i = 0;i<2;i++)
        {
            if(tlb[0][i]==virtualPage)
            {
                int physicalPage = tlb[1][i];
                int physicalAddress 
                = physicalPage * 1024 + pageOffset;
                return memory[physicalAddress];
            }

        }
        //if mapping did not exist, return null
        return -1;
    }

    public void Write(int address, byte value)
    {
        int virtualPage = address/1024
        , pageOffset = address%1024;
        //puts the mapping for the input virtual page into the tlb if missing.
        if(tlb[0][0] != virtualPage && tlb[0][1] != virtualPage)
        OS.GetMapping(virtualPage);
        for(int i = 0;i<2;i++)
        {
            if(tlb[0][i]==virtualPage)
            {
                int physicalPage = tlb[1][i];
                int physicalAddress 
                = physicalPage * 1024 + pageOffset;
                memory[physicalAddress] = value;
                return;
            }
        }
        

    }

    public void setProcessID(int processID)
    {
        this.processID = processID;
    }
    public int getProcessID()
    {
        return processID;
    }
    abstract void main();
    /*
    allows the operating system to request this process to stop
    */
    public void requestStop()
    {
        quantumExpired = true;
    }
    /*
     * If the semaphore's available permits are 0, the process
     * cannot continue.
     */
    boolean isStopped()
    {
        return semaphore.availablePermits() == 0;
    }
    /*
     * Returns true when the process's thread is not alive.
     */
    public boolean isDone()
    {
        return !thread.isAlive();
    }
    public void start()
    {

        semaphore.release();
    }
    public void stop()
    {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {e.printStackTrace();}
    }
    public void cooperate() throws IOException, Exception
    {
        if(quantumExpired)
        {
            quantumExpired = false;
            OS.SwitchProcess();
        }
    }

    @Override
    public void run() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) 
        {e.printStackTrace();}
        main();
    }

    
}