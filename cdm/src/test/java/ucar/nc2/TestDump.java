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

import junit.framework.*;

import java.io.*;

/**
 * Simple example to print contents of an existing netCDF file of
 * unknown structure, much like ncdump.  A difference is the nesting of
 * multidimensional array data is represented by nested brackets, so the
 * output is not legal CDL that can be used as input for ncgen.
 *
 * @author Russ Rew
 * @version $Id: TestDump.java 51 2006-07-12 17:13:13Z caron $ */

public class TestDump extends TestCase  {

  public TestDump( String name) {
    super(name);
  }

  File tempFile;
  FileOutputStream out;
  protected void setUp() throws Exception {
    tempFile = File.createTempFile("TestLongOffset", "out");
    out = new FileOutputStream( tempFile);
  }
  protected void tearDown() throws Exception {
    out.close();
    tempFile.delete();
  }

  public void testNCdump() {
    try {
      NCdump.print( TestLocal.cdmTestDataDir +"testWrite.nc", out, false, true, false, false, null, null);
      NCdump.printNcML( TestLocal.cdmTestDataDir +"testWriteRecord.nc", out);
    } catch (IOException ioe) {
      ioe.printStackTrace();
      assert (false);
    }

    System.out.println( "**** testNCdump done");
  }

}
