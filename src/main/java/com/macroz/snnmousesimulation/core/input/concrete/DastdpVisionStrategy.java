package com.macroz.snnmousesimulation.core.input.concrete;

import com.macroz.snnmousesimulation.core.input.InputFrame;
import com.macroz.snnmousesimulation.core.input.InputStrategy;
import com.macroz.snnmousesimulation.utility.ConfigParameterReader;
import com.macroz.snnmousesimulation.world.AgentView;
import com.macroz.snnmousesimulation.world.Food;

import java.util.List;
import java.util.Map;
import java.util.Random;

public final class DastdpVisionStrategy implements InputStrategy {

    private final double rangeCurrentGain;
    private final double range;
    private final double halfFovRadians;
    private final double centerAngleRadians;
    private final double partnerCenterAngleRadians;
    private final double objectRadius;
    private final double cycleMs;
    private final Random random;

    private long lastCycleStart = -1;

    private DastdpVisionStrategy(double rangeCurrentGain, double range, double fovDegrees,
                                 double centerAngleDegrees, double partnerCenterAngleDegrees,
                                 double objectRadius, double cycleMs, Long seed) {
        if (fovDegrees <= 0 || range <= 0 || rangeCurrentGain < 0) {
            throw new IllegalArgumentException("fov, range must be positive; range_current_gain non-negative.");
        }
        this.rangeCurrentGain = rangeCurrentGain;
        this.range = range;
        this.halfFovRadians = Math.toRadians(fovDegrees) / 2.0;
        this.centerAngleRadians = Math.toRadians(centerAngleDegrees);
        this.partnerCenterAngleRadians = Math.toRadians(partnerCenterAngleDegrees);
        this.objectRadius = objectRadius;
        this.cycleMs = cycleMs;
        this.random = seed == null ? new Random() : new Random(seed);
    }

    public static DastdpVisionStrategy create(Map<String, Object> params) {
        return new DastdpVisionStrategy(
                ConfigParameterReader.getDouble(params, "range_current_gain", 30.0),
                ConfigParameterReader.getDouble(params, "range", 300.0),
                ConfigParameterReader.getDouble(params, "fov", 90.0),
                ConfigParameterReader.getDouble(params, "center_angle", 0.0),
                ConfigParameterReader.getDouble(params, "partner_center_angle", 0.0),
                ConfigParameterReader.getDouble(params, "object_radius", 0.0),
                ConfigParameterReader.getDouble(params, "cycle_ms", 70.0),
                ConfigParameterReader.getOptionalLong(params, "seed"));
    }

    @Override
    public double[] calculateCurrents(InputFrame frame, int targetNeuronCount) {
        double[] currents = new double[targetNeuronCount];
        if (targetNeuronCount <= 0) return currents;

        long cycleStart = (long) (frame.simulationTimeMs() - (frame.simulationTimeMs() % cycleMs));
        if (cycleStart == lastCycleStart) {
            return currents;
        }
        lastCycleStart = cycleStart;

        double myValue = wtaValue(frame);
        if (myValue <= 0.0) return currents;

        for (int i = 0; i < targetNeuronCount; i++) {
            currents[i] = poissonDraw(random, rangeCurrentGain * myValue);
        }
        return currents;
    }

    private double wtaValue(InputFrame frame) {
        double own = rawValue(frame, centerAngleRadians);
        double partner = rawValue(frame, partnerCenterAngleRadians);
        if (own > 0 && partner > 0) {
            if (partner > own) own = 0.0;
            else if (own > partner) { }
            else own = 0.0;
        }
        return own;
    }

    private double rawValue(InputFrame frame, double center) {
        AgentView agent = frame.agent();
        List<Food> food = frame.world().food();
        double best = 0.0;
        for (Food f : food) {
            double dx = frame.world().wrapDeltaX(f.x() - agent.x());
            double dy = frame.world().wrapDeltaY(f.y() - agent.y());
            double dist = Math.sqrt(dx * dx + dy * dy) - objectRadius;
            if (dist < 0) dist = 0;
            if (dist >= range) continue;

            double bearing = normalize(Math.atan2(dy, dx) - agent.angle());
            if (Math.abs(bearing - center) > halfFovRadians) continue;

            double value = 1.0 - dist / range;
            if (value > best) best = value;
        }
        return best;
    }

    private static double normalize(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }

    static long poissonDraw(Random rng, double mean) {
        if (mean <= 0.0) return 0;
        if (mean > 6_000.0) {
            return Math.max(0, Math.round(mean + Math.sqrt(mean) * rng.nextGaussian()));
        }
        double L = Math.exp(-mean);
        long k = 0;
        double p = 1.0;
        do {
            k++;
            p *= rng.nextDouble();
        } while (p > L);
        return k - 1;
    }
}
