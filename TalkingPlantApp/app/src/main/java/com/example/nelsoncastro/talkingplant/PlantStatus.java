package com.example.nelsoncastro.talkingplant;

public class PlantStatus {

    boolean autoWatering, beingWatered, greet;
    int light, water;
    float temperature;

    public PlantStatus() {

    }

    public PlantStatus(boolean autoWatering, boolean beingWatered, boolean greet, int light, float temperature, int water) {
        this.autoWatering = autoWatering;
        this.greet = greet;
        this.beingWatered = beingWatered;
        this.light = light;
        this.water = water;
        this.temperature = temperature;
    }

    public boolean isAutoWatering() {
        return autoWatering;
    }

    public void setAutoWatering(boolean autoWatering) {
        this.autoWatering = autoWatering;
    }

    public boolean isGreet() {
        return greet;
    }

    public void setGreet(boolean greet) {
        this.greet = greet;
    }

    public boolean isBeingWatered() {
        return beingWatered;
    }

    public void setBeingWatered(boolean beingWatered) {
        this.beingWatered = beingWatered;
    }

    public int getLight() {
        return light;
    }

    public void setLight(int light) {
        this.light = light;
    }

    public int getWater() {
        return water;
    }

    public void setWater(int water) {
        this.water = water;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }
}
