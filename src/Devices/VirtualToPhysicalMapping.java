package Devices;

public class VirtualToPhysicalMapping {
    public int physicalPageNumber, diskPageNumber;

    public VirtualToPhysicalMapping()
    {
        //the position in memory
        physicalPageNumber = -1;
        /*IF its memory was stolen, this number corresponds
         * to the pages position on the disk.*/
        diskPageNumber = -1;
    }
}
