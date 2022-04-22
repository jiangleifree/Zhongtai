package cn.cnic.zhongtai.system.handler;

import cn.cnic.zhongtai.system.model.Task;
import org.springframework.stereotype.Component;

@Component
public interface TaskHandler {

    /**
     * 需要实现该方法
     * @param oneTask
     */
   void handle(Task oneTask);
}
