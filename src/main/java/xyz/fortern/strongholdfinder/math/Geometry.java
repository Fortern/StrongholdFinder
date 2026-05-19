package xyz.fortern.strongholdfinder.math;

public class Geometry {
    public static final double EPSILON = 1.0E-10;

    /**
     * Computes intersection of rays AB and CD (rays starting at A and C respectively).
     *
     * @param a Coordinates of point A
     * @param b Coordinates of point B
     * @param c Coordinates of point C
     * @param d Coordinates of point D
     * @return The intersection of rays AB and CD. No intersection if the vector's x or y is NaN.
     */
    public static Vector2 intersectRays(Vector2 a, Vector2 b, Vector2 c, Vector2 d) {
        // direction of ray AB
        Vector2 ab = a.vectorTo(b);
        // direction of ray CD
        Vector2 cd = c.vectorTo(d);
        // C - A  (note: we used from(A,C) = C - A)
        Vector2 r = a.vectorTo(c);

        double denom = ab.cross(cd);

        if (Math.abs(denom) > EPSILON) {
            // Unique intersection of infinite lines, check parameters t and u
            // parameter on AB
            double t = r.cross(cd) / denom;
            // parameter on CD
            double u = r.cross(ab) / denom;
            if (t >= -EPSILON && u >= -EPSILON) {
                return a.add(ab.scale((float) t));
            }
        }

        return new Vector2(Float.NaN, Float.NaN);
    }
}
