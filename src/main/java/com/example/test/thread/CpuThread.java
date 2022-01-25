package com.example.test.thread;

import com.example.test.common.Constants;
import com.example.test.task.CpuImproveTask;
import lombok.extern.slf4j.Slf4j;

/**
 * Author: chenwenhao
 * Date: 13:40 2020/8/24
 * Content:
 */
@Slf4j
public class CpuThread extends Thread{

    private Integer rate;
    private long runtime;
    private String enable;
    /**
     * 定时任务间隔时间
     */
    private long betweenTime;

    @Override
    public void run() {
        if (!"true".equals(enable)) {
            return;
        }
        long start = System.currentTimeMillis();
        while(true){
            //当前的cpu使用率
            int usedRate = Constants.ENV_MAP.get(Constants.CPU_RATE_KEY) == null ? 0 : Constants.ENV_MAP.get(Constants.CPU_RATE_KEY);
            //需要占用的cpu使用率
            int needToUseRate = Constants.ENV_MAP.get(Constants.CUR_CPU_NEED_USED_RATE) == null ? 0 : Constants.ENV_MAP.get(Constants.CUR_CPU_NEED_USED_RATE);;
            log.info("当前cpu使用率为：" + usedRate + "   需要占用的cpu使用率为：" + needToUseRate);
            int busyTime = needToUseRate*10;
            int idleTime = 1000 - busyTime;
            long startTime = System.currentTimeMillis();
            if ((startTime - start) > runtime || (startTime - start) > betweenTime) {
                break;
            }
            while((System.currentTimeMillis() - startTime)<=busyTime);
            try {
                if (idleTime > 0) {
                    Thread.sleep(idleTime);
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        log.info("cpu定时任务结束！");
    }



    public Integer getRate() {
        return rate;
    }

    public void setRate(Integer rate) {
        this.rate = rate;
    }

    public long getRuntime() {
        return runtime;
    }

    public void setRuntime(long runtime) {
        this.runtime = runtime;
    }

    public String getEnable() {
        return enable;
    }

    public void setEnable(String enable) {
        this.enable = enable;
    }

    public long getBetweenTime() {
        return betweenTime;
    }

    public void setBetweenTime(long betweenTime) {
        this.betweenTime = betweenTime;
    }
}
