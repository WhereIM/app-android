package im.where.whereim.geo;

import org.junit.Assert;

import org.junit.Test;

/**
 * Created by buganini on 04/02/17.
 */
public class QuadTreeTest {
    @Test
    public void fromLatLng() throws Exception {
        String qt;

        qt = QuadTree.fromLatLng(21.24842223562701, -29.00390625, 5.0);
        Assert.assertEquals(qt, "03321");

        qt = QuadTree.fromLatLng(-21.24842223562701, 29.00390625, 5);
        Assert.assertEquals(qt, "30012");

        qt = QuadTree.fromLatLng(24.74355741657781, 121.07182502746582, 14);
        Assert.assertEquals(qt, "13212312220312");
    }

    @Test
    public void toLatLng() throws Exception {
        QuadTree.LatLng latLng;

        latLng = QuadTree.toLatLng("312");
        Assert.assertEquals(latLng, new QuadTree.LatLng(-40.979898069620134, 90.0));

        latLng = QuadTree.toLatLng("123210");
        Assert.assertEquals(latLng, new QuadTree.LatLng(21.943045533438166, 56.25));
    }

    @Test
    public void toBound() throws Exception {
        QuadTree.Bound b;

        b = QuadTree.toBound("123");
        Assert.assertEquals(b, new QuadTree.Bound(40.97989806962013, 0.0, 90.0, 45.0));

        b = QuadTree.toBound("123210");
        Assert.assertEquals(b, new QuadTree.Bound(21.943045533438166, 16.636191878397653, 61.875, 56.25));
    }

    @Test
    public void interpolate() throws Exception {
        String[] enums = QuadTree.interpolate("012", "123");
        Assert.assertArrayEquals(enums, new String[]{"012","013","102","103","030","031","120","121","032","033","122","123"});
    }
}