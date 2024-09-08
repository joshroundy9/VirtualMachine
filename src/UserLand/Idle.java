package UserLand;

import java.io.IOException;
import java.util.concurrent.Semaphore;

public class Idle extends UserlandProcess{
    public Idle()
    {
        thread = new Thread();
        semaphore = new Semaphore(1);
        quantumExpired = false;
    }
    @Override
    public void main() {
        
        while(true)
        {
            try {
                Thread.sleep(50);
                cooperate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
