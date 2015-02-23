package filters;

import ij.*;
import ij.process.*;
import ij.plugin.filter.*;

public class Normalize {

/* Methods for image (contrast) normalization, OrientationMapping

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

  /* Normalization of the image contrast (32 bit float images):
     Divide by local standard deviation
  */
  public static ImageProcessor divideStdDevBlur(ImageProcessor ip, double varRadius, double blurRadius) {
    ImageProcessor newIp = ip.duplicate();
    new RankFilters().rank(newIp, varRadius, RankFilters.VARIANCE);
    newIp.sqrt();
    new GaussianBlur().blurGaussian(newIp, blurRadius, blurRadius, .01);
    ImageProcessor normIp = ip.duplicate();
    normIp.copyBits(newIp, 0, 0, Blitter.DIVIDE);
    return normIp;
  }
  
  // Normalization of image intensity with Gaussian Blur
  public static ImageProcessor subtractBlurred(ImageProcessor ip, double blurRadius) {
    ImageProcessor normIp = ip.duplicate();
    new GaussianBlur().blurGaussian(normIp, blurRadius, blurRadius, .01);
    // ip - normIp = -normIp + ip:
    normIp.multiply(-1.0);
    normIp.copyBits(ip, 0, 0, Blitter.ADD);
    return normIp;
  }


}