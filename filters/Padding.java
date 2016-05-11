package filters;

import ij.*;
import ij.process.*;

public class Padding {

/* Methods for image padding for the FHT, OrientationMapping

  Version: 1.1 (2016-05-11, 12:21 mmohn)
  
  Copyright (c) 2016 Michael Mohn and Ossi Lehtinen, Ulm University
    
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

  // The following functions can be used to transform images of any shape
  // into valid input for the FastHartleyTransform

  // check whether ImageProcessor is square image width power of two width
  public static boolean paddingNeeded(ImageProcessor ip) {
    boolean isTransformable = true;
    int width = ip.getWidth();
    int height = ip.getHeight();
    if ( !(width == height) ) isTransformable = false;
    if ( width%2 != 0 ) isTransformable = false;
    while (width%2 == 0) width = width / 2;
    if (width != 1) isTransformable = false;
    return !isTransformable;
  }
  
  public static int getPaddedSize(ImageProcessor ip) {
    int width = ip.getWidth();
    int height = ip.getHeight();
    int maxDim = Math.max(width, height);
    int newDim = 2;
    while (newDim < maxDim) newDim *= 2;
    return newDim;
  }
  
  // pad a given ImageProcessor to a square image with power of two width
  public static ImageProcessor getPaddedProcessor(ImageProcessor ip) {
    if ( paddingNeeded(ip) ) {
      int width = ip.getWidth();
      int height = ip.getHeight();
      int newDim = getPaddedSize(ip);
      ImageProcessor newIp = ip.resize(newDim, newDim);
      newIp.multiply(0);
      double mean = ip.getStatistics().mean;
      newIp.add(mean);
      int xOff = (newDim - width) / 2;
      int yOff = (newDim - height) / 2;
      newIp.copyBits(ip, xOff, yOff, Blitter.COPY);
      return newIp;
    } else {
      return ip.duplicate();
    }
  }
  
  // crop an ImageProcessor to the given original dimensions
  public static ImageProcessor getCroppedProcessor(ImageProcessor ip, int width, int height) {
    int origWidth = ip.getWidth();
    int origHeight = ip.getHeight();
    ImageProcessor newIp = ip.duplicate();
    if ((width <= origWidth) && (height <= origHeight)) {
      int xOff = (origWidth - width) / 2;
      int yOff = (origHeight - height) / 2;
      newIp.setRoi(xOff, yOff, width, height);
      newIp = newIp.crop();
    }
    return newIp;
  }


}