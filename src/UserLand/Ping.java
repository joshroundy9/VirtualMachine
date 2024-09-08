package UserLand;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import Kernel.KernelMessage;
import OperatingSystem.OS;

public class Ping extends UserlandProcess{

    public Ping()
    {
        semaphore = new Semaphore(1);
        thread=new Thread();
        quantumExpired = false;
    }
    @Override
    void main() {
        while(true)
        {
            KernelMessage kernelMessage = null;
            try {
                kernelMessage = OS.waitForMessage();
            } catch (Exception e) {
                e.printStackTrace();
            }
            kernelMessage.setMessage(kernelMessage.getMessage()+1);
            kernelMessage.setReceiverPid(OS.GetPidByName("Pong"));
            OS.SendMessage(kernelMessage);
            try {
                OS.AllocateMemory(100*1024);
                cooperate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
