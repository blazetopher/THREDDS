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
package ucar.nc2;

import ucar.ma2.*;

import java.util.List;
import java.nio.ByteBuffer;

import net.jcip.annotations.Immutable;

/**
 * An Attribute has a name and a value, used for associating arbitrary metadata with a Variable or a Group.
 * The value can be a one dimensional array of Strings or numeric values.
 * <p/>
 * Attributes are immutable.
 *
 * @author caron
 */

@Immutable
public class Attribute {
  private final String name;
  private String svalue; // optimization for common case of String valued attribute
  private DataType dataType;
  private int nelems; // can be 0 or greater
  private Array values;

  /**
   * Get the name of this Attribute.
   * Attribute names are unique within a NetcdfFile's global set, and within a Variable's set.
   *
   * @return name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the data type of the Attribute value.
   *
   * @return DataType
   */
  public DataType getDataType() {
    return dataType;
  }

  /**
   * True if value is an array (getLength() > 1)
   *
   * @return if its an array.
   */
  public boolean isArray() {
    return (getLength() > 1);
  }

  /**
   * Get the length of the array of values
   *
   * @return number of elementss in the array.
   */
  public int getLength() {
    return nelems;
  }

  /**
   * Find whether the underlying data should be interpreted as unsigned.
   * Only affects byte, short, and int.
   *
   * @return true if the data is unsigned integer type.
   */
  public boolean isUnsigned() {
    return (values != null) && values.isUnsigned();
  }

  /**
   * Get the value as an Array.
   *
   * @return Array of values.
   */
  public Array getValues() {
    if (values == null && svalue != null) {
      values = Array.factory(String.class, new int[]{1});
      values.setObject(values.getIndex(), svalue);
    }

    return values;
  }

  /**
   * Get the value as an Object.
   *
   * @param index which index
   * @return ith value as an Object.
   */
  public Object getValue(int index) {
    if (isString()) return getStringValue(index);
    return getNumericValue(index);
  }

  /**
   * True if value is of type String and not null.
   *
   * @return if its a String and not null.
   */
  public boolean isString() {
    return (dataType == DataType.STRING) && null != getStringValue();
  }

  /**
   * Retrieve String value; only call if isString() is true.
   *
   * @return String if this is a String valued attribute, else null.
   * @see Attribute#isString
   */
  public String getStringValue() {
    if (dataType != DataType.STRING) return null;
    return (svalue != null) ? svalue : _getStringValue(0);
  }

  /**
   * Retrieve ith String value; only call if isString() is true.
   *
   * @param index which index
   * @return ith String value (if this is a String valued attribute and index in range), else null.
   * @see Attribute#isString
   */
  public String getStringValue(int index) {
    if (dataType != DataType.STRING) return null;
    if ((svalue != null) && (index == 0)) return svalue;
    return _getStringValue(index);
  }

  private String _getStringValue(int index) {
    if ((index < 0) || (index >= nelems)) return null;
    return (String) values.getObject(index);
  }

  /**
   * Retrieve numeric value.
   * Equivalent to <code>getNumericValue(0)</code>
   *
   * @return the first element of the value array, or null if its a String.
   */
  public Number getNumericValue() {
    return getNumericValue(0);
  }

  /// these deal with array-valued attributes

  /**
   * Retrieve a numeric value by index. If its a String, it will try to parse it as a double.
   *
   * @param index the index into the value array.
   * @return Number <code>value[index]</code>, or null if its a non-parsable String or
   *         the index is out of range.
   */
  public Number getNumericValue(int index) {
    if (isString() || (index < 0) || (index >= nelems))
      return null;

    if (dataType == DataType.STRING) {
      try {
        return new Double(getStringValue(index));
      }
      catch (NumberFormatException e) {
        return null;
      }
    }

    // LOOK can attributes be enum valued? for now, no
    if (dataType == DataType.BYTE)
      return values.getByte(index);
    else if (dataType == DataType.SHORT)
      return values.getShort(index);
    else if (dataType == DataType.INT)
      return values.getInt(index);
    else if (dataType == DataType.FLOAT)
      return values.getFloat(index);
    else if (dataType == DataType.DOUBLE)
      return values.getDouble(index);
    else if (dataType == DataType.LONG)
      return values.getLong(index);

    return null;
  }

