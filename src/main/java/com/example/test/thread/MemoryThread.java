package com.example.test.thread;

import com.example.test.common.Constants;
import com.example.test.task.MemImproveTask;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.omg.SendingContext.RunTime;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Author: chenwenhao
 * Date: 13:40 2020/8/24
 * Content:
 */
@Data
@Slf4j
public class MemoryThread extends Thread{

    private double memSetRate;
    private long runtime;

    @Override
    public void run() {
        log.info("内存占用程序开始运行！");

        //一次内存申请128M，计算需要申请多少次
//        long count = needUseMem / 1024 / 128 + 1;
//        if (count < 1) {
//            count = 1;
//        }
//        List<int[]> memList = new ArrayList<>();
        //128M内存,因为int占用4个字节
//        int m = 1024*1024*32;
//        for (int i = 0; i < count; i++) {
//            memList.add(new int[m]);
//        }

        long start = System.currentTimeMillis();
        ByteBuffer buffer = null;
        while(true){
            int increaseMem = Constants.ENV_MAP.get(Constants.INCREASE_MEM_NEED_USED);
            //如果本次循环需要占用的内存在0M到100M之间，则不需要释放内存重新占用
            if (increaseMem <= 1024*100 && increaseMem >= 0) {
                continue;
            }
            //需要占用的内存
            int needUseMem = Constants.ENV_MAP.get(Constants.CUR_MEM_NEED_USED);
            if (buffer != null) {
                ((DirectBuffer)buffer).cleaner().clean();
            }
            //需要占用的内存大于0时，才去占用
            if (needUseMem > 0) {
                buffer = ByteBuffer.allocateDirect((int)needUseMem * 1024);
            }
            try {
                //休眠30秒，防止cpu占用
                Thread.sleep(30*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long startTime = System.currentTimeMillis();
            if ((startTime - start) > runtime) {
                log.info("内存占用程序结束运行！");
                break;
            }

        }
        //强制释放内存
        System.gc();
        System.runFinalization();
        System.gc();
        if (buffer != null) {
            ((DirectBuffer)buffer).cleaner().clean();
        }
        log.info("线程结束！");
    }

    private long getNeedUseMem() {
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
        //需要占用的内存大小，单位为kb
        long needUseMem = Math.round(memTotal * (memSetRate - memUsedRate)/100);
        return needUseMem;
    }
}
