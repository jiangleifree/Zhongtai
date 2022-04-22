package cn.cnic.zhongtai.utils;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class ArrayBlockQueueTest {

    class ReadThread implements Runnable{
        private Queue<String> queue;
        public ReadThread(Queue queue) {
            this.queue = queue;
        }
        @Override
        public void run() {
            String record = null;
            while ((record = queue.poll()) != null) {
                System.out.println(record);
            }
        }
    }
    class WriteThread implements Runnable{
        private Queue<String> queue;
        public WriteThread(Queue queue) {
            this.queue = queue;
        }
        @Override
        public void run() {
            int n = 0;
            while (n < 5) {
                n++;
                queue.add(String.format("test%d", n));
                System.out.println("add queue");
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void testArrayBlockQueue() {
        Queue queue = new ArrayBlockingQueue(1024);
        ReadThread readThread = new ReadThread(queue);
        WriteThread writeThread =  new WriteThread(queue);
        writeThread.run();
        readThread.run();
    }

    public static void main(String[] args) {
        ArrayBlockQueueTest test = new ArrayBlockQueueTest();
        test.testArrayBlockQueue();
    }
}
