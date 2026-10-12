package com.macroz.snnmousesimulation.core.output.concrete;

import com.macroz.snnmousesimulation.core.output.OutputStrategy;
import com.macroz.snnmousesimulation.utility.ConfigParameterReader;
import com.macroz.snnmousesimulation.world.Agent;

import java.util.Map;

public final class DastdpMotorDriveStrategy implements OutputStrategy {

    private final double vMaxPerSecond;
    private final double vMinPerSecond;
    private final double wheelBasePx;
    private final long cycleMs;

    private int leftSpikes = 0;
    private int rightSpikes = 0;
    private double cycleElapsedMs = 0.0;

    private DastdpMotorDriveStrategy(double vMaxPerSecond, double vMinPerSecond,
                                     double wheelBasePx, long cycleMs) {
        this.vMaxPerSecond = vMaxPerSecond;
        this.vMinPerSecond = vMinPerSecond;
        this.wheelBasePx = wheelBasePx;
        this.cycleMs = cycleMs;
    }

    public static DastdpMotorDriveStrategy create(Map<String, Object> params) {
        double vMax = ConfigParameterReader.getDouble(params, "v_max_per_s", 312.0);
        double vMin = ConfigParameterReader.getDouble(params, "v_min_per_s", 250.0);
        double wheelBase = ConfigParameterReader.getDouble(params, "wheel_base_px", 10.0);
        long cycle = (long) ConfigParameterReader.getDouble(params, "cycle_ms", 70.0);
        return new DastdpMotorDriveStrategy(vMax, vMin, wheelBase, cycle);
    }

    @Override
    public void apply(Agent agent, boolean[] firedLocalIndices, double deltaTimeMs) {
        int count = firedLocalIndices.length;
        if (count < 2) return;

        int midPoint = count / 2;
        for (int i = 0; i < midPoint; i++) {
            if (firedLocalIndices[i]) leftSpikes++;
        }
        for (int i = midPoint; i < count; i++) {
            if (firedLocalIndices[i]) rightSpikes++;
        }
        cycleElapsedMs += deltaTimeMs;

        if (cycleElapsedMs + 1e-9 < cycleMs) return;

        double dtSeconds = cycleElapsedMs / 1000.0;
        int cmp = Integer.compare(leftSpikes, rightSpikes);
        double vLeft = cmp > 0 ? vMaxPerSecond : vMinPerSecond;
        double vRight = cmp > 0 ? vMinPerSecond
                : cmp < 0 ? vMaxPerSecond : 0.5 * (vMaxPerSecond + vMinPerSecond);

        double v = 0.5 * (vLeft + vRight);
        double omega = (vLeft - vRight) / wheelBasePx;

        double dTheta = omega * dtSeconds;
        double dx;
        double dy;
        if (Math.abs(dTheta) < 1e-9) {
            double d = v * dtSeconds;
            dx = d * Math.cos(agent.getAngle());
            dy = d * Math.sin(agent.getAngle());
        } else {
            double theta0 = agent.getAngle();
            double theta1 = theta0 + dTheta;
            double r = v / omega;
            dx = r * (Math.sin(theta1) - Math.sin(theta0));
            dy = -r * (Math.cos(theta1) - Math.cos(theta0));
        }
        agent.move(Math.hypot(dx, dy), dTheta);

        leftSpikes = 0;
        rightSpikes = 0;
        cycleElapsedMs = 0.0;
    }
}
