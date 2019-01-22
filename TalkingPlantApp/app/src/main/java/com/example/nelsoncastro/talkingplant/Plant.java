package com.example.nelsoncastro.talkingplant;

public class Plant {

    private String name, personality;
    private int minWater, maxWater, minLight, maxLight;
    private float minTemperature, maxTemperature;

    public Plant(String name, int minLight, int maxLight, int minWater, int maxWater, float minTemperature, float maxTemperature, String personality) {
        this.name = name;
        this.minLight = minLight;
        this.maxLight = maxLight;
        this.minWater = minWater;
        this.maxWater = maxWater;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.personality = personality;
    }

    public String getName() {
        return name;
    }

    public int getMinLight() {
        return minLight;
    }

    public void setMinLight(int minLight) {
        this.minLight = minLight;
    }

    public int getMaxLight() {
        return maxLight;
    }

    public void setMaxLight(int maxLight) {
        this.maxLight = maxLight;
    }

    public int getMinWater() {
        return minWater;
    }

    public void setMinWater(int minWater) {
        this.minWater = minWater;
    }

    public int getMaxWater() {
        return maxWater;
    }

    public void setMaxWater(int maxWater) {
        this.maxWater = maxWater;
    }

    public float getMinTemperature() {
        return minTemperature;
    }

    public void setMinTemperature(int minTemperature) {
        this.minTemperature = minTemperature;
    }

    public float getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(int maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public String getPersonality() {
        return personality;
    }
}
