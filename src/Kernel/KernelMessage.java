package Kernel;

public class KernelMessage {
        private int senderPid,receiverPid,message;
        private byte[] data;
        /*
         * Copy constructor that generates a new message from a previous one
         */
        public KernelMessage(KernelMessage otherMessage)
        {
            this.senderPid = otherMessage.getSenderPid();
            this.receiverPid = otherMessage.getReceiverPid();
            this.data = otherMessage.getData();
            this.message = otherMessage.getMessage();
        }
        
        public KernelMessage(int senderPid, int receiverPid, int message, byte[] data)
        {
            this.senderPid = senderPid;
            this.receiverPid = receiverPid;
            this.message = message;
            this.data = data;
        }
        public int getSenderPid() {
            return senderPid;
        }
        public void setSenderPid(int senderPid) {
            this.senderPid = senderPid;
        }
        public int getReceiverPid() {
            return receiverPid;
        }
        public void setReceiverPid(int receiverPid) {
            this.receiverPid = receiverPid;
        }
        public int getMessage() {
            return message;
        }
        public void setMessage(int message) {
            this.message = message;
        }
        public byte[] getData() {
            return data.clone();
        }
        public void setData(byte[] data) {
            this.data = data;
        }

}
