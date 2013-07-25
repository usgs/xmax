# Simple run (currently will only look for log4j.properties in the current dir e.g., ./log4j.properties )
#java  -Xms512M -Xmx512M -jar xmax.jar

# Same but give a path to seed files:
#java  -Xms512M -Xmx512M -jar xmax.jar -d '/Users/mth/mth/ASLData/xs0/seed/*/2012/2012_160_*/00_LH*seed'
#java  -Xms512M -Xmx512M -jar xmax.jar -d '/Users/mth/mth/ASLData/xs0/seed/*/2012/*/00_LH*seed'
#java  -Xms1024M -Xmx1024M -jar xmax.jar -d '/Users/mth/mth/ASLData/xs0/seed/*/2012/*/00_BH*seed'
java  -Xms1024M -Xmx1024M -jar xmax.jar -d '/Users/mth/mth/ASLData/xs0/seed/*/201?/*/00_BH*seed' -b 2012,158,00:00:00 -e 2012,161,00:00:00

#java  -Xms1024M -Xmx1024M -jar xmax.jar -d '/Users/mth/mth/ASLData/xs0/seed/*/201?/*/00_BH*seed' -T
#java  -Xms1024M -Xmx1024M -jar xmax.jar -t
#java  -Xms1024M -Xmx1024M -jar xmax.jar -t

# Here's a way to explicitly point to a log4j.properties file:
#java -Dlog4j.configuration=file:./src/log4j.properties -Xms512M -Xmx512M -jar xmax.jar -d '/Users/mth/mth/ASLData/xs0/seed/*/2012/2012_160_*/00_LH*seed'
