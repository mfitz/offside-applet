Offside Applet
===============

A Java applet to explain the Offside Rule in Association Football. I wrote the applet itself back in 2003, but I have recently wrapped it in a Maven project. 

![Offside applet screenshot](/src/main/resources/images/offside-applet.png "Offside applet screenshot")

![Offside applet screenshot 2](/src/main/resources/images/offside-applet-offside.png "Offside applet screenshot 2")

![Offside applet screenshot 3](/src/main/resources/images/offside-applet-drawing.png "Offside applet screenshot 3")



Building a distro
===========

Uses Apache Maven (http://maven.apache.org/). To compile and build a distro in zip format,
use:

    mvn clean assembly:assembly 
    
Running the applet
============

Having built a distro, you can unpack the subsequent zip file to a local directory. There you will see a new subdirectory called something like "offside-applet-1.0-SNAPSHOT". Inside this directory you will find a jar file, an image file, and an HTML file. You can run the applet locally using the JDK's appletviewer tool:

    appletviewer offside-applet.html

Alternatively, you could open offside-applet.html in a java-enabled browser. As with all Java applets, your particular browser and JRE combination may be different from those on which I have tested this method of running the applet, and that difference may cause some form of fail. But hey - that's applets for you. I did write this back in 2003, so YMMV. There's a reason applets died out, right?

You could also drop the 3 files into the same directory of a web server and serve the applet up over HTTP, if you fancy. I've done that on my personal website at http://www.michaelfitzmaurice.com/OffsideApplet.html

Running the applet in an IDE
============

Everything the applet needs at runtime is in the same directory when you build and unpack the distro. However, this is not the case when launching the applet inside your IDE. The football pitch image file is in a different directory from the applet's runtime codebase, so we need a way to tell the applet where to find it. I added an applet parameter called "pitch-image" to allow this configuration. You should use a relative path for this parameter, for example in Eclipse, I configured it thus:

    ../../src/main/resources/images/footballPitch.gif
    
![Offside applet pitch-image param](/src/main/resources/images/offside-applet-eclipse-ide.png "Offside applet pitch-image")    


 




