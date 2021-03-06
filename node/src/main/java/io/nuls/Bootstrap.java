package io.nuls;


import io.nuls.core.constant.NulsConstant;
import io.nuls.core.exception.NulsException;
import io.nuls.core.i18n.I18nUtils;
import io.nuls.core.manager.ModuleManager;
import io.nuls.core.module.NulsModule;
import io.nuls.core.module.service.ModuleService;
import io.nuls.core.utils.cfg.ConfigLoader;
import io.nuls.core.utils.log.Log;

import java.io.IOException;

/**
 * System start class
 */
public class Bootstrap {

    public static void main(String[] args) {
        do {
            //load nuls.ini
            try {
                ConfigLoader.loadIni(NulsConstant.CONFIG_FILE);
            } catch (IOException e) {
                Log.error("Client start faild", e);
                break;
            }
            //set system language
            try {
                String language = ConfigLoader.getCfgValue(NulsConstant.CFG_SYSTEM_SECTION, NulsConstant.CFG_SYSTEM_LANGUAGE);
                I18nUtils.setLanguage(language);
            } catch (NulsException e) {
                Log.error(e);
            }
            //init modules
//            initDB();
            initMQ();
            initP2p();
            //init rpc server
            boolean result = initRpcServer();
            if (!result) {
                break;
            }
            Log.info("");
        } while (false);
        System.out.println("--------------------------------------------");
        System.out.println(ModuleManager.getInstance().getInfo());
        System.out.println("--------------------------------------------");
    }

    private static boolean initDB() {
        NulsModule dbModule = regModule(NulsConstant.CFG_BOOTSTRAP_DB_MODULE);
        dbModule.start();
        return true;
    }

    private static boolean initMQ() {
        NulsModule module = regModule(NulsConstant.CFG_BOOTSTRAP_QUEUE_MODULE);
        module.start();
        return true;
    }

    private static boolean initP2p() {
        NulsModule module = regModule(NulsConstant.CFG_BOOTSTRAP_P2P_MODULE);
        module.start();
        return true;
    }

    /**
     * @return result
     */
    private static boolean initRpcServer() {
        NulsModule module = regModule(NulsConstant.CFG_BOOTSTRAP_RPC_SERVER_MODULE);
        module.start();
        return true;
    }


    private static NulsModule regModule(String key) {
        String moduleClass = null;
        try {
            moduleClass = ConfigLoader.getCfgValue(NulsConstant.CFG_BOOTSTRAP_SECTION, key);
        } catch (NulsException e) {
            Log.error(e);
        }
        try {
            return ModuleService.getInstance().loadModule(moduleClass);
        } catch (ClassNotFoundException e) {
            Log.error(e);
        } catch (IllegalAccessException e) {
            Log.error(e);
        } catch (InstantiationException e) {
            Log.error(e);
        }
        return null;
    }

}
