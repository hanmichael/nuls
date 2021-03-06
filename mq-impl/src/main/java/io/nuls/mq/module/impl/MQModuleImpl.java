package io.nuls.mq.module.impl;


import io.nuls.core.thread.NulsThread;
import io.nuls.mq.entity.StatInfo;
import io.nuls.mq.manager.QueueManager;
import io.nuls.mq.module.MQModule;
import io.nuls.mq.service.impl.DisruptorServiceImpl;
import io.nuls.mq.service.impl.QueueServiceImpl;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Niels on 2017/9/27.
 * nuls.io
 */
public class MQModuleImpl extends MQModule {

    private ScheduledExecutorService service;

    public MQModuleImpl() {
        super();
    }

    @Override
    public void start() {
        NulsThread t1 = new NulsThread(this, "queueStatusLogThread") {
            @Override
            public void run() {
                QueueManager.logQueueStatus();
            }
        };
        //启动速度统计任务
        service = new ScheduledThreadPoolExecutor(1);
        service.scheduleAtFixedRate(t1, 0, QueueManager.getLatelySecond(), TimeUnit.SECONDS);
        this.registerService(service);
        this.registerService(QueueServiceImpl.getInstance());
        this.registerService(DisruptorServiceImpl.getInstance());
        QueueManager.setRunning(true);
    }

    @Override
    public void shutdown() {
        QueueManager.setRunning(false);
        service.shutdown();
    }

    @Override
    public void destroy() {
        shutdown();
    }

    @Override
    public String getInfo() {
        StringBuilder str = new StringBuilder();
        str.append("moduleName:");
        str.append(getModuleName());
        str.append(",moduleStatus:");
        str.append(getStatus());
        str.append(",ThreadCount:");
        List<NulsThread> threadList = this.getThreadList();
        str.append(threadList.size());
        str.append("ThreadInfo:\n");
        for (NulsThread t : threadList) {
            str.append(t.getInfo());
        }
        str.append("QueueInfo:\n");
        List<StatInfo> list = QueueManager.getAllStatInfo();
        for (StatInfo si : list) {
            str.append(si.toString());
        }
        return str.toString();
    }

    @Override
    public String getVersion() {
        return null;
    }
}
