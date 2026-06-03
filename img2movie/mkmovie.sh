#!/bin/sh

# Copyright (c) 2013 Takashi OKADA
# All rights reserved.

# Simple scripts to make a movie, convert a movie format or speed.

# print usage
usage() {
    echo "usage: sh mkmovie.sh [options]"
    echo "examples:"
    echo "  sh mkmovie.sh img2movie"
    echo "  sh mkmovie.sh avi2yuv out.avi"
    echo "  sh mkmovie.sh yuv2wmv out.avi 12"
    echo "  sh mkmovie.sh yuv2wmvseconds out.avi 12 180"
    echo "  sh mkmovie.sh svi2h264s out.avi"
}

# check whether the command exists or not?
chk_command() {
    if which $1 > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

remove_file_extension() {
    filename=`echo $1 | sed -e "s/\.[^.]*$//g"`
}

# check ffmpeg and mencoder path
# if you don't use mencoder, please remove 'mencoder' from following commands 
# array.
#commands=('ffmpeg' 'mencoder')
commands=('ffmpeg')
for command in ${commands[*]} ; do
    if ! chk_command $command ; then
        echo "${command} does not exist!"
        exit
    fi
done

command=''
dict='img'
iformat='jpg'
input='out.avi'
output='out.avi'
prefix='capture'
ratio=1
speed=1
index=0
help="FALSE"
while getopts c:d:f:i:o:p:r:s:x:vh opt; do
    case "$opt" in
    "c") command=$OPTARG
        ;;
    "d") dict=$OPTARG
        ;;
    "f") iformat=$OPTARG
        ;;
    "i") input=$OPTARG
        ;;
    "o") output=$OPTARG
        ;;
    "p") prefix=$OPTARG
        ;;
    "r") ratio=$OPTARG
        ;;
    "s") speed=$OPTARG
        ;;
    "x") index=$OPTARG
        ;;
    "v") verbose="TRUE"
        ;;
    "h") help="TRUE"
        ;;
    "?") echo "invalid option"
         exit 1
        ;;
    esac
done

shift `expr $OPTIND - 1`


if [ "${command}" = "" ]; then
    command="$1"
fi
echo $command $dict $output $ratio $speed
#echo $1

if [ "${command}" = "usage" -o "${help}" = "TRUE" ]; then
    usage
###############################################################################
# make avi from images
#   make a raw avi file from images. $dict sets the directory that images are 
#   put. $prefix is the name prefix fo images. The file name is assumed as :
#   $dict/$prefix1.$iformat $dict/$prefix2.$iformat ...
###############################################################################
elif [ "${command}" = "img2movie" ]; then
    if ["${index}" = "0" ]; then
	idx="%d"
    else
	idx="%${index}d"
    fi
    echo ffmpeg -r 1 -y -i $dict/$prefix$idx.$iformat -vcodec mjpeg -qscale 0 $output
    ffmpeg -r 1 -y -i $dict/$prefix$idx.$iformat -vcodec mjpeg -qscale 0 $output
###############################################################################
# make yuv file
# Caution: The file size of yuv is so huge. Be careful!
###############################################################################
elif [ "${command}" = "yuv" ]; then
    remove_file_extension $output
    echo ffmpeg -y -i $input $filename.yuv
    ffmpeg -y -i $input $filename.yuv
###############################################################################
# make flv movie 
###############################################################################
elif [ "${command}" = "avi2flv" ]; then
    echo "avi2flv"
    echo ffmpeg -y -i $2  -an -vcodec libx264 -level 41 -crf 25 -bufsize 20000k -maxrate 25000k -g 250 -r 29.97 -s 640x480 -coder 1 -flags +loop -cmp +chroma -partitions +parti4x4+partp8x8+partb8x8 -me_method umh -subq 7 -me_range 16 -keyint_min 25 -sc_threshold 40 -i_qfactor 0.71 -rc_eq 'blurCplx^(1-qComp)' -bf 16 -b_strategy 1 -bidir_refine 1 -refs 6 -deblockalpha 0 -deblockbeta 0 './out.flv'
    ffmpeg -y -i $2  -an -vcodec libx264 -level 41 -crf 25 -bufsize 20000k -maxrate 25000k -g 250 -r 29.97 -s 640x480 -coder 1 -flags +loop -cmp +chroma -partitions +parti4x4+partp8x8+partb8x8 -me_method umh -subq 7 -me_range 16 -keyint_min 25 -sc_threshold 40 -i_qfactor 0.71 -rc_eq 'blurCplx^(1-qComp)' -bf 16 -b_strategy 1 -bidir_refine 1 -refs 6 -deblockalpha 0 -deblockbeta 0 -ss 540 './out.flv'
###############################################################################
# make wmv movie
#   $2: input yuv file
#   $3: movie speed ratio, 4 means 4 times faster thant real time speed.
###############################################################################
elif [ "${command}" = "wmv" ]; then
    remove_file_extension $output
    echo ffmpeg -y -i $input -vcodec wmv2 -an $filename.wmv
    ffmpeg -y -i $input -vcodec wmv2 -an $filename.wmv
    #ffmpeg -pix_fmt yuvj420p -s 800x600 -r $3 -i $2 -vcodec wmv2 -an ./out.wmv
###############################################################################
# make wmv movie with time
#   $2: input yuv file
#   $3: movie speed ratio, 4 means 4 times faster thant real time speed.
#   $4: movie time (seconds)
###############################################################################
elif [ "${command}" = "h264" ]; then
    remove_file_extension $output
    echo ffmpeg -y -i $input -vcodec libx264 -an $filename.mp4
    ffmpeg -y -i $input -vcodec libx264 -an $filename.mp4
    #ffmpeg -pix_fmt yuvj420p -s 1556x1079 -r $3 -i $2 -vcodec wmv2 -an -t $4 -ss 2 ./out.wmv
elif [ "${command}" = "speed" ]; then
    #echo mencoder -speed $speed $input -vfm ffmpeg -ovc x264 -vf scale=1920:1080 -lavcopts vcodec=libx264:vbitrate=1200 -o $output
    #mencoder -speed $speed $input -vfm ffmpeg -ovc x264 -vf scale=1920:1080 -lavcopts vcodec=libx264:vbitrate=1200 -o $output
    mencoder -speed $speed $input -vfm ffmpeg -ovc copy -o $output
else
    usage
fi

