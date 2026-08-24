package com.porfirio.orariprocida2011.entity;

import java.time.ZonedDateTime;

// NOTE: this class should be renamed to "Forecast" probably

public class Osservazione {

    public enum Direction {
        N, NE, E, SE, S, SW, W, NW
    }

    private Direction windDirection;
    private float windSpeed;
    private ZonedDateTime time;

    public Osservazione(float windSpeed, float windAngle, ZonedDateTime time) {
        setWindDirection(windAngle);
        setWindSpeed(windSpeed);
        setTime(time);
    }

    public Osservazione(float windSpeed, Direction windDirection, ZonedDateTime time) {
        setWindDirection(windDirection);
        setWindSpeed(windSpeed);
        setTime(time);
    }

    public float getWindBeaufort() {
        if (windSpeed <= 1)
            return 0;
        else if (windSpeed > 1 && windSpeed < 6)
            return (1 + (windSpeed - 3) / (5 - 1));
        else if (windSpeed >= 6 && windSpeed < 12)
            return (2 + (windSpeed - 8.5f) / (11 - 6));
        else if (windSpeed >= 12 && windSpeed < 20)
            return (3 + (windSpeed - 15.5f) / (19 - 12));
        else if (windSpeed >= 20 && windSpeed < 29)
            return (4 + (windSpeed - 24) / (28 - 20));
        else if (windSpeed >= 29 && windSpeed < 39)
            return (5 + (windSpeed - 33.5f) / (38 - 29));
        else if (windSpeed >= 39 && windSpeed < 50)
            return (6 + (windSpeed - 44) / (49 - 39));
        else if (windSpeed >= 50 && windSpeed < 62)
            return (7 + (windSpeed - 55.5f) / (61 - 50));
        else if (windSpeed >= 62 && windSpeed < 75)
            return (8 + (windSpeed - 68) / (74 - 62));
        else if (windSpeed >= 75 && windSpeed < 89)
            return (9 + (windSpeed - 81.5f) / (88 - 75));
        else if (windSpeed >= 89 && windSpeed < 103)
            return (10 + (windSpeed - 95.5f) / (102 - 89));
        else if (windSpeed >= 103 && windSpeed < 118)
            return (11 + (windSpeed - 110) / (117 - 103));
        else
            return 12.0f;
    }

    public Direction getWindDirection() {
        return windDirection;
    }

    private void setWindDirection(float angle) {
        int i = (int) (angle / 45) % 360;
        setWindDirection(Direction.values()[i]);
    }

    private void setWindDirection(Direction direction) {
        this.windDirection = direction;
    }

    public float getWindSpeed() {
        return windSpeed;
    }

    private void setWindSpeed(float speedKPH) {
        if (windSpeed == 0 || speedKPH > 0)
            windSpeed = speedKPH;
    }

    public ZonedDateTime getTime() {
        return time;
    }

    private void setTime(ZonedDateTime time) {
        this.time = time;
    }

}
