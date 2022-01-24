package com.example.test.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author: chenwenhao
 * Date: 13:46 2020/12/15
 * Content:
 */
public class Constants {

    /**
     * 存放系统信息，比如cpu使用率
     */
    public static final Map<String, Integer> ENV_MAP = new ConcurrentHashMap<>();
    /**
     * cpu使用率key
     */
    public static final String CPU_RATE_KEY = "cpuRate";
    /**
     * 本次需要的cpu占用率
     */
    public static final String CUR_CPU_NEED_USED_RATE = "curCpuNeedUsedRate";
    /**
     * 本次需要的内存占用量
     */
    public static final String CUR_MEM_NEED_USED = "curMemNeedUsed";

    /**
     * 本次增量更新的内存占用量
     */
    public static final String INCREASE_MEM_NEED_USED = "increaseMem";
}
