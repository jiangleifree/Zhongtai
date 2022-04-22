package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.DataManage;
import cn.cnic.zhongtai.system.model.DataManageExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DataManageMapper {
    int countByExample(DataManageExample example);

    int deleteByExample(DataManageExample example);

    int deleteByPrimaryKey(Long id);

    int insert(DataManage record);

    int insertSelective(DataManage record);

    List<DataManage> selectByExample(DataManageExample example);

    DataManage selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") DataManage record, @Param("example") DataManageExample example);

    int updateByExample(@Param("record") DataManage record, @Param("example") DataManageExample example);

    int updateByPrimaryKeySelective(DataManage record);

    int updateByPrimaryKey(DataManage record);


    //开始自定义

    /**
     * 查询所有
     * @return
     */
    List<DataManage> selectDataManageList(@Param("type") String type);

    void changeDataImportProgress(Long id, String progress);

    boolean deleteDataManageByIds(@Param("ids")List<Integer> ids);

    boolean deleteDataManageById(@Param("id") int id);

    void updateDataManageName(@Param("id") String id,@Param("dataName") String dataName);


    int getTotalCount();

    void setDataImportResult(Long id, String result);
}