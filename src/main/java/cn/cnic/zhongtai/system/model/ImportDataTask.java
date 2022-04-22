package cn.cnic.zhongtai.system.model;

import lombok.Data;

@Data
public class ImportDataTask extends Task {


    public ImportDataTask(){
        super();
    }
    public ImportDataTask(String taskType){
        super();
        setTaskType(taskType);
    }
}

