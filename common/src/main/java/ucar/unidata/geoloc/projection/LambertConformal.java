/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.unidata.geoloc.projection;

import ucar.unidata.geoloc.*;
import ucar.unidata.util.Format;
import ucar.unidata.util.Parameter;

/**
 * Lambert Conformal Projection, one or two standard parallels, spherical earth.
 * Projection plane is a cone whose vertex lies on the line of the earth's axis,
 * and intersects the earth at two parellels (par1, par2), or is tangent to the earth at
 * one parellel par1 = par2. The cone is flattened by splitting along the longitude = lon0+180.
 * <p/>
 * See John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 104
 *
 * @author John Caron
 * @see Projection
 * @see ProjectionImpl
 */

public class LambertConformal extends ProjectionImpl {


  /**
   * constants from Snyder's equations
   */
  private double n, F, rho;

  /**
   * Earth's radius time F
   */
  private double earthRadiusTimesF;

  /**
   * lon naught
   */
  private double lon0Degrees;

  /**
   * lat/lon in radians
   */
  private double lat0, lon0;  // radians

  /**
   * parallel 1 and 2
   */
  private double par1, par2;  // degrees

  private double falseEasting, falseNorthing, earth_radius;

  /**
   * origin
   */
  private LatLonPointImpl origin;  // why are we keeping this?
  //private double maxY;       // for crossSeam

  public ProjectionImpl constructCopy() {
    return new LambertConformal(getOriginLat(), getOriginLon(), getParallelOne(), getParallelTwo(),
        getFalseEasting(), getFalseNorthing());
  }

  /**
   * Constructor with default parameters
   */
  public LambertConformal() {
    this(40.0, -105.0, 20.0, 60.0, 0.0, 0.0, Earth.getRadius()*.001);
  }

  /**
   * Construct a LambertConformal Projection, two standard parellels.
   * For the one standard parellel case, set them both to the same value.
   *
   * @param lat0 lat origin of the coord. system on the projection plane
   * @param lon0 lon origin of the coord. system on the projection plane
   * @param par1 standard parallel 1
   * @param par2 standard parallel 2
   * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
   */
  public LambertConformal(double lat0, double lon0, double par1, double par2) {
    this(lat0, lon0, par1, par2, 0.0, 0.0, Earth.getRadius()*.001);
  }

  /**
   * Construct a LambertConformal Projection, two standard parellels.
   * For the one standard parellel case, set them both to the same value.
   *
   * @param lat0           lat origin of the coord. system on the projection plane
   * @param lon0           lon origin of the coord. system on the projection plane
   * @param par1           standard parallel 1
   * @param par2           standard parallel 2
   * @param false_easting  natural_x_coordinate + false_easting = x coordinate in km
   * @param false_northing natural_y_coordinate + false_northing = y coordinate  in km
   * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
   */
  public LambertConformal(double lat0, double lon0, double par1,
                          double par2, double false_easting,
                          double false_northing) {
    this(lat0, lon0, par1, par2, false_easting, false_northing, Earth.getRadius()*.001);

  }

