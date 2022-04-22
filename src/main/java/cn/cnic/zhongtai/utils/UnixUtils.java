package cn.cnic.zhongtai.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.StringTokenizer;

@Slf4j
public class UnixUtils {

    /**
     * @param cmd
     * @param isReturnLog 是否返回输出日志
     * @return
     */
    public static String run(String[] cmd, boolean isReturnLog) {


        StringBuilder retOut = new StringBuilder();
        if (SystemUtils.IS_OS_LINUX) {
            Process process = null;
            Scanner input = null;

            String[] cmds = new String[2 + cmd.length];
            cmds[0] = "/bin/bash";
            cmds[1] = "-c";
            for (int i = 0; i < cmd.length; i++) {
                cmds[2 + i] = cmd[i];
            }
            try {
                // 使用Runtime来执行command，生成Process对象
                log.error("cmds");
                log.error(Arrays.toString(cmds));
                process = Runtime.getRuntime().exec(cmds);

                process.waitFor();

                if (isReturnLog) { //需要返回输出日志
                    InputStream is = process.getInputStream();
                    input = new Scanner(is);
                    while (input.hasNextLine()) {
                        retOut.append(input.nextLine() + "\n");
                    }
                    return retOut.toString();
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (input != null) {
                    input.close();
                }
                if (process != null) {
                    process.destroy();
                }
            }

        } else if (SystemUtils.IS_OS_WINDOWS) {
            log.info("windows");
        }
        return retOut.toString();
    }

    public static int[] getMemInfo() throws IOException, InterruptedException {
        File file = new File("/proc/meminfo");
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(file)));
        int[] result = new int[3];
        String str = null;
        StringTokenizer token = null;
        while ((str = br.readLine()) != null) {
            token = new StringTokenizer(str);
            if (!token.hasMoreTokens())
                continue;

            str = token.nextToken();
            if (!token.hasMoreTokens())
                continue;

            if (str.equalsIgnoreCase("MemTotal:"))
                result[0] = Integer.parseInt(token.nextToken());
            else if (str.equalsIgnoreCase("MemFree:"))
                result[1] = Integer.parseInt(token.nextToken());
            else if (str.equalsIgnoreCase("MemAvailable:"))
                result[2] = Integer.parseInt(token.nextToken());
        }

        return result;
    }

    public static Desk getDeskUsage() {
        Desk desk = new Desk();
        try {
            Runtime rt = Runtime.getRuntime();
            Process p = rt.exec("df -l");// df -hl 查看硬盘空间
            BufferedReader in = null;
            StringTokenizer token = null;
            try {
                in = new BufferedReader(new InputStreamReader(
                        p.getInputStream()));
                String str = null;

                int line = 0;
                while ((str = in.readLine()) != null) {
                    line++;
                    if (line != 2) {
                        continue;
                    }
                    token = new StringTokenizer(str);
                    if (token.hasMoreTokens()) {
                        token.nextToken();
                    }
                    if (token.hasMoreTokens()) {
                        str = token.nextToken();
                        desk.setTotal(str);
                    }
                    if (token.hasMoreTokens()) {
                        str = token.nextToken();
                        desk.setUsed(str);
                    }

                    if (token.hasMoreTokens()) {
                        str = token.nextToken();
                        desk.setAvailable(str);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return desk;
    }

    @Data
    public static class Desk {
        private String total;
        private String used;
        private String available;

    }
}
