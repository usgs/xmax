# Simple run (currently will only look for log4j.properties in the current dir e.g., ./log4j.properties )
#java  -Xms512M -Xmx512M -jar xmax.jar

# Same but give a path to seed files:
java  -Xms512M -Xmx512M -jar xmax.jar -d '/Users/mth/mth/ASLData/xs0/seed/*/2012/2012_160_*/00_LH*seed'

# Here's a way to explicitly point to a log4j.properties file:
#java -Dlog4j.configuration=file:./src/log4j.properties -Xms512M -Xmx512M -jar xmax.jar -d '/Users/mth/mth/ASLData/xs0/seed/*/2012/2012_160_*/00_LH*seed'
