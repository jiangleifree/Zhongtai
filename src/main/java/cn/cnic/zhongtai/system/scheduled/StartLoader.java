package cn.cnic.zhongtai.system.scheduled;

import cn.cnic.zhongtai.system.handler.DataImportJDBCHandler;
import cn.cnic.zhongtai.system.handler.Neo4jToHiveHandler;
import cn.cnic.zhongtai.utils.BaseHolder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/*@Component
@Order(value = 1)*/
public class StartLoader implements ApplicationRunner {
    public static final int DEFAULT_CLIENT_BEAT_THREAD_COUNT = Runtime.getRuntime()
            .availableProcessors() > 1 ? Runtime.getRuntime().availableProcessors() / 2
            : 1;
    DataImportJDBCHandler dataImportJDBCHandler = BaseHolder.getBean("dataImportJDBCHandler");
    Neo4jToHiveHandler neo4jToHiveHandler = BaseHolder.getBean("neo4jToHiveHandler");
    private ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(DEFAULT_CLIENT_BEAT_THREAD_COUNT, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("schedule.task");
            return thread;
        }
    });
    ;

    @Override
    public void run(ApplicationArguments args) throws Exception {

    }

    class ScheduledTask implements Runnable {
        @Override
        public void run() {
            dataImportJDBCHandler.handle();
            neo4jToHiveHandler.handler();
            executorService.schedule(new ScheduledTask(), 5 * 1000, TimeUnit.MILLISECONDS);
        }
    }
}
