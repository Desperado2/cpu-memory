package com.example.test.task;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.test.thread.MemoryThread;
import lombok.extern.slf4j.Slf4j;

import com.sun.management.OperatingSystemMXBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @Author: chenwenhao
 * Date: 11:28 2020/12/7
 * Content:
 */
@Slf4j
@Component
@EnableScheduling
public class MemImproveTask {

    @Value("${mem.enable}")
    private String enable;
    @Value("${mem.rate}")
    private String rate;
    @Value("${mem.runtime}")
    private String runtime;

    /**
     * 获取系统名称
     */
    private static String osName = System.getProperty("os.name");

    @Scheduled(cron = "${mem.schedule}" )
    public void improveMemRate() {
        log.info("设置的内存占用率为：" + rate);
        //如果没有开启内存占用程序，就停止
        if (!"true".equals(enable)) {
            return;
        }
        //设置的内存占用率
        double memSetRate = Double.parseDouble(rate);
        long lRuntime = Long.parseLong(runtime);
        lRuntime = lRuntime*60*1000;
        MemoryThread memoryThread = new MemoryThread();
        memoryThread.setMemSetRate(memSetRate);
        memoryThread.setRuntime(lRuntime);
        memoryThread.start();
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
}
