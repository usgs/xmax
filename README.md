xmax
====

###Purpose
    XMAX waveform display for .seed files

###Configuration
    The main configuration is found in config.xml. File is under
    source control where user can change system paths accordingly

######Data Setup

    Wildcarded mask of data files to load (when no cmd line args given):
```xml
    <DataMask>resources/DATA</DataMask>
```

    Temporary directory to store decompressed traces in the internal format:
```xml
    <TempPath>resources/DATA_TEMP</TempPath>
```

    Quality control data file:
```xml
    <QCdataFile>/home/max/DATA/QC.xml</QCdataFile>
```

    Station data file extras (events, station info, multiplexed data):
```xml
    <EventFileMask>resources/*.ndk</EventFileMask>
    <StationInfoFile>resources/gsn_sta_list</StationInfoFile>
    <ResponsePath>resources/Responses</ResponsePath>
    <AllowMultiplexedData>true</AllowMultiplexedData>
```

######View Setup

    Show big cursor and status bar:
```xml
    <ShowBigCursor>true</ShowBigCursor>
    <ShowStatusBar>true</ShowStatusBar>
```

    Show command buttons:
```xml
    <ShowCommandButtons>true</ShowCommandButtons>
    <ShowCommandButtonsTop>false</ShowCommandButtonsTop>
```

    Panel order and count (trace, network, station, channel):
```xml
    <PanelCountUnit>1</PanelCountUnit>
    <UnitsInFrame>1</UnitsInFrame>
```

######SessionData Setup
    
    Frame position and dimensions:
```xml
    <State>NORMAL</State>
    <PosY>25</PosY>
    <PoxX>96</PosX>
    <Width>1373</Width>
    <Heighth>789</Heighth>
```

    Set user directories (will change per server):
```xml
    <MSEED>/Users/mth/mth/Projects/xmax</MSEED>
    <ASCII>/Users/mth/mth/Projects/xmax</ASCII>
    <SAC>/Users/mth/mth/Projects/xmax</SAC> 
```

###Build

######Ant Setup/Execution

    Ant commands to clean, compile and package xmax (uses build.xml)
```bash
    ant jar		# create distribution xmax.jar (default target)
    ant run		# run java com.isti.xmax.XMAX from /build dir
```

    Ant target tree dependencies
```bash
    ant jar		-> depends="compile, build-plugins, libraries"
    ant compile -> depends="init"
    ant init	-> depends="clean_build"
```

    Ant target descriptions (order of operations)
```bash
    ant clean_build 	-> delete build/ directory
    ant init			-> create build/ and build/resources/ directories
    ant compile			-> compile java src/ code to build/ directory
    ant build-plugins	-> compile java plugins/ code
    ant libraries		-> add lib/ and plugins/ to build/ directory
    ant jar				-> create distribution xmax.jar (default target)
```

###Usage

######Basic Execution Display

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

######Basic Execution Serial Dump

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

######Basic Execution in Eclipse

    Eclipse reads seed files from config DataMask at runtime (config.xml):
```xml
    <!-- Default DataMask path -->
    <DataMask>resources/Data/</DataMask>
   
    <!-- User DataMask examples --> 
    <DataMask>mseed/II_AAK/2013_026/</DataMask>
    <DataMask>seed/IU_PTGA/2014_193/</DataMask>
```
