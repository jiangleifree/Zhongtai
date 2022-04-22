package cn.cnic.zhongtai.system.neo4j;

import lombok.Data;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class Neo4jFactory {

    private static Config defaultConfig
            = Config.build()
            .withEncryption()
            .withConnectionTimeout(120, TimeUnit.SECONDS)
            .withMaxConnectionLifetime(60, TimeUnit.MINUTES)
            .withMaxConnectionPoolSize(2000)
            .withConnectionAcquisitionTimeout(20, TimeUnit.SECONDS)
            .toConfig();

    private static ConcurrentHashMap<String, Driver> driverPool = new ConcurrentHashMap();
    private static Lock lock = new ReentrantLock();

    /**
     * @param connectionTimeout
     * @param maxConnectionLifetime
     * @param maxConnectionPoolSize
     * @param connectionAcquisitionTimeout
     * @return
     */
    public Config newConfig(long connectionTimeout,
                            long maxConnectionLifetime,
                            int maxConnectionPoolSize,
                            long connectionAcquisitionTimeout) {
        Config config
                = Config.build()
                .withEncryption()
                .withConnectionTimeout(connectionTimeout, TimeUnit.SECONDS)
                .withMaxConnectionLifetime(maxConnectionLifetime, TimeUnit.MINUTES)
                .withMaxConnectionPoolSize(maxConnectionPoolSize)
                .withConnectionAcquisitionTimeout(connectionAcquisitionTimeout, TimeUnit.SECONDS)
                .toConfig();
        return config;
    }

    public static void addDriverToPool(String key, Driver driver) {
        lock.lock();
        try {
            removeDriverFromPool(key);
            driverPool.put(key, driver);
        } finally {
            lock.unlock();
        }
    }

    public static Driver removeDriverFromPool(String key) {
        lock.lock();
        try {
            Driver remove = driverPool.remove(key);
            if (remove != null) {
                remove.close();
            }
            return remove;
        } finally {
            lock.unlock();
        }
    }

    public static Optional<Driver> getDriverFromPool(String key) {
        lock.lock();
        try {
            return Optional.of(driverPool.get(key));
        } catch (Exception e) {
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    public static boolean containsKeyDriver(String key) {
        return driverPool.containsKey(key);
    }

    public static boolean containsDriver(Driver driver) {
        return driverPool.contains(driver);
    }

    /**
     * @param uri
     * @param userName
     * @param password
     * @return
     */
    public static Optional<Driver> newDriver(String uri, String userName, String password) {
       return newDriver(uri, userName, password, defaultConfig);
    }

    /**
     * @param uri
     * @param userName
     * @param password
     * @param config
     * @return
     */
    public static Optional<Driver> newDriver(String uri, String userName, String password, Config config) {
        try {
            return Optional.of(GraphDatabase.driver(uri, AuthTokens.basic(userName, password), config));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
