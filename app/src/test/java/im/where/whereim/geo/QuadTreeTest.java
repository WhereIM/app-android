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
        Assert.assertEquals("03321", qt);

        qt = QuadTree.fromLatLng(-21.24842223562701, 29.00390625, 5);
        Assert.assertEquals("30012", qt);

        qt = QuadTree.fromLatLng(24.74355741657781, 121.07182502746582, 14);
        Assert.assertEquals("13212312220312", qt);

        qt = QuadTree.fromLatLng(62.11228306230811, -312.2815094402622, 2);
        Assert.assertEquals("12", qt);
    }

    @Test
    public void toLatLng() throws Exception {
        QuadTree.LatLng latLng;

        latLng = QuadTree.toLatLng("312");
        Assert.assertEquals(new QuadTree.LatLng(-40.979898069620134, 90.0), latLng);

        latLng = QuadTree.toLatLng("123210");
        Assert.assertEquals(new QuadTree.LatLng(21.943045533438166, 56.25), latLng);
    }

    @Test
    public void toBound() throws Exception {
        QuadTree.Bound b;

        b = QuadTree.toBound("123");
        Assert.assertEquals(new QuadTree.Bound(40.97989806962013, 0.0, 90.0, 45.0), b);

        b = QuadTree.toBound("123210");
        Assert.assertEquals(new QuadTree.Bound(21.943045533438166, 16.636191878397653, 61.875, 56.25), b);
    }

    @Test
    public void interpolate() throws Exception {
        String[] enums;
        enums = QuadTree.interpolate("012", "123");
        Assert.assertArrayEquals(new String[]{"012","013","102","103","030","031","120","121","032","033","122","123"}, enums);

        enums = QuadTree.interpolate("10", "21");
        Assert.assertArrayEquals(new String[]{"10","11","00","01","12","13","02","03","30","31","20","21"}, enums);
    }
}