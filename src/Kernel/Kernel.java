package Kernel;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import Devices.Device;
import Devices.FakeFileSystem;
import Devices.VirtualFileSystem;
import Devices.VirtualToPhysicalMapping;
import Kernel.Scheduler.ProcessPriority;
import OperatingSystem.OS;
import OperatingSystem.OS.CallType;
import UserLand.PCB;
import UserLand.UserlandProcess;

public class Kernel implements Runnable,Device{
    //KERNELAND PROCESS
    private Thread thread;
    public Semaphore semaphore;
    private Scheduler scheduler;
    private VirtualFileSystem vfs;
    private VirtualToPhysicalMapping[] freeList;
    public Kernel(VirtualFileSystem vfs) {
        //boolean for each block of 1024 bytes
        freeList = new VirtualToPhysicalMapping[1024];
        thread = new Thread();
        this.vfs = vfs;
        semaphore = new Semaphore(1);
        scheduler = new Scheduler(vfs);
        thread.start();
        FakeFileSystem ffs = new FakeFileSystem(); 
    }

    /*
     * Runs the current process in the OS in an infinite loop automatically changing
     * the current process for resource allocation.
     */
    public void run() {
        System.out.println("Kernel Started");
        while (true) {
            
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (OS.currentCall != null)
                switch (OS.currentCall) {
                    case CREATE_PROCESS:
                        try {
                            CreateProcess(scheduler.getCurrentProcess().getProcess());
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            
                            e.printStackTrace();
                        }
                        break;
                    case SWITCH_PROCESS:
                        try {
                            scheduler.SwitchProcess();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
            scheduler.getCurrentProcess().run(scheduler.getCurrentProcess().getPriority());
        }
    }

    
    public void start() {
        // thread.start();
        semaphore.release();
    }

    /*
     * If there is space in memory for the requested size, allocate
     * the memory for this process.
     * 
     * If there is no remaining space for the requested memory
     * steal memory from a different process and store the data
     * in the swap file and allocate the newly freed memory.
     * 
     * Do not record where the stolen memory was located,
     * only record where it is stored on the swap file.
     */
    public int AllocateMemory(int size) throws IOException, Exception
    {
        //if the size of the allocation is not divisible by 1024, fail
        if(size%1024 != 0)
        return -1;
        int numberOfPages = size/1024;
        for(int i = 0; i < 1024;i++)
        {
            if(freeList[i] == null)
            {
                for(int j = i;j<i+numberOfPages;j++)
                if(freeList[j] != null)
                {
                    /*if an allocated section of the array is found while
                     * checking if it is open, make i equal to j to continue searching 
                     * after the page that was allocated
                    */
                    i=j;
                    //cancel out of this loop
                    j=i+numberOfPages;
                }
                for(int j = i;j<i+numberOfPages;j++)
                {
                    freeList[j] = new VirtualToPhysicalMapping();
                    freeList[j].physicalPageNumber = j;
                }
                return i;
            }
        }
        //if the memory could not be allocated, repeatedly take memory from other processes.
        
        PCB selectedProcess = scheduler.getRandomProcess();
        for(int i = 0; i < 100; i++)
        {
            if(selectedProcess.getVirtualToPhysicalMapping(i).physicalPageNumber != -1)
            {
            selectedProcess.getVirtualToPhysicalMapping(i).diskPageNumber = OS.pageNumber;
            OS.ffs.Write(OS.pageNumber++, Read(selectedProcess.getVirtualToPhysicalMapping(i).physicalPageNumber,1024));
            return i;
            }
        }
        return -1;

    }
    public boolean FreeMemory(int pointer, int size)
    {
        //if the size is not a multiple of 1024, fail.
        if(size%1024 != 0 || size+pointer > 1023)
        return false;

        for(int i = pointer;i<pointer+size;i++)
        {
            freeList[i] = null;
            getCurrentlyRunning().removePageMapping(i);
        }
        return true;

    }
    public int CreateProcess(UserlandProcess up, ProcessPriority priority) throws IOException, Exception {
        return scheduler.CreateProcess(up, priority);
    }

    public int CreateProcess(UserlandProcess up) throws IOException, Exception {
        return scheduler.CreateProcess(up);
    }

    public void SwitchProcess() throws IOException, Exception {
        OS.currentCall = CallType.SWITCH_PROCESS;
        //tcp is cleared in the schedulers method.
        scheduler.SwitchProcess();
    }
    public PCB getCurrentlyRunning()
    {
        return scheduler.getCurrentProcess();
    }

    public void Sleep(int ms) throws IOException, Exception {
        scheduler.Sleep(ms);
    }

    public void release() {
        semaphore.release();
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    /*KERNEL DEVICE METHODS */
    /*Each process has its own list of devices, the kernel
     * can access and manage them with its VFS which includes
     * all devices being used by processes.
     * 
     * A currently running process can call Open in the kernel
     * to gain access to a device. If they
     */
    @Override
    public int Open(String s) throws Exception {
        int[] processDevices = getCurrentlyRunning().getDevices();
        for(int i = 0;i<processDevices.length;i++)
        {
            if(processDevices[i] == -1)
            {
                int vfsID = vfs.Open(s);
                if(vfsID == -1) return -1;
                processDevices[i] = vfsID;
                return i;
            }
        }
        return -1;
    }   

    @Override
    public void Close(int id) throws Exception {
        //closes the current process and clears the array position in currentlyrunning
        vfs.Close(getCurrentlyRunning().getDevices()[id]);
        getCurrentlyRunning().getDevices()[id] = -1;
    }

    @Override
    public byte[] Read(int id, int size) throws Exception {
        return vfs.Read(getCurrentlyRunning().getDevices()[id], size);
    }

    @Override
    public void Seek(int id, int to) throws IOException {
        vfs.Seek(getCurrentlyRunning().getDevices()[id], to);
    }

    @Override
    public int Write(int id, byte[] data) throws IOException {
       return vfs.Write(getCurrentlyRunning().getDevices()[id], data);
    }
    /*
     * Returns the PID of the current process.
     */
    public int GetPid()
    {
        return scheduler.GetPid();
    }
    /*
     * Gets the pid of a process with the class name of the input string.
     */
    public int GetPidByName(String name)
    {
        return scheduler.getPidByName(name);
    }
    /*
     * Sends a message to a receiver, if the receiver is waiting it is removed from the waitlist.
     */
    public void SendMessage(KernelMessage km)
    {
        KernelMessage newMessage = new KernelMessage(km);
        newMessage.setSenderPid(scheduler.getCurrentProcess().getPid());

        PCB receiverPcb = scheduler.getPcbByPid(km.getReceiverPid());
        receiverPcb.addMessage(newMessage);
        if(receiverPcb!= null)
        {
            scheduler.removeFromWait(receiverPcb);
        }
    }
    /*
     * Returns the message if there is already one waiting, otherwise adds the current process to the waiting queue.
     */
    public KernelMessage waitForMessage() throws IOException, Exception
    {
        KernelMessage message = scheduler.getCurrentProcess().getNextMessage();
        if(message == null)
        {
            /*adds the current process to the waitlist and switches process.
             * If the process is added to the waitlist, it is then awoken when a message is sent.
            */
            scheduler.addToWaitList();
            return null;
        } else return message;
        
    }
}
