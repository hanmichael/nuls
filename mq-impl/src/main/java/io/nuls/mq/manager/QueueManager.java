package io.nuls.mq.manager;

import io.nuls.core.constant.ErrorCode;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.utils.log.Log;
import io.nuls.mq.entity.StatInfo;
import io.nuls.mq.entity.impl.StatInfoImpl;
import io.nuls.mq.fqueue.exception.FileFormatException;
import io.nuls.mq.intf.NulsQueue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 持久化队列管理器
 * Created by Niels on 2017/9/20.
 */
public abstract class QueueManager {
    private static final Map<String, NulsQueue> queuesMap = new HashMap<>();
    private static final Map<String, Lock> lockMap = new HashMap<>();
    //统计日志时间段
    private static final int LatelySecond = 10;

    private static boolean Running = false;

    public static void logQueueStatus() {
        for (Map.Entry<String, NulsQueue> entry : queuesMap.entrySet()) {
            try {
                NulsQueue queue = entry.getValue();
                long nowIn = queue.getStatInfo().getInCount().get();
                long nowOut = queue.getStatInfo().getOutCount().get();
                long latelyInTps = (nowIn - queue.getStatInfo().getLastInCount()) / queue.getStatInfo().getLatelySecond();
                long latelyOutTps = (nowOut - queue.getStatInfo().getLastOutCount()) / queue.getStatInfo().getLatelySecond();
                queue.getStatInfo().setLatelyInTps(latelyInTps);
                queue.getStatInfo().setLatelyOutTps(latelyOutTps);
                queue.getStatInfo().setLastInCount(nowIn);
                queue.getStatInfo().setLastOutCount(nowOut);
                Log.info(queue.getStatInfo().toString());
            } catch (Exception e) {
            }
        }
    }

    public static final int getLatelySecond() {
        return LatelySecond;
    }

    /**
     * 将队列加入管理中
     *
     * @param queueName 队列名称
     * @param queue     队列实例
     */
    public static void initQueue(String queueName, NulsQueue queue) {
        initQueue(queueName, queue, LatelySecond);
    }

    /**
     * 将队列加入管理中
     *
     * @param queueName    队列名称
     * @param queue        队列实例
     * @param latelySecond 统计日志时间段
     */
    public static void initQueue(String queueName, NulsQueue queue, int latelySecond) {
        if(!Running){
            throw new NulsRuntimeException(ErrorCode.FAILED,"The DBModule is not running!");
        }
        if (queuesMap.containsKey(queueName)) {
            throw new NulsRuntimeException(ErrorCode.FAILED,"queue name is allready exist");
        }
        if (latelySecond == 0) {
            latelySecond = LatelySecond;
        }
        Log.debug("队列初始化，名称：{}，单个文件最大大小：{}", queue.getQueueName(), queue.getMaxSize());
        queue.setStatInfo(new StatInfoImpl(queue.getQueueName(), queue.size(), latelySecond));
        queuesMap.put(queueName, queue);
        lockMap.put(queueName, new ReentrantLock());
    }

    public static void destroyQueue(String queueName) throws IOException, FileFormatException { if(!Running){
        throw new NulsRuntimeException(ErrorCode.FAILED,"The DBModule is not running!");
    }
        NulsQueue queue = queuesMap.get(queueName);
        if (null == queue) {
            throw new NulsRuntimeException(ErrorCode.FAILED,"queue not exist");
        }
        queue.distroy();
        queuesMap.remove(queueName);
        Log.debug("队列销毁，名称：{}。", queueName);
    }

    public static Object take(String queueName) throws InterruptedException { if(!Running){
        throw new NulsRuntimeException(ErrorCode.FAILED,"The DBModule is not running!");
    }
        NulsQueue queue = queuesMap.get(queueName);
        if (null == queue) {
            throw new NulsRuntimeException(ErrorCode.FAILED,"queue not exist");
        }
        Object value = queue.take();
        queue.getStatInfo().takeOne();
        Log.debug("从队列中取出数据，名称：{}，当前长度：{}。", queueName, queue.size());
        return value;
    }

    public static Object poll(String queueName) { if(!Running){
        throw new NulsRuntimeException(ErrorCode.FAILED,"The DBModule is not running!");
    }
        NulsQueue queue = queuesMap.get(queueName);
        if (null == queue) {
            throw new NulsRuntimeException(ErrorCode.FAILED,"queue not exist");
        }
        Object obj = queue.poll();
        boolean notNull = null != obj;
        if (notNull) {
            queue.getStatInfo().takeOne();
            Log.debug("从队列中取出数据，名称：{}，当前长度：{}。", queueName, queue.size());
        }
        return obj;
    }

    public static void offer(String queueName, Object item) { if(!Running){
        throw new NulsRuntimeException(ErrorCode.FAILED,"The DBModule is not running!");
    }
        NulsQueue queue = queuesMap.get(queueName);
        if (null == queue) {
            throw new NulsRuntimeException(ErrorCode.FAILED,"queue not exist");
        }

        queue.offer(item);
        queue.getStatInfo().putOne();
        Log.debug("向队列中加入数据，名称：{}，当前长度：{}。", queueName, queue.size());
    }

    public static void clear(String queueName) { if(!Running){
        throw new NulsRuntimeException(ErrorCode.FAILED,"The DBModule is not running!");
    }
        NulsQueue queue = queuesMap.get(queueName);
        if (null == queue) {
            throw new NulsRuntimeException(ErrorCode.FAILED,"queue not exist");
        }
        Log.debug("清空队列数据，名称：{}，当前长度：{}。", queueName, queue.size());
        queue.clear();
    }

    public static void close(String queueName) throws NulsRuntimeException { if(!Running){
        throw new NulsRuntimeException(ErrorCode.FAILED,"The DBModule is not running!");
    }
        NulsQueue queue = queuesMap.get(queueName);
        if (null == queue) {
            throw new NulsRuntimeException(ErrorCode.FAILED,"queue not exist");
        }
        try{
            queue.close();
        }catch(Exception e){
            throw new NulsRuntimeException(e);
        }
        Log.debug("关闭队列实例，名称：{}，当前长度：{}。", queueName, queue.size());
    }

    public static long size(String queueName) { if(!Running){
        throw new NulsRuntimeException(ErrorCode.FAILED,"The DBModule is not running!");
    }
        NulsQueue queue = queuesMap.get(queueName);
        if (null == queue) {
            throw new NulsRuntimeException(ErrorCode.FAILED,"queue not exist");
        }
        return queue.size();
    }

    public static long getMaxSize(String queueName) { if(!Running){
        throw new NulsRuntimeException(ErrorCode.FAILED,"The DBModule is not running!");
    }
        NulsQueue queue = queuesMap.get(queueName);
        if (null == queue) {
            throw new NulsRuntimeException(ErrorCode.FAILED,"queue not exist");
        }
        return queue.getMaxSize();
    }

    public static StatInfo getStatInfo(String queueName) { if(!Running){
        throw new NulsRuntimeException(ErrorCode.FAILED,"The DBModule is not running!");
    }
        NulsQueue queue = queuesMap.get(queueName);
        if (null == queue) {
            throw new NulsRuntimeException(ErrorCode.FAILED,"queue not exist");
        }
        return queue.getStatInfo();
    }

    public static List<StatInfo> getAllStatInfo(){
        List<StatInfo> list = new ArrayList<>();
        for(NulsQueue queue:queuesMap.values()){
            list.add(queue.getStatInfo());
        }
        return list;
    }
    public static void setRunning(boolean running) {
        Running = running;
    }
}
