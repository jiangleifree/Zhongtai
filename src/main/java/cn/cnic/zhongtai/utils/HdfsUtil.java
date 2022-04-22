package cn.cnic.zhongtai.utils;


import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Hdfs工具类
 * Created  by wdd on 2019/11/13
 **/
@Slf4j
@Component
public class HdfsUtil {

    /**
     * HDFS的路径,core-site.xml中配置的端口号
     */
    @Value("${syspara.hdfs_path}")
    private String HDFS_PATH;

    /**
     * 解决无权限访问,设置远程hadoop的linux用户名称
     */
    @Value("${syspara.user}")
    private String USER;


    /**
     * connecting HDFS 初始化资源
     */
    public FileSystem getFS() {
        log.info("=====================start connecting HDFS========================");
        Configuration conf = new Configuration();
        FileSystem fileSystem = null;
        try {
            fileSystem = FileSystem.get(new URI(HDFS_PATH), conf, USER);
            return fileSystem;
        } catch (URISyntaxException e) {
            log.error(e.getLocalizedMessage());
            e.printStackTrace();
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 关闭FileSystem
     *
     * @param fileSystem
     */
    public void closeFS(FileSystem fileSystem) {
        if (fileSystem != null) {
            try {
                fileSystem.close();
                log.info("=====================end HDFS========================");
            } catch (IOException e) {
                log.error(e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * ls
     */
    public void listFiles(String specialPath) {
        FileSystem fileSystem = null;
        try {
            fileSystem = this.getFS();

            FileStatus[] fstats = fileSystem.listStatus(new Path(specialPath));

            for (FileStatus fstat : fstats) {
                log.info(fstat.isDirectory() ? "directory" : "file");
                log.info("Permission:" + fstat.getPermission());
                log.info("Owner:" + fstat.getOwner());
                log.info("Group:" + fstat.getGroup());
                log.info("Size:" + fstat.getLen());
                log.info("Replication:" + fstat.getReplication());
                log.info("Block Size:" + fstat.getBlockSize());
                log.info("Name:" + fstat.getPath());

                log.info("#############################");
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("link err");
        } finally {
            if (fileSystem != null) {
                try {
                    fileSystem.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public List<String> getListFiles(String specialPath) {
        FileSystem fileSystem = null;
        List<String> listFileNames = new ArrayList<>();
        try {
            fileSystem = this.getFS();

            FileStatus[] fstats = fileSystem.listStatus(new Path(specialPath));

            for (FileStatus fstat : fstats) {
                log.info(fstat.isDirectory() ? "directory" : "file");
                log.info("Name:" + fstat.getPath());
                listFileNames.add(fstat.getPath().getName());
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("link err");
        } finally {
            if (fileSystem != null) {
                try {
                    fileSystem.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return listFileNames;
    }

    /**
     * cat
     *
     * @param hdfsFilePath
     */
    public void cat(String hdfsFilePath) {
        FileSystem fileSystem = null;
        try {
            fileSystem = this.getFS();

            FSDataInputStream fdis = fileSystem.open(new Path(hdfsFilePath));

            IOUtils.copyBytes(fdis, System.out, 1024);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeStream(fileSystem);
        }

    }

    /**
     * 创建目录
     *
     * @param hdfsFilePath
     */
    public void mkdir(String hdfsFilePath) throws Exception {

        FileSystem fileSystem = this.getFS();

        try {
            boolean success = fileSystem.mkdirs(new Path(hdfsFilePath));
            if (success) {
                log.info(hdfsFilePath + " Create directory or file successfully");
            }
        } catch (Exception e) {
            log.error("创建目录失败", e);
            throw new Exception("创建目录失败", e);
        } finally {
            this.closeFS(fileSystem);
        }
    }

    /**
     * 创建目录
     *
     * @param hdfsFilePath
     */
    public void mkdir(FileSystem fileSystem, String hdfsFilePath) throws Exception {

        try {
            boolean success = fileSystem.mkdirs(new Path(hdfsFilePath));
            if (success) {
                log.info(hdfsFilePath + " Create directory or file successfully");
            }
        } catch (Exception e) {
            log.error("创建目录失败", e);
            throw new Exception("创建目录失败", e);
        }
    }

    /**
     * 删除文件或目录
     *
     * @param hdfsFilePath
     * @param recursive    递归
     */
    public void rm(FileSystem fileSystem, String hdfsFilePath, boolean recursive) {
        try {
            boolean success = fileSystem.delete(new Path(hdfsFilePath), recursive);
            if (success) {
                log.info("delete successfully");
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除文件或目录
     *
     * @param hdfsFilePath
     * @param recursive    递归
     */
    public void rm(String hdfsFilePath, boolean recursive) {
        FileSystem fileSystem = this.getFS();
        try {
            boolean success = fileSystem.delete(new Path(hdfsFilePath), recursive);
            if (success) {
                log.info("delete successfully");
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            this.closeFS(fileSystem);
        }
    }

    /**
     * 上传文件到HDFS
     *
     * @param localFilePath
     * @param hdfsFilePath
     */
    public void put(String localFilePath, String hdfsFilePath) {
        FileSystem fileSystem = this.getFS();
        try {
            FSDataOutputStream fdos = fileSystem.create(new Path(hdfsFilePath));
            FileInputStream fis = new FileInputStream(new File(localFilePath));
            IOUtils.copyBytes(fis, fdos, 1024);
            log.info(hdfsFilePath + "  file put successfully");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeStream(fileSystem);
            this.closeFS(fileSystem);
        }
    }

    /**
     * 上传文件到HDFS
     *
     * @param localFilePath
     * @param hdfsFilePath
     */
    public void put(FileSystem fileSystem, String localFilePath, String hdfsFilePath) {

        FSDataOutputStream fdos = null;
        FileInputStream fis = null;
        try {
            fdos = fileSystem.create(new Path(hdfsFilePath));
            fis = new FileInputStream(new File(localFilePath));
            IOUtils.copyBytes(fis, fdos, 1024);
            log.info(hdfsFilePath + "  file put successfully");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //IOUtils.closeStream(fileSystem);
            if (null != fdos) {
                try {
                    fdos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void read(String fileName) throws Exception {

        // get filesystem
        FileSystem fileSystem = this.getFS();

        Path readPath = new Path(fileName);

        // open file
        FSDataInputStream inStream = fileSystem.open(readPath);

        try {
            // read
            IOUtils.copyBytes(inStream, System.out, 4096, false);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // close Stream
            IOUtils.closeStream(inStream);
            this.closeFS(fileSystem);
        }
    }

    /**
     * 下载文件到本地
     *
     * @param localFilePath
     * @param hdfsFilePath
     */
    public void get(String localFilePath, String hdfsFilePath) {
        FileSystem fileSystem = this.getFS();
        try {
            FSDataInputStream fsis = fileSystem.open(new Path(hdfsFilePath));
            FileOutputStream fos = new FileOutputStream(new File(localFilePath));
            IOUtils.copyBytes(fsis, fos, 1024);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeStream(fileSystem);
            this.closeFS(fileSystem);
        }
    }

    /**
     * 从hdfs复制文件夹 把hdfsPath中的文件夹复制到localPath文件夹中
     *
     * @param hdfsPath
     * @param localPath
     */
    public void copyHdfsDirToLocalDir(String hdfsPath, String localPath) {

        FileSystem fileSystem = this.getFS();
        try {
            copy(fileSystem, hdfsPath, localPath);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("hdfs copy error");
        } finally {
            IOUtils.closeStream(fileSystem);
            this.closeFS(fileSystem);
        }
    }

    private void copy(FileSystem fs, String hdfsPath, String localPath) throws IOException {
        FileStatus[] fstats = fs.listStatus(new Path(hdfsPath));
        for (FileStatus status : fstats) {
            if (status.isDirectory()) {
                new File(localPath + "/" + status.getPath().getName()).mkdirs();
                copy(fs, hdfsPath + "/" + status.getPath().getName(), localPath + "/" + status.getPath().getName());
            } else {
                new File(localPath + "/" + status.getPath().getName()).createNewFile();
                fs.copyToLocalFile(new Path(hdfsPath + "/" + status.getPath().getName()),
                        new Path(localPath + "/" + status.getPath().getName()));
            }
        }

    }

   /* public void writeOrcFile(FileSystem fileSystem, String fileName) throws IOException {
        FileOutputFormat outFormat = new OrcOutputFormat();
        OrcSerde orcSerde = new OrcSerde();

        List<String> columnNames = new ArrayList<>();

        ObjectInspector objectInspector = ObjectInspectorFactory.getReflectionObjectInspector(Byte.class, ObjectInspectorFactory.ObjectInspectorOptions.JAVA);
        List<ObjectInspector> columnTypeInspectors = new ArrayList<>();
        columnTypeInspectors.add(objectInspector);

        StructObjectInspector inspector = (StructObjectInspector) ObjectInspectorFactory
                .getStandardStructObjectInspector(columnNames, columnTypeInspectors);

        Configuration conf = fileSystem.getConf();
        RecordWriter writer = outFormat.getRecordWriter(fileSystem, new JobConf(conf), fileName, Reporter.NULL);

        MutablePair<List<Object>, Boolean> transportResult = new MutablePair<>();
        transportResult.setLeft(new ArrayList<>());
        transportResult.setRight(false);

        if (!transportResult.getRight()) {
            writer.write(NullWritable.get(), orcSerde.serialize(transportResult.getLeft(), inspector));
        }

        writer.close(Reporter.NULL);

    }*/

    public void write(String localPath, String hdfspath) throws Exception {
        FileInputStream inStream = new FileInputStream(
                new File(localPath)
        );
        FileSystem fileSystem = this.getFS();

        Path writePath = new Path(hdfspath);

        // Output Stream
        FSDataOutputStream outStream = fileSystem.create(writePath);


        try {
            IOUtils.copyBytes(inStream, outStream, 4096, false);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeStream(inStream);
            IOUtils.closeStream(outStream);
        }

    }

    /**
     * 检查文件或者文件夹是否存在
     *
     * @param filename
     * @return
     */
    public boolean checkFileExist(String filename) {
        FileSystem fileSystem = this.getFS();
        try {
            Path f = new Path(filename);
            return fileSystem.exists(f);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.closeFS(fileSystem);
        }
        return false;
    }

    /**
     * 检查文件或者文件夹是否存在
     *
     * @param filename
     * @return
     */
    public boolean checkFileExist(FileSystem fileSystem, String filename) {
        try {
            Path f = new Path(filename);
            return fileSystem.exists(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * copy  也可以做上传
     *
     * @param localFilePath
     * @param hdfsFilePath
     */
    public void copyFromLocalFile(String localFilePath, String hdfsFilePath) {
        FileSystem fileSystem = this.getFS();
        try {
            Path localPath = new Path(localFilePath);
            Path hdfsPath = new Path(hdfsFilePath);
            fileSystem.copyFromLocalFile(localPath, hdfsPath);
            log.info(hdfsFilePath + "file copyFromLocalFile successfully");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeStream(fileSystem);
        }
    }


}
