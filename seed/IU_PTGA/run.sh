# Make list of directories for days
#for i in {60..99}
#do
#	mkdir "2014_0${i}"
#done

# Copy files from xs0
for i in {60..99}
do
	cp "/xs0/seed/IU_PTGA/2014/2014_0${i}_IU_PTGA"/10_LH*  "2014_0${i}"
done