  /**
   * Instances which have same content are equal.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if ((o == null) || !(o instanceof Attribute)) return false;

    final Attribute att = (Attribute) o;

    if (!name.equals(att.name)) return false;
    if (nelems != att.nelems) return false;
    if (!dataType.equals(att.dataType)) return false;

    if (svalue != null) return svalue.equals(att.getStringValue());

    if (values != null) {
      for (int i = 0; i < getLength(); i++) {
        int r1 = isString() ? getStringValue(i).hashCode() : getNumericValue(i).hashCode();
        int r2 = att.isString() ? att.getStringValue(i).hashCode() : att.getNumericValue(i).hashCode();
        if (r1 != r2) return false;
      }
    }

    return true;
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37 * result + getName().hashCode();
      result = 37 * result + nelems;
      result = 37 * result + getDataType().hashCode();
      if (svalue != null)
        result = 37 * result + svalue.hashCode();
      else if (values != null) {
        for (int i = 0; i < getLength(); i++) {
          int h = isString() ? getStringValue(i).hashCode() : getNumericValue(i).hashCode();
          result = 37 * result + h;
        }
      }
      hashCode = result;
    }
    return hashCode;
  }

  private int hashCode = 0;

  /**
   * CDL representation, not strict
   *
   * @return CDL representation
   */
  @Override
  public String toString() {
    return toString(false);
  }

  /**
   * CDL representation
   *
   * @param strict if true, create strict CDL, escaping names
   * @return CDL representation
   */
  public String toString(boolean strict) {
    StringBuilder buff = new StringBuilder();
    buff.append(strict ? NetcdfFile.escapeName(getName()) : getName());
    if (isString()) {
      buff.append(" = ");
      for (int i = 0; i < getLength(); i++) {
        if (i != 0) buff.append(", ");
        String val = getStringValue(i);
        if (val != null)
          buff.append("\"").append(NCdumpW.encodeString(val)).append("\"");
      }
    } else {
      buff.append(" = ");
      for (int i = 0; i < getLength(); i++) {
        if (i != 0) buff.append(", ");
        buff.append(getNumericValue(i));
        if (dataType == DataType.FLOAT)
          buff.append("f");
        else if (dataType == DataType.SHORT) {
          if (isUnsigned()) buff.append("US");
          else buff.append("S");
        } else if (dataType == DataType.BYTE) {
          if (isUnsigned()) buff.append("UB");
          else buff.append("B");
        } else if (dataType == DataType.LONG) {
          if (isUnsigned()) buff.append("UL");
          else buff.append("L");
        } else if (dataType == DataType.INT) {
          if (isUnsigned()) buff.append("U");
        }
      }
    }
    return buff.toString();
  }


  ///////////////////////////////////////////////////////////////////////////////

  /**
   * Copy constructor
   *
   * @param name name of Attribute
   * @param from copy value from here.
   */
  public Attribute(String name, Attribute from) {
    this.name = name;
    this.dataType = from.dataType;
    this.nelems = from.nelems;
    this.svalue = from.svalue;
    this.values = from.values;
  }

  /**
   * Create a String-valued Attribute.
   *
   * @param name name of Attribute
   * @param val  value of Attribute
   */
  public Attribute(String name, String val) {
    this.name = name;
    setStringValue(val);
  }

  /**
   * Create a scalar numeric-valued Attribute.
   *
   * @param name name of Attribute
   * @param val  value of Attribute
   */
  public Attribute(String name, Number val) {
    this.name = name;
    int[] shape = new int[1];
    shape[0] = 1;
    DataType dt = DataType.getType(val.getClass());
    Array vala = Array.factory(dt.getPrimitiveClassType(), shape);
    Index ima = vala.getIndex();
    vala.setDouble(ima.set0(0), val.doubleValue());
    setValues(vala);
  }

  /**
   * Construct attribute with Array of values.
   *
   * @param name   name of attribute
   * @param values array of values.
   */
  public Attribute(String name, Array values) {
    this.name = name;
    setValues(values);
  }

  /**
   * Construct an empty attribute with no values
   *
   * @param name     name of attribute
   * @param dataType type of Attribute.
   */
  public Attribute(String name, DataType dataType) {
    this.name = name;
    this.dataType = dataType == DataType.CHAR ? DataType.STRING : dataType;
    this.nelems = 0;
  }

