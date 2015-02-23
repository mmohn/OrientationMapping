package filters;

import ij.*;
import ij.process.*;

public class FilterMasks {

/* Methods for FFT Filter Masks, OrientationMapping

  Version: 1.0 (2015-02-22, 21:01 mmohn)
  
  Copyright (c) 2015 Michael Mohn and Ossi Lehtinen, Ulm University
    
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    ----------------------------------------------------------------------------
*/

  // create a filter stack for Fourier filtering
  public static ImageStack createStack(int width, int height, int n, int m, double phi0, double rmin, double rmax) {
  /* Arguments:
     ---------------------------------------------------
     width, height	width and height of the masks
     n			number of slices
     m			rotational symmetry of the masks
     phi0		offset angle
     rmin, rmax		limits for bandpass filtering
     ---------------------------------------------------
  */
    ImageStack targetStack = ImageStack.create(width, height, n, 8); // 8 bit
    // coordinates of image center
    int x0 = width/2;
    int y0 = height/2;
    double deltaphi = 360.0/(m*n); // angle between adjacent segments
    for (int s=1; s<=n; s++) {
      IJ.showProgress((s-1)*1.0/n);
      ImageProcessor targetIp = targetStack.getProcessor(s);
      // left bound of the first segment of the s'th orientation:
      double offset = modAngle(phi0 + (s-1)*deltaphi - (deltaphi/2));
      offset = modAngle(offset);
      // iterate over all pixels, check for rmin, rmax and angles
      for (int i=0; i<width; i++) {
	for (int j=0; j<height; j++) {
	  int x = i-x0;
	  int y = -j+y0;
	  double r = getRadius(x, y);		
	  if ((r >= rmin) && (r <= rmax)) { // bandpass filter
	    double phi = getAngle(x, y);
	    phi = modAngle(phi-offset); // shift angle by selected offset
	    // compare to m-fold roationally symmetric pattern, relative to the
	    // left border of one segment, factor 10 allows for better angular
	    // resolution (~0.1 deg)!
	    if ( Math.round(phi*10)%Math.round(360.0/m*10) <=
		 Math.round(deltaphi*10) ) {
	      targetIp.putPixelValue(i, j, 1);
	    }
	  }
	}
      }
    }
    return targetStack;  
  }
  
  // cartesian coordinates to radius
  public static double getRadius(int x, int y) {
    return Math.sqrt( Math.pow(x,2) + Math.pow(y,2) );
  }

  // cartesian coordinates to angle
  public static double getAngle(int x, int y) {
    // note that according to the following definition,
    // the x=0 axis corresponds to phi=0, and phi increases clockwise!
    double phi = Math.toDegrees( Math.atan2(x, y) );
    if (phi < 0) phi += 360; // convert to range 0-360 deg
      return phi;
    }

  // make sure angle is within range 0-360 deg
  public static double modAngle(double phi) {
    boolean isNegative = false;
    if (phi < 0) {
      isNegative = true;
      phi *= -1;
    }
    while (phi >= 360.0) phi -= 360;
    if (isNegative) phi = 360 - phi;  
      return phi;
  }
  
}