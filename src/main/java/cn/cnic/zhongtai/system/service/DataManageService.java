package cn.cnic.zhongtai.system.service;


import cn.cnic.zhongtai.system.model.DataManage;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface DataManageService {

    /**
     * 查询所有
     * @return
     */
    List<DataManage> selectDataManageList(String type);

    /**
     * 下载文件
     * @param request
     * @param response
     */
    void downloadFruitsFile(HttpServletRequest request, HttpServletResponse response);



    /**
     * 删除
     * @param id
     * @return
     */
    boolean deleteDataManageById(String id);

    void updateDataManageName(String id,String dataName);

    /**
     * 统计数量
     * @return
     */
    int getTotalCount();

}
