package cn.cnic.zhongtai.system.service;


import cn.cnic.zhongtai.system.model.vo.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public interface DataImportService {


    /**
     * 数据导入处理接口
     * @param dataManualFile
     * @return
     */
    Map<String,Object> setDataFile(DataManualFileImportVo dataManualFile);

    /**
     * 根据选择的模型进行创建表操作
     * @param tableId
     * @return
     */
    boolean createTableByHiveDb(String tableId);

    /**
     * 根据表名查看表是否存在
     * @param tableName
     * @return
     */
    boolean validateTableNameExist(String tableName);


    /**
     * 在线收割数据处理接口
     * @param dataAddToFtpVo
     * @return
     */
    Map<String,Object> setDataByFtp(DataAddToFtpVo dataAddToFtpVo) throws Exception;

    void changeDataImportProgress(Long id, String progress);

    void setDataImportResult(Long id, String result);

    List<String> getAllTables(String userName, String password, String jdbcUrl);

    void dataImportJDBC(String jdbcUrl, String userName, String password, String[] table,String importType, String modelTypeName);

    void dataImportJDBCNew(String jdbcUrl, String userName, String password, List<DataImportJDBCVo.Table> tables, String modelTypeName, String pid);

    void neo4jToHive(String modelTypeName, String boltUrl, String userName, String password, String tag);

    void neo4jToHiveNew(DataImportJDBCVo dataImportJDBCVo);

    void neo4jToHiveTest(String boltUrl, String userName, String password);

    void doFinalWork(List<DataImportJDBCVo.Table> datas, String pid, String url);

    Map<String, List<String>> getLabelsAndRelations(String boltUrl, String userName, String password);

    /**
     *  http方式数据接入
     * @param httptoDataVo
     * @return
     */
    Map<String,Object> httpToData(HttptoDataVo httptoDataVo);

    void createModelAndMapping(String jsonParam);

}
