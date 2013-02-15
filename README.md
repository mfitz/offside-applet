Offside Applet
===============

A Java applet to explain the Offside Rule in Association Football. I wrote the applet itself back in 2003, but I have recently wrapped it in a Maven project. 

Building
===========

Uses Apache Maven (http://maven.apache.org/). To compile and build a distro in zip format,
use:

    mvn clean assembly:assembly 

You can then unpack the subsequent zip file to a local directory, wherein you will see a new subdirectory called something like "offside-applet-1.0-SNAPSHOT". Inside this directory you will find a jar file, an image file, and an HTML file. You can run the applet locally using the JDK's appletviewer tool:

    appletviewer offside-applet.html

Alternatively, you could open offside-applet.html in a java-enabled browser. As with all Java applets, your particular browser and JRE combination may be different from those on which I have tested this method of running the applet, and that difference may cause some form of fail. But hey - that's applets for you. I did write this back in 2003, so, you know - YMMV. There's a reason applets died out, right?

You could also drop the 3 files into the same directory of a web server and serve the applet up over HTTP, if you fancy. I've done that on my personal website at http://www.michaelfitzmaurice.com/OffsideApplet.html 



