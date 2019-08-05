package com.mbientlab.metawear.tutorial.exergame;

/**
 * Created by rohilsheth on 4/10/18.
 */

public class Enemy {
    int xpos;
    int fallSpeed;

    public int getXpos() {
        return xpos;
    }
    public void setXpos(int xpos) {
        this.xpos = xpos;
    }
    public int randomXpos1(){
        return (int) (Math.random()*325);
    }
    public int randomXpos2(){
        return (int) (Math.random()*325);
    }

    public int getFallSpeed() {
        return fallSpeed;
    }

    public void setFallSpeed(int fallSpeed) {
        this.fallSpeed = fallSpeed;
    }

}
