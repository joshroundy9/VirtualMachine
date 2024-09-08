package UserLand;

import java.util.concurrent.Semaphore;

import Devices.FakeFileSystem;
import OperatingSystem.OS;

public class VirtualMemoryTest {
   public static void main(String args[]) throws Exception
   {
    Ping ping = new Ping();
    OS.Startup(ping);
    for(int i = 0;i<20;i++)
    {
    Ping pingDuplicate = new Ping();
    OS.CreateProcess(ping);
    }
   }
}
