package cn.cnic.zhongtai.system.controller;

import cn.cnic.zhongtai.utils.CommonUtils;
import cn.cnic.zhongtai.utils.JsonUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/file")
public class FileController {

    @PostMapping(value = "/upload/picture")
    @ResponseBody
    public String uploadPic(MultipartFile file) {
        Map<String, Object> retMessage = new HashMap<>();
        String fileName = file.getOriginalFilename();
        String targetFileName = CommonUtils.getUUID32() + fileName;
        String targetFilePath = "/data/tobaccoZhongtai/file/" + targetFileName;
        File targetFile = new File(targetFilePath);
        try {
            file.transferTo(targetFile);
            retMessage.put("attarchId", targetFileName);
            retMessage.put("code", 200);
        } catch (Exception e) {
            e.printStackTrace();
            retMessage.put("errMsg", e.getLocalizedMessage());
            retMessage.put("code", 500);
        }
        return JsonUtils.toJsonNoException(retMessage);

    }


    @GetMapping("/get/picture")
    public void getPic(@RequestParam(value = "attarchId", defaultValue = "timg.jpg")String attarchId, HttpServletResponse response) {

        FileInputStream fis = null;
        OutputStream os = null;
        try {
            fis = new FileInputStream("/data/tobaccoZhongtai/file/" + attarchId);
            //fis = new FileInputStream("d:/" + attarchId);
            os = response.getOutputStream();
            int count = 0;
            byte[] buffer = new byte[1024 * 8];
            while ((count = fis.read(buffer)) != -1) {
                os.write(buffer, 0, count);
                os.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