    /**
     * Construct a LambertConformal Projection, two standard parellels.
     * For the one standard parellel case, set them both to the same value.
     *
     * @param lat0           lat origin of the coord. system on the projection plane
     * @param lon0           lon origin of the coord. system on the projection plane
     * @param par1           standard parallel 1
     * @param par2           standard parallel 2
     * @param false_easting  natural_x_coordinate + false_easting = x coordinate in km
     * @param false_northing natural_y_coordinate + false_northing = y coordinate  in km
     * @param earth_radius  radius of the earth in km
     * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
     */
    public LambertConformal(double lat0, double lon0, double par1,
                            double par2, double false_easting,
                            double false_northing, double earth_radius) {

    this.lat0 = Math.toRadians(lat0);
    this.lon0 = Math.toRadians(lon0);

    this.par1 = par1;
    this.par2 = par2;

    this.falseEasting = false_easting;
    this.falseNorthing = false_northing;
    this.earth_radius = earth_radius;

    origin = new LatLonPointImpl(lat0, lon0);
    precalculate();

    addParameter(ATTR_NAME, "lambert_conformal_conic");
    addParameter("latitude_of_projection_origin", lat0);
    addParameter("longitude_of_central_meridian", lon0);
    if (par2 == par1) {
      addParameter("standard_parallel", par1);
    } else {
      double[] data = new double[2];
      data[0] = par1;
      data[1] = par2;
      addParameter(new Parameter("standard_parallel", data));
    }
    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      addParameter("false_easting", false_easting);
      addParameter("false_northing", false_northing);
      addParameter("units", "km");
    }
  }

  /**
   * Precalculate some stuff
   */
  private void precalculate() {
    if (Math.abs(lat0 - PI_OVER_2) < TOLERANCE) {
      throw new IllegalArgumentException("LambertConformal lat0 = 90");
    }
    if (Math.abs(lat0 + PI_OVER_2) < TOLERANCE) {
      throw new IllegalArgumentException("LambertConformal lat0 = -90");
    }
    if (Math.abs(par1 - 90.0) < TOLERANCE) {
      throw new IllegalArgumentException("LambertConformal par1 = 90");
    }
    if (Math.abs(par1 + 90.0) < TOLERANCE) {
      throw new IllegalArgumentException("LambertConformal par1 = -90");
    }
    if (Math.abs(par2 - 90.0) < TOLERANCE) {
      throw new IllegalArgumentException("LambertConformal par2 = 90");
    }
    if (Math.abs(par2 + 90.0) < TOLERANCE) {
      throw new IllegalArgumentException("LambertConformal par2 = -90");
    }

    double par1r = Math.toRadians(this.par1);
    double par2r = Math.toRadians(this.par2);

    double t1 = Math.tan(Math.PI / 4 + par1r / 2);
    double t2 = Math.tan(Math.PI / 4 + par2r / 2);

    if (Math.abs(par2 - par1) < TOLERANCE) {  // single parallel
      n = Math.sin(par1r);
    } else {
      n = Math.log(Math.cos(par1r) / Math.cos(par2r))
          / Math.log(t2 / t1);
    }

    double t1n = Math.pow(t1, n);
    F = Math.cos(par1r) * t1n / n;
    earthRadiusTimesF = earth_radius * F;

    double t0n = Math.pow(Math.tan(Math.PI / 4 + lat0 / 2), n);
    rho = earthRadiusTimesF / t0n;

    lon0Degrees = Math.toDegrees(lon0);
    // need to know the pole value for crossSeam
    //Point2D pt = latLonToProj( 90.0, 0.0);
    //maxY = pt.getY();
    //System.out.println("LC = " +pt);
  }

  /**
   * Clone this projection.
   *
   * @return Clone of this
   */
  public Object clone() {
    LambertConformal cl = (LambertConformal) super.clone();
    cl.origin = new LatLonPointImpl(getOriginLat(), getOriginLon());
    return cl;
  }


  /**
   * Check for equality with the Object in question
   *
   * @param proj object to check
   * @return true if they are equal
   */
  public boolean equals(Object proj) {
    if (!(proj instanceof LambertConformal)) {
      return false;
    }

    LambertConformal oo = (LambertConformal) proj;
    return ((this.getParallelOne() == oo.getParallelOne())
        && (this.getParallelTwo() == oo.getParallelTwo())
        && (this.getOriginLat() == oo.getOriginLat())
        && (this.getOriginLon() == oo.getOriginLon())
        && this.defaultMapArea.equals(oo.defaultMapArea));
  }

  // bean properties

  /**
   * Get the second standard parallel
   *
   * @return the second standard parallel
   */
  public double getParallelTwo() {
    return par2;
  }

  /**
   * Set the second standard parallel
   *
   * @param par the second standard parallel
   */
  public void setParallelTwo(double par) {
    par2 = par;
    precalculate();
  }

  //Keep the  mispelled name for persisted objects

  /**
   * Set the second standard parallel.
   *
   * @param par the second standard parallel
   * @deprecated use setParallelTwo(double). Keep the mispelled name
   *             for persisted objects
   */
  public void setParellelTwo(double par) {
    par2 = par;
    precalculate();
  }

  /**
   * Get the first standard parallel
   *
   * @return the first standard parallel
   */
  public double getParallelOne() {
    return par1;
  }

  /**
   * Set the first standard parallel
   *
   * @param par the first standard parallel
   */
  public void setParallelOne(double par) {
    par1 = par;
    precalculate();
  }

  //Keep the  mispelled name for persisted objects

  /**
   * Set the first standard parallel.
   *
   * @param par the first standard parallel
   * @deprecated use setParallelOne(double). Keep the mispelled name
   *             for persisted objects
   */
  public void setParellelOne(double par) {
    par1 = par;
    precalculate();
  }

  /**
   * Get the origin longitude.
   *
   * @return the origin longitude.
   */
  public double getOriginLon() {
    return origin.getLongitude();
  }

  /**
   * Set the origin longitude.
   *
   * @param lon the origin longitude.
   */
  public void setOriginLon(double lon) {
    origin.setLongitude(lon);
    lon0 = Math.toRadians(lon);
    precalculate();
  }

  /**
   * Get the origin latitude.
   *
   * @return the origin latitude.
   */
  public double getOriginLat() {
    return origin.getLatitude();
  }

  /**
   * Set the origin latitude.
   *
   * @param lat the origin latitude.
   */
  public void setOriginLat(double lat) {
    origin.setLatitude(lat);
    lat0 = Math.toRadians(lat);
    precalculate();
  }

  /**
   * Get the false easting, in km.
   *
   * @return the false easting.
   */
  public double getFalseEasting() {
    return falseEasting;
  }

  /**
   * Set the false_easting, in km.
   * natural_x_coordinate + false_easting = x coordinate
   *
   * @param falseEasting x offset
   */
  public void setFalseEasting(double falseEasting) {
    this.falseEasting = falseEasting;
  }


  /**
   * Get the false northing, in km.
   *
   * @return the false northing.
   */
  public double getFalseNorthing() {
    return falseNorthing;
  }

  /**
   * Set the false northing, in km.
   * natural_y_coordinate + false_northing = y coordinate
   *
   * @param falseNorthing y offset
   */
  public void setFalseNorthing(double falseNorthing) {
    this.falseNorthing = falseNorthing;
  }


  /**
   * Get the label to be used in the gui for this type of projection
   *
   * @return Type label
   */
  public String getProjectionTypeLabel() {
    return "Lambert conformal conic";
  }

  /**
   * Create a String of the parameters.
   *
   * @return a String of the parameters
   */
  public String paramsToString() {
    return " origin " + origin.toString() + " parallels: "
        + Format.d(par1, 3) + " " + Format.d(par2, 3);
  }


  /**
   * Create a WKS string
   *
   * @return WKS string
   */
  public String toWKS() {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append("PROJCS[\"").append(getName()).append("\",");
    if (true) {
      sbuff.append("GEOGCS[\"Normal Sphere (r=6371007)\",");
      sbuff.append("DATUM[\"unknown\",");
      sbuff.append("SPHEROID[\"sphere\",6371007,0]],");
    } else {
      sbuff.append("GEOGCS[\"WGS 84\",");
      sbuff.append("DATUM[\"WGS_1984\",");
      sbuff.append("SPHEROID[\"WGS 84\",6378137,298.257223563],");
      sbuff.append("TOWGS84[0,0,0,0,0,0,0]],");
    }
    sbuff.append("PRIMEM[\"Greenwich\",0],");
    sbuff.append("UNIT[\"degree\",0.0174532925199433]],");
    sbuff.append("PROJECTION[\"Lambert_Conformal_Conic_1SP\"],");
    sbuff.append("PARAMETER[\"latitude_of_origin\",").append(getOriginLat()).append("],");  // LOOK assumes getOriginLat = getParellel
    sbuff.append("PARAMETER[\"central_meridian\",").append(getOriginLon()).append("],");
    sbuff.append("PARAMETER[\"scale_factor\",1],");
    sbuff.append("PARAMETER[\"false_easting\",").append(falseEasting).append("],");
    sbuff.append("PARAMETER[\"false_northing\",").append(falseNorthing).append("],");

    return sbuff.toString();
  }

  /**
   * Get the scale for the lat.
   *
   * @param lat lat to use
   * @return scale
   */
  public double getScale(double lat) {
    lat = Math.toRadians(lat);
    double t = Math.tan(Math.PI / 4 + lat / 2);
    double tn = Math.pow(t, n);
    double r1 = n * F;
    double r2 = Math.cos(lat) * tn;
    return r1 / r2;
  }


  /**
   * This returns true when the line between pt1 and pt2 crosses the seam.
   * When the cone is flattened, the "seam" is lon0 +- 180.
   *
   * @param pt1 point 1
   * @param pt2 point 2
   * @return true when the line between pt1 and pt2 crosses the seam.
   */
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    // either point is infinite
    if (ProjectionPointImpl.isInfinite(pt1)
        || ProjectionPointImpl.isInfinite(pt2)) {
      return true;
    }
    // opposite signed X values, larger then 20,000 km
    return (pt1.getX() * pt2.getX() < 0)
        && (Math.abs(pt1.getX() - pt2.getX()) > 20000.0);
  }

  /*MACROBODY
    latLonToProj {} {
      fromLat = Math.toRadians(fromLat);
      double dlon = LatLonPointImpl.lonNormal(fromLon - lon0Degrees);
      double theta = n * Math.toRadians(dlon);
      double tn = Math.pow( Math.tan(PI_OVER_4 + fromLat/2), n);
      double r = earthRadiusTimesF / tn;
      toX = r * Math.sin(theta);
      toY = rho - r * Math.cos(theta);
    }
    projToLatLon {} {
      double rhop = rho;
      if (n < 0) {
          rhop *= -1.0;
          fromX *= -1.0;
          fromY *= -1.0;
      }

      double yd = (rhop - fromY);
      double theta = Math.atan2( fromX, yd);
      double r = Math.sqrt( fromX*fromX + yd*yd);
      if (n < 0.0)
          r *= -1.0;

      toLon = (Math.toDegrees(theta/n + lon0));

      if (Math.abs(r) < TOLERANCE) {
          toLat = ((n < 0.0) ? -90.0 : 90.0);
      } else {
          double rn = Math.pow( EARTH_RADIUS * F / r, 1/n);
          toLat = Math.toDegrees(2.0 * Math.atan( rn) - Math.PI/2);
      }
          }

  MACROBODY*/

  /*BEGINGENERATED*/

  /*
  Note this section has been generated using the convert.tcl script.
  This script, run as:
  tcl convert.tcl LambertConformal.java
  takes the actual projection conversion code defined in the MACROBODY
  section above and generates the following 6 methods
  */


  /**
   * Convert a LatLonPoint to projection coordinates
   *
   * @param latLon convert from these lat, lon coordinates
   * @param result the object to write to
   * @return the given result
   */
  public ProjectionPoint latLonToProj(LatLonPoint latLon,
                                      ProjectionPointImpl result) {
    double toX, toY;
    double fromLat = latLon.getLatitude();
    double fromLon = latLon.getLongitude();


    fromLat = Math.toRadians(fromLat);
    double dlon = LatLonPointImpl.lonNormal(fromLon - lon0Degrees);
    double theta = n * Math.toRadians(dlon);
    double tn = Math.pow(Math.tan(PI_OVER_4 + fromLat / 2), n);
    double r = earthRadiusTimesF / tn;
    toX = r * Math.sin(theta);
    toY = rho - r * Math.cos(theta);

    result.setLocation(toX + falseEasting, toY + falseNorthing);
    return result;
  }

  /**
   * Convert projection coordinates to a LatLonPoint
   * Note: a new object is not created on each call for the return value.
   *
   * @param world  convert from these projection coordinates
   * @param result the object to write to
   * @return LatLonPoint convert to these lat/lon coordinates
   */
  public LatLonPoint projToLatLon(ProjectionPoint world,
                                  LatLonPointImpl result) {
    double toLat, toLon;
    double fromX = world.getX() - falseEasting;
    double fromY = world.getY() - falseNorthing;
    double rhop = rho;

    if (n < 0) {
      rhop *= -1.0;
      fromX *= -1.0;
      fromY *= -1.0;
    }

    double yd = (rhop - fromY);
    double theta = Math.atan2(fromX, yd);
    double r = Math.sqrt(fromX * fromX + yd * yd);
    if (n < 0.0) {
      r *= -1.0;
    }

    toLon = (Math.toDegrees(theta / n + lon0));

    if (Math.abs(r) < TOLERANCE) {
      toLat = ((n < 0.0)
          ? -90.0
          : 90.0);
    } else {
      double rn = Math.pow(earth_radius * F / r, 1 / n);
      toLat = Math.toDegrees(2.0 * Math.atan(rn) - Math.PI / 2);
    }

    result.setLatitude(toLat);
    result.setLongitude(toLon);
    return result;
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from     array of lat/lon coordinates: from[2][n], where
   *                 (from[latIndex][i], from[lonIndex][i]) is the (lat,lon)
   *                 coordinate of the ith point
   * @param to       resulting array of projection coordinates: to[2][n]
   *                 where (to[0][i], to[1][i]) is the (x,y) coordinate of
   *                 the ith point
   * @param latIndex index of lat coordinate; must be 0 or 1
   * @param lonIndex index of lon coordinate; must be 0 or 1
   * @return the "to" array
   */
  public float[][] latLonToProj(float[][] from, float[][] to, int latIndex,
                                int lonIndex) {
    int cnt = from[0].length;
    float[] fromLatA = from[latIndex];
    float[] fromLonA = from[lonIndex];
    float[] resultXA = to[INDEX_X];
    float[] resultYA = to[INDEX_Y];
    double toX, toY;

    for (int i = 0; i < cnt; i++) {
      double fromLat = fromLatA[i];
      double fromLon = fromLonA[i];

      fromLat = Math.toRadians(fromLat);
      double dlon = LatLonPointImpl.lonNormal(fromLon - lon0Degrees);
      double theta = n * Math.toRadians(dlon);
      double tn = Math.pow(Math.tan(PI_OVER_4 + fromLat / 2), n);
      double r = earthRadiusTimesF / tn;
      toX = r * Math.sin(theta);
      toY = rho - r * Math.cos(theta);

      resultXA[i] = (float) (toX + falseEasting);
      resultYA[i] = (float) (toY + falseNorthing);
    }
    return to;
  }

  /**
   * Convert projection coordinates to lat/lon coordinate.
   *
   * @param from array of projection coordinates: from[2][n], where
   *             (from[0][i], from[1][i]) is the (x, y) coordinate
   *             of the ith point
   * @param to   resulting array of lat/lon coordinates: to[2][n] where
   *             (to[0][i], to[1][i]) is the (lat, lon) coordinate of
   *             the ith point
   * @return the "to" array
   */
  public float[][] projToLatLon(float[][] from, float[][] to) {
    int cnt = from[0].length;
    float[] fromXA = from[INDEX_X];
    float[] fromYA = from[INDEX_Y];
    float[] toLatA = to[INDEX_LAT];
    float[] toLonA = to[INDEX_LON];
    double toLat, toLon;
    for (int i = 0; i < cnt; i++) {
      double fromX = fromXA[i] - falseEasting;
      double fromY = fromYA[i] - falseNorthing;
      double rhop = rho;

      if (n < 0) {
        rhop *= -1.0;
        fromX *= -1.0;
        fromY *= -1.0;
      }

      double yd = (rhop - fromY);
      double theta = Math.atan2(fromX, yd);
      double r = Math.sqrt(fromX * fromX + yd * yd);
      if (n < 0.0) {
        r *= -1.0;
      }

      toLon = (Math.toDegrees(theta / n + lon0));

      if (Math.abs(r) < TOLERANCE) {
        toLat = ((n < 0.0)
            ? -90.0
            : 90.0);
      } else {
        double rn = Math.pow(earth_radius * F / r, 1 / n);
        toLat = Math.toDegrees(2.0 * Math.atan(rn) - Math.PI / 2);
      }

      toLatA[i] = (float) toLat;
      toLonA[i] = (float) toLon;
    }
    return to;
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from     array of lat/lon coordinates: from[2][n], where
   *                 (from[latIndex][i], from[lonIndex][i]) is the (lat,lon)
   *                 coordinate of the ith point
   * @param to       resulting array of projection coordinates: to[2][n]
   *                 where (to[0][i], to[1][i]) is the (x,y) coordinate of
   *                 the ith point
   * @param latIndex index of lat coordinate; must be 0 or 1
   * @param lonIndex index of lon coordinate; must be 0 or 1
   * @return the "to" array
   */
  public double[][] latLonToProj(double[][] from, double[][] to,
                                 int latIndex, int lonIndex) {
    int cnt = from[0].length;
    double[] fromLatA = from[latIndex];
    double[] fromLonA = from[lonIndex];
    double[] resultXA = to[INDEX_X];
    double[] resultYA = to[INDEX_Y];
    double toX, toY;

    for (int i = 0; i < cnt; i++) {
      double fromLat = fromLatA[i];
      double fromLon = fromLonA[i];

      fromLat = Math.toRadians(fromLat);
      double dlon = LatLonPointImpl.lonNormal(fromLon - lon0Degrees);
      double theta = n * Math.toRadians(dlon);
      double tn = Math.pow(Math.tan(PI_OVER_4 + fromLat / 2), n);
      double r = earthRadiusTimesF / tn;
      toX = r * Math.sin(theta);
      toY = rho - r * Math.cos(theta);

      resultXA[i] = toX + falseEasting;
      resultYA[i] = toY + falseNorthing;
    }
    return to;
  }

  /**
   * Convert projection coordinates to lat/lon coordinate.
   *
   * @param from array of projection coordinates: from[2][n], where
   *             (from[0][i], from[1][i]) is the (x, y) coordinate
   *             of the ith point
   * @param to   resulting array of lat/lon coordinates: to[2][n] where
   *             (to[0][i], to[1][i]) is the (lat, lon) coordinate of
   *             the ith point
   * @return the "to" array
   */
  public double[][] projToLatLon(double[][] from, double[][] to) {
    int cnt = from[0].length;
    double[] fromXA = from[INDEX_X];
    double[] fromYA = from[INDEX_Y];
    double[] toLatA = to[INDEX_LAT];
    double[] toLonA = to[INDEX_LON];
    double toLat, toLon;
    for (int i = 0; i < cnt; i++) {
      double fromX = fromXA[i] - falseEasting;
      double fromY = fromYA[i] - falseNorthing;
      double rhop = rho;

      if (n < 0) {
        rhop *= -1.0;
        fromX *= -1.0;
        fromY *= -1.0;
      }

      double yd = (rhop - fromY);
      double theta = Math.atan2(fromX, yd);
      double r = Math.sqrt(fromX * fromX + yd * yd);
      if (n < 0.0) {
        r *= -1.0;
      }

      toLon = (Math.toDegrees(theta / n + lon0));

      if (Math.abs(r) < TOLERANCE) {
        toLat = ((n < 0.0)
            ? -90.0
            : 90.0);
      } else {
        double rn = Math.pow(earth_radius * F / r, 1 / n);
        toLat = Math.toDegrees(2.0 * Math.atan(rn) - Math.PI / 2);
      }

      toLatA[i] = toLat;
      toLonA[i] = toLon;
    }
    return to;
  }

  /*ENDGENERATED*/


}









