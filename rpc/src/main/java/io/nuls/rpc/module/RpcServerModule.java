package io.nuls.rpc.module;


import io.nuls.core.module.NulsModule;

/**
 * Created by Niels on 2017/9/26.
 * nuls.io
 */
public abstract class RpcServerModule extends NulsModule {
    protected RpcServerModule(){
        super(RpcServerModule.class.getSimpleName());
    }
}
