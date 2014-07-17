#!/bin/bash

# Create sequential seed day directories
CWD=$(pwd)
STAT="IU_TUC"
YR=2013
echo
<<COMMENT1
echo "Make day directories:"
for x in {111..170}; do
	STR=$YR'_'$x
	echo $STR 
	mkdir $STR
done
COMMENT1

# Copy day directories from /xs0/seed to day directories in CWD 
SEEDDIR="/home/agonzales/Documents/seed/$STAT"
<<COMMENT2
echo $SEEDDIR
for x in {100..170}; do
	STR=$YR'_'$x	
	SEED=$YR'_'$x'_'$STAT
	echo $SEED:	
	files=$(ls $SEEDDIR/$YR/$SEED | grep 20_LN)
	for f in $files; do
		echo "Copying: $SEEDDIR/$YR/$SEED/$f to $STR"	
		cp $SEEDDIR/$YR/$SEED/$f $STR	
	done	
	echo
done
COMMENT2

# Check if seed copies are correct
days=$(ls $CWD | grep 2013)
for dir in $days; do 
	echo $dir:
	ls -lt $dir
	echo
done
