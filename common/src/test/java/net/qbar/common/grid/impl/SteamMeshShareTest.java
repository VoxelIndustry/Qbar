package net.qbar.common.grid.impl;

import net.qbar.common.steam.LimitedSteamTank;
import net.qbar.common.steam.SteamTank;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

@RunWith(Parameterized.class)
public class SteamMeshShareTest
{
    @Parameterized.Parameters
    public static Collection<Object> data()
    {
        return Arrays.asList(Integer.MAX_VALUE, 64);
    }

    private int throttle;

    public SteamMeshShareTest(int throttle)
    {
        this.throttle = throttle;
    }

    @Test
    public void testOneEmptyOneFull()
    {
        SteamTank fullTank = new SteamTank(1000, 1000, 1);
        SteamTank emptyTank = new SteamTank(0, 1000, 1);

        SteamMesh mesh = new SteamMesh(this.throttle);
        mesh.addHandler(fullTank);
        mesh.addHandler(emptyTank);

        assertThat(mesh.getPressure()).isEqualTo(0.5f);

        // Used to simulate a full share with a throttle of 64
        for (int i = 0; i < 16; i++)
            mesh.tick();

        assertThat(fullTank.getSteam()).isEqualTo(emptyTank.getSteam());
        assertThat(fullTank.getSteam()).isEqualTo(500);
        assertThat(mesh.getPressure()).isEqualTo(0.5f);
    }

    @Test
    public void testOneEmptyOneFullUneven()
    {
        SteamTank fullTank = new SteamTank(1000, 1000, 1);
        SteamTank emptyTank = new SteamTank(0, 500, 1);

        SteamMesh mesh = new SteamMesh(this.throttle);
        mesh.addHandler(fullTank);
        mesh.addHandler(emptyTank);

        assertThat(mesh.getPressure()).isEqualTo(0.66f, offset(0.01f));

        // Used to simulate a full share with a throttle of 64
        for (int i = 0; i < 16; i++)
            mesh.tick();

        assertThat(fullTank.getPressure()).isEqualTo(emptyTank.getPressure(), offset(0.01f));
        assertThat(fullTank.getSteam()).isBetween(666, 667);
        assertThat(emptyTank.getSteam()).isBetween(333, 334);

        assertThat(mesh.getPressure()).isEqualTo(0.66f, offset(0.01f));
    }

    @Test
    public void testTwoEmptyOneFull()
    {
        SteamTank fullTank = new SteamTank(1000, 1000, 1);
        SteamTank emptyTank1 = new SteamTank(0, 500, 1);
        SteamTank emptyTank2 = new SteamTank(0, 500, 1);

        SteamMesh mesh = new SteamMesh(this.throttle);
        mesh.addHandler(fullTank);
        mesh.addHandler(emptyTank1);
        mesh.addHandler(emptyTank2);

        assertThat(mesh.getPressure()).isEqualTo(0.5f);

        // Used to simulate a full share with a throttle of 64
        for (int i = 0; i < 16; i++)
            mesh.tick();

        assertThat(fullTank.getPressure()).isEqualTo(emptyTank1.getPressure(), offset(0.01f));
        assertThat(fullTank.getPressure()).isEqualTo(emptyTank2.getPressure(), offset(0.01f));
        assertThat(fullTank.getSteam()).isEqualTo(500);
        assertThat(emptyTank1.getSteam()).isEqualTo(250);
        assertThat(emptyTank2.getSteam()).isEqualTo(250);

        assertThat(mesh.getPressure()).isEqualTo(0.5f);
    }

    @Test
    public void testTwoFullUneven()
    {
        SteamTank fullTankBig = new SteamTank(2000, 1000, 2);
        SteamTank fullTankSmall = new SteamTank(1000, 1000, 1);

        SteamMesh mesh = new SteamMesh(this.throttle);
        mesh.addHandler(fullTankBig);
        mesh.addHandler(fullTankSmall);

        assertThat(mesh.getPressure()).isEqualTo(1.5f);

        mesh.tick();

        assertThat(fullTankBig.getSteam()).isEqualTo(2000);
        assertThat(fullTankSmall.getSteam()).isEqualTo(1000);
        assertThat(mesh.getPressure()).isEqualTo(1.5f);
    }

    @Test
    public void testOneEmptyOneFullThrottle()
    {
        SteamTank fullTank = new LimitedSteamTank(1000, 1000, 1, 64);
        SteamTank emptyTank = new LimitedSteamTank(0, 1000, 1, 48);

        SteamMesh mesh = new SteamMesh(this.throttle);
        mesh.addHandler(fullTank);
        mesh.addHandler(emptyTank);

        assertThat(mesh.getPressure()).isEqualTo(0.5f);

        // Used to simulate a full share with a throttle of 48
        for (int i = 0; i < 20; i++)
            mesh.tick();

        assertThat(mesh.getSteam()).isEqualTo(1000);
        assertThat(fullTank.getSteam()).isEqualTo(emptyTank.getSteam());
        assertThat(fullTank.getSteam()).isEqualTo(500);
        assertThat(mesh.getPressure()).isEqualTo(0.5f);

        // Reverse throttle

        fullTank = new LimitedSteamTank(1000, 1000, 1, 48);
        emptyTank = new LimitedSteamTank(0, 1000, 1, 64);

        mesh = new SteamMesh(this.throttle);
        mesh.addHandler(fullTank);
        mesh.addHandler(emptyTank);

        assertThat(mesh.getPressure()).isEqualTo(0.5f);

        // Used to simulate a full share with a throttle of 48
        for (int i = 0; i < 20; i++)
            mesh.tick();

        assertThat(mesh.getSteam()).isEqualTo(1000);
        assertThat(fullTank.getSteam()).isEqualTo(emptyTank.getSteam());
        assertThat(fullTank.getSteam()).isEqualTo(500);
        assertThat(mesh.getPressure()).isEqualTo(0.5f);
    }
}
