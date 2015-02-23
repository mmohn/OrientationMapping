import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import ij.plugin.filter.*;
import ij.plugin.*;
import filters.*;
import mapping.*;

public class Orientation_Mapping implements PlugInFilter, KeyListener, MouseListener {

/*
  This ImageJ plugin allows to create RGB maps for grains with different
  orientations in HRTEM images of polycrystalline samples.
  
  Version: 1.1 (2015-02-23, 16:30 mmohn)
  
  Dependencies:
  - mapping.MapRGB Version 1.0
  - filters.FilterMasks Version 1.0
  - filters.Normalize Version 1.0
  - filters.Padding Version 1.0
  
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

  public int setup(String arg, ImagePlus imp) {
    this.imp = imp;
    return DOES_32; // may be generalized to all images types: DOES_ALL
  }
  
  // Program information
  String pluginName = "OrientationMapping";
  String pluginVersion = "1.1";
  
  // Global variables & default values
  int n = 3; // number of orientations
  int m = 6; // rotational symmetry
  boolean doNormalize = true;
  boolean saveLog = false;
  double stdDevRadius1; // radius of the stdDev filter (normalization)
  double stdDevRadius2; // radius of the stdDev filter (mapping)
  double blurRadius = 50; // radius of the Gaussian blur (normalization)
  int startHue, stopHue, hueRange; // hues range for RGB mapping
  boolean showNormalized, showFilterMasks, showFourierFiltered, showFiltered, show32bitStack, showRGBFilterMasks;
  
  // Images, ImageStacks, ImageProcessors and Titles
  ImagePlus imp; // original image (PlugInFilter)
  ImageProcessor ip2; // global version of ip
  String originalTitle;
  int width, height; // width and height of the original image
  
  // variables for the FHT
  FHT fht;
  ImagePlus fhtImp;
  ImageWindow fhtWin;
  ImageCanvas fhtCan;
  int fhtSize; // width/height of the FHT
  int x0, y0; // coordinates of the FHT center
  Overlay fhtOverlay;
  int step;
  String hint;
  TextRoi hintRoi;
  Font font = new Font("Arial", Font.PLAIN, 12);
  int hintOffset = 10;
  Boolean isHint = false;
  Boolean isR0Set = false;
  PointRoi reflectionRoi;
  Boolean isReflection = false;
  OvalRoi minCircle;
  Boolean isMinCircle = false;
  OvalRoi maxCircle;
  Boolean isMaxCircle = false;
  
  // variables for the FilterMasks
  double r0, phi0; // position of the selected reflection
  double rmin, rmax; // radii for low and high freq. threshold
  
  
  
  public void run(ImageProcessor ip) {
  
    // get the name of the original image
    originalTitle = imp.getTitle();
    ip2 = ip.duplicate();
    width = ip2.getWidth();
    height = ip2.getHeight();
  
    // Create GenericDialog
    GenericDialog gd = new GenericDialog("Orientation Mapping");
    gd.addMessage("--- B A S I C   S E T T I N G S ---");
    gd.addNumericField("Number of orientations:", n, 0);
    gd.addNumericField("Rotational symmetry:", m, 0);
    gd.addCheckbox("Save log file", saveLog);
    gd.addMessage("--- C O N T R A S T   N O R M A L I Z A T I O N ---");
    gd.addCheckbox("Normalize contrast", doNormalize);
    gd.addNumericField("Radius for StdDev (px) [¹]:", stdDevRadius1, 2);
    gd.addNumericField("Radius for Gaussian Blur (px):", blurRadius, 2);
    gd.addMessage("--- S H O W / H I D E   R E S U L T S ---");
    String[] checkboxTitles = {"Show Normalized image", "Show filter masks", "Show Fourier filtered images", "Show filtered images", "Show 32 bit RGB stack", "Show RGB filter mask"};
    boolean[] checkboxDefaults = {false, false, false, false, false, false};
    gd.addCheckboxGroup(3, 2, checkboxTitles, checkboxDefaults, null);
    gd.addMessage("--- C O L O R   R A N G E ---");
    String[] colorArray = {"red", "yellow", "green", "cyan", "blue", "magenta"};
    String defaultColor = colorArray[0];
    gd.addChoice("Start", colorArray, defaultColor);
    gd.addChoice("Stop", colorArray, defaultColor);
    gd.addMessage("--- S t d D e v   F I L T E R ---");
    gd.addNumericField("Radius for StdDev (px) [¹]:", stdDevRadius2, 2);
    gd.addMessage("[¹] StdDev radii <= 0 will replaced with an estimated value.");
    gd.showDialog();
    if ( gd.wasCanceled() ) {
      IJ.error("Plugin canceled!");
      return;
    }
    // Read values from GenericDialog
    n = (int) Math.round(gd.getNextNumber());
    if (n < 2) {
      IJ.error("Input Error", "Number of orientations must be greater than 1.");
      return;
    }
    m = (int) Math.round(gd.getNextNumber());
    if (m < 2) {
      IJ.error("Input Error", "Rotational symmetry must be at least two-fold.");
      return;
    }
    saveLog = gd.getNextBoolean();
    doNormalize = gd.getNextBoolean();
    stdDevRadius1 = gd.getNextNumber();
    blurRadius = gd.getNextNumber();
    if (blurRadius < 0) blurRadius *= -1.0;
    showNormalized = gd.getNextBoolean();
    showFilterMasks = gd.getNextBoolean();
    showFourierFiltered = gd.getNextBoolean();
    showFiltered = gd.getNextBoolean();
    show32bitStack = gd.getNextBoolean();
    showRGBFilterMasks = gd.getNextBoolean();
    String firstColor = gd.getNextChoice();
    String lastColor = gd.getNextChoice();
    startHue = Arrays.asList(colorArray).indexOf(firstColor)*60;
    stopHue = Arrays.asList(colorArray).indexOf(firstColor)*60;
    if (stopHue < startHue) stopHue += 360;
    hueRange = 360;
    if (stopHue != startHue) hueRange = stopHue - startHue;
    stdDevRadius2 = gd.getNextNumber();
    
    // calculate and show the PowerSpectrum of the original image
    fht = new FHT(Padding.getPaddedProcessor(ip));
    fhtSize = Padding.getPaddedSize(ip);
    x0 = fhtSize/2;
    y0 = x0;
    fht.transform();
    fhtImp = new ImagePlus("Power Spectrum of " + originalTitle, fht.getPowerSpectrum());
    fhtImp.show();
    
    // add Mouse and KeyListeners for user input (FHT)
    fhtWin = fhtImp.getWindow();
    fhtCan = fhtImp.getCanvas();
    fhtWin.addKeyListener(this);
    fhtCan.addKeyListener(this);
    fhtCan.addMouseListener(this);
    
    /* first step: select reflection 
    (other steps will be started by the KeyListener!)
    */
    fhtOverlay = new Overlay();
    fhtImp.setOverlay(fhtOverlay);
    hint = "Step 1: Click on the image to select a reflection";
    updateHint();
    IJ.setTool("point");
    step = 1;
    fhtImp.updateAndDraw();
  
  } // END of run method
  
  
  /* Methods for updates in the FHT Overlay */
  
  // refresh hint in overlay
  public void updateHint() {
    if (isHint) fhtOverlay.remove(hintRoi);
    hintRoi = new TextRoi(hintOffset, hintOffset, hint, font);
    hintRoi.setStrokeColor(Color.yellow);
    hintRoi.setNonScalable(true);
    fhtOverlay.add(hintRoi);
    isHint = true;
    fhtImp.updateAndDraw();    
  }
  
  // update the position of the reflection marker
  public void updateReflection(int x, int y) {
    if (isReflection) fhtOverlay.remove(reflectionRoi);
    reflectionRoi = new PointRoi(x, y);
    fhtOverlay.add(reflectionRoi);
    isReflection = true;
    fhtImp.updateAndDraw();
  }
  
  // update the ROI for the lower freq. limit
  public void updateMinCircle(int drmin) {
    if (isMinCircle) fhtOverlay.remove(minCircle);
    minCircle = new OvalRoi(x0-drmin, y0-drmin, 2*drmin, 2*drmin);
    fhtOverlay.add(minCircle);
    isMinCircle = true;
    fhtImp.updateAndDraw();
  }
  
  // update the ROI for the upper freq. limit
  public void updateMaxCircle(int drmax) {
    if (isMaxCircle) fhtOverlay.remove(maxCircle);
    maxCircle = new OvalRoi(x0-drmax, y0-drmax, 2*drmax, 2*drmax);
    fhtOverlay.add(maxCircle);
    isMaxCircle = true;
    fhtImp.updateAndDraw();
  }
  
  
  /* MouseListener(s) */
    
  public void mouseClicked(MouseEvent e) {
    int x = fhtCan.offScreenX(e.getX());
    int y = fhtCan.offScreenY(e.getY());
    int rx = x - x0;
    int ry = -y + y0;
    if (step == 1) {
      r0 = FilterMasks.getRadius(rx, ry);
      phi0 = FilterMasks.getAngle(rx, ry);
      hint = "Selected reflection: (r=" + Math.round(r0) + 
	     ", phi=" + Math.round(phi0) + "). Press ENTER to continue.";
      updateHint();
      updateReflection(x, y);
      isR0Set = true;
    }
    if (step == 2) {
      if (FilterMasks.getRadius(rx, ry) <= r0) {
	rmin = FilterMasks.getRadius(rx, ry);
	hint = "Selected lower threshold: rmin=" + Math.round(rmin) +
		". Press ENTER to continue.";
	updateHint();
	int drmin = (int) Math.round(rmin);
	updateMinCircle(drmin);
	fhtImp.updateAndDraw();
      }
    }
    if (step == 3) {
      if (FilterMasks.getRadius(rx, ry) >= r0) {
	rmax = FilterMasks.getRadius(rx, ry);
	hint = "Selected upper threshold: rmax=" + Math.round(rmax) +
		". Press ENTER to continue.";
	updateHint();
	int drmax = (int) Math.round(rmax);
	updateMaxCircle(drmax);
	fhtImp.updateAndDraw();
      }
    }
  }
  public void mousePressed(MouseEvent e) {}	
  public void mouseReleased(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}
  
  
  /* Key Listener(s) */
  
  public void keyTyped(KeyEvent e) {
    if (e.getKeyChar() == 10) { // ENTER key
      if ((step == 1) && isR0Set) {
	hint = "Step 2: Select lower frequency threshold. Press ENTER to skip.";
	updateHint();
	fhtWin.toFront();
	step++;
      }
      else if (step == 2) {
	hint = "Step 3: Select upper frequency threshold. Press ENTER to skip.";
	updateHint();
	fhtWin.toFront();
	step++;
      }
      else if (step == 3) {
	fhtCan.removeMouseListener(this);
	fhtCan.removeKeyListener(this);
	fhtWin.removeKeyListener(this);
	fhtImp.close();
	step = 0;
	if (stdDevRadius2 <= 0) { // estimate it
	  stdDevRadius2 = fhtSize*2.0/r0;
	}
	if (stdDevRadius1 <= 0) {
	  stdDevRadius1 = fhtSize*2.0/r0;
	}
	
	// create filter masks and show them
	ImageStack filterMasksIs = FilterMasks.createStack(fhtSize, fhtSize, n, m, phi0, rmin, rmax);
	if ( showFilterMasks ) {
	  // filterMasksIs can't be duplicated or cloned, but duplicating the ImagePlus
	  // prevents the displayed image from future modifications of filterMasksIs ...
	  ImagePlus showImp = new ImagePlus("", filterMasksIs).duplicate();
	  showImp.setTitle("Filter Masks for " + originalTitle); // image title without this line: "DUP_..."
	  showImp.show();
	  IJ.setMinAndMax(0, 1);
	}
	
	// normalize the original image, and show it
	if ( doNormalize ) {
	  ip2 = Normalize.divideStdDevBlur(ip2, stdDevRadius1, blurRadius);
	  if ( showNormalized ) {
	    ImagePlus showImp1 = new ImagePlus("Normalized " + originalTitle, ip2.duplicate());
	    showImp1.show();
	    new ContrastEnhancer().stretchHistogram(showImp1, 0.5);
	    showImp1.updateAndDraw();
	  }
	}
	
	// calculate the FHT of the original or normalized image
	fht = new FHT(Padding.getPaddedProcessor(ip2));
	fht.transform();
	fht.swapQuadrants(); // zero freq. at center of image (like in PowerSpectrum)
	
	// for each orientation, multiply filter masks and FHT, and perform inverse transform
	ImageStack fftFilteredIs = new ImageStack(width, height); // create an empty ImageStack
	FHT tempFHT = fht; // initialize copy of the FHT
	for (int s = 1; s <= n; s++) {
	  tempFHT = fht.getCopy();
	  tempFHT.copyBits(filterMasksIs.getProcessor(s), 0, 0, Blitter.MULTIPLY);
	  tempFHT.swapQuadrants();
	  tempFHT.inverseTransform();
	  fftFilteredIs.addSlice("" + s, Padding.getCroppedProcessor(tempFHT.duplicate(), width, height));
	}
	
	// show the FFT filtered images
	if ( showFourierFiltered ) {
	  ImagePlus showImp2 = new ImagePlus("FFT Filtered " + originalTitle, fftFilteredIs);
	  showImp2.show();
	  new ContrastEnhancer().stretchHistogram(showImp2, 0.5);
	  showImp2.updateAndDraw();
	}
	
	// apply variance filter and sqrt to the FFT filtered stack
	ImageStack filteredIs = new ImageStack(width, height);
	for (int s = 1; s <= n; s++) {
	  ImageProcessor tempIp = fftFilteredIs.getProcessor(s).duplicate();
	  new RankFilters().rank(tempIp, stdDevRadius2, RankFilters.VARIANCE);
	  tempIp.sqrt();
	  filteredIs.addSlice("" + s, tempIp);
	}
	
	// show the filtered stack
	if ( showFiltered ) {
	  ImagePlus showImp3 = new ImagePlus("Filtered " + originalTitle, filteredIs);
	  showImp3.show();
	  new ContrastEnhancer().stretchHistogram(showImp3, 0.5);
	  showImp3.updateAndDraw();
	}
	
	// map filterMasks stack to 32 bit RGB stack, normalize and show it
	if ( showRGBFilterMasks ) {
	  ImagePlus filterMasksImp = new ImagePlus("", filterMasksIs);
	  new StackConverter(filterMasksImp).convertToGray32();
	  ImageStack mappedMasksIs = MapRGB.mapStackToRGB(filterMasksImp.getStack(), startHue, hueRange);
	  double globalMaximum = 0;
	  for (int s = 1; s <= 3; s++) {
	    // apply a median filter to remove outliers at the overlap between adjacent segments
	    new RankFilters().rank(mappedMasksIs.getProcessor(s), 2, RankFilters.MEDIAN);
	    double tempMaximum = mappedMasksIs.getProcessor(s).getStatistics().max;
	    globalMaximum = Math.max(globalMaximum, tempMaximum);
	  }
	  for (int s = 1; s <= 3; s++) {
	    mappedMasksIs.getProcessor(s).multiply(255.0/globalMaximum);
	  }
	  ImagePlus mappedMasksImp = new ImagePlus("RGB Filter Mask of " + originalTitle, mappedMasksIs);
	  new StackConverter(mappedMasksImp).convertToGray8();
	  new ImageConverter(mappedMasksImp).convertRGBStackToRGB();
	  mappedMasksImp.show();
	}
	
	// map filtered stack to a 32 bit RGB stack, normalize and show it
	ImageStack mappedIs = MapRGB.mapStackToRGB(filteredIs, startHue, hueRange);
	double globalMin = mappedIs.getProcessor(1).getStatistics().min;
	double globalMax = mappedIs.getProcessor(1).getStatistics().max;
	for (int s = 2; s <= 3; s++) {
	  double tempMin = mappedIs.getProcessor(s).getStatistics().min;
	  double tempMax = mappedIs.getProcessor(s).getStatistics().max;
	  globalMin = Math.min(globalMin, tempMin);
	  globalMax = Math.max(globalMax, tempMax);
	}
	double range = globalMax - globalMin;
	for (int s = 1; s <= 3; s++) {
	  mappedIs.getProcessor(s).subtract(globalMin);
	  mappedIs.getProcessor(s).multiply(255.0/range);
	}
	if ( show32bitStack ) {
	  // mappedIs can't be duplicated or cloned, but duplicating the ImagePlus
	  // prevents the displayed image from future modifications of mappedIs ...
	  ImagePlus showImp4 = new ImagePlus("", mappedIs).duplicate();
	  showImp4.setTitle("32bit RGB Stack of " + originalTitle);
	  showImp4.show();
	  new ContrastEnhancer().stretchHistogram(showImp4, 0.5);
	  showImp4.updateAndDraw();
	}
	
	// convert to 8 bit and create RGB image
	ImagePlus result = new ImagePlus("Orientation Map of " + originalTitle, mappedIs);
	new ContrastEnhancer().stretchHistogram(result, 0.5); // very important for 8bit conversion
	new StackConverter(result).convertToGray8();
	new ImageConverter(result).convertRGBStackToRGB();
	result.show();
	
	// save log file
	if ( saveLog ) {
	  SaveDialog sd = new SaveDialog("Save log file", "log", ".txt");
	  String filepath = sd.getDirectory() + sd.getFileName();
	  try{
	    PrintWriter pw = new PrintWriter(new FileWriter(filepath));
	    String firstLine = pluginName + " Version " + pluginVersion + ", " + new Date().toString();
	    pw.println(firstLine);
	    pw.println(firstLine.replaceAll(".", "-"));
	    pw.println("Original Image:     " + originalTitle);
	    if (doNormalize) {
	      pw.println("Normalization:      StdDev r = " + String.format("%,.2f", stdDevRadius1) + " px");
	      pw.println("                    Gaussian r = " + String.format("%,.2f", blurRadius) + " px");
	    }
	    pw.println("Reflection:         r = " + 
			String.format("%,.2f", r0) + " px, phi = " + String.format("%,.2f", phi0) + " deg");
	    pw.println("Bandpass filter:    rmin = " + 
			String.format("%,.2f", rmin) + " px, rmax = " + String.format("%,.2f", rmax) + " px");
	    int stopHue2 = startHue + hueRange;
	    pw.println("Colors:             " + startHue + " <= hue <= " + stopHue2);
	    pw.println("StdDev filter:      r = " + String.format("%,.2f", stdDevRadius2) + " px");
	    pw.close();
	  } catch (IOException ioe) {
	    IJ.error("Error", "Could not save file " + filepath);
	  }
	}
	
	// reset ImageJ tool and progress bar
	IJ.setTool("rectangle");
	IJ.showProgress(1.0);
	
      } // END of step 3
    } // END of "if getKeyChar == ..."
  } // END of keyTyped
  public void keyPressed(KeyEvent e) {}
  public void keyReleased(KeyEvent e) {}

  
  

} // END of Orientation_Mapping class