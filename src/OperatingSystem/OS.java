package OperatingSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import Devices.FakeFileSystem;
import Devices.VirtualFileSystem;
import Devices.VirtualToPhysicalMapping;
import UserLand.Idle;
import UserLand.PCB;
import UserLand.UserlandProcess;

import Kernel.Kernel;
import Kernel.KernelMessage;
import Kernel.Scheduler.ProcessPriority;

public class OS {
    public enum CallType {
        CREATE_PROCESS, SWITCH_PROCESS
    }
    private static VirtualFileSystem vfs;
    private static Kernel kernel;
    public static CallType currentCall;
    private static ArrayList<Object> parameters = new ArrayList<>();
    private static Object returnValue = null;
    public static FakeFileSystem ffs;
    public static int pageNumber;

    public static int CreateProcess(UserlandProcess up) throws IOException, Exception {
        parameters.add(up);
        currentCall = CallType.CREATE_PROCESS;
        // switch to the kernel
        kernel.start();

        kernel.CreateProcess(up);
        return up.getProcessID();
    }

    public static int CreateProcess(UserlandProcess up, ProcessPriority priority) throws IOException, Exception {
        parameters.add(up);
        currentCall = CallType.CREATE_PROCESS;
        // switch to the kernel
        kernel.start();

        kernel.CreateProcess(up, priority);
        return up.getProcessID();
    }
    public static int AllocateMemory(int size) throws IOException, Exception
    {
        return kernel.AllocateMemory(size);
    }
    public static void Startup(UserlandProcess init) throws IOException, Exception {
        UserlandProcess.memory = new byte[1048576];
        UserlandProcess.tlb = new byte[2][2];
        vfs = new VirtualFileSystem();
        //starts ffs on startup
        ffs = new FakeFileSystem();
        //opens Swap file on startup.
        try {
            ffs.Open("Swap");
        } catch (Exception e) {
            System.out.println("Could not open swap on startup.");}
        //defined page number
        pageNumber = 0;
        kernel = new Kernel(vfs);

        kernel.CreateProcess(init);
        kernel.CreateProcess(new Idle());
        kernel.run();
    }
    public static void writeToSwap(VirtualToPhysicalMapping mapping) throws IOException, Exception
    {
        ffs.Write(pageNumber*1024, Read(mapping.physicalPageNumber, 1024));
        mapping.diskPageNumber = pageNumber;
        mapping.physicalPageNumber = -1;
        pageNumber++;
    }

    public static void InvokeKernel() {
        kernel.release();
        try {
            kernel.semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void SwitchProcess() throws IOException, Exception {
        kernel.SwitchProcess();
    }

    public static void Sleep(int ms) throws IOException, Exception {
        kernel.Sleep(ms);
        return;
    }
    public static int Open(String s) throws Exception {
        return kernel.Open(s);
    }   

    /*
     * For the current process, set a random index in the tlb
     * to map the given virtual page number to the physical page number.
     */
    public static void GetMapping(int virtualPageNumber)
    {
        int physicalAddress = getCurrentlyRunning().getPhysicalAddress(virtualPageNumber);
        Random random = new Random();
        int randomlyUpdatedIndex = random.nextInt(2);
        UserlandProcess.tlb[0][randomlyUpdatedIndex] = (byte)virtualPageNumber;
        UserlandProcess.tlb[1][randomlyUpdatedIndex] = (byte)physicalAddress;
    }

    
    public static void Close(int id) throws Exception {
        kernel.Close(id);
    }
    public static byte[] Read(int id, int size) throws Exception {
        return kernel.Read(id, size);
    }
    public static void Seek(int id, int to) throws IOException {
        kernel.Seek(id, to);
    }
    public static int Write(int id, byte[] data) throws IOException {
       return kernel.Write(id, data);
    }
    public static int GetPid()
    {
        return kernel.GetPid();
    }
    public static int GetPidByName(String name)
    {
        return kernel.GetPidByName(name);
    }
    public static void SendMessage(KernelMessage message)
    {
        kernel.SendMessage(message);
    }
    public static KernelMessage waitForMessage() throws IOException, Exception
    {
       return kernel.waitForMessage();
    }
    public static PCB getCurrentlyRunning()
    {
        return kernel.getCurrentlyRunning();
    }
}
