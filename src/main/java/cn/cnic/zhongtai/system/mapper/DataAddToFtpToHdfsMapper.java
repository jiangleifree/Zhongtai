package cn.cnic.zhongtai.system.mapper;

import cn.cnic.zhongtai.system.model.DataAddToFtpToHdfs;
import cn.cnic.zhongtai.system.model.DataAddToFtpToHdfsExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface DataAddToFtpToHdfsMapper {
    int countByExample(DataAddToFtpToHdfsExample example);

    int deleteByExample(DataAddToFtpToHdfsExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(DataAddToFtpToHdfs record);

    int insertSelective(DataAddToFtpToHdfs record);

    List<DataAddToFtpToHdfs> selectByExample(DataAddToFtpToHdfsExample example);

    DataAddToFtpToHdfs selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") DataAddToFtpToHdfs record, @Param("example") DataAddToFtpToHdfsExample example);

    int updateByExample(@Param("record") DataAddToFtpToHdfs record, @Param("example") DataAddToFtpToHdfsExample example);

    int updateByPrimaryKeySelective(DataAddToFtpToHdfs record);

    int updateByPrimaryKey(DataAddToFtpToHdfs record);
}