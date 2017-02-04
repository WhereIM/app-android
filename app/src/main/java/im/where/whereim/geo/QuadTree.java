package im.where.whereim.geo;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by buganini on 04/02/17.
 */

public class QuadTree {
    public static class LatLng{
        double latitude;
        double longitude;

        public LatLng(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public LatLng() {
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof  LatLng){
                LatLng ll = (LatLng) o;
                return ll.latitude == this.latitude && ll.longitude == this.longitude;
            }
            return false;
        }
    }

    public static class Bound{
        double north;
        double south;
        double east;
        double west;

        public Bound(double north, double south, double east, double west) {
            this.north = north;
            this.south = south;
            this.east = east;
            this.west = west;
        }

        public Bound() {
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof  Bound){
                Bound b = (Bound) o;
                return b.north == this.north && b.south == this.south && b.east == this.east && b.west == this.west;
            }
            return false;
        }
    }

    public static String fromLatLng(double lat, double lng, double level){
        double sinLat = Math.sin(lat * Math.PI/180);
        double y = 0.5 - Math.log((1 + sinLat) / (1 - sinLat)) / (4 * Math.PI);
        double x = (lng + 180) / 360;

        StringBuilder r = new StringBuilder();
        for(int i=0;i<level;i+=1){
            int blat = (y >= 0.5)?1:0;
            int blng = (x >= 0.5)?1:0;
            r.append((blat << 1) | blng);
            y = (y - 0.5*blat) * 2;
            x = (x - 0.5*blng) * 2;
        }
        return r.toString();
    }

    public static LatLng toLatLng(String qt){
        double p5 = 1;
        double y = 0;
        double x = 0;

        for(int i=0;i<qt.length();i+=1){
            int c = Character.getNumericValue(qt.charAt(i));
            p5 *= 0.5;
            y += ((c >> 1) & 1) * p5;
            x += (c & 1) * p5;
        }

        LatLng latlng = new LatLng();
        latlng.latitude = 90 - 360 * Math.atan(Math.exp((y-0.5) * 2 * Math.PI)) / Math.PI;
        latlng.longitude = 360 * (x-0.5);
        return latlng;
    }

    public static Bound toBound(String qt){
        double p5 = 1;
        double y = 0;
        double x = 0;
        for(int i=0;i<qt.length();i+=1){
            int c = Character.getNumericValue(qt.charAt(i));
            p5 *= 0.5;
            y += ((c >> 1) & 1) * p5;
            x += (c & 1) * p5;
        }
        Bound b = new Bound();
        b.north = 90 - 360 * Math.atan(Math.exp((y-0.5) * 2 * Math.PI)) / Math.PI;
        b.west = 360 * (x-0.5);
        b.south = 90 - 360 * Math.atan(Math.exp((y-0.5+p5) * 2 * Math.PI)) / Math.PI;
        b.east = 360 * (x-0.5+p5);
        return b;
    }

    private static int[] xVec(String v){
        int[] xv = new int[v.length()];
        for(int i=0;i<v.length();i+=1){
            int c = Character.getNumericValue(v.charAt(i));
            xv[i] = c & 1;
        }
        return xv;
    }

    private static int[] yVec(String v){
        int[] yv = new int[v.length()];
        for(int i=0;i<v.length();i+=1){
            int c = Character.getNumericValue(v.charAt(i));
            yv[i] = (c>>1) & 1;
        }
        return yv;
    }

    private static String xyVec(int[] xv, int[] yv){
        StringBuilder s = new StringBuilder();
        for(int i=0;i<xv.length;i+=1){
            s.append(yv[i]<<1 | xv[i]);
        }
        return s.toString();
    }

    private static int[] inc(int[] v){
        int[] r = Arrays.copyOf(v, v.length);
        for(int i=v.length-1;i>=0;i-=1){
            if(r[i] == 0){
                r[i] = 1;
                break;
            }else{
                r[i] = 0;

            }
        }
        return r;
    }

    public static String[] interpolate(String start, String end){
        ArrayList<String> ret = new ArrayList<>();

        int[] startX = xVec(start);
        int[] startY = yVec(start);
        int[] endX = xVec(end);
        int[] endY = yVec(end);
        int[] xv = startX;
        int[] yv = startY;
        int i=0;
        while(i<50){
            i+=1;
            String vec = xyVec(xv, yv);
            ret.add(vec);
            if(Arrays.equals(xv, endX)){
                if(Arrays.equals(yv, endY)){
                    break;
                }else{
                    xv = startX;
                    yv = inc(yv);
                }
            }else{
                xv = inc(xv);
            }
        }
        return ret.toArray(new String[0]);
    }
}