  /**
   * Construct attribute with list of String or Number values.
   *
   * @param name   name of attribute
   * @param values list of values. must be String or Number, must all be the same type, and have at least 1 member
   */
  public Attribute(String name, List values) {
    this.name = name;
    int n = values.size();
    Object pa = null;

    Class c = values.get(0).getClass();
    if (c == String.class) {
      String[] va = new String[n];
      pa = va;
      for (int i = 0; i < n; i++) va[i] = (String) values.get(i);

    } else if (c == Integer.class) {
      int[] va = new int[n];
      pa = va;
      for (int i = 0; i < n; i++) va[i] = (Integer) values.get(i);

    } else if (c == Double.class) {
      double[] va = new double[n];
      pa = va;
      for (int i = 0; i < n; i++) va[i] = (Double) values.get(i);

    } else if (c == Float.class) {
      float[] va = new float[n];
      pa = va;
      for (int i = 0; i < n; i++) va[i] = (Float) values.get(i);

    } else if (c == Short.class) {
      short[] va = new short[n];
      pa = va;
      for (int i = 0; i < n; i++) va[i] = (Short) values.get(i);

    } else if (c == Byte.class) {
      byte[] va = new byte[n];
      pa = va;
      for (int i = 0; i < n; i++) va[i] = (Byte) values.get(i);

    } else if (c == Long.class) {
      long[] va = new long[n];
      pa = va;
      for (int i = 0; i < n; i++) va[i] = (Long) values.get(i);

    } else {
      throw new IllegalArgumentException("unknown type for Attribute = " + c.getName());
    }

    setValues(Array.factory(c, new int[]{n}, pa));
  }


  /**
   * A copy constructor using a ucar.unidata.util.Parameter.
   * Need to do this so ucar.unidata.geoloc package doesnt depend on ucar.nc2 library
   *
   * @param param copy info from here.
   */
  public Attribute(ucar.unidata.util.Parameter param) {
    this.name = param.getName();

    if (param.isString()) {
      setStringValue(param.getStringValue());

    } else {
      double[] values = param.getNumericValues();
      int n = values.length;
      Array vala = Array.factory(DataType.DOUBLE.getPrimitiveClassType(), new int[]{n}, values);
      setValues(vala);
    }
  }

  //////////////////////////////////////////
  // the following make this mutable, but its restricted to subclasses


  /**
   * Constructor. Must also set value
   *
   * @param name name of Attribute
   */
  protected Attribute(String name) {
    this.name = name;
  }

  /**
   * set the value as a String, trimming trailing zeroes
   *
   * @param val value of Attribute
   */
  private void setStringValue(String val) {
    if (val == null)
      throw new IllegalArgumentException("Attribute value cannot be null");

    // get rid of trailing zeroes
    int len = val.length();
    while ((len > 0) && (val.charAt(len - 1) == 0))
      len--;
    if (len != val.length())
      val = val.substring(0, len);

    this.svalue = val;
    this.nelems = 1;
    this.dataType = DataType.STRING;

    //values = Array.factory(String.class, new int[]{1});
    //values.setObject(values.getIndex(), val);
    //setValues(values);
  }

  /**
   * set the values from an Array
   *
   * @param arr value of Attribute
   */
  protected void setValues(Array arr) {
    if (arr == null) {
      dataType = DataType.STRING;
      return;
    }
    
    if (DataType.getType(arr.getElementType()) == null)
      throw new IllegalArgumentException("Cant set Attribute with type " + arr.getElementType());

    if (arr.getElementType() == char.class) { // turn CHAR into STRING
      ArrayChar carr = (ArrayChar) arr;
      if (carr.getRank() == 1) { // common case
        svalue = carr.getString();
        this.nelems = 1;
        this.dataType = DataType.STRING;
        return;
      }
      // otherwise its an array of Strings
      arr = carr.make1DStringArray();
    }

    // this should be a utility somewhere
    if (arr.getElementType() == ByteBuffer.class) { // turn OPAQUE into BYTE
      int totalLen = 0;
      arr.resetLocalIterator();
      while (arr.hasNext()) {
        ByteBuffer bb = (ByteBuffer) arr.next();
        totalLen += bb.limit();
      }
      byte[] ba = new byte[totalLen];
      int pos = 0;
      arr.resetLocalIterator();
      while (arr.hasNext()) {
        ByteBuffer bb = (ByteBuffer) arr.next();
        System.arraycopy(bb.array(), 0, ba, pos, bb.limit());
        pos += bb.limit();
      }
      arr = Array.factory(DataType.BYTE, new int[]{totalLen}, ba);
    }

    if (arr.getRank() > 1)
      arr = arr.reshape(new int[]{(int) arr.getSize()}); // make sure 1D

    this.values = arr;
    this.nelems = (int) arr.getSize();
    this.dataType = DataType.getType(arr.getElementType());
    hashCode = 0;
  }

}