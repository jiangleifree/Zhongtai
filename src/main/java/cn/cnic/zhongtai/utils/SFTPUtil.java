package cn.cnic.zhongtai.utils;


import com.jcraft.jsch.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
/**
 * 类说明 sftp工具类
 */
public class SFTPUtil {
	private static Logger logger = LoggerUtil.getLogger();

	private ChannelSftp sftp;

	private Session session;
	/** SFTP 登录用户名*/
	private String username;
	/** SFTP 登录密码*/
	private String password;
	/** 私钥 */
	private String privateKey;
	/** SFTP 服务器地址IP地址*/
	private String host;
	/** SFTP 端口*/
	private int port;


	/**
	 * 构造基于密码认证的sftp对象
	 */
	public SFTPUtil(String username, String password, String host, int port) {
		this.username = username;
		this.password = password;
		this.host = host;
		this.port = port;
	}

	/**
	 * 构造基于秘钥认证的sftp对象
	 */
	public SFTPUtil(String username, String host, int port, String privateKey) {
		this.username = username;
		this.host = host;
		this.port = port;
		this.privateKey = privateKey;
	}

	public SFTPUtil(){}




	/**
	 * 连接sftp服务器
	 */
	public ChannelSftp login() {
		try {
			JSch jsch = new JSch();
			if (privateKey != null) {
				jsch.addIdentity(privateKey);// 设置私钥
			}

			session = jsch.getSession(username, host, port);

			if (password != null) {
				session.setPassword(password);
			}
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");

			session.setConfig(config);
			session.connect();

			Channel channel = session.openChannel("sftp");
			channel.connect();

			sftp = (ChannelSftp) channel;
		} catch (JSchException e) {
			e.printStackTrace();
			logger.info("getSftp exception：",e);
		}
		return  sftp;
	}

	/**
	 * 关闭连接 server
	 */
	public void logout() {
		if (sftp != null) {
			if (sftp.isConnected()) {
				sftp.disconnect();
			}
		}
		if (session != null) {
			if (session.isConnected()) {
				session.disconnect();
			}
		}
	}


	public void cd(String directory) throws SftpException {
		if (directory != null && !"".equals(directory) && !"/".equals(directory)) {
			sftp.cd(directory);
		}

	}

	/**
	 * 将输入流的数据上传到sftp作为文件。文件完整路径=uploadPath+sftpFileName
	 *
	 * @param basePath 基础路径  一般就是“/”
	 * @param uploadPath    上传到该目录
	 * @param sftpFileName sftp端文件名
	 */
	public boolean upload(String basePath,String uploadPath, String sftpFileName, InputStream input) {
		try {
			sftp.cd(basePath);
			String[] folds = uploadPath.split("/");
			for(String fold : folds){
				if(fold.length()>0){
					try {
						sftp.cd(fold);
					}catch (Exception e){
						logger.info("目录"+fold+",不存在开始创建目录");
						sftp.mkdir(fold);
						sftp.cd(fold);
					}
				}
			}
			//上传
			sftp.put(input,sftpFileName);
			logger.info(sftpFileName+"文件上传成功");
		}catch (SftpException e) {
			logger.info("上传失败",e);
			return false;
		} finally{
			try {
				logout();
			} catch (Exception e) {
				logger.info("Disconnect error",e);
				return false;
			}
		}
		return true;
	}


	/**
	 * 下载文件。
	 *
	 * @param directory    下载目录
	 * @param downloadFile 下载的文件
	 * @param saveFile     存在本地的路径
	 */
	public void download(String directory, String downloadFile, String saveFile) {
		System.out.println("download:" + directory + " downloadFile:" + downloadFile + " saveFile:" + saveFile);

		File file = null;
		try {
			if (directory != null && !"".equals(directory)) {
				sftp.cd(directory);
			}
            File initFile = new File(saveFile);
			//目录文件不存在的话创建一下
            if (!initFile.getParentFile().exists()) {
                initFile.getParentFile().mkdirs();
            }
			file = new File(saveFile);
			sftp.get(downloadFile, new FileOutputStream(file));
		} catch (SftpException e) {
			e.printStackTrace();
			if (file != null) {
				file.delete();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			if (file != null) {
				file.delete();
			}
		}

	}

	/**
	 * 下载文件
	 *
	 * @param directory    下载目录
	 * @param downloadFile 下载的文件名
	 * @return 字节数组
	 */
	public byte[] download(String directory, String downloadFile) throws SftpException, IOException {
		if (directory != null && !"".equals(directory)) {
			sftp.cd(directory);
		}
		InputStream is = sftp.get(downloadFile);

		byte[] fileData = IOUtils.toByteArray(is);

		return fileData;
	}


	/**
	 * 删除文件
	 *
	 * @param directory  要删除文件所在目录
	 * @param deleteFile 要删除的文件
	 */
	public void delete(String directory, String deleteFile) throws SftpException {
		if (directory != null && !"".equals(directory)) {
			sftp.cd(directory);
		}
		sftp.rm(deleteFile);
	}


	/**
	 * 列出目录下的文件
	 *
	 * @param directory 要列出的目录
	 */
	public Vector<ChannelSftp.LsEntry> listFiles(String directory) throws SftpException {
		return sftp.ls(directory);
	}

	public boolean isExistsFile(String directory, String fileName) {

		List<String> findFilelist = new ArrayList();
		ChannelSftp.LsEntrySelector selector = new ChannelSftp.LsEntrySelector() {
			@Override
			public int select(ChannelSftp.LsEntry lsEntry) {
				if (lsEntry.getFilename().equals(fileName)) {
					findFilelist.add(fileName);
				}
				return 0;
			}
		};

		try {
			sftp.ls(directory, selector);
		} catch (SftpException e) {
			e.printStackTrace();
		}

		if (findFilelist.size() > 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 判断目录是否存在
	 * @param path
	 * @param sftp
	 * @return
	 */
	public boolean isExistDir(String path,ChannelSftp sftp){
		boolean  isExist=false;
		try {
			SftpATTRS sftpATTRS = sftp.lstat(path);
			isExist = true;
			return sftpATTRS.isDir();
		} catch (Exception e) {
			if (e.getMessage().toLowerCase().equals("no such file")) {
				isExist = false;
			}
		}
		return isExist;

	}


	/**
	 * 获取目录下所有文件,并指定下载某中类型文件(待完善)
	 * @param directory
	 * @param saveFile
	 */
	public void downloadByDirectory (String directory, String saveFile) {
		System.out.println("download:" + directory +    " saveFile:" + saveFile);

		File file = null;
		try {
			if (directory != null && !"".equals(directory)) {
				sftp.cd(directory);
			}
			Vector<ChannelSftp.LsEntry>  listFile = listFiles(directory);

			for (ChannelSftp.LsEntry file1 : listFile)
			{
				String fileName = file1.getFilename();
				if(fileName.contains(".json") ||  fileName.contains(".csv")){
					System.out.println(fileName);
					file = new File(saveFile);
					sftp.get(fileName, new FileOutputStream(file));
				}

			}

		} catch (SftpException e) {
			e.printStackTrace();
			if (file != null) {
				file.delete();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			if (file != null) {
				file.delete();
			}
		}

	}

}
