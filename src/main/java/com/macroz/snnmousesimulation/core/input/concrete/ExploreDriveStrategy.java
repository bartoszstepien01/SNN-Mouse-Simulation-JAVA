package com.macroz.snnmousesimulation.core.input.concrete;

import com.macroz.snnmousesimulation.core.input.InputFrame;
import com.macroz.snnmousesimulation.core.input.InputStrategy;
import com.macroz.snnmousesimulation.utility.ConfigParameterReader;

import java.util.Map;
import java.util.Random;

public final class ExploreDriveStrategy implements InputStrategy {

    private final double exploreCurrentMean;
    private final long cycleMs;
    private final Random random;
    private boolean driveLeftCached;
    private double cachedCycleStart = -1.0;

    private ExploreDriveStrategy(double exploreCurrentMean, long cycleMs, Long seed) {
        this.exploreCurrentMean = exploreCurrentMean;
        this.cycleMs = cycleMs;
        this.random = seed == null ? new Random() : new Random(seed);
    }

    public static ExploreDriveStrategy create(Map<String, Object> params) {
        return new ExploreDriveStrategy(
                ConfigParameterReader.getDouble(params, "explore_current_mean", 2.35),
                (long) ConfigParameterReader.getDouble(params, "cycle_ms", 70.0),
                ConfigParameterReader.getOptionalLong(params, "seed"));
    }

    @Override
    public double[] calculateCurrents(InputFrame frame, int targetNeuronCount) {
        if (targetNeuronCount <= 0) {
            return new double[0];
        }
        int midPoint = targetNeuronCount / 2;

        if (cycleMs > 0) {
            double cycleStart = frame.simulationTimeMs() - (frame.simulationTimeMs() % cycleMs);
            if (cycleStart != cachedCycleStart) {
                cachedCycleStart = cycleStart;
                driveLeftCached = random.nextBoolean();
            }
        } else {
            driveLeftCached = random.nextBoolean();
        }
        double[] currents = new double[targetNeuronCount];
        int lo = driveLeftCached ? 0 : midPoint;
        int hi = driveLeftCached ? midPoint : targetNeuronCount;
        for (int i = lo; i < hi; i++) {
            currents[i] = poissonDraw(exploreCurrentMean);
        }
        return currents;
    }

    private long poissonDraw(double mean) {
        if (mean > 6_000.0) {
            return (long) Math.round(mean + Math.sqrt(mean) * random.nextGaussian());
        }
        double L = Math.exp(-mean);
        long k = 0;
        double p = 1.0;
        do {
            k++;
            p *= random.nextDouble();
        } while (p > L);
        return k - 1;
    }
}
