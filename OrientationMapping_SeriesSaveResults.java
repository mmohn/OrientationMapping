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

public class OrientationMapping_SeriesSaveResults implements PlugIn {

/*
  This version to the OrientationMapping plugin applies a set of pre-defined filter
  masks to a series of TIFF images.
  
  Version: 0.11 (2019-07-24, 11:42 mmohn)
  
  Dependencies:
  - mapping.MapRGB Version 1.0
  - filters.FilterMasks Version 1.1
  - filters.Normalize Version 1.0
  - filters.Padding Version 1.1
  
  Copyright (c) 2019 Michael Mohn, Ulm University
    
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
    
  If results of the plugin/method are used in a scientific publication, please
  cite the article:
  Ossi Lehtinen, Hannu-Pekka Komsa, Artem Pulkin, Michael Brian Whitwick,
  Ming-Wei Chen, Tibor Lehnert, Michael J. Mohn, Oleg V. Yazyev, Andras Kis,
  Ute Kaiser, and Arkady V. Krasheninnikov, "Atomic scale microstructure and
  properties of Se-deficient two-dimensional MoSe2", ACS Nano (2015),
  DOI: 10.1021/acsnano.5b00410
    
    ----------------------------------------------------------------------------
*/
  
  // Program information
  String pluginName = "OrientationMapping_SeriesSaveResults";
  String pluginVersion = "0.11";
  
  // Global variables & default values
  boolean doNormalize = true;
  double stdDevRadius1; // radius of the stdDev filter (normalization)
  double stdDevRadius2; // radius of the stdDev filter (mapping)
  double blurRadius = 50; // radius of the Gaussian blur (normalization)
  int startHue, stopHue, hueRange; // hues range for RGB mapping
  
  // List of open images, by name
  int[] imageIDs; // -> WindowManager getIDList()
  int nImages; // number of open images
  String[] titles; // for titles via IDs
  
  // Variables for the processed image stacks
  ImagePlus impSeries; // ImagePlus for the raw data
  ImagePlus impMasks; // ImagePlus for the filter masks
  ImageStack isSeries;
  ImageStack isMasks;
  ImageProcessor ipSeries;
  ImageProcessor ip2; // copy of ipSeries for filtered images
  ImageProcessor ipMasks;
  
  // properties of the "raw" image stack
  int width, height, nSlicesSeries;
  String seriesTitle;
  
  // properties of the filter mask stack
  String masksTitle;
  int nSlicesMasks;
  
  
  // variables for the FHT
  FHT fht, fhtMasked;
  ImagePlus fhtImp;
  int fhtSize; // width/height of the FHT
  
  // Save dialog
  String path;
  
  
  
