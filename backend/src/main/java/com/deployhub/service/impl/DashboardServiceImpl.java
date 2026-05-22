package com.deployhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.deployhub.entity.App;
import com.deployhub.entity.DeployRecord;
import com.deployhub.entity.Server;
import com.deployhub.mapper.AppMapper;
import com.deployhub.mapper.DeployRecordMapper;
import com.deployhub.mapper.ServerMapper;
import com.deployhub.service.DashboardService;
import com.deployhub.vo.DashboardStatsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private ServerMapper serverMapper;

    @Autowired
    private AppMapper appMapper;

    @Autowired
    private DeployRecordMapper deployRecordMapper;

    @Override
    public DashboardStatsVO getStats() {
        DashboardStatsVO vo = new DashboardStatsVO();

        vo.setTotalServers(serverMapper.selectCount(new LambdaQueryWrapper<Server>()));
        vo.setTotalApps(appMapper.selectCount(new LambdaQueryWrapper<App>()));

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        vo.setTodayDeploys(deployRecordMapper.selectCount(
                new LambdaQueryWrapper<DeployRecord>().ge(DeployRecord::getCreateTime, todayStart)));

        Long totalCount = deployRecordMapper.selectCount(new LambdaQueryWrapper<>());
        if (totalCount == 0) {
            vo.setSuccessRate(100.0);
        } else {
            Long successCount = deployRecordMapper.selectCount(
                    new LambdaQueryWrapper<DeployRecord>().eq(DeployRecord::getStatus, "SUCCESS"));
            vo.setSuccessRate(Math.round(successCount * 1000.0 / totalCount) / 10.0);
        }

        return vo;
    }
}
