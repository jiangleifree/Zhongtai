package cn.cnic.zhongtai.system.listener;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class QueueConsumerListener {
    BlockingQueue<Runnable> queue = new ArrayBlockingQueue(1000);
    //创建一个自定义的线程池
    ThreadPoolExecutor executor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(), //corePoolSize
            20,
            120L,
            TimeUnit.SECONDS,
            queue
    );


    //queue模式的消费者
    @JmsListener(destination = "${spring.activemq.queue-name}", containerFactory = "queueListener")
    public void readActiveQueue(String message) throws InterruptedException {
        MessageTask task = new MessageTask(message);
        executor.submit(task);
    }

    private static class MessageTask implements Runnable {
        private String msg;

        public MessageTask(String msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("当前线程:" + Thread.currentThread().getName() + "处理任务" + this.msg);
        }
    }
}
