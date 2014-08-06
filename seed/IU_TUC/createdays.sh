#!/bin/bash

# Create sequential seed day directories
CWD=$(pwd)
STAT="IU_TUC"
YR="2014"
RES01="agonzales@aslres01.cr.usgs.gov:/tr1/telemetry_days/$STAT/$YR"

echo
echo "Make day directories:"
for x in {210..218}; do
    STR=$YR'_'$x
    echo $STR 
    mkdir $STR
done

# Secure copy files from aslres01:/tr1/telemetry_days/IU_TUC/2014/
echo "Copying files from: $RES01"
for x in {210..218}; do
    STR=$YR'_'$x
    echo "Copying day: $STR"	
    $(scp $RES01'/'$STR'/'00_LH* $STR)
done
echo "Done copying days 210-218"

# Copy day directories from /xs0/seed to day directories in CWD 
#SEEDDIR="/home/agonzales/Documents/seed/$STAT"
#echo $SEEDDIR
#for x in {100..170}; do
#	STR=$YR'_'$x	
#	SEED=$YR'_'$x'_'$STAT
#	echo $SEED:	
	#files=$(ls $SEEDDIR/$YR/$SEED | grep 20_LN)
#	for f in $files; do
#		echo "Copying: $SEEDDIR/$YR/$SEED/$f to $STR"	
#		cp $SEEDDIR/$YR/$SEED/$f $STR	
#	done	
#	echo
#done

# Check if seed copies are correct
#days=$(ls $CWD | grep 2013)
#for dir in $days; do 
#	echo $dir:
#	ls -lt $dir
#	echo
#done