  public void run(String arg) {
    /*
    Instead of PlugInFilter, this is now based on the standard PlugIn class
    so the plugin is not associated with any ImagePlus or ImageProcessor yet.
    */
  
    // Get list of open images, with IDs to access them and names to show in dropdown menus
    // titles = WindowManager.getImageTitles(); // this does not exist!
    imageIDs = WindowManager.getIDList();
    if (imageIDs == null) {
        IJ.error("No open image windows.");
        return;
    }
    nImages = imageIDs.length;
    //IJ.showMessage("Number of images: " + nImages);
    titles = new String[nImages];
    for (int i=0; i<nImages; i++) {
        titles[i] = WindowManager.getImage(imageIDs[i]).getTitle();
        //IJ.showMessage(titles[i]);
    }
    String topTitle = WindowManager.getCurrentImage().getTitle();
    //IJ.showMessage(topTitle);
  
    // Create GenericDialog
    GenericDialog gd = new GenericDialog("Orientation Mapping");
    gd.addMessage("--- I M A G E   S T A C K S ---");
    gd.addChoice("Image Series", titles, topTitle);
    gd.addChoice("Filter Masks", titles, topTitle);
    gd.addMessage("--- C O N T R A S T   N O R M A L I Z A T I O N ---");
    gd.addCheckbox("Normalize contrast", doNormalize);
    gd.addNumericField("Radius for StdDev (px):", stdDevRadius1, 2);
    gd.addNumericField("Radius for Gaussian Blur (px):", blurRadius, 2);
    gd.addMessage("--- C O L O R   R A N G E ---");
    gd.addMessage("The used colors are picked at equal distances along the\n"
		  +"color range limited by the selected start and end values.");
    String[] colorArray = {"red", "yellow", "green", "cyan", "blue", "magenta"};
    String defaultColor = colorArray[0];
    gd.addChoice("Start", colorArray, defaultColor);
    gd.addChoice("End", colorArray, defaultColor);
    gd.addMessage("Having the same color as the start and end value\n" + 
		  "results in the full RGB range to be used.");
    gd.addMessage("--- S t d D e v   F I L T E R ---");
    gd.addNumericField("Radius for StdDev (px):", stdDevRadius2, 2);
    gd.showDialog();
    if ( gd.wasCanceled() ) {
      IJ.error("Plugin canceled!");
      return;
    }

    // Read values from GenericDialog
    seriesTitle = gd.getNextChoice();
    masksTitle = gd.getNextChoice();
    doNormalize = gd.getNextBoolean();
    stdDevRadius1 = gd.getNextNumber();
    blurRadius = gd.getNextNumber();
    if (blurRadius < 0) blurRadius *= -1.0; // still okay, to avoid completely wrong input
    String firstColor = gd.getNextChoice();
    String lastColor = gd.getNextChoice();
    startHue = Arrays.asList(colorArray).indexOf(firstColor)*60;
    stopHue = Arrays.asList(colorArray).indexOf(lastColor)*60;
    if (stopHue <= startHue) stopHue += 360;
    hueRange = stopHue - startHue;
    stdDevRadius2 = gd.getNextNumber();
    
    // get ImagePlus, Stacks and Processors
    impSeries = WindowManager.getImage(seriesTitle);
    ipSeries = impSeries.getProcessor(); // access different slices later
    nSlicesSeries = impSeries.getNSlices();
    impMasks = WindowManager.getImage(masksTitle);
    ipMasks = impMasks.getProcessor();
    nSlicesMasks = impMasks.getNSlices();
    
    // get width and height of input stack
    width = ipSeries.getWidth();
    height = ipSeries.getHeight();
    
    // Dialog: choose directory
    DirectoryChooser dc = new DirectoryChooser("Save location");
    path = dc.getDirectory();
    
    // loop over all slices of the image series, apply all filters
    // -> this is the main part of the whole PlugIn
    for (int i=0; i<nSlicesSeries; i++) {
    
        // accessing images and pre-processing
        impSeries.setSlice(i+1); // note that slices have one-based numbering
        ipSeries = impSeries.getProcessor();
        String sliceTitle = impSeries.getImageStack().getSliceLabel(i+1);
        ip2 = ipSeries.duplicate();
        if (doNormalize) {
            ip2 = Normalize.divideStdDevBlur(ip2, stdDevRadius1, blurRadius);
        }
        
        // the Fourier transform
        fht = new FHT(Padding.getPaddedProcessor(ip2));
        fht.transform();
        fht.swapQuadrants(); // zero freq. at center of image (like in PowerSpectrum)
        
        // loop over all filter masks
        ImageStack fftFilteredIs = new ImageStack(width, height);
        for (int j=0; j<nSlicesMasks; j++) {
            impMasks.setSlice(j+1);
            ipMasks = impMasks.getProcessor();
            fhtMasked = fht.getCopy();
            fhtMasked.copyBits(ipMasks, 0, 0, Blitter.MULTIPLY);
            fhtMasked.swapQuadrants();
            fhtMasked.inverseTransform();
            fftFilteredIs.addSlice(""+j, Padding.getCroppedProcessor(fhtMasked.duplicate(), width, height));
        }
        
        // perform variance filter
        ImageStack filteredIs = new ImageStack(width, height);
        for (int j = 1; j <= nSlicesMasks; j++) {
            ImageProcessor tempIp = fftFilteredIs.getProcessor(j).duplicate();
            new RankFilters().rank(tempIp, stdDevRadius2, RankFilters.VARIANCE);
            tempIp.sqrt();
            filteredIs.addSlice("" + j, tempIp);
        }
        
        // map filtered stack to a 32 bit RGB stack, normalize it
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
            mappedIs.getProcessor(s).multiply(1.0/range);
        }
        
        // convert to 8bit, create RGB image and add to stack
        ImagePlus temp = new ImagePlus(sliceTitle, mappedIs);
        new ContrastEnhancer().stretchHistogram(temp, 0.5); // very important for 8bit conversion
        new StackConverter(temp).convertToGray8();
        new ImageConverter(temp).convertRGBStackToRGB();
        new FileSaver(temp).saveAsTiff(path+"OrientationMap_"+i+".tif");
        
        
    }
    
    // reset progress bar (even if it was not used in this PlugIn itself)
    IJ.showProgress(1.0);
    IJ.showMessage("Saved " + nSlicesSeries + " images to folder " + path + ".");
    

  } // END of run method
  

} // END of Orientation_Mapping class
