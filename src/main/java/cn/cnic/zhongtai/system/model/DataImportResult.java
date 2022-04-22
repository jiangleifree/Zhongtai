package cn.cnic.zhongtai.system.model;

import lombok.Data;

import java.util.List;

@Data
public class DataImportResult {
    private String tableName;
    private String result;
    private String getTotal;
    private String failTotal;
    private String errMsg;
    private String hdfsUrl;
    private String hiveTableName;
    private List<String> column;
    private String importType;

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        if ("failed".equals(result)){
            sb.append(tableName)
                    .append(":导入失败,")
                    .append(errMsg);
        } else if ("success".equals(result)){
            sb.append(tableName)
                    .append(":导入成功,")
                    .append("一共拉取")
                    .append(getTotal)
                    .append("条数据, 导入失败了")
                    .append(failTotal)
                    .append("条数据");
        }
        return sb.toString();
    }
}
