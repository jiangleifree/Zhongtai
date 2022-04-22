package cn.cnic.zhongtai.utils;

import cn.cnic.zhongtai.system.Constant;
import cn.cnic.zhongtai.utils.writer.TextCsvWriterManager;
import cn.cnic.zhongtai.utils.writer.UnstructuredWriter;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.*;

import static cn.cnic.zhongtai.system.Constant.HDFS_DELIMITER_CHAR;

public class FileUtils {

    private static Logger logger = LoggerUtil.getLogger();


    /**
     * 上传方法
     *
     * @param file
     * @param path
     * @param filename 自定义名称
     * @return
     */
    public static String upload(MultipartFile file, String path, String filename) {
        Map<String, String> rtnMap = new HashMap<String, String>();
        rtnMap.put("code", "500");
        if (!file.isEmpty()) {

            SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd-HHmmss");
            Date nowDate = new Date();
            String nowDateformat = sdf.format(nowDate);

            //文件名
            String saveFileName = file.getOriginalFilename();

            //获取小数点前内容,图片名称
            String prefix = saveFileName.substring(0, saveFileName.lastIndexOf("."));

            //后缀.图片格式类型
            int one = saveFileName.lastIndexOf(".");
            String Suffix = saveFileName.substring((one + 1), saveFileName.length());

            if (StringUtils.isNotBlank(filename)) {
                saveFileName = nowDateformat + "-" + filename + "." + Suffix;
            }

            File saveFile = new File(path + saveFileName);
            if (!saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }
            long fileSize = 0;
            try {

                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(saveFile));
                out.write(file.getBytes());
                out.flush();
                out.close();
                FileInputStream fis = new FileInputStream(saveFile);
                fileSize = fis.available();
                rtnMap.put("url", path + saveFileName);
                rtnMap.put("fileName", saveFileName);
                rtnMap.put("fileSize", String.valueOf(fileSize));
                rtnMap.put("msgInfo", "上传成功");
                rtnMap.put("code", "200");
                logger.info(saveFile.getName() + " 文件上传成功");
            } catch (Exception e) {
                logger.error("上传失败", e);
                rtnMap.put("msgInfo", "上传失败" + e.getMessage());
            }
        } else {
            logger.info("上传失败，因为文件为空.");
            rtnMap.put("msgInfo", "上传失败，文件为空.");
        }
        return JsonUtils.toJsonNoException(rtnMap);
    }


    public static String readJsonFile(String fileUrl) {
        String jsonStr = "";
        try {
            File jsonFile = new File(fileUrl);
            FileReader fileReader = new FileReader(jsonFile);
            Reader reader = new InputStreamReader(new FileInputStream(jsonFile), "utf-8");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
            logger.info(jsonStr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonStr;
    }

    /**
     * 获取项目访问路径
     *
     * @return
     */
    public static String getUrl() {
        HttpServletRequest request = getRequest();
        String scheme = request.getScheme();//http
        String serverName = request.getServerName();//localhost
        int serverPort = request.getServerPort();//8080
        String contextPath = request.getContextPath();//项目名
        String url = scheme + "://" + serverName + ":" + serverPort + contextPath;//http://127.0.0.1:8080/test
        return url;
    }


    /**
     * 统一获取request
     *
     * @return
     */
    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = (HttpServletRequest) requestAttributes.resolveReference(RequestAttributes.REFERENCE_REQUEST);
        return request;
    }


    public static void readJsonFileAndWrite(File jsonFile, String path) {
        String jsonStr = "";
        try {
            FileReader fileReader = new FileReader(jsonFile);

            Reader reader = new InputStreamReader(new FileInputStream(jsonFile), "utf-8");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();

            JSONObject jobj = null;
            JSONArray array = null;
            try {
                //标准的json格式
                array = JSONArray.parseArray(jsonStr);
                logger.info(array.toString());
            } catch (Exception e) {
                //mysql导出的json
                jobj = JSON.parseObject(jsonStr);
                array = jobj.getJSONArray("RECORDS");
            }

            try {
                File csv = new File(path); // CSV数据文件

                BufferedWriter bw = new BufferedWriter(new FileWriter(csv, true)); // 附加
                // 添加新的数据行
                for (Object record : array) {
                    bw.write(record.toString());
                    bw.newLine();
                }
                bw.close();
            } catch (FileNotFoundException e) {
                // File对象的创建过程中的异常捕获
                e.printStackTrace();
            } catch (IOException e) {
                // BufferedWriter在关闭对象捕捉异常
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * MultipartFile 转 File
     *
     * @param file
     * @throws Exception
     */
    public static File multipartFileToFile(MultipartFile file) {

        File toFile = null;
        if (file.equals("") || file.getSize() <= 0) {
            file = null;
        } else {
            InputStream ins = null;
            try {
                ins = file.getInputStream();
                toFile = new File(file.getOriginalFilename());
                inputStreamToFile(ins, toFile);
                ins.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return toFile;
    }

    /**
     * 写jsonString到file
     *
     * @param jsonStr
     * @param filePath
     */
    public static void writeJsonToFile(String jsonStr, String filePath) {
        Writer write = null;
        try {
            File jsonFile = new File(filePath);
            //如果已经存在 先删除
            if (jsonFile.exists()) {
                jsonFile.delete();
            }
            File parent = jsonFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            //创建json文件
            jsonFile.createNewFile();
            //写入json文件
            write = new OutputStreamWriter(new FileOutputStream(jsonFile), "UTF-8");
            write.write(jsonStr);
            write.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                write.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 导出csv文件
     *
     * @param dataList
     * @param outPutPath
     * @param filename
     * @return
     */
    public static File createCSVFile(List<List<Object>> dataList, String outPutPath, String filename) {
        return createCSVFile(Collections.EMPTY_LIST, dataList, outPutPath, filename);
    }

    /**
     * 导出csv文件
     *
     * @param dataList
     * @param outPutPath
     * @param filename
     * @return
     */
    public static File createCSVFileUseCsvWriter(List<String> header, List<List<Object>> dataList, String outPutPath, String filename) {
        File csvFile = null;
        BufferedWriter writer = null;
        UnstructuredWriter unstructuredWriter = null;
        try {
            //先创建文件
            csvFile = new File(outPutPath + File.separator + filename + ".csv");
            File parent = csvFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            csvFile.createNewFile();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                    csvFile), "UTF-8"), 1024);
            unstructuredWriter = TextCsvWriterManager
                    .produceUnstructuredWriter("csv", HDFS_DELIMITER_CHAR, writer);
            // insert header
            if (!CollectionUtils.isEmpty(header)) {
                unstructuredWriter.writeOneRecord(header);
            }

            //插入数据
            StringBuilder sb = null;
            for (List<Object> line : dataList) {
                List<String> splitedRows = new ArrayList<>();
                for (Object row : line) {
                    sb = new StringBuilder(1);
                    sb.append(row);
                    splitedRows.add(sb.toString());
                }
                unstructuredWriter.writeOneRecord(splitedRows);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(unstructuredWriter);
        }
        return csvFile;
    }

    public static File createCSVFile(List<Object> head, List<List<Object>> dataList, String outPutPath, String filename) {

        File csvFile = null;
        BufferedWriter csvWtriter = null;
        try {
            csvFile = new File(outPutPath + File.separator + filename + ".csv");
            File parent = csvFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            csvFile.createNewFile();

            // GB2312使正确读取分隔符","
            csvWtriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                    csvFile), "utf-8"), 1024);
            // 写入BOM签名
            csvWtriter.write(getBOM());

            // 写入文件内容
            for (List<Object> row : dataList) {
                writeRow(row, csvWtriter);
            }
            csvWtriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                csvWtriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return csvFile;
    }

    /**
     * 功能说明：获取UTF-8编码文本文件开头的BOM签名。
     * BOM(Byte Order Mark)，是UTF编码方案里用于标识编码的标准标记。例：接收者收到以EF BB BF开头的字节流，就知道是UTF-8编码。
     *
     * @return UTF-8编码文本文件开头的BOM签名
     */
    private static String getBOM() {

        byte b[] = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        return new String(b);
    }

    private static void writeRow(List<Object> row, BufferedWriter csvWriter) throws IOException {
        // 写入文件头部
        for (int i = 0; i < row.size(); i++) {
            if (i != row.size() - 1) {
                StringBuffer sb = new StringBuffer();
                String rowStr = sb.append(row.get(i)).append(Constant.HDFS_DELIMITER).toString();
                csvWriter.write(rowStr);
            } else {
                StringBuffer sb = new StringBuffer();
                String rowStr = sb.append(row.get(i)).toString();
                csvWriter.write(rowStr);
            }
        }
        csvWriter.newLine();
    }

    //获取流文件
    private static void inputStreamToFile(InputStream ins, File file) {
        try {
            OutputStream os = new FileOutputStream(file);
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = ins.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
            ins.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 向文件结尾追加内容
     *
     * @param fileName
     * @param content
     */
    public static void appendContentToFile(String fileName, String content) {
        appendContentToFile(new File(fileName), content);
    }

    /**
     * 向文件结尾追加内容
     *
     * @param targetFile
     * @param content
     */
    public static void appendContentToFile(File targetFile, String content) {
        FileWriter writer = null;
        try {
            //打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileWriter(targetFile, true);
            writer.write(content);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 文件设置权限
     *
     * @param dirFile
     * @throws IOException
     */
    public static void changeFolderPermission(File dirFile) {
        Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_WRITE);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        try {
            Path path = Paths.get(dirFile.getAbsolutePath());
            Files.setPosixFilePermissions(path, perms);
        } catch (Exception e) {
            logger.error("Change folder " + dirFile.getAbsolutePath() + " permission failed.");
        }
    }

    /**
     * 新建文件, 如果文件已经存在, 删除重新
     * 如果不存在, 直接创建
     *
     * @param path
     * @throws IOException
     */
    public static boolean newFile(String path) throws IOException {
        File var = new File(path);
        File parent = new File(var.getParent());
        if (!parent.exists()) {
            parent.mkdirs();
        }
        if (var.exists()) {
            if (var.isDirectory()) {
                logger.warn(String.format("path {%s} exist and is a directory"), path);
                return false;
            } else {
                logger.warn(String.format("path {%s} exist, delete", path));
                var.delete();
                var.createNewFile();
            }
        } else {
            var.createNewFile();
        }
        return true;
    }

}
