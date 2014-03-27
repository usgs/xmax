# run.sh - Uncomment to run xmax:

### DISPLAY
# No arguments - will output [Quick Examples] and try to read data from resources/DATA (specified in config.xml):
#java  -Xms512M -Xmx512M -jar xmax.jar

# -d: Read data files found on path:
#java  -Xms512M -Xmx512M -jar xmax.jar -d '/Users/mth/mth/ASLData/xs0/seed/IU_ANMO/2012/2012_1{59,60}_*/00_LHZ*seed'

# -t: Read serialized data from resources/DATA_TEMP (specified in config.xml)
#java  -Xms512M -Xmx512M -jar xmax.jar -t

# -t -d: Read serialized data from resources/DATA_TEMP (specified in config.xml) AND from dataPath:
#java  -Xms512M -Xmx512M -jar xmax.jar -t -d '/Users/mth/mth/ASLData/xs0/seed/IU_ANMO/2012/2012_1{59,60}_*/00_LHZ*seed'


### SERIAL DUMP
# -T: Read any data files found in resources/DATA and dump serialized data into  resources/DATA_TEMP:
#     Note that this will wipe out any existing serialized data in resources/DATA_TEMP
#java  -Xms512M -Xmx512M -jar xmax.jar -T 

# -T -d: Read data files found on path and dump serialized data into  resources/DATA_TEMP:
#        Note that this will wipe out any existing serialized data in resources/DATA_TEMP
#java  -Xms512M -Xmx512M -jar xmax.jar -T -d '/Users/mth/mth/ASLData/xs0/seed/IU_ANMO/2012/2012_1{59,60}_*/00_LHZ*seed'

# -T -d -t: Same as above, but ALSO read existing serialized data in resources/DATA_TEMP
#           Note that this will APPEND new serialized data to that already in resources/DATA_TEMP
#java  -Xms512M -Xmx512M -jar xmax.jar -T -d '/Users/mth/mth/ASLData/xs0/seed/IU_ANMO/2012/2012_1{59,60}_*/00_LHZ*seed'

# By default, xmax looks for ./log4j.properties to specify the log level
# Here's a way to explicitly point to a log4j.properties file:
#java -Dlog4j.configuration=file:./src/log4j.properties -Xms512M -Xmx512M -jar xmax.jar -d '/Users/mth/mth/ASLData/xs0/seed/*/2012/2012_160_*/00_LH*seed'
