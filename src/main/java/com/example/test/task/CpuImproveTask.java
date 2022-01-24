package com.example.test.task;

import com.example.test.thread.CpuThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.*;

/**
 * Author: chenwenhao
 * Date: 12:39 2020/8/24
 * Content:
 */
@Component
@EnableScheduling
@Slf4j
public class CpuImproveTask {

    @Value("${cpu.enable}")
    private String enable;
    @Value("${cpu.rate}")
    private String rate;
    @Value("${cpu.runtime}")
    private String runtime;

    private static final int CPUTIME = 500;

    private static final int PERCENT = 100;

    private static final int FAULTLENGTH = 10;

    /**
     * 获取系统名称
     */
    private static String osName = System.getProperty("os.name");

    @Scheduled(cron = "${cpu.schedule}")
    public void improveCupRate() {
        if (!"true".equals(enable)) {
            return;
        }
        Integer iRate = Integer.parseInt(rate);
        if (iRate > 80) {
            return;
        }
        int cpuCount = Runtime.getRuntime().availableProcessors();
        long lRuntime = Long.parseLong(runtime);
        lRuntime = lRuntime*60*1000;
        for (int i = 0; i < cpuCount; i++) {
            CpuThread thread = new CpuThread();
            thread.setEnable(enable);
            thread.setRate(iRate);
            thread.setRuntime(lRuntime);
            thread.start();
        }
    }

    /**
     * 获取cpu使用率
     * @return
     */
    public int getCpuRate() {
        //windows系统
        if (osName.toLowerCase().contains("windows")
                || osName.toLowerCase().contains("win")) {
            try {
                String procCmd =
                        System.getenv("windir")
                                + "\\system32\\wbem\\wmic.exe process get Caption,CommandLine,KernelModeTime,ReadOperationCount,ThreadCount,UserModeTime,WriteOperationCount";
                // 取进程信息
                long[] c0 = readCpuForWin(Runtime.getRuntime().exec(procCmd));
                Thread.sleep(CPUTIME);
                long[] c1 = readCpuForWin(Runtime.getRuntime().exec(procCmd));
                if (c0 != null && c1 != null) {
                    long idletime = c1[0] - c0[0];
                    long busytime = c1[1] - c0[1];
                    return Double.valueOf(PERCENT * (busytime) * 1.0 / (busytime + idletime)).intValue();
                }
                else {
                    return 100;
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
                return 100;
            }
        } else {
            return getCpuUsageForLinux();
        }
    }

    public static int getCpuUsageForLinux() {
        try {
            Map<?, ?> map1 = linuxCpuInfo();
            Thread.sleep( 100);
            Map<?, ?> map2 = linuxCpuInfo();

            long user1 = Long.parseLong(map1.get("user").toString());
            long nice1 = Long.parseLong(map1.get("nice").toString());
            long system1 = Long.parseLong(map1.get("system").toString());
            long idle1 = Long.parseLong(map1.get("idle").toString());

            long user2 = Long.parseLong(map2.get("user").toString());
            long nice2 = Long.parseLong(map2.get("nice").toString());
            long system2 = Long.parseLong(map2.get("system").toString());
            long idle2 = Long.parseLong(map2.get("idle").toString());

            long total1 = user1 + system1 + nice1;
            long total2 = user2 + system2 + nice2;
            float total = total2 - total1;

            long totalIdle1 = user1 + nice1 + system1 + idle1;
            long totalIdle2 = user2 + nice2 + system2 + idle2;
            float totalidle = totalIdle2 - totalIdle1;

            float cpusage = (total / totalidle) * 100;
            return (int) cpusage;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 100;
    }

    /**
     * 功能：CPU使用信息
     * */
    public static Map<?, ?> linuxCpuInfo() {
        InputStreamReader inputs = null;
        BufferedReader buffer = null;
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            inputs = new InputStreamReader(new FileInputStream("/proc/stat"));
            buffer = new BufferedReader(inputs);
            String line = "";
            while (true) {
                line = buffer.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("cpu")) {
                    StringTokenizer tokenizer = new StringTokenizer(line);
                    List<String> temp = new ArrayList<String>();
                    while (tokenizer.hasMoreElements()) {
                        String value = tokenizer.nextToken();
                        temp.add(value);
                    }
                    map.put("user", temp.get(1));
                    map.put("nice", temp.get(2));
                    map.put("system", temp.get(3));
                    map.put("idle", temp.get(4));
                    map.put("iowait", temp.get(5));
                    map.put("irq", temp.get(6));
                    map.put("softirq", temp.get(7));
                    map.put("stealstolen", temp.get(8));
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                buffer.close();
                inputs.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return map;
    }

    //读取cpu相关信息
    private static long[] readCpuForWin(final Process proc) {
        long[] retn = new long[2];
        try {
            proc.getOutputStream().close();
            InputStreamReader ir = new InputStreamReader(proc.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            String line = input.readLine();
            if (line == null || line.length() < FAULTLENGTH) {
                return null;
            }
            int capidx = line.indexOf("Caption");
            int cmdidx = line.indexOf("CommandLine");
            int rocidx = line.indexOf("ReadOperationCount");
            int umtidx = line.indexOf("UserModeTime");
            int kmtidx = line.indexOf("KernelModeTime");
            int wocidx = line.indexOf("WriteOperationCount");
            long idletime = 0;
            long kneltime = 0;
            long usertime = 0;
            while ((line = input.readLine()) != null) {
                if (line.length() < wocidx) {
                    continue;
                }
                // 字段出现顺序：Caption,CommandLine,KernelModeTime,ReadOperationCount,ThreadCount,UserModeTime,WriteOperation
                String caption = substring(line, capidx, cmdidx - 1).trim();
                String cmd = substring(line, cmdidx, kmtidx - 1).trim();
                if (cmd.indexOf("wmic.exe") >= 0) {
                    continue;
                }
                String s1 = substring(line, kmtidx, rocidx - 1).trim();
                String s2 = substring(line, umtidx, wocidx - 1).trim();
                if (caption.equals("System Idle Process") || caption.equals("System")) {
                    if (s1.length() > 0)
                        idletime += Long.valueOf(s1).longValue();
                    if (s2.length() > 0)
                        idletime += Long.valueOf(s2).longValue();
                    continue;
                }
                if (s1.length() > 0)
                    kneltime += Long.valueOf(s1).longValue();
                if (s2.length() > 0)
                    usertime += Long.valueOf(s2).longValue();
            }
            retn[0] = idletime;
            retn[1] = kneltime + usertime;
            return retn;
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                proc.getInputStream().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;

    }

    /**
     * 由于String.subString对汉字处理存在问题（把一个汉字视为一个字节)，因此在 包含汉字的字符串时存在隐患，现调整如下：
     *
     * @param src 要截取的字符串
     * @param start_idx 开始坐标（包括该坐标)
     * @param end_idx 截止坐标（包括该坐标）
     * */
    private static String substring(String src, int start_idx, int end_idx) {
        byte[] b = src.getBytes();
        String tgt = "";
        for (int i = start_idx; i <= end_idx; i++) {
            tgt += (char) b[i];
        }
        return tgt;
    }
}
