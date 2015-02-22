package mapping;

import ij.*;
import ij.process.*;

public class MapRGB {

/* Methods for RGB Stacks, OrientationMapping Version: 1.0 (2015-02-22, 21:01 mmohn)
  
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

  // map an n slice stack to a 3-slice RGB stack, using n different colors
  public static ImageStack mapStackToRGB(ImageStack inputIs, int startHue, int hueRange) {    
    ImageStack targetIs = new ImageStack(inputIs.getWidth(), inputIs.getHeight()); // empty stack
    int n = inputIs.getSize();
    String[] colorStr = {"Red", "Green", "Blue"};
    for (int s = 1; s <= n; s++) {
      int hue = (int) startHue + (int) Math.round( (s-1)*hueRange*1.0/n );
      int[] rgb = hueToRGB(hue%360);
      ImageProcessor tempIp = inputIs.getProcessor(s).duplicate(); // initialize
      for (int i = 0; i < 3; i++) {
	tempIp = inputIs.getProcessor(s).duplicate();
	tempIp.multiply(rgb[i]*1.0/n);
	if (s == 1) {
	  targetIs.addSlice(colorStr[i], tempIp);
	} else {
	  targetIs.getProcessor(i+1).copyBits(tempIp, 0, 0, Blitter.ADD);
	}
      }
    }
    return targetIs;
  }
  
  // convert hue value to array with R,G and B values
  public static int[] hueToRGB(int hue) {
    // increment/decrement for RGB values, per deg
    double step = 4.25;
    // limits for hue: 0 and 359
    if (hue < 0) hue = 0;
    if (hue > 359) hue = 359;
    // array for RGB values
    int[] rgb = new int[3];

    // map hue value to RGB array
    if (hue <= 60) { // red to yellow
      rgb[0] = 255;
      rgb[1] = (int) Math.round(step*hue);
    }
    else if (hue <= 120) { // yellow to green
      hue -= 60;
      rgb[0] = (int) 255 - (int) Math.round(step*hue);
      rgb[1] = 255;
    }
    else if (hue <= 180) { // green to cyan
      hue -= 120;
      rgb[1] = 255;
      rgb[2] = (int) Math.round(step*hue);
    }
    else if (hue <= 240) { // cyan to blue
      hue -= 180;
      rgb[1] = (int) 255 - (int) Math.round(step*hue);
      rgb[2] = 255;
    }
    else if (hue <= 300) { // blue to magenta
      hue -= 240;
      rgb[0] = (int) Math.round(step*hue);
      rgb[2] = 255;
    }
    else { // magenta to red
      hue -= 300;
      rgb[0] = 255;
      rgb[2] = (int) 255 - (int) Math.round(step*hue);
    }
    return rgb;
  }


}