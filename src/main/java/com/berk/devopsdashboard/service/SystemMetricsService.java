package com.berk.devopsdashboard.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import java.text.DecimalFormat;

@Service
@Slf4j
public class SystemMetricsService {

    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hardware;
    private long[] prevTicks;

    public SystemMetricsService() {
        this.systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        this.prevTicks = hardware.getProcessor().getSystemCpuLoadTicks();
    }
    public double getCpuUsage() {
        CentralProcessor processor = hardware.getProcessor();
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        prevTicks = processor.getSystemCpuLoadTicks();
        return Math.round(cpuLoad * 100.0) / 100.0;
    }
    public double getRamUsage() {
        GlobalMemory memory = hardware.getMemory();
        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;
        
        double percentage = ((double) used / total) * 100;
        return Math.round(percentage * 100.0) / 100.0;
    }
    public String getTotalRam() {
        GlobalMemory memory = hardware.getMemory();
        return formatBytes(memory.getTotal());
    }
    private String formatBytes(long bytes) {
        long gb = 1024 * 1024 * 1024;
        double value = (double) bytes / gb;
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(value) + " GB";
    }
}