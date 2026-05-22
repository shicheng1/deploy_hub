package com.deployhub.vo;

import lombok.Data;

@Data
public class DashboardStatsVO {

    private Long totalServers;

    private Long totalApps;

    private Long todayDeploys;

    private Double successRate;
}
