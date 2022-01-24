package com.example.test.task;

import com.example.test.common.Constants;
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
public class CpuInfoTask {

    @Value("${cpu.enable}")
    private String cpuEnable;
    @Value("${cpu.rate}")
    private String rate;
    @Value("${mem.rate}")
    private String memRate;
    @Value("${mem.enable}")
    private String memEnable;

    private static final int CPUTIME = 500;

    private static final int PERCENT = 100;

    private static final int FAULTLENGTH = 10;

    /**
     * 获取系统名称
     */
    private static String osName = System.getProperty("os.name");

    /**
     *每两分钟获取一次cpu使用信息,存放到map中
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void InitCupRate() {
        if ("true".equals(cpuEnable)) {
            int cpuRate = getCpuRate();
            int lastRate = Constants.ENV_MAP.get(Constants.CUR_CPU_NEED_USED_RATE) == null ? 0 : Constants.ENV_MAP.get(Constants.CUR_CPU_NEED_USED_RATE);
            Integer iRate = Integer.parseInt(rate);
            int curNeedUsedRate = iRate - cpuRate + lastRate;
            if (curNeedUsedRate < 0) {
                curNeedUsedRate = 0;
            }
            if (curNeedUsedRate > 60) {
                curNeedUsedRate = 60;
            }
            Constants.ENV_MAP.put(Constants.CUR_CPU_NEED_USED_RATE, curNeedUsedRate);
            Constants.ENV_MAP.put(Constants.CPU_RATE_KEY, cpuRate);
        }
        if ("true".equals(memEnable)) {
            //设置的内存占用率
            double memSetRate = Double.parseDouble(rate);
            int oldNeedUseMem = Constants.ENV_MAP.get(Constants.CUR_MEM_NEED_USED) == null ? 0 : Constants.ENV_MAP.get(Constants.CUR_MEM_NEED_USED);
            //获取此次需要占用的内存
            long needUseMem = this.getNeedUseMem(memSetRate,oldNeedUseMem);
            if (needUseMem < 0) {
                needUseMem = 0;
            }
            Constants.ENV_MAP.put(Constants.CUR_MEM_NEED_USED, (int)needUseMem);
        }

    }

    private long getNeedUseMem(Double memSetRate, int oldNeedUseMem) {
        Map<String, Object> map = MemImproveTask.getMem();
        //总内存大小，单位是kb
        long memTotal = 0L;
        if (map.get("MemTotal") != null) {
            memTotal = Long.parseLong(map.get("MemTotal").toString());
        }
        long memFree = 0L;
        if (map.get("MemAvailable") != null) {
            memFree = Long.parseLong(map.get("MemAvailable").toString());
        } else if(map.get("MemFree") != null) {
            memFree = Long.parseLong(map.get("MemFree").toString());
        }
        //如果总内存或者空闲内存有一个小于0就停止
        if (memFree <= 0 || memTotal <=0 ) {
            return 0;
        }
        //当前内存使用率
        double memUsedRate = ((double)(memTotal - memFree)/memTotal) * 100;
        //本次增量更新的内存大小为
        long increaseMem = Math.round(memTotal * (memSetRate - memUsedRate)/100);
        Constants.ENV_MAP.put(Constants.INCREASE_MEM_NEED_USED, (int) increaseMem);
        //需要占用的内存大小，单位为kb
        long needUseMem = increaseMem  + oldNeedUseMem;
        return needUseMem;
    }

    public static Map<String, Object> getMem() {
        Map<String, Object> map = new HashMap<String, Object>();
        //如果是windows系统
        if (osName.toLowerCase().contains("windows")
                || osName.toLowerCase().contains("win")) {
//            try {
//                OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory
//                        .getOperatingSystemMXBean();
//                // 总的物理内存+虚拟内存
//                long totalvirtualMemory = osmxb.getTotalPhysicalMemorySize();
//                map.put("MemTotal", totalvirtualMemory/1024);
//                // 剩余的物理内存
//                long freePhysicalMemorySize = osmxb.getFreePhysicalMemorySize();
//                map.put("MemAvailable", freePhysicalMemorySize/1024);
//            } catch (Exception e) {
//                log.error("系统异常", e);
//            }
            return map;
        } else {
            InputStreamReader inputs = null;
            BufferedReader buffer = null;
            try {
                inputs = new InputStreamReader(new FileInputStream("/proc/meminfo"));
                buffer = new BufferedReader(inputs);
                String line = "";
                while (true) {
                    line = buffer.readLine();
                    if (line == null) {
                        break;
                    }
                    int beginIndex = 0;
                    int endIndex = line.indexOf(":");
                    if (endIndex != -1) {
                        String key = line.substring(beginIndex, endIndex);
                        beginIndex = endIndex + 1;
                        endIndex = line.length();
                        String memory = line.substring(beginIndex, endIndex);
                        String value = memory.replace("kB", "").trim();
                        map.put(key, value);
                    }
                }


            } catch (Exception e) {
                log.error("系统异常",e);
            } finally {
                try {
                    buffer.close();
                    inputs.close();
                } catch (Exception e2) {
                    log.error("系统异常",e2);
                }
            }

        }
        return map;
    }

    /**
     * 获取cpu使用率
     * @return
     */
    public static int getCpuRate() {
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
            Thread.sleep( 5*1000);
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

    public static void main(String[] args) throws Exception {
        //CPU
        String cmd = System.getenv("windir") + "\\system32\\wbem\\wmic.exe process get Caption,CommandLine,KernelModeTime,ReadOperationCount,ThreadCount,UserModeTime,WriteOperationCount";
        //获取进程信息
        long[] c0 = readCpuForWin(Runtime.getRuntime().exec(cmd));
        Thread.sleep(CPUTIME);
        long[] c1 = readCpuForWin(Runtime.getRuntime().exec(cmd));
        if(c0 != null && c1 != null){
            long idleTime = c1[0] - c0[0];
            long useTime = c1[1] - c0[1];
            System.out.println("CPU使用率：" + Double.valueOf(PERCENT * useTime / (idleTime + useTime)).intValue() + "%");
        }
    }



}
