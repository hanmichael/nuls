package io.nuls.db.module.impl;

import com.alibaba.druid.pool.DruidDataSource;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.thread.NulsThread;
import io.nuls.core.utils.aop.AopUtils;
import io.nuls.db.dao.BlockDao;
import io.nuls.db.dao.filter.DBMethodFilter;
import io.nuls.db.dao.impl.mybatis.BlockDaoImpl;
import io.nuls.db.dao.impl.mybatis.session.SessionManager;
import io.nuls.db.exception.DBException;
import io.nuls.db.module.DBModule;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by zhouwei on 2017/9/27.
 * nuls.io
 */
public class MybatisDBModuleImpl extends DBModule {

    private SqlSessionFactory sqlSessionFactory;

    @Override
    public void start(){
        try {
            initSqlSessionFactory();
            initService();
        }catch (Exception e) {
            e.printStackTrace();
            throw new DBException(ErrorCode.DB_MODULE_START_FAIL);
        }

    }


    private void initSqlSessionFactory() throws IOException, SQLException {
        String resource = "mybatis/mybatis-config.xml";
        InputStream in = Resources.getResourceAsStream(resource);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(in);
        SessionManager.sqlSessionFactory = sqlSessionFactory;
    }


    private void initService() {
        BlockDao blockDao = AopUtils.createProxy(BlockDaoImpl.class, new DBMethodFilter());
        this.registerService(blockDao);
    }

    @Override
    public void shutdown() {
//        QueueManager.setRunning(false);
//        dbExecutor.shutdown();
        if(sqlSessionFactory != null) {
            DruidDataSource druidDataSource = (DruidDataSource) sqlSessionFactory.getConfiguration().getEnvironment().getDataSource();
            druidDataSource.close();
            sqlSessionFactory = null;
        }
    }

    @Override
    public void destroy(){
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
//        str.append("QueueInfo:\n");
//        List<StatInfo> list = QueueManager.getAllStatInfo();
//        for(StatInfo si :list){
//            str.append(si.toString());
//        }
        return str.toString();
    }

    @Override
    public String getVersion() {
        return null;
    }

    public SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

}
