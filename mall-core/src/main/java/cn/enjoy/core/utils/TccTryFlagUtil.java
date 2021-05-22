package cn.enjoy.core.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TCC事务第一阶段成功标识
 * @Classname TccTryFlagUtil
 * @Description TODO
 * @Author Jack
 * Date 2020/10/20 16:29
 * Version 1.0
 */
public class TccTryFlagUtil {
    private static Map<String,String> map = new ConcurrentHashMap<>();

    public static void setFlag(String xid,String v) {
        map.put(xid,v);
    }

    public static String getFlag(String xid) {
        return map.get(xid);
    }

    public static void removeFlag(String xid) {
        map.remove(xid);
    }
}
