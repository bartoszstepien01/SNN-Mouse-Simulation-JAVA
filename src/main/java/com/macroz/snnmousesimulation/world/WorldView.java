package com.macroz.snnmousesimulation.world;

import java.util.Collections;
import java.util.List;

public final class WorldView {

    private final List<Food> food;
    private final double width;
    private final double height;

    private WorldView(List<Food> food, double width, double height) {
        this.food = Collections.unmodifiableList(food);
        this.width = width;
        this.height = height;
    }

    public static WorldView from(World world) {
        return new WorldView(world.getFood(), world.getWidth(), world.getHeight());
    }

    public static WorldView of(List<Food> food, double width, double height) {
        return new WorldView(food, width, height);
    }

    public static WorldView of(List<Food> food) {
        return new WorldView(food, 0.0, 0.0);
    }

    public List<Food> food() {
        return food;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public double wrapDeltaX(double dx) {
        return World.wrapDelta(dx, width);
    }

    public double wrapDeltaY(double dy) {
        return World.wrapDelta(dy, height);
    }
}
