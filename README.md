XMAX [![Build Status](https://travis-ci.org/usgs/xmax.svg?branch=master)](https://travis-ci.org/usgs/xmax)
====

### Purpose
   XMAX serves as a waveform display for seismic data and
   can perform various processing operations on this data
   such as band filtering, rotation, power spectra plotting, and
   similar analysis features.

### Configuration
   The main configuration is found in config.xml. File is under
    source control where user can change system paths accordingly

###### Data Setup

   Wildcarded mask of data files to load (when no cmd line args given):
```xml
    <DataMask>seed/IU_TUC/2014_21*</DataMask>
```

   Temporary directory to store decompressed traces in the internal format:
```xml
    <TempPath>resources/DATA_TEMP</TempPath>
```

   Quality control data file:
```xml
    <QCdataFile>/home/max/DATA/QC.xml</QCdataFile>
```

   Station data file extras (events, station info, multiplexed data, picks):
```xml
    <EventFileMask>resources/qcmt.ndk</EventFileMask>
    <StationInfoFile>resources/gsn_sta_list</StationInfoFile>
    <ResponsePath>resources/Responses</ResponsePath>
    <AllowMultiplexedData>true</AllowMultiplexedData>
    <PickPath>resources/Picks</PickPath>
```

###### View Setup

    Show big cursor, status bar, buttons:
```xml
    <ShowBigCursor>true</ShowBigCursor>
    <ShowStatusBar>true</ShowStatusBar>
    <ShowCommandButtons>true</ShowCommandButtons>
    <ShowCommandButtonsTop>false</ShowCommandButtonsTop>
```

    Panel order and count (trace, network, station, channel):
```xml
    <PanelCountUnit>1</PanelCountUnit>
    <UnitsInFrame>1</UnitsInFrame>
    <LogFile>logs/xmax.log</LogFile>
```

### Build

   In order to build this software, the asl-java-utils submodule must be
    configured correctly relative to the project root. This project is public
    and can be accessed by the url https://code.usgs.gov:asl/asl-java-utils.git.
    This may require manual setting when the publicly-available
    github mirror is used, as the default is to use an SSH protocol (requiring authentication).

###### Gradle Setup/Execution

   Gradle commands to run, clean, build and package xmax (uses build.xml)
```bash
    gradle run              # run java com.isti.xmax.XMAX from /build dir
    gradle clean            # delete build/ directory
    gradle standardBuild    # compile java src/ code to build/ directory. Builds everything including jar
    gradle jar              # create distribution build/libs/xmax-[version].jar (default target)
```

### Usage

###### Notes on run-time features

XMAX supports loading data into the program while running. 
If no configuration file is specified or available, XMAX will
attempt to launch itself with a minimal default configuration.
A configuration file is strongly recommended but may not be
necessary for all operations of the program, like viewing traces.

When rotating data, the progam requires that the data region under
analysis (i.e, what is shown in the graphs) is a contiguous region
of data, possessing no gaps. This can be verified by the gap
coloration mode; in this mode if a trace is shown as a single 
color, then it is able to be rotated. 

Data may have gaps outside the active display region; if so, then
on zoom-out the program will reset the rotation. Check the data
names in the graph panel to verify: data that is rotated will have
a "ROT. STD." or "ROT. (degrees)" message after its name. 

###### Basic Execution Display

   No arguments specified (reads data from resources/DATA in config.xml):
```ruby
    java -Xms512M -Xmx512M -jar xmax.jar
```

   Read data files found on path (-d option):
```ruby
    java -Xms512M -Xmx512M -jar xmax.jar -d '/xs0/seed/IU_ANMO/2012/2012_1{59,60}_*/00_LHZ*seed'
```

   Read serialized data from resources/DATA_TEMP specified in config.xml (-t option):
```ruby
    java -Xms512M -Xmx512M -jar xmax.jar -t
```

   Read serialized data from resources/DATA_TEMP AND from dataPath (-t -d options):
```ruby
    java -Xms512M -Xmx512M -jar xmax.jar -t -d '/xs0/seed/IU_ANMO/2012/2012_1{59,60}_*/00_LHZ*seed'
```

###### Basic Execution Serial Dump

   These serial dump operations are considered deprecated due to removal of serialized
   data objects in later Java releases, and depending on JVM settings are unlikely to work.

   Read any data files found in resources/DATA and dump serialized data into resources/DATA_TEMP (-T option):
    (**NOTE: This will wipe out any existing serialized data in resources/DATA_TEMP)
```ruby
    java -Xms512M -Xmx512M -jar xmax.jar -T
```

   Read data files found on path and dump serialized data in resources/DATA_TEMP (-T -d options):
    (**NOTE: This will wipe out any existing serialized data in resources/DATA_TEMP)
```ruby
    java -Xms512M -Xmx512M -jar xmax.jar -T -d '/xs0/seed/IU_ANMO/2012/2012_1{59,60}_*/00_LHZ*seed'
```

   Same as above but ALSO read existing serialized data in resources/DATA_TEMP (-T -d -t options):
    (**NOTE: This will APPEND new serialized data to that already in resources/DATA_TEMP)
```ruby
    java -Xms512M -Xmx512M -jar xmax.jar -T -t -d '/xs0/seed/IU_ANMO/2012/2012_1{59,60}_*/00_LHZ*seed'
```

   Explicitly pointing to a log4j.properties file (default is ./log4j.properties)
```ruby
    java -Dlog4j.configuration=file:./src/log4j.properties -Xms512M -Xmx512M -jar xmax.jar -d '/xs0/seed/IU_ANMO/2012/2012_1{59,60}_*/00_LHZ*seed'
```

###### Basic Execution in Eclipse

   Eclipse reads seed files from config DataMask at runtime (config.xml):
```xml
    <!-- Default DataMask path -->
    <DataMask>resources/Data/</DataMask>
   
    <!-- User DataMask examples --> 
    <DataMask>mseed/II_AAK/2013_026/</DataMask>
    <DataMask>seed/IU_PTGA/2014_193/</DataMask>
```
