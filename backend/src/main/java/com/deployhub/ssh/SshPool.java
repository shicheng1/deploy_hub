package com.deployhub.ssh;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SshPool {

    @Value("${deploy.ssh.pool.max-total:20}")
    private int maxTotal;

    @Value("${deploy.ssh.pool.max-idle:10}")
    private int maxIdle;

    @Value("${deploy.ssh.pool.min-idle:2}")
    private int minIdle;

    @Value("${deploy.ssh.pool.timeout:30000}")
    private int timeout;

    private final ConcurrentHashMap<String, GenericObjectPool<Session>> poolMap = new ConcurrentHashMap<>();

    private String buildKey(String host, int port) {
        return host + ":" + port;
    }

    public Session borrowSession(String host, int port, String username, String password, String privateKey) throws Exception {
        String key = buildKey(host, port);
        GenericObjectPool<Session> pool = poolMap.computeIfAbsent(key, k -> createPool(host, port, username, password, privateKey));
        return pool.borrowObject();
    }

    public void returnSession(String host, int port, Session session) {
        String key = buildKey(host, port);
        GenericObjectPool<Session> pool = poolMap.get(key);
        if (pool != null && session != null) {
            pool.returnObject(session);
        }
    }

    public void invalidateSession(String host, int port, Session session) {
        String key = buildKey(host, port);
        GenericObjectPool<Session> pool = poolMap.get(key);
        if (pool != null && session != null) {
            try {
                pool.invalidateObject(session);
            } catch (Exception e) {
                log.error("销毁SSH Session失败", e);
            }
        }
    }

    public void removePool(String host, int port) {
        String key = buildKey(host, port);
        GenericObjectPool<Session> pool = poolMap.remove(key);
        if (pool != null) {
            pool.close();
        }
    }

    private GenericObjectPool<Session> createPool(String host, int port, String username, String password, String privateKey) {
        GenericObjectPoolConfig<Session> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(false);

        return new GenericObjectPool<>(new BasePooledObjectFactory<Session>() {
            @Override
            public Session create() throws JSchException {
                Session session = SshClient.createSession(host, port, username, password, privateKey);
                session.setTimeout(timeout);
                session.connect();
                return session;
            }

            @Override
            public PooledObject<Session> wrap(Session session) {
                return new DefaultPooledObject<>(session);
            }

            @Override
            public boolean validateObject(PooledObject<Session> p) {
                return p.getObject().isConnected();
            }

            @Override
            public void destroyObject(PooledObject<Session> p) {
                p.getObject().disconnect();
            }
        }, config);
    }

    @PreDestroy
    public void destroy() {
        poolMap.forEach((key, pool) -> pool.close());
        poolMap.clear();
    }
}
