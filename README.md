# Orientation Mapping Plugin (ImageJ)

*Version 1.4, Copyright (c) 2015 Michael Mohn and Ossi Lehtinen, Ulm University*

## Description
This ImageJ plugin allows for orientation mapping in High-resolution Transmission Electron Microscopy (HRTEM) images of polycrystalline samples.

## Installation
Copy all files and folders in a new subfolder "Orientation_Mapping" in the ImageJ Plugin folder. Make sure that the directory structure is unchanged.
The plugin should be available in the "Plugins" menu after ImageJ has been restarted.  
If necessary, compile the source file "Orientation_Mapping.java" using the "Compile and Run..." function of ImageJ.


## Requirements for the input image
The input image has to show multiple grains of same lattice constant with atomic resolution. In the FFT of the image, the reflections of the individual grains should then be found on a ring.  
Non-square images and images with other than power-of-two dimensions can be used, as they will be padded to a larger square with power-of-two width.

Currently, only 32 bit images can be processed by the plugin.


## Functional principle

### Fourier filtering
In this plugin, different grains are located by Fourier filtering of the HRTEM image. Filter masks will be applied to the Fast Fourier Transform (FFT) / Fast Hartley Transform (FHT) of the image, with rotational symmetry based on the symmetry of the analysed structure.

*Note, that the plugin has been designed and tested with images of atomically thin samples, and may produce unexpected results with images of, e.g., thicker samples or samples with nano particles.Note, that the plugin has been designed and tested with images of atomically thin samples, and may produce unexpected results with images of, e.g., thicker samples or samples with nano particles.*

### Quantification of the local contrast
The local contrast of the original and filtered images is quantified using the variance filter (Process > Filters > Variance...) of ImageJ. Subsequent calculation of the square root yields the local standard deviation (StdDev) of the pixel values as a measure for the contrast, analogous to the root mean square (RMS) contrast definition.
Unless changed manually in the dialog window (to a value > 0), the StdDev radii will be estimated by the plugin (~2x lattice spacing).

### Contrast normalization
According to the above contrast quantification, the image contrast can be normalized by dividing by the local standard deviation. By default, the plugin divides by a StdDev filtered, blurred version (Gaussian blur, radius: 50 pixels) of the original image.


## Basic usage
1. Open the HRTEM image, or select an image which is already open. If necessary, convert it to 32 bit (Image > Type > 32 bit).
2. Run the "Orientation Mapping" plugin from the "Plugins" menu.
3. In the first section of the dialog window, enter the rotational symmetry (*m*) of the investigated structure (e.g. 6-fold for a hexagonal structure). The "number of orientations" value *n* determines the number of segments per reflection. In total, this yields a total number of *n* \* *m* segments for the whole 360 degrees.
4. Follow the instructions in the shown FFT of the image. After each step, press the ENTER key to proceed.  
    * In the first step, you will be asked to click on one of the reflections. Your choice will be indicated by a point selection. You may correct the chosen position until you want to continue with the next step.
    * In the second step, the radius for the lower frequency threshold (highpass filter) is selected by clicking in the FFT window. A circular selection will appear if your selection is valid. You may skip this step by pressing ENTER without any selection. In the latter case, no highpass filtering will be performed.
    * In the third step, the upper frequency threshold is selected likewise.
5. According to your input, the plugin will now create a stack of *n* filter masks. Each mask will then be separately applied to the FFT of the original (optionally normalized) image, and Fourier filtered images are obtained using the inverse FFT. Finally, colors are assigned to the  filtered images, such that each orientation will show up with a different color in the resulting RGB image.


## Advanced usage

### Log files
If the "Save log file" option is selected in the dialog window, a save dialog will show up after the orientation map has been created. The log file contains all parameters needed to reproduce the results.

### Contrast normalization
By default, the plugin will normalize the contrast of the original image. Usually, images with normalized contrast result in more uniform orientation maps. However, you may want to deactivate the normalization to prevent amplification of noise, or adjust the parameters in the "Contrast normalization" section of the dialog to enhance the results.

### Results
In the "Show/Hide Results" section of the dialog window, you can activate additional output images with intermediate or supplemental results:

* "Show normalized image": If the above-mentioned normalization option is activated as well, a normalized version of the original image will be shown. (Image title: "Normalized ...")
* "Show filter masks": A stack with the *n* binary (8 bit) filter masks will be shown. (Image title: "Filter Masks for ...")
* "Show Fourier filtered images": Shows the Fourier filtered images in an *n* slice stack. (Image title: "FFT Filtered ...")
* "Show filtered images": Same as above, after StdDev filtering. (Image title: "Filtered ...")
* "Show 32 bit RGB stack": The above StdDev filtered images, mapped to a 32 bit RGB stack (3 slices for Red, Green and Blue channel). (Image title: "32bit RGB Stack of ...")
* "Show RGB filter mask": A "colormap" for the orientation map will be shown. The colormap is created by mapping the filter masks to an RGB image. (Image title: "RGB Filter Mask for ...")

### Color range
For the RGB orientation map, the filtered images for the *n* different orientations are mapped to *n* colors with different hue. By default, the used colors are picked at equal distances along the full RGB color range. The color range can however be constrained by selecting different start and end colors. Note that the order of the start and end values is important, and that having the same color as the start and end value always results in the full RGB range to be used.

*Examples for n = 3 (hue values in brackets):*

* **red as start and end value:** red (0), green (120), blue (240)
* **start = blue, end = red:** blue (240), magenta (300), red (0)
* **start = red, end = blue:** red (0), green (120), blue (240)

Note that the first and third example are only equivalent for *n* = 3, but not for, e.g., *n* = 6!

### Filter radii
By default, the radii of the standard deviation filters are estimated by the plugin. They can however be changed manually, as described in the section "Functional principle".

## Refering to this plugin
If results of the plugin/method are used in a scientific publication, please cite the article:

Ossi Lehtinen, Hannu-Pekka Komsa, Artem Pulkin, Michael Brian Whitwick, Ming-Wei Chen, Tibor Lehnert, Michael J. Mohn, Oleg V. Yazyev, Andras Kis, Ute Kaiser, and Arkady V. Krasheninnikov, *Atomic scale microstructure and properties of Se-deficient two-dimensional MoSe2*, ACS Nano (2015), DOI: 10.1021/acsnano.5b00410


