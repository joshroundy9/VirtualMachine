package UserLand;

import Kernel.Scheduler.ProcessPriority;

import java.util.LinkedList;

import Devices.VirtualToPhysicalMapping;
import Kernel.KernelMessage;

public class PCB {
    private int pid;
    private UserlandProcess up;
    private ProcessPriority priority;
    private int[] devices;
    private String name;
    private LinkedList<KernelMessage> messageHistory;
    private VirtualToPhysicalMapping[] pageTable;

    public PCB(UserlandProcess up) {
        pageTable = new VirtualToPhysicalMapping[100];
        for (int i = 0; i < 100; i++)
            pageTable[i] = new VirtualToPhysicalMapping();

        devices = new int[] { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };
        priority = ProcessPriority.HIGHPRIORITY;
        this.up = up;
        pid = up.getProcessID();
        name = up.getClass().getSimpleName();
        messageHistory = new LinkedList<>();
    }

    public void stop() {
        up.stop();
        while (!up.isStopped()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public int getPhysicalAddress(int virtualAddress) {
        return pageTable[virtualAddress].physicalPageNumber;
    }

    public boolean isDone() {
        return up.isDone();
    }

    public void removePageMapping(int page) {
        for (int i = 0; i < 100; i++) {
            if (pageTable[i].diskPageNumber == page) {
                pageTable[i].diskPageNumber = -1;
                pageTable[i].physicalPageNumber = -1;
                return;
            }
        }
    }
    public VirtualToPhysicalMapping getVirtualToPhysicalMapping(int index)
    {
        return pageTable[index];
    }

    public int getPhysicalPageMapping(int virtualMapping) {
        return pageTable[virtualMapping].physicalPageNumber;
    }

    public void run(ProcessPriority priority) {
        this.priority = priority;
        up.start();
    }

    public ProcessPriority getPriority() {
        return priority;
    }

    public UserlandProcess getProcess() {
        return up;
    }

    public int[] getDevices() {
        return devices;
    }

    public int getPid() {
        return pid;
    }

    public String getName() {
        return name;
    }

    public void addMessage(KernelMessage message) {
        messageHistory.add(message);
    }

    public KernelMessage getNextMessage() {
        if (messageHistory.size() > 0)
            return messageHistory.removeFirst();
        else
            return null;
    }
}
