package Kernel;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;

import Devices.VirtualFileSystem;
import OperatingSystem.OS;
import UserLand.Idle;
import UserLand.PCB;
import UserLand.UserlandProcess;

public class Scheduler {
    public enum ProcessPriority {
        LOWPRIORITY, MEDIUMPRIORITY, HIGHPRIORITY
    }
    private LinkedList<PCB> lowPriority;
    private LinkedList<PCB> mediumPriority;
    private LinkedList<PCB> highPriority;
    private HashMap<Instant,PCB> sleepingProcesses;
    private LinkedList<PCB> waitingProcesses;
    private Timer timer;
    private PCB currentProcess;
    public int processID = 0;
    private Random random;
    private Clock clock;
    private VirtualFileSystem vfs;

    public Scheduler(VirtualFileSystem vfs) {
        random = new Random();
        lowPriority = new LinkedList<>();
        mediumPriority = new LinkedList<>();
        highPriority = new LinkedList<>();
        //sleepingProcesses = new LinkedList<>();
        sleepingProcesses = new HashMap<>();
        timer = new Timer();
        clock = Clock.systemDefaultZone();
        currentProcess = new PCB(new Idle());
        this.vfs = vfs;
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                currentProcess.stop();
            }

        }, 250, 1);
        waitingProcesses = new LinkedList<>();
    }

    public void Sleep(int ms) throws IOException, Exception {

        sleepingProcesses.put(clock.instant().plusMillis(ms),currentProcess);
        OS.SwitchProcess();
    }

    private ProcessPriority getNextProcessPriority() {
        /*
         * will use a 6/3/1 distribution between
         * high medium and low priority processes
         */
        int randomInt = random.nextInt(10);
        if (randomInt >= 5)
            return ProcessPriority.HIGHPRIORITY;
        else if (randomInt >= 1)
            return ProcessPriority.MEDIUMPRIORITY;
        else
            return ProcessPriority.LOWPRIORITY;
    }

    /*
     * Adds the inputted process to the corresponding priority list
     * using the input process priority.
     */
    public int CreateProcess(UserlandProcess process, ProcessPriority priority) throws IOException, Exception {
        switch (priority) {
            case HIGHPRIORITY:
                highPriority.add(new PCB(process));
                break;
            case MEDIUMPRIORITY:
                mediumPriority.add(new PCB(process));
                break;
            case LOWPRIORITY:
                lowPriority.add(new PCB(process));
                break;
        }
        if (currentProcess == null)
            SwitchProcess();
        process.setProcessID(processID);
        processID++;
        return process.getProcessID();
    }

    /*
     * Adds the input process to the list, defaulting to
     */
    public int CreateProcess(UserlandProcess process) throws IOException, Exception {
        highPriority.add(new PCB(process));
        if (currentProcess == null)
            SwitchProcess();
        process.setProcessID(processID);
        processID++;
        return process.getProcessID();
    }

    /*
     * Switches the currently running process,
     * if the currently running process is complete or no longer exists,
     * it is not re-added back to the front of the list.
     */
    public void SwitchProcess() throws IOException, Exception {
        //When process is switched, clear tlb
        UserlandProcess.tlb = new byte[2][2];
        //check for sleeping processes before taking one from the queue
        PCB sleepingProcessReady = getNextSleepingProcess();
        if(sleepingProcessReady != null)
        {
            if(currentProcess!=null)
            getPriorityList(currentProcess.getPriority()).add(currentProcess);
            /*if there is a valid sleeping process it is used instead of one from the queue.
            processes that call sleep do not get demoted.*/
            currentProcess = sleepingProcessReady;
            currentProcess.run(currentProcess.getPriority());
            return;
        }
        LinkedList<PCB> currentPriorityProcesses = getPriorityList(getNextProcessPriority());
        
        if (currentPriorityProcesses.size() > 0) {

            if(currentProcess!=null)
            getPriorityList(currentProcess.getPriority()).add(currentProcess);
            
            PCB head = currentPriorityProcesses.removeFirst();
            // if the process is complete or doesn't exist, do not add it back to the list.
            if (head != null && !head.isDone())
                if(!waitingProcesses.contains(currentProcess))
                currentPriorityProcesses.add(head);
            else {
                /*when deleting the process we must close all of its open devices*/
                for(int i : currentProcess.getDevices())
                {
                    //if device is not null, close it
                    if(i != -1)
                        try {
                            vfs.Close(i);
                        } catch (Exception e) {e.printStackTrace();}
                }
                //when deleting process, also free all memory it was using
            }

            if (currentPriorityProcesses.size() > 0 && currentPriorityProcesses.getFirst() != null)
                currentProcess = currentPriorityProcesses.getFirst();
            /*if a process did not call sleep and simply was recycled in the queue,
             * it will get demoted.
            */
            //recover any memory that was taken for another process.
            for(int i = 0;i<100;i++)
            {
                if(currentProcess.getVirtualToPhysicalMapping(i).diskPageNumber != -1)
                {
                int location = OS.AllocateMemory(1024);
                OS.Write(location, OS.ffs.Read(currentProcess.getVirtualToPhysicalMapping(i).diskPageNumber, 1024));
                }
            }
            currentProcess.run(getDemotedPriority(currentProcess.getPriority()));
        }
    }
    /*
     * Returns a random process from any priority that has physical
     * memory allocated to it.
     */
    public PCB getRandomProcess() throws IOException, Exception
    {
        Stream<PCB> allProcesses = 
        Stream.concat(lowPriority.stream(),Stream.concat(mediumPriority.stream(), 
        Stream.concat(highPriority.stream(), Stream.concat(sleepingProcesses.values().stream(), waitingProcesses.stream()))));
        PCB[] processes = (PCB[])allProcesses.toArray();
        while(true)
        {
            PCB selectedProcess = processes[random.nextInt(processes.length)];
            for(int i = 0;i<100;i++)
            {
                if(selectedProcess.getPhysicalAddress(i) != -1)
                return selectedProcess;
            }
        }
        
    }
    /*
     * Takes the hashmap of instants and pcbs and returns the sleeping process that has the
     * earliest wake up time that has not already been fulfilled.
     */
    private PCB getNextSleepingProcess()
    {
        Instant earliestInstant = clock.instant();
        for(Instant instant : sleepingProcesses.keySet())
        {
            if(instant.isBefore(earliestInstant))
            earliestInstant = instant;
        }
        return sleepingProcesses.get(earliestInstant);
    }
    /*
     * returns the priority that a process will get if demoted.
     */
    private ProcessPriority getDemotedPriority(ProcessPriority input)
    {
        switch(input)
        {
            case HIGHPRIORITY:
            return ProcessPriority.MEDIUMPRIORITY;
            
            case MEDIUMPRIORITY, LOWPRIORITY:
            return ProcessPriority.LOWPRIORITY;
        }
        return null;
    }
    private LinkedList<PCB> getPriorityList(ProcessPriority priority)
    {
        switch (getNextProcessPriority()) {
            case HIGHPRIORITY:
                return highPriority;
                
            case MEDIUMPRIORITY:
                return mediumPriority;
                
            case LOWPRIORITY:
                return lowPriority;
                
            default:
                return highPriority;
        }
    }
    public PCB getCurrentProcess()
    {
        return currentProcess;
    }
    public int GetPid()
    {
        return currentProcess.getPid();
    }
    /*
     * Returns A process with this name. Does not count duplicates and only returns the first process with this name.
     * Returns -1 if not found.
     */
    public int getPidByName(String name)
    {
        LinkedList<PCB> allProcesses = new LinkedList<>();
        allProcesses.addAll(highPriority);
        allProcesses.addAll(mediumPriority);
        allProcesses.addAll(lowPriority);
        allProcesses.addAll(sleepingProcesses.values());
        for(PCB pcb : allProcesses)
        {
            if(pcb.getName().equals(name))
            return pcb.getPid();
        }
        return -1;
    }
    public PCB getPcbByPid(int pid)
    {
        LinkedList<PCB> allProcesses = new LinkedList<>();
        allProcesses.addAll(highPriority);
        allProcesses.addAll(mediumPriority);
        allProcesses.addAll(lowPriority);
        allProcesses.addAll(sleepingProcesses.values());
        
        for(PCB pcb : allProcesses)
        {
            if(pcb.getPid() == pid)
            return pcb;
        }
        return null;
    }
    public void addToWaitList() throws IOException, Exception
    {
        waitingProcesses.add(currentProcess);
        SwitchProcess();
    }
    public void removeFromWait(PCB pcb)
    {
        if(waitingProcesses.contains(pcb))
        {
            waitingProcesses.remove(pcb);
            //when removing from a waitlist, the process is added back to be first in line for its priority.
            getPriorityList(pcb.getPriority()).addFirst(pcb);
        }
    }
}
