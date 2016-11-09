package de.htwdresden;

import org.junit.Assert;

public class StatisticTest {
    @org.junit.Test
    public void start() throws Exception {
        Statistic stat = Statistic.start();
        double startTime = stat.getStartTime();
        System.out.println(" Start Time : " + startTime );
        Assert.assertTrue(startTime > 0 );
        Assert.assertEquals(startTime,stat.getStartTime(), 0.00001);

    }
    @org.junit.Test
    public void timer() throws Exception {
        Statistic stat = Statistic.start();
        double startTime = stat.getStartTime();

        System.out.println(" Start Time : " + startTime );
        double playTime1 = stat.getPlayTime();
        System.out.println(" Play Time 1: " + playTime1 );
        Thread.sleep(2000);
        double playTime2 = stat.getPlayTime();
        System.out.println(" Play Time 2: " + playTime2 );
        Assert.assertTrue(playTime1 < playTime2);


    }

}