package com.macroz.snnmousesimulation.world;

import com.macroz.snnmousesimulation.core.SnnNetworkData;
import com.macroz.snnmousesimulation.core.input.InputFrame;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class World {
    @Getter
    private final double width;
    @Getter
    private final double height;

    @Getter
    private double simulationTimeMs = 0;
    @Getter
    private final Agent agent;
    @Getter
    private final List<Food> food;
    private final WorldView worldView;
    private List<AgentEvent> pendingAgentEvents = List.of();
    private final List<AgentEvent> currentTickEvents = new ArrayList<>();

    private final Random random = new Random();

    private static final double EAT_RADIUS = 30.0;
    @Getter
    private int foodEatenCount = 0;

    public World(double width, double height, int numberOfFood, SnnNetworkData snnNetworkData) {
        this.width = width;
        this.height = height;
        this.food = new ArrayList<>();
        this.worldView = WorldView.from(this);
        this.agent = new Agent(width / 2, height / 2, snnNetworkData);
        initializeFood(numberOfFood);
    }

    public void update(double deltaTime) {
        InputFrame inputFrame = new InputFrame(
                AgentView.from(agent),
                worldView,
                pendingAgentEvents,
                simulationTimeMs,
                deltaTime
        );

        agent.update(inputFrame);
        pendingAgentEvents = List.of();
        currentTickEvents.clear();
        handleBoundaries();

        List<Food> collidedFoods = new ArrayList<>();
        for (Food f : food) {
            if (distanceSquared(f.x(), f.y(), agent.getX(), agent.getY()) < EAT_RADIUS * EAT_RADIUS) {
                collidedFoods.add(f);
            }
        }
        for (Food f : collidedFoods) {
            handleFoodCollision(f, simulationTimeMs);
        }

        pendingAgentEvents = currentTickEvents.isEmpty() ? List.of() : List.copyOf(currentTickEvents);
        simulationTimeMs += deltaTime;
    }

    private void handleFoodCollision(Food eatenFood, double timestampMs) {
        food.remove(eatenFood);
        foodEatenCount++;
        agent.applyReward();
        currentTickEvents.add(new FoodEaten(timestampMs));
        spawnSingleFood();
    }

    private void initializeFood(int numberOfFood) {
        for (int i = 0; i < numberOfFood; i++) {
            spawnSingleFood();
        }
    }

    private void spawnSingleFood() {
        double x;
        double y;
        do {
            x = random.nextDouble(0, width);
            y = random.nextDouble(0, height);
        } while (distanceSquared(x, y, agent.getX(), agent.getY()) < EAT_RADIUS * EAT_RADIUS);
        food.add(new Food(x, y));
    }

    private double distanceSquared(double x1, double y1, double x2, double y2) {
        double dx = wrapDelta(x1 - x2, width);
        double dy = wrapDelta(y1 - y2, height);
        return dx * dx + dy * dy;
    }

    public static double wrapDelta(double delta, double size) {
        if (size <= 0.0) return delta;
        double half = size * 0.5;
        double rem = (delta + half) % size;
        if (rem < 0) {
            rem += size;
        }
        return rem - half;
    }

    public static double wrapCoordinate(double coord, double size) {
        if (size <= 0.0) return coord;
        double wrapped = coord % size;
        if (wrapped < 0) {
            wrapped += size;
        }
        return wrapped == 0.0 ? 0.0 : wrapped;
    }

    private void handleBoundaries() {
        double x = agent.getX();
        double y = agent.getY();

        double wrappedX = wrapCoordinate(x, width);
        double wrappedY = wrapCoordinate(y, height);

        if (wrappedX != x) {
            agent.setX(wrappedX);
        }
        if (wrappedY != y) {
            agent.setY(wrappedY);
        }
    }
}
