#!/bin/bash
# If already there, we don't need to download again
if [ ! -f "lib/JEvalResp.jar" ]; then
  # The original file is not actually compressed despite name being tar.gz
  wget -O jevalresp.tar http://ds.iris.edu/pub/programs/JEvalResp/JEvalResp_v1.80/JEvalResp_v1.80_dist.tar.gz

  # But added a z anyway since it doesn't hurt anything.
  tar -xvzf jevalresp.tar JEvalResp_v1.80_dist/JEvalResp.jar
  cd JEvalResp_v1.80_dist

  # Remove outdated copies of SeisFile and SeedCodec, then rebuild jar
  unzip JEvalResp.jar -d JEvalResp
  rm JEvalResp.jar

  cd JEvalResp
  rm -rf edu/sc
  jar -cf JEvalResp.jar .

  # Backout and copy rebuilt jar to lib/
  cd ../..
  mv JEvalResp_v1.80_dist/JEvalResp/JEvalResp.jar lib/

  # Cleanup
  rm -rf JEvalResp_v1.80_dist
  rm jevalresp.tar
fi

