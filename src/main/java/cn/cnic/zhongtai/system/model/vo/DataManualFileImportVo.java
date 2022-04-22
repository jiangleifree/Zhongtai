package cn.cnic.zhongtai.system.model.vo;


import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DataManualFileImportVo {

    //获取选择到的模型id
    private String modelId;

    //获取填写的导入名称
    private String importName;
    //获取选择到的文件类型
    private String fileType;

    private MultipartFile file;

    //操作类型(追加/覆盖/新建 ：0/1/2)
    private String processingMode;

    //虚拟目录的id
    private int dataMappingId;
    //虚拟目录的name
    private String dataMappingName;


}