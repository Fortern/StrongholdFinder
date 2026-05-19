package xyz.fortern.strongholdfinder.math;

public record Vector2(double x, double z) {

    public Vector2 vectorTo(final Vector2 vec) {
        return new Vector2(vec.x - this.x, vec.z - this.z);
    }

    public double cross(Vector2 v) {
        return this.x * v.z - this.z * v.x;
    }

    public Vector2 add(Vector2 v) {
        return new Vector2(x + v.x, z + v.z);
    }

    public Vector2 scale(final double scale) {
        return new Vector2(x * scale, z * scale);
    }

}
