package cn.cnic.zhongtai.system.service.Impl;

import cn.cnic.zhongtai.system.mapper.DataManageMapper;
import cn.cnic.zhongtai.system.model.DataManage;
import cn.cnic.zhongtai.system.service.DataManageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Transactional
@Service
@Slf4j
public class DataManageServiceImpl implements DataManageService {

    @Resource
    private DataManageMapper dataManageMapper;

    public List<DataManage> selectDataManageList(String type){
        return  dataManageMapper.selectDataManageList(type);
    }

    public void downloadFruitsFile(HttpServletRequest request, HttpServletResponse response)  {
        String path = request.getParameter("path");
        if(StringUtils.isNotBlank(path) ){
            // 下载本地文件后的文件名
            int one = path.lastIndexOf("/");
            String fileName = path.substring((one+1),path.length());
            try {
                if (request.getHeader("User-Agent").toUpperCase().indexOf("MSIE") > 0) {
                    fileName = URLEncoder.encode(fileName, "UTF-8");
                } else {
                    fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
                }
            } catch (IOException e) {
                e.printStackTrace();
                log.error(e.getLocalizedMessage());
            }
            // 读到流中
            InputStream inStream = null;// 文件的存放路径
            try {
                inStream = new FileInputStream(path);
                // 设置输出的格式
                response.reset();
                /* 设置文件ContentType类型，这样设置，会自动判断下载文件类型 */
                response.setContentType("multipart/form-data");
                /* 设置文件头：最后一个参数是设置下载文件名 */
                response.addHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                // 循环取出流中的数据
                byte[] b = new byte[100];
                int len;
                try {
                    while ((len = inStream.read(b)) > 0)
                        response.getOutputStream().write(b, 0, len);
                    inStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    log.error(e.getLocalizedMessage());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                log.error(e.getLocalizedMessage());
            }

        }
    }


    public boolean deleteDataManageById(String id) {
        if (StringUtils.isNotBlank(id) && id.contains(",") && id.split(",").length > 0) {
            String[]  ids = id.split(",");
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < ids.length; i++) {
                if (!"".equals(ids[i]))
                    list.add(Integer.parseInt(ids[i]));
            }
            boolean deleteDataManages = dataManageMapper.deleteDataManageByIds(list);
            return deleteDataManages ? true : false;
        } else {
            boolean deleteDataManage = dataManageMapper.deleteDataManageById(Integer.parseInt(id));
            return deleteDataManage  ? true : false;
        }
    }


    public void updateDataManageName(String id,String dataName){
        dataManageMapper.updateDataManageName(id,dataName);
    }



    @Override
    public int getTotalCount() {
        return dataManageMapper.getTotalCount();
    }
}
