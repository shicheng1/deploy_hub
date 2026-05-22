package com.deployhub.deploy;

import com.deployhub.entity.DeployRecord;
import com.deployhub.mapper.DeployRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
public class DeployEngine {

    @Autowired
    private DeployRecordMapper deployRecordMapper;

    private ThreadPoolExecutor executor;

    @PostConstruct
    public void init() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = corePoolSize * 2;
        executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @PreDestroy
    public void destroy() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public Long submitDeploy(DeployTask task) {
        DeployRecord record = new DeployRecord();
        record.setAppId(task.getAppId());
        record.setServerId(task.getServerId());
        record.setVersion(task.getVersion());
        record.setDeployScript(task.getScript());
        record.setStatus(task.isRollback() ? DeployStatus.ROLLING_BACK.name() : DeployStatus.PENDING.name());
        record.setOperator(task.getOperator());
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        deployRecordMapper.insert(record);

        task.setRecordId(record.getId());
        executor.submit(task);

        log.info("提交部署任务, recordId={}", record.getId());
        return record.getId();
    }

    public void batchDeploy(List<DeployTask> tasks) {
        for (DeployTask task : tasks) {
            submitDeploy(task);
        }
        log.info("批量提交部署任务, count={}", tasks.size());
    }

    public boolean isRunning(Long recordId) {
        return executor.getActiveCount() > 0;
    }
}
